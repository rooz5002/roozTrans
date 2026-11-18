package com.rooz.translator

import android.graphics.Bitmap
import android.graphics.Rect
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import kotlinx.coroutines.tasks.await

/** One piece of text found on a screen/photo, plus its translation once we have it. */
data class TextItem(val rect: Rect, val original: String, val lineHeight: Int, val lineCount: Int = 1, var translated: String? = null, var pinyin: String? = null)

/** Everything that runs on the phone with no internet. */
object Offline {
    // Chinese recognizer also reads Latin script. Model is bundled in the APK (no download).
    private var rec: com.google.mlkit.vision.text.TextRecognizer? = null
    private val recognizer get() = synchronized(this) { rec ?: TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build()).also { rec = it } }

    /**
     * Free the OCR and translation models (tens of MB) while the screen is off. A smaller process is far less
     * likely to be killed by battery savers, and a killed process is what turns the screen translator off.
     * They load again on the next use (about a second).
     */
    fun release() {
        synchronized(this) { rec?.let { runCatching { it.close() } }; rec = null }
        synchronized(translators) { translators.values.forEach { runCatching { it.close() } }; translators.clear() }
    }

    suspend fun ocr(bmp: Bitmap): List<TextItem> {
        val result = recognizer.process(InputImage.fromBitmap(bmp, 0)).await()
        return result.textBlocks.mapNotNull { b ->
            val box = b.boundingBox ?: return@mapNotNull null
            val lines = b.lines.map { it.text.trim() }.filter { it.isNotEmpty() }
            if (lines.isEmpty()) return@mapNotNull null
            // Chinese lines join without spaces, English lines with a space
            val joined = if (Lang.isChinese(lines.joinToString(""))) lines.joinToString("") else lines.joinToString(" ")
            val lh = b.lines.mapNotNull { it.boundingBox?.height() }.average().takeIf { !it.isNaN() }?.toInt() ?: box.height()
            TextItem(box, joined, lh, lines.size)
        }
    }

    private val translators = HashMap<Dir, Translator>()
    private fun translator(dir: Dir): Translator = synchronized(translators) {
        translators.getOrPut(dir) {
            val (s, t) = if (dir == Dir.EN_ZH) TranslateLanguage.ENGLISH to TranslateLanguage.CHINESE
                         else TranslateLanguage.CHINESE to TranslateLanguage.ENGLISH
            Translation.getClient(TranslatorOptions.Builder().setSourceLanguage(s).setTargetLanguage(t).build())
        }
    }

    suspend fun modelsReady(): Boolean {
        val m = RemoteModelManager.getInstance()
        val zh = m.isModelDownloaded(TranslateRemoteModel.Builder(TranslateLanguage.CHINESE).build()).await()
        val en = m.isModelDownloaded(TranslateRemoteModel.Builder(TranslateLanguage.ENGLISH).build()).await()
        return zh && en
    }

    /** Needs internet to Google (do it before entering China). ~30 MB per language. */
    suspend fun downloadModels() {
        val cond = DownloadConditions.Builder().build()
        translator(Dir.EN_ZH).downloadModelIfNeeded(cond).await()
        translator(Dir.ZH_EN).downloadModelIfNeeded(cond).await()
    }

    suspend fun translate(texts: List<String>, dir: Dir): List<String> {
        val tr = translator(dir)
        return texts.map { tr.translate(it).await() }
    }
}
