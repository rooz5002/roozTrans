package com.rooz.translator

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.SpannableString
import android.text.Spanned
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File

/**
 * Minimal, chat-style home screen: app name + gear on top, empty page in the middle that fills with
 * translations, and at the bottom a direction pill, camera + gallery, and the text box with a send button.
 */
class MainActivity : AppCompatActivity() {
    private lateinit var prefs: Prefs
    private lateinit var engine: Engine
    private lateinit var pal: Palette
    private lateinit var themeId: String
    private lateinit var input: EditText
    private lateinit var send: ImageView
    private lateinit var dirBtn: TextView
    private lateinit var feed: LinearLayout
    private lateinit var feedScroll: ScrollView
    private lateinit var hint: View
    private lateinit var fixBar: View
    private var job: Job? = null
    private var photoUri: Uri? = null

    private val takePhoto = registerForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) photoUri?.let(::openImage)
    }
    private val pickPhoto = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> uri?.let(::openImage) }

    override fun onCreate(savedInstanceState: Bundle?) {
        themeId = Themes.apply(this)
        super.onCreate(savedInstanceState)
        prefs = Prefs(this); engine = Engine(this); pal = Themes.current(this)

        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(pal.bg) }

        // ---- top bar: name + gear ----
        root.addView(hbox {
            setPadding(dp(20), dp(10), dp(8), dp(6))
            weighted(TextView(context).apply {
                textSize = 20f; setTypeface(typeface, android.graphics.Typeface.BOLD); setTextColor(pal.text)
                text = SpannableString("roozTrans").apply {
                    setSpan(ForegroundColorSpan(pal.accent), 4, 9, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            })
            addView(iconButton(R.drawable.ic_gear, pal.muted, 44, 10) {
                startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
            })
        })

        // ---- warning when the phone has stopped the screen translator (battery savers) ----
        fixBar = hbox {
            setPadding(dp(14), dp(10), dp(6), dp(10)); background = rounded(pal.surface, 14, pal.accent)
            visibility = View.GONE
            weighted(TextView(context).apply {
                text = "⚠ Your phone stopped the screen translator. Tap to fix."; textSize = 14f; setTextColor(pal.text)
            })
            addView(TextView(context).apply {
                text = "✕"; textSize = 16f; setTextColor(pal.muted); setPadding(dp(12), dp(4), dp(10), dp(4))
                setOnClickListener { prefs.a11yWasOn = false; fixBar.visibility = View.GONE }   // "I turned it off myself"
            })
            setOnClickListener { KeepAlive.showRepair(this@MainActivity, KeepAlive.state(this@MainActivity)) }
        }
        root.addView(fixBar, LinearLayout.LayoutParams(-1, -2).apply { setMargins(dp(12), 0, dp(12), dp(4)) })

        // ---- the big empty page (fills with results) ----
        feed = vbox().apply { setPadding(dp(16), dp(8), dp(16), dp(16)) }
        feedScroll = ScrollView(this).apply { isFillViewport = true; addView(feed) }
        hint = vbox {
            gravity = Gravity.CENTER
            addView(TextView(context).apply {
                text = "English ⇄ 中文"; textSize = 22f; setTextColor(pal.text); alpha = .85f; gravity = Gravity.CENTER
            })
            addView(TextView(context).apply {
                text = "Type below, or use the camera or a photo"; textSize = 14f; setTextColor(pal.muted)
                gravity = Gravity.CENTER; setPadding(0, dp(6), 0, 0)
            })
        }
        root.addView(FrameLayout(this).apply {
            addView(feedScroll, FrameLayout.LayoutParams(-1, -1))
            addView(hint, FrameLayout.LayoutParams(-1, -1))
        }, LinearLayout.LayoutParams(-1, 0, 1f))

        // ---- direction pill (half) + camera + gallery (other half) ----
        root.addView(hbox {
            setPadding(dp(12), dp(4), dp(12), dp(8))
            dirBtn = TextView(context).apply {
                textSize = 15f; setTextColor(pal.text); gravity = Gravity.CENTER
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                background = rounded(pal.surface, 22); setOnClickListener { cycleDir() }
                compoundDrawablePadding = dp(6)
            }
            addView(dirBtn, LinearLayout.LayoutParams(0, dp(44), 2f).apply { marginEnd = dp(8) })
            addView(pillIcon(R.drawable.ic_camera) { openCamera() }, LinearLayout.LayoutParams(0, dp(44), 1f).apply { marginEnd = dp(8) })
            addView(pillIcon(R.drawable.ic_gallery) {
                pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }, LinearLayout.LayoutParams(0, dp(44), 1f))
        })
        showDir()

        // ---- text box + little send button ----
        root.addView(hbox {
            setPadding(dp(14), dp(4), dp(6), dp(4))
            gravity = Gravity.BOTTOM
            background = rounded(pal.surface, 26, pal.line)
            input = EditText(context).apply {
                hint = "Type to translate…"; textSize = 16f; maxLines = 6; minHeight = dp(44)
                setTextColor(pal.text); setHintTextColor(pal.muted); background = null
                setPadding(dp(4), dp(10), dp(4), dp(10))
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                    override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                    override fun afterTextChanged(s: Editable?) { updateSend() }
                })
            }
            weighted(input)
            send = ImageView(context).apply {
                setImageResource(R.drawable.ic_send); imageTintList = ColorStateList.valueOf(pal.onAccent)
                background = rounded(pal.accent, 18); setPadding(dp(8), dp(8), dp(8), dp(8))
                setOnClickListener { translateText() }; contentDescription = "Translate"
            }
            addView(send, LinearLayout.LayoutParams(dp(36), dp(36)).apply { setMargins(dp(4), 0, 0, dp(4)) })
        }, LinearLayout.LayoutParams(-1, -2).apply { setMargins(dp(12), 0, dp(12), dp(12)) })
        updateSend()

        ViewCompat.setOnApplyWindowInsetsListener(root) { v, ins ->
            val b = ins.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            v.setPadding(b.left, b.top, b.right, b.bottom); ins
        }
        setContentView(root)
        if (savedInstanceState == null && !prefs.onboarded) startActivity(Intent(this, WelcomeActivity::class.java))
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); handleIntent(intent) }

    override fun onResume() {
        super.onResume()
        if (Prefs(this).theme != themeId) { recreate(); return }   // colour changed in Settings
        TranslatorService.instance?.refreshBubble()
        // give Android a moment to (re)bind the service after the app process starts, then check it really runs
        fixBar.removeCallbacks(checkService); fixBar.postDelayed(checkService, 1500)
        lifecycleScope.launch { runCatching { engine.online.route(force = false) } }   // warm the IP check
        lifecycleScope.launch { Updater.autoCheck(this@MainActivity) }
    }

    private val checkService = Runnable {
        val broken = prefs.a11yConsent && prefs.a11yWasOn && KeepAlive.state(this) != KeepAlive.State.RUNNING
        fixBar.visibility = if (broken) View.VISIBLE else View.GONE
    }

    private fun handleIntent(i: Intent?) {
        i ?: return
        val text = when (i.action) {
            Intent.ACTION_PROCESS_TEXT -> i.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()
            Intent.ACTION_SEND -> if (i.type?.startsWith("image/") == true) {
                @Suppress("DEPRECATION") (i.getParcelableExtra<Uri>(Intent.EXTRA_STREAM))?.let(::openImage); null
            } else i.getStringExtra(Intent.EXTRA_TEXT)
            else -> null
        }
        if (!text.isNullOrBlank()) { input.setText(text); translateText() }
    }

    // ---- direction: Auto → EN→中 → 中→EN ----
    private fun cycleDir() { prefs.dirMode = (prefs.dirMode + 1) % 3; showDir() }
    private fun showDir() {
        dirBtn.text = when (prefs.dirMode) { 1 -> "EN → 中"; 2 -> "中 → EN"; else -> "Auto · EN ⇄ 中" }
    }

    private fun updateSend() {
        val has = input.text.isNotBlank()
        send.alpha = if (has) 1f else .35f; send.isEnabled = has
    }

    private fun translateText() {
        val text = input.text.toString().trim()
        if (text.isEmpty()) return
        val dir = when (prefs.dirMode) { 1 -> Dir.EN_ZH; 2 -> Dir.ZH_EN; else -> Lang.detect(text) }
        input.setText("")
        hint.visibility = View.GONE

        // your text: a soft bubble on the right
        feed.addView(TextView(this).apply {
            this.text = text; textSize = 15f; setTextColor(pal.text); setTextIsSelectable(true)
            background = rounded(pal.surface, 18); setPadding(dp(14), dp(10), dp(14), dp(10))
        }, LinearLayout.LayoutParams(-2, -2).apply { gravity = Gravity.END; setMargins(dp(48), dp(14), 0, if (dir == Dir.ZH_EN) dp(2) else dp(8)) })
        // Chinese in → its pinyin under your text
        if (dir == Dir.ZH_EN && prefs.showPinyin) Pinyin.of(text)?.let { py ->
            feed.addView(pinyinView(py).apply { gravity = Gravity.END },
                LinearLayout.LayoutParams(-2, -2).apply { gravity = Gravity.END; setMargins(dp(48), 0, dp(4), dp(8)) })
        }
        // translation: plain text on the page
        val out = TextView(this).apply {
            this.text = "…"; textSize = 19f; setTextColor(pal.text); setTextIsSelectable(true)
            setLineSpacing(0f, 1.15f); setPadding(dp(2), 0, dp(2), 0)
        }
        feed.addView(out, LinearLayout.LayoutParams(-1, -2))
        // Chinese out → its pinyin under the translation
        val outPy = pinyinView("").apply { visibility = View.GONE }
        feed.addView(outPy, LinearLayout.LayoutParams(-1, -2))
        val meta = TextView(this).apply { textSize = 12f; setTextColor(pal.muted); this.text = dir.label }
        val copy = TextView(this).apply {
            this.text = "Copy"; textSize = 12f; setTextColor(pal.accent); setPadding(dp(12), dp(6), dp(2), dp(6))
            setTypeface(typeface, android.graphics.Typeface.BOLD); setOnClickListener { copy(out.text.toString()) }
        }
        feed.addView(hbox { setPadding(dp(2), dp(2), 0, 0); weighted(meta); addView(copy) })
        scrollDown()

        job?.cancel()
        job = lifecycleScope.launch {
            engine.translate(listOf(text), dir, offlineOnly = false,
                onResult = { t, who ->
                    out.text = t.first(); meta.text = "${dir.label} · $who"
                    val py = if (dir == Dir.EN_ZH && prefs.showPinyin) Pinyin.of(t.first()) else null
                    outPy.text = py ?: ""; outPy.visibility = if (py != null) View.VISIBLE else View.GONE
                    scrollDown()
                },
                onError = { e -> if (out.text == "…") out.text = ""; meta.text = "⚠ $e" })
        }
    }

    private fun pinyinView(s: String) = TextView(this).apply {
        text = s; textSize = 14f; setTextColor(pal.muted); setTextIsSelectable(true); setPadding(dp(2), dp(2), dp(2), dp(2))
    }

    private fun scrollDown() = feedScroll.post { feedScroll.fullScroll(View.FOCUS_DOWN) }

    private fun copy(s: String) {
        if (s.isBlank() || s == "…") return
        (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("roozTrans", s))
        Toast.makeText(this, "Copied", Toast.LENGTH_SHORT).show()
    }

    private fun iconButton(res: Int, tint: Int, sizeDp: Int, padDp: Int, onClick: () -> Unit) = ImageView(this).apply {
        setImageResource(res); imageTintList = ColorStateList.valueOf(tint)
        setPadding(dp(padDp), dp(padDp), dp(padDp), dp(padDp))
        layoutParams = ViewGroup.LayoutParams(dp(sizeDp), dp(sizeDp))
        val ta = obtainStyledAttributes(intArrayOf(android.R.attr.selectableItemBackgroundBorderless))
        background = ta.getDrawable(0); ta.recycle()
        setOnClickListener { onClick() }
    }

    private fun pillIcon(res: Int, onClick: () -> Unit) = ImageView(this).apply {
        setImageResource(res); imageTintList = ColorStateList.valueOf(pal.accent)
        scaleType = ImageView.ScaleType.CENTER_INSIDE; setPadding(dp(10), dp(10), dp(10), dp(10))
        background = rounded(pal.surface, 22); setOnClickListener { onClick() }
    }

    private fun openCamera() {
        val dir = File(cacheDir, "photos").apply { mkdirs() }
        val f = File(dir, "photo_${System.currentTimeMillis()}.jpg")
        photoUri = FileProvider.getUriForFile(this, "$packageName.files", f)
        takePhoto.launch(photoUri!!)
    }

    private fun openImage(uri: Uri) = try {
        startActivity(Intent(this, ImageActivity::class.java).setData(uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
    } catch (e: Exception) { Toast.makeText(this, "Can't open that image: ${e.message}", Toast.LENGTH_LONG).show() }
}
