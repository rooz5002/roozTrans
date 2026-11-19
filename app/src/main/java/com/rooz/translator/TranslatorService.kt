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
    private var overlay: ResultView? = null
    private var busy = false
    private val dp get() = resources.displayMetrics.density

    override fun onServiceConnected() {
        instance = this
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        prefs = Prefs(this)
        prefs.a11yWasOn = true
        engine = Engine(this)
        KeepAliveService.clearStopped(this)
        KeepAliveService.start(this)   // keeps the process at foreground priority while the phone sleeps
        safe { refreshBubble() }
        // Samsung can drop overlay windows across screen off/on: re-check when the screen comes back.
        // Screen off: drop the big ML models so battery savers have less reason to kill us.
        val f = android.content.IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON); addAction(Intent.ACTION_USER_PRESENT); addAction(Intent.ACTION_SCREEN_OFF)
        }
        safe { if (android.os.Build.VERSION.SDK_INT >= 33) registerReceiver(screenRx, f, RECEIVER_NOT_EXPORTED) else registerReceiver(screenRx, f) }
    }

    /**
     * An exception escaping any callback kills the process, and Android then switches the accessibility
     * service off ("Not working" on Samsung, unticked on Huawei). So nothing is allowed to escape.
     */
    private inline fun safe(block: () -> Unit) { try { block() } catch (t: Throwable) { android.util.Log.w("roozTrans", "ignored", t) } }

    private val screenRx = object : android.content.BroadcastReceiver() {
        override fun onReceive(c: android.content.Context, i: Intent) = safe {
            if (i.action == Intent.ACTION_SCREEN_OFF) { if (overlay == null && !busy) Offline.release(); return@safe }
            ensureBubble(); bubble?.postDelayed({ safe { ensureBubble() } }, 1500)
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= TRIM_MEMORY_BACKGROUND && overlay == null && !busy) safe { Offline.release() }
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

    /** Only called when the service is switched off in Settings (a killed process gets no callback). */
    override fun onUnbind(intent: Intent?): Boolean {
        safe { prefs.a11yWasOn = false; KeepAliveService.stop(this) }
        return super.onUnbind(intent)
    }

    override fun onDestroy() { runCatching { unregisterReceiver(screenRx) }; instance = null; removeOverlay(); bubble?.let { runCatching { wm.removeView(it) } }; scope.cancel(); super.onDestroy() }
    // fires on app/window switches: a cheap moment to self-heal a vanished bubble
    override fun onAccessibilityEvent(event: AccessibilityEvent?) = safe { ensureBubble() }
    override fun onInterrupt() {}

    // ---------------- bubble ----------------

    fun refreshBubble() = safe {
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
                MotionEvent.ACTION_UP -> safe {
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

    override fun onKeyEvent(event: KeyEvent): Boolean = try { onKey(event) } catch (t: Throwable) { false }

    private fun onKey(event: KeyEvent): Boolean {
        if (!::prefs.isInitialized || !prefs.volumeTrigger || event.keyCode != KeyEvent.KEYCODE_VOLUME_DOWN) return false
        if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
            val now = SystemClock.uptimeMillis()
            if (now - lastVolDown < 450) {
                lastVolDown = 0; swallowUp = true
                // first press already lowered the volume: put it back
                (getSystemService(AUDIO_SERVICE) as AudioManager)
                    .adjustSuggestedStreamVolume(AudioManager.ADJUST_RAISE, AudioManager.USE_DEFAULT_STREAM_TYPE, 0)
                capture()   // same as a bubble tap: next step, or translate
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
        scope.launch { delay(700); safe { capture() } }
    }

    fun capture() {
        if (busy) return
        overlay?.let { it.next(); return }   // tap again: pinyin → translation → back to the original screen
        busy = true
        scope.launch { Updater.autoCheck(this@TranslatorService) }
        bubble?.visibility = View.INVISIBLE
        scope.launch {
            delay(120) // let the bubble disappear from the frame
            takeScreenshot(Display.DEFAULT_DISPLAY, mainExecutor, object : TakeScreenshotCallback {
                override fun onSuccess(s: ScreenshotResult) {
                    bubble?.visibility = View.VISIBLE
                    val bmp = try {
                        Bitmap.wrapHardwareBuffer(s.hardwareBuffer, s.colorSpace)?.copy(Bitmap.Config.ARGB_8888, false)
                    } catch (t: Throwable) { null } finally { runCatching { s.hardwareBuffer.close() } }
                    if (bmp == null) { done("Screenshot failed"); return }
                    scope.launch {
                        try { process(bmp) } catch (t: Throwable) { setBubbleWorking(false); done("Translation failed: ${t.message}") }
                    }
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

        val view = showOverlay(todo, dir, bmp)
        if (dir == Dir.ZH_EN) {   // pinyin of the Chinese on screen: instant, no translation needed
            kotlinx.coroutines.withContext(Dispatchers.Default) { todo.forEach { it.pinyin = Pinyin.of(it.original) } }
            view.invalidate()
        }
        engine.translate(todo.map { it.original }, dir,
            onResult = { t, who ->
                todo.forEachIndexed { i, it -> it.translated = t[i]; if (dir == Dir.EN_ZH) it.pinyin = Pinyin.of(t[i]) }
                view.update(who)
            },
            onError = { view.status("⚠ $it") })
        setBubbleWorking(false)
        busy = false
    }

    private fun setBubbleWorking(on: Boolean) { bubble?.alpha = if (on) 0.35f else 0.85f }

    // ---------------- result overlay ----------------

    private enum class Show { ORIGINAL, PINYIN, TRANSLATION }

    /**
     * Full-screen result layer. Tapping the bubble (or Volume Down ×2) steps through [steps] and then closes:
     * Chinese screen: pinyin → English → original. English screen: Chinese (+ pinyin) → original.
     * Pinch or double-tap to zoom; drag to move around while zoomed.
     */
    private inner class ResultView(val items: List<TextItem>, val dir: Dir, private var shot: Bitmap?) : FrameLayout(this@TranslatorService) {
        val renderer = OverlayRenderer(dp)
        val steps = if (dir == Dir.ZH_EN && prefs.showPinyin) listOf(Show.PINYIN, Show.TRANSLATION) else listOf(Show.TRANSLATION)
        var mode = steps.first()
        var hits: List<Pair<RectF, TextItem>> = emptyList()
        val statusTv = TextView(context).apply { setTextColor(Color.WHITE); textSize = 13f }
        private val loc = IntArray(2)
        private val chips = LinkedHashMap<Show, TextView>()
        private var who = "translating…"

        // zoom state: screen = view * z + (tx, ty)
        private var z = 1f; private var tx = 0f; private var ty = 0f

        init {
            setWillNotDraw(false)
            setBackgroundColor(Color.argb(40, 0, 0, 0))
            val bar = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
                background = GradientDrawable().apply { setColor(Color.argb(230, 30, 30, 30)); cornerRadius = 22 * dp }
                val p = (10 * dp).toInt(); setPadding(p * 2, p, p, p)
                addView(statusTv, LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
                val names = if (dir == Dir.ZH_EN) listOfNotNull(Show.ORIGINAL to "原", (Show.PINYIN to "拼音").takeIf { prefs.showPinyin }, Show.TRANSLATION to "EN")
                            else listOf(Show.ORIGINAL to "EN", Show.TRANSLATION to "中文")
                names.forEach { (m, label) -> btn(label) { show(m) }.also { chips[m] = it; addView(it) } }
                addView(btn("Copy") { copy(items.mapNotNull { textFor(it) }.joinToString("\n")) })
                addView(btn("✕") { removeOverlay() })
            }
            addView(bar, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, Gravity.BOTTOM).apply {
                val m = (12 * dp).toInt(); setMargins(m, m, m, (56 * dp).toInt())
            })
            refreshBar()
        }

        private fun btn(label: String, onClick: () -> Unit) = TextView(context).apply {
            text = label; setTextColor(Color.WHITE); textSize = 15f; gravity = Gravity.CENTER
            val p = (10 * dp).toInt(); setPadding(p, p / 2, p, p / 2); setOnClickListener { safe(onClick) }
        }

        fun textFor(it: TextItem): String? = when (mode) {
            Show.ORIGINAL -> it.original
            Show.PINYIN -> it.pinyin ?: it.original
            Show.TRANSLATION -> it.translated?.let { t ->
                val py = it.pinyin
                if (dir == Dir.EN_ZH && prefs.showPinyin && py != null) "$t\n$py" else t
            }
        }

        fun show(m: Show) { mode = m; refreshBar(); invalidate() }

        /** Bubble tap while open: next step, then close. */
        fun next() {
            val i = steps.indexOf(mode)
            if (i < 0 || i + 1 >= steps.size) removeOverlay() else show(steps[i + 1])
        }

        private fun refreshBar() {
            chips.forEach { (m, v) ->
                v.background = if (m == mode) GradientDrawable().apply { setColor(Color.argb(90, 255, 255, 255)); cornerRadius = 14 * dp } else null
            }
            val next = if (mode != Show.ORIGINAL && steps.indexOf(mode) + 1 < steps.size) "next" else "close"
            statusTv.text = "${dir.label} · ${who.removeSuffix(" · on phone")}\nbubble = $next · pinch = zoom"
        }

        fun update(who: String) { this.who = who; refreshBar(); invalidate() }
        fun status(s: String) { who = s; refreshBar() }
        fun release() { shot = null }

        override fun onDraw(c: Canvas) {
            super.onDraw(c)
            getLocationOnScreen(loc)
            val ox = -loc[0].toFloat(); val oy = -loc[1].toFloat()
            c.save(); c.translate(tx, ty); c.scale(z, z)
            // zoomed: the live screen underneath no longer lines up, so draw the screenshot under the boxes
            if (z > 1.01f) shot?.let { c.drawBitmap(it, ox, oy, null) }
            hits = renderer.draw(c, items, ox, oy, 1f) { textFor(it) }
            c.restore()
        }

        private fun clamp() {
            tx = tx.coerceIn(width - width * z, 0f); ty = ty.coerceIn(height - height * z, 0f)
        }

        private fun zoomBy(f: Float, fx: Float, fy: Float) {
            val nz = (z * f).coerceIn(1f, 6f); val k = nz / z
            tx = fx - (fx - tx) * k; ty = fy - (fy - ty) * k; z = nz; clamp(); invalidate()
        }

        private val scaler = android.view.ScaleGestureDetector(context, object : android.view.ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(d: android.view.ScaleGestureDetector): Boolean { zoomBy(d.scaleFactor, d.focusX, d.focusY); return true }
        })

        private val gestures = android.view.GestureDetector(context, object : android.view.GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent) = true
            override fun onScroll(e1: MotionEvent?, e2: MotionEvent, dx: Float, dy: Float): Boolean {
                if (z > 1f && !scaler.isInProgress) { tx -= dx; ty -= dy; clamp(); invalidate() }
                return true
            }
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean { safe { tap(e.x, e.y) }; return true }
            override fun onDoubleTap(e: MotionEvent): Boolean {
                if (z > 1.01f) { z = 1f; tx = 0f; ty = 0f; invalidate() } else zoomBy(2.5f, e.x, e.y)
                return true
            }
        })

        private fun tap(x: Float, y: Float) {
            val ux = (x - tx) / z; val uy = (y - ty) / z
            val hit = hits.lastOrNull { it.first.contains(ux, uy) }
            when {
                hit != null -> copy("${textFor(hit.second) ?: ""}\n(${hit.second.original})")
                z <= 1.01f -> removeOverlay()   // tap on empty space closes; while zoomed it doesn't (too easy to hit)
            }
        }

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouchEvent(e: MotionEvent): Boolean {
            safe { scaler.onTouchEvent(e); gestures.onTouchEvent(e) }
            return true
        }
    }

    private fun copy(s: String) {
        (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("roozTrans", s))
        Toast.makeText(this, "Copied", Toast.LENGTH_SHORT).show()
    }

    private fun showOverlay(items: List<TextItem>, dir: Dir, shot: Bitmap): ResultView {
        removeOverlay()
        val v = ResultView(items, dir, shot)
        val lp = WindowManager.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT).apply {
            gravity = Gravity.TOP or Gravity.START
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            fitInsetsTypes = 0
        }
        try { wm.addView(v, lp) } catch (t: Throwable) { done("Can't show the translation: ${t.message}"); return v }
        overlay = v
        // keep bubble above the overlay so it can be tapped to close
        bubble?.let { b -> runCatching { wm.removeView(b); wm.addView(b, bubbleLp) } }
        return v
    }

    private fun removeOverlay() { overlay?.let { it.release(); runCatching { wm.removeView(it) } }; overlay = null }
}
