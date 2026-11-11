package com.rooz.translator

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max

/** Shows a photo with translations painted over it, plus a readable list underneath. */
class ImageActivity : AppCompatActivity() {
    private lateinit var img: ImageView
    private lateinit var status: TextView
    private lateinit var list: TextView
    private var source: Bitmap? = null
    private var items: List<TextItem> = emptyList()
    private var showOriginal = false

    override fun onCreate(savedInstanceState: Bundle?) {
        Themes.apply(this)
        super.onCreate(savedInstanceState)
        img = ImageView(this).apply { adjustViewBounds = true }
        status = label("Reading text…", 13f).apply { alpha = .7f }
        list = TextView(this).apply { textSize = 16f; setTextIsSelectable(true); setPadding(0, dp(8), 0, dp(24)) }
        val root = vbox(12) {
            addView(hbox {
                weighted(status)
                addView(button("原 / EN", outlined = true) { showOriginal = !showOriginal; render() })
            })
            addView(img)
            addView(list)
        }
        val scroll = ScrollView(this).apply { addView(root) }
        ViewCompat.setOnApplyWindowInsetsListener(scroll) { v, ins ->
            val b = ins.getInsets(WindowInsetsCompat.Type.systemBars()); v.setPadding(b.left, b.top, b.right, b.bottom); ins
        }
        setContentView(scroll)
        intent.data?.let(::load) ?: finish()
    }

    private fun load(uri: Uri) = lifecycleScope.launch {
        val bmp = runCatching { withContext(Dispatchers.IO) { decode(uri) } }.getOrElse { status.text = "Can't open image: ${it.message}"; return@launch }
        source = bmp; img.setImageBitmap(bmp)
        val found = runCatching { Offline.ocr(bmp) }.getOrElse { status.text = "Text reading failed: ${it.message}"; return@launch }
        val dir = Lang.dominant(found.map { it.original })
        items = found.filter { Lang.needs(it.original, dir) }
        if (items.isEmpty()) { status.text = "No ${dir.srcName} text found"; return@launch }
        status.text = "${dir.label} · translating ${items.size} pieces…"
        Engine(this@ImageActivity).translate(items.map { it.original }, dir,
            onResult = { t, who -> items.forEachIndexed { i, it -> it.translated = t[i] }; status.text = "${dir.label} · $who"; render() },
            onError = { status.text = "⚠ $it" })
    }

    private fun decode(uri: Uri): Bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(contentResolver, uri)) { d, info, _ ->
        d.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        d.isMutableRequired = false
        val longest = max(info.size.width, info.size.height)
        if (longest > 2400) { val s = 2400f / longest; d.setTargetSize((info.size.width * s).toInt(), (info.size.height * s).toInt()) }
    }

    private fun render() {
        val src = source ?: return
        val out = src.copy(Bitmap.Config.ARGB_8888, true)
        // The photo is shown at screen width, so 1 dp on screen = (photo px / screen px) * density photo pixels
        val shownWidth = resources.displayMetrics.widthPixels - dp(24)
        val density = resources.displayMetrics.density * src.width / shownWidth.toFloat()
        OverlayRenderer(density).draw(Canvas(out), items, showOriginal = showOriginal)
        img.setImageBitmap(out)
        list.text = items.joinToString("\n\n") { "${it.translated ?: "…"}\n   ${it.original}" }
    }
}
