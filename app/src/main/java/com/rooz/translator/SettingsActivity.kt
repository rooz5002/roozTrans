package com.rooz.translator

import android.os.Bundle
import android.text.InputType
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {
    private lateinit var prefs: Prefs
    private val saves = ArrayList<() -> Unit>()
    private lateinit var svcTv: TextView
    private lateinit var modelsTv: TextView
    private lateinit var netTv: TextView
    private val modes = listOf("auto" to "Auto (by IP country)", "offline" to "Offline only",
        "claude" to "Always Claude", "openai" to "Always OpenAI", "deepseek" to "Always DeepSeek")

    override fun onCreate(savedInstanceState: Bundle?) {
        Themes.apply(this)
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)
        val pal = Themes.current(this)
        lateinit var testTv: TextView
        val root = vbox(16) {
            addView(label("Settings", 22f, bold = true).apply { setPadding(0, 0, 0, dp(12)) })
            addView(card {
                addView(label("Colour", 16f, bold = true))
                addView(label("Tap one to try it — the whole app changes straight away.", 13f).apply { alpha = .7f })
                Themes.all.chunked(4).forEach { row ->
                    addView(hbox {
                        setPadding(0, dp(10), 0, 0)
                        row.forEach { p -> weighted(swatch(p, p.id == pal.id) {
                            if (p.id != prefs.theme) { saveAll(); prefs.theme = p.id; recreate() }
                        }) }
                    })
                }
            })
            addView(card {
                addView(label("Screen translator", 16f, bold = true))
                addView(label("Tap the floating bubble, double-press Volume Down, or use the \"Translate screen\" Quick Settings tile.", 13f).apply { alpha = .7f })
                svcTv = label(""); addView(svcTv)
                addView(button("Turn on screen translator", outlined = true) { openAccessibility() })
                modelsTv = label(""); addView(modelsTv)
                addView(button("Download offline language packs", outlined = true) { downloadModels() })
                netTv = label(""); addView(netTv)
                addView(button("Check network / which AI", outlined = true) { checkNet(force = true) })
            })
            addView(card {
                addView(label("Online translation", 16f, bold = true))
                addView(label("Auto: in mainland China → DeepSeek. Elsewhere → Claude, then OpenAI, then DeepSeek. Offline is always the fallback.", 13f).apply { alpha = .7f })
                val sp = Spinner(context).apply {
                    adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, modes.map { it.second })
                    setSelection(modes.indexOfFirst { it.first == prefs.mode }.coerceAtLeast(0))
                }
                addView(sp); saves += { prefs.mode = modes[sp.selectedItemPosition].first }
                addView(switch("Improve offline result online (when available)", prefs.onlineEnhance) { prefs.onlineEnhance = it })
            })
            addView(card {
                addView(label("API keys (stored only on this phone)", 16f, bold = true))
                field("Claude API key", prefs.claudeKey, secret = true) { prefs.claudeKey = it }
                field("Claude model", prefs.claudeModel) { prefs.claudeModel = it }
                field("OpenAI API key", prefs.openaiKey, secret = true) { prefs.openaiKey = it }
                field("OpenAI model", prefs.openaiModel) { prefs.openaiModel = it }
                field("DeepSeek API key", prefs.deepseekKey, secret = true) { prefs.deepseekKey = it }
                field("DeepSeek model", prefs.deepseekModel) { prefs.deepseekModel = it }
            })
            addView(card {
                addView(label("Triggers", 16f, bold = true))
                addView(switch("Floating bubble", prefs.showBubble) { prefs.showBubble = it; TranslatorService.instance?.refreshBubble() })
                addView(switch("Double-press Volume Down = translate screen", prefs.volumeTrigger) { prefs.volumeTrigger = it })
            })
            if (Updater.ENABLED) addView(card {
                addView(label("Updates · installed v${BuildConfig.VERSION_NAME}", 16f, bold = true))
                addView(switch("Update automatically", prefs.autoUpdate) { prefs.autoUpdate = it })
                addView(switch("Only on Wi-Fi (updates are ~60 MB)", prefs.updateWifiOnly) { prefs.updateWifiOnly = it })
                val st = label(Updater.clearStaleStatus(this@SettingsActivity).let { prefs.updateStatus }, 13f).apply { alpha = .75f }
                addView(button("Check for updates now", outlined = true) {
                    if (!packageManager.canRequestPackageInstalls()) {
                        st.text = "Allow \"Install unknown apps\" for roozTrans, then come back and tap again."
                        startActivity(android.content.Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                            android.net.Uri.parse("package:$packageName")))
                        return@button
                    }
                    st.text = "⏳ Checking…"
                    lifecycleScope.launch { st.text = runCatching { Updater.checkAndInstall(this@SettingsActivity) }.getOrElse { "✗ ${it.message}" } }
                })
                addView(st)
            })
            addView(card {
                addView(label("Test", 16f, bold = true))
                testTv = label("", 14f).apply { setTextIsSelectable(true) }
                addView(button("Test network + all 3 services") {
                    saveAll(); testTv.text = "⏳ testing…"
                    lifecycleScope.launch {
                        val o = Online(this@SettingsActivity)
                        val sb = StringBuilder(o.route(force = true).describe())
                        for (p in listOf("claude", "openai", "deepseek")) { sb.append("\n\n").append(o.test(p)); testTv.text = sb }
                    }
                })
                addView(testTv)
            })
        }
        root.addView(card {
            addView(label("About · roozTrans v${BuildConfig.VERSION_NAME}", 16f, bold = true))
            addView(label("Free, no ads, no tracking. Machine translation can be wrong — don't rely on it for medical, legal, safety or other important decisions.", 13f).apply { alpha = .7f })
            addView(button("Terms of use & disclaimer", outlined = true) { openUrl(TERMS_URL) })
            addView(button("Privacy policy", outlined = true) { openUrl(PRIVACY_URL) })
            addView(button("Source code", outlined = true) { openUrl(SOURCE_URL) })
        })
        val scroll = ScrollView(this).apply { addView(root) }
        ViewCompat.setOnApplyWindowInsetsListener(scroll) { v, ins ->
            val b = ins.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime()); v.setPadding(b.left, b.top, b.right, b.bottom); ins
        }
        setContentView(scroll)
        if (savedInstanceState == null && intent.getBooleanExtra(EXTRA_ENABLE_A11Y, false)) openAccessibility()
    }

    private fun android.widget.LinearLayout.field(name: String, value: String, secret: Boolean = false, save: (String) -> Unit) {
        addView(label(name, 13f).apply { setPadding(0, dp(8), 0, 0) })
        val e = EditText(context).apply {
            setText(value); isSingleLine = true
            inputType = if (secret) InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                        else InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        }
        addView(e); saves += { save(e.text.toString()) }
    }

    private fun switch(s: String, v: Boolean, on: (Boolean) -> Unit) = SwitchCompat(this).apply {
        text = s; isChecked = v; setPadding(0, dp(6), 0, dp(6)); setOnCheckedChangeListener { _, c -> on(c) }
    }

    override fun onResume() {
        super.onResume()
        svcTv.text = if (TranslatorService.instance != null) "✅ Screen translator is ON" else "⭕ Screen translator is OFF"
        lifecycleScope.launch {
            modelsTv.text = if (runCatching { Offline.modelsReady() }.getOrDefault(false)) "✅ Offline EN/中 packs ready"
                            else "⚠ Offline packs missing — download before going to China"
        }
        checkNet(force = false)
    }

    /** A round sample of the palette (page colour with its accent dot) and its name; ringed when selected. */
    private fun swatch(p: Palette, selected: Boolean, onClick: () -> Unit) = vbox {
        gravity = android.view.Gravity.CENTER_HORIZONTAL
        val cur = Themes.current(context)
        addView(android.widget.FrameLayout(context).apply {
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.OVAL; setColor(p.bg)
                setStroke(dp(if (selected) 3 else 1), if (selected) cur.accent else cur.line)
            }
            addView(View(context).apply {
                background = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.OVAL; setColor(p.accent)
                }
            }, android.widget.FrameLayout.LayoutParams(dp(20), dp(20), android.view.Gravity.CENTER))
        }, android.widget.LinearLayout.LayoutParams(dp(48), dp(48)))
        addView(label(p.name, 12f, bold = selected).apply {
            gravity = android.view.Gravity.CENTER; setPadding(0, dp(4), 0, 0); maxLines = 1
        })
        setOnClickListener { onClick() }
    }

    /**
     * Google Play "prominent disclosure": say what the accessibility service does and get a clear yes
     * before sending the user to Android's Accessibility settings.
     */
    private fun openAccessibility() {
        if (prefs.a11yConsent) { showAccessibilitySteps(); return }
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("roozTrans uses the Accessibility service")
            .setMessage(getString(R.string.a11y_disclosure))
            .setPositiveButton("Agree") { _, _ -> prefs.a11yConsent = true; showAccessibilitySteps() }
            .setNegativeButton("No thanks", null)
            .setCancelable(false)
            .show()
    }

    private fun showAccessibilitySteps() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Turn on screen translator")
            .setMessage("1. Tap \"Accessibility\" → Installed apps → roozTrans screen translator → On.\n\n" +
                "If you see \"App was denied access\" / \"Restricted setting\":\n" +
                "2. Tap \"App info\" below → ⋮ (top right) → Allow restricted settings → confirm.\n" +
                "3. Then come back and do step 1 again.\n\n" +
                "Samsung: if ⋮ has no such option, turn off Settings → Security and privacy → Auto Blocker first.")
            .setPositiveButton("Accessibility") { _, _ -> startActivity(android.content.Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
            .setNeutralButton("App info") { _, _ ->
                startActivity(android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.parse("package:$packageName")))
            }
            .show()
    }

    private fun downloadModels() {
        modelsTv.text = "⏳ Downloading (~60 MB)…"
        lifecycleScope.launch {
            runCatching { Offline.downloadModels() }
                .onSuccess { modelsTv.text = "✅ Offline EN/中 packs ready" }
                .onFailure { modelsTv.text = "✗ Download failed: ${it.message} (Google servers are blocked in China)" }
        }
    }

    private fun checkNet(force: Boolean) {
        netTv.text = "⏳ Checking network…"
        lifecycleScope.launch { netTv.text = Online(this@SettingsActivity).route(force).describe() }
    }

    private fun openUrl(u: String) { runCatching { startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(u))) } }

    companion object {
        const val EXTRA_ENABLE_A11Y = "enable_a11y"
        const val SOURCE_URL = "https://github.com/rooz5002/roozTrans"
        const val TERMS_URL = "$SOURCE_URL/blob/main/TERMS.md"
        const val PRIVACY_URL = "$SOURCE_URL/blob/main/PRIVACY.md"
    }

    private fun saveAll() = saves.forEach { it() }
    override fun onPause() { saveAll(); super.onPause() }
}
