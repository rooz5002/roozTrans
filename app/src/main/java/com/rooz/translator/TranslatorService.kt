package com.rooz.translator

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.drawable.GradientDrawable
import android.media.AudioManager
import android.os.SystemClock
import android.view.Display
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * The screen translator. As an accessibility service it can (without extra permission prompts):
 *  - draw the floating bubble and the result overlay,
 *  - take screenshots (Android 11+),
 *  - see Volume Down presses (double-press = translate).
 */
class TranslatorService : AccessibilityService() {

    companion object { @Volatile var instance: TranslatorService? = null }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var wm: WindowManager
    private lateinit var prefs: Prefs
    private lateinit var engine: Engine
    private var bubble: View? = null
    private var bubbleLp: WindowManager.LayoutParams? = null
    private var overlay: View? = null
    private var busy = false
    private val dp get() = resources.displayMetrics.density

    override fun onServiceConnected() {
        instance = this
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        prefs = Prefs(this)
        engine = Engine(this)
        refreshBubble()
        // Samsung can drop overlay windows across screen off/on: re-check when the screen comes back
        val f = android.content.IntentFilter().apply { addAction(Intent.ACTION_SCREEN_ON); addAction(Intent.ACTION_USER_PRESENT) }
        if (android.os.Build.VERSION.SDK_INT >= 33) registerReceiver(screenRx, f, RECEIVER_NOT_EXPORTED) else registerReceiver(screenRx, f)
    }

    private val screenRx = object : android.content.BroadcastReceiver() {
        override fun onReceive(c: android.content.Context, i: Intent) { ensureBubble(); bubble?.postDelayed({ ensureBubble() }, 1500) }
    }

    /** Re-add the bubble if the system removed/detached its window. Cheap; safe to call often. */
    private fun ensureBubble() {
        if (!::prefs.isInitialized || !prefs.showBubble) return
        val b = bubble
        if (b != null && b.isAttachedToWindow) { if (b.visibility != View.VISIBLE && !busy) b.visibility = View.VISIBLE; return }
        b?.let { runCatching { wm.removeView(it) } }
        bubble = null
        runCatching { addBubble() }
    }

    override fun onDestroy() { runCatching { unregisterReceiver(screenRx) }; instance = null; removeOverlay(); bubble?.let { runCatching { wm.removeView(it) } }; scope.cancel(); super.onDestroy() }
    // fires on app/window switches: a cheap moment to self-heal a vanished bubble
    override fun onAccessibilityEvent(event: AccessibilityEvent?) { ensureBubble() }
    override fun onInterrupt() {}

    // ---------------- bubble ----------------

    fun refreshBubble() {
        if (prefs.showBubble && bubble == null) addBubble()
        if (!prefs.showBubble) bubble?.let { runCatching { wm.removeView(it) }; bubble = null }
        bubble?.background = bubbleBg()
    }

    /** Round bubble in the chosen colour theme's accent, with a white ring. */
    private fun bubbleBg() = android.graphics.drawable.GradientDrawable().apply {
        shape = android.graphics.drawable.GradientDrawable.OVAL
        val a = Themes.current(this@TranslatorService).accent
        setColor((a and 0x00FFFFFF) or (0xDD shl 24)); setStroke((2 * resources.displayMetrics.density).toInt(), android.graphics.Color.WHITE)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun addBubble() {
        val size = (52 * dp).toInt()
        val v = ImageView(this).apply {
            setImageResource(R.drawable.ic_bubble)
            background = bubbleBg()
            val p = (12 * dp).toInt(); setPadding(p, p, p, p)
            alpha = 0.85f
        }
        val lp = WindowManager.LayoutParams(size, size, WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT).apply { gravity = Gravity.TOP or Gravity.START; x = prefs.bubbleX; y = prefs.bubbleY }

        var downX = 0f; var downY = 0f; var startX = 0; var startY = 0; var moved = false; var downAt = 0L
        v.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> { downX = e.rawX; downY = e.rawY; startX = lp.x; startY = lp.y; moved = false; downAt = SystemClock.uptimeMillis() }
                MotionEvent.ACTION_MOVE -> {
                    val mx = e.rawX - downX; val my = e.rawY - downY
                    if (abs(mx) > 12 * dp || abs(my) > 12 * dp) moved = true
                    if (moved) { lp.x = startX + mx.toInt(); lp.y = startY + my.toInt(); wm.updateViewLayout(v, lp) }
                }
                MotionEvent.ACTION_UP -> {
                    if (moved) { prefs.bubbleX = lp.x; prefs.bubbleY = lp.y }
                    else if (SystemClock.uptimeMillis() - downAt > 600) openApp()   // long-press: open text box
                    else capture()
                }
            }
            true
        }
        wm.addView(v, lp); bubble = v; bubbleLp = lp
    }

    private fun openApp() = startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))

    // ---------------- volume key ----------------

    private var lastVolDown = 0L
    private var swallowUp = false

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (!prefs.volumeTrigger || event.keyCode != KeyEvent.KEYCODE_VOLUME_DOWN) return false
        if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
            val now = SystemClock.uptimeMillis()
            if (now - lastVolDown < 450) {
                lastVolDown = 0; swallowUp = true
                // first press already lowered the volume: put it back
                (getSystemService(AUDIO_SERVICE) as AudioManager)
                    .adjustSuggestedStreamVolume(AudioManager.ADJUST_RAISE, AudioManager.USE_DEFAULT_STREAM_TYPE, 0)
                if (overlay != null) removeOverlay() else capture()
                return true
            }
            lastVolDown = now
            return false
        }
        if (event.action == KeyEvent.ACTION_UP && swallowUp) { swallowUp = false; return true }
        return false
    }

    // ---------------- capture → OCR → translate ----------------

    fun captureFromTile() {
        performGlobalAction(GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE)
        scope.launch { delay(700); capture() }
    }

    fun capture() {
        if (busy) return
        if (overlay != null) { removeOverlay(); return }
        busy = true
        scope.launch { Updater.autoCheck(this@TranslatorService) }
        bubble?.visibility = View.INVISIBLE
        scope.launch {
            delay(120) // let the bubble disappear from the frame
            takeScreenshot(Display.DEFAULT_DISPLAY, mainExecutor, object : TakeScreenshotCallback {
                override fun onSuccess(s: ScreenshotResult) {
                    val hw = Bitmap.wrapHardwareBuffer(s.hardwareBuffer, s.colorSpace)
                    val bmp = hw?.copy(Bitmap.Config.ARGB_8888, false)
                    s.hardwareBuffer.close()
                    bubble?.visibility = View.VISIBLE
                    if (bmp == null) { done("Screenshot failed"); return }
                    scope.launch { process(bmp) }
                }
                override fun onFailure(code: Int) { bubble?.visibility = View.VISIBLE; done("Screenshot failed ($code)") }
            })
        }
    }

    private fun done(msg: String? = null) { busy = false; msg?.let { Toast.makeText(this, it, Toast.LENGTH_SHORT).show() } }

    private suspend fun process(bmp: Bitmap) {
        setBubbleWorking(true)
        val items = runCatching { Offline.ocr(bmp) }.getOrElse { setBubbleWorking(false); done("Text reading failed: ${it.message}"); return }
        val dir = Lang.dominant(items.map { it.original })
        val todo = items.filter { Lang.needs(it.original, dir) }
        if (todo.isEmpty()) { setBubbleWorking(false); done("No ${dir.srcName} text found"); return }

        val view = showOverlay(todo, dir)
        engine.translate(todo.map { it.original }, dir,
            onResult = { t, who -> todo.forEachIndexed { i, it -> it.translated = t[i] }; view.update(who) },
            onError = { view.status("⚠ $it") })
        setBubbleWorking(false)
        busy = false
    }

    private fun setBubbleWorking(on: Boolean) { bubble?.alpha = if (on) 0.35f else 0.85f }

    // ---------------- result overlay ----------------

    private inner class ResultView(val items: List<TextItem>, val dir: Dir) : FrameLayout(this@TranslatorService) {
        val renderer = OverlayRenderer(dp)
        var showOriginal = false
        var hits: List<Pair<RectF, TextItem>> = emptyList()
        val statusTv = TextView(context).apply { setTextColor(Color.WHITE); textSize = 13f }
        private val loc = IntArray(2)

        init {
            setWillNotDraw(false)
            setBackgroundColor(Color.argb(40, 0, 0, 0))
            val bar = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
                background = GradientDrawable().apply { setColor(Color.argb(230, 30, 30, 30)); cornerRadius = 22 * dp }
                val p = (10 * dp).toInt(); setPadding(p * 2, p, p, p)
                addView(statusTv, LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
                addView(btn("原/EN") { showOriginal = !showOriginal; invalidate() })
                addView(btn("Copy") { copy(items.mapNotNull { it.translated }.joinToString("\n")) })
                addView(btn("✕") { removeOverlay() })
            }
            addView(bar, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, Gravity.BOTTOM).apply {
                val m = (12 * dp).toInt(); setMargins(m, m, m, (56 * dp).toInt())
            })
            statusTv.text = "${dir.label} · translating…"
        }

        private fun btn(label: String, onClick: () -> Unit) = TextView(context).apply {
            text = label; setTextColor(Color.WHITE); textSize = 15f
            val p = (10 * dp).toInt(); setPadding(p, p / 2, p, p / 2); setOnClickListener { onClick() }
        }

        fun update(who: String) { statusTv.text = "${dir.label} · $who   (tap box = copy, tap empty = close)"; invalidate() }
        fun status(s: String) { statusTv.text = s }

        override fun onDraw(c: Canvas) {
            super.onDraw(c)
            getLocationOnScreen(loc)
            hits = renderer.draw(c, items, -loc[0].toFloat(), -loc[1].toFloat(), 1f, showOriginal)
        }

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouchEvent(e: MotionEvent): Boolean {
            if (e.action == MotionEvent.ACTION_UP) {
                val hit = hits.lastOrNull { it.first.contains(e.x, e.y) }
                if (hit != null) copy("${hit.second.translated ?: ""}\n(${hit.second.original})") else removeOverlay()
            }
            return true
        }
    }

    private fun copy(s: String) {
        (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("roozTrans", s))
        Toast.makeText(this, "Copied", Toast.LENGTH_SHORT).show()
    }

    private fun showOverlay(items: List<TextItem>, dir: Dir): ResultView {
        removeOverlay()
        val v = ResultView(items, dir)
        val lp = WindowManager.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT).apply {
            gravity = Gravity.TOP or Gravity.START
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            fitInsetsTypes = 0
        }
        wm.addView(v, lp); overlay = v
        // keep bubble above the overlay so it can be tapped to close
        bubble?.let { b -> runCatching { wm.removeView(b); wm.addView(b, bubbleLp) } }
        return v
    }

    private fun removeOverlay() { overlay?.let { runCatching { wm.removeView(it) } }; overlay = null }
}
