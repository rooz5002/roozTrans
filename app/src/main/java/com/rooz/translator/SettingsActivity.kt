package com.rooz.translator

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.launch

/**
 * Settings, one compact card per topic, each with its own colour stripe:
 * offline method · online AI · languages · personalise · screen translator · updates · about.
 */
class SettingsActivity : AppCompatActivity() {
    private lateinit var prefs: Prefs
    private lateinit var pal: Palette
    private val saves = ArrayList<() -> Unit>()
    private lateinit var svcTv: TextView
    private lateinit var svcBtn: MaterialButton
    private lateinit var batTv: TextView
    private lateinit var repairTv: TextView
    private lateinit var modelsTv: TextView
    private lateinit var netTv: TextView
    private lateinit var modeHint: TextView
    private val keyDots = HashMap<String, TextView>()

    // card colours: readable on both the light and the dark palettes
    private fun hue(light: String, dark: String) = Color.parseColor(if (pal.dark) dark else light)
    private val cOffline get() = hue("#2E7D32", "#81C784")
    private val cOnline get() = hue("#1565C0", "#64B5F6")
    private val cLang get() = hue("#6A1B9A", "#CE93D8")
    private val cLook get() = pal.accent
    private val cScreen get() = hue("#E65100", "#FFB74D")
    private val cUpdate get() = hue("#00838F", "#4DD0E1")
    private val cAbout get() = hue("#546E7A", "#B0BEC5")
    private val cOk get() = hue("#2E7D32", "#81C784")
    private val cWarn get() = hue("#B26A00", "#FFB74D")

    override fun onCreate(savedInstanceState: Bundle?) {
        Themes.apply(this)
        super.onCreate(savedInstanceState)
        prefs = Prefs(this); pal = Themes.current(this)

        val root = vbox().apply { setPadding(dp(12), dp(8), dp(12), dp(16)) }
        root.addView(TextView(this).apply {
            text = "Settings"; textSize = 20f; setTypeface(typeface, Typeface.BOLD); setTextColor(pal.text)
            setPadding(dp(4), dp(4), 0, dp(8))
        })

        // ---------- 1. offline ----------
        root.addView(section("📴", "Offline translation", cOffline) {
            hint("Works with no internet. Used first, and always as the fallback.")
            addView(choice(listOf(
                "fast" to "Fast · on phone (~60 MB)",
                "quality" to "High quality · AI model (~1 GB)",
            ), prefs.offlineEngine, cOffline, disabled = setOf("quality")) { prefs.offlineEngine = it })
            hint("High quality needs a newer phone (6 GB+ RAM). Coming in the next version.")
            modelsTv = status(); addView(row(modelsTv, small("Download", cOffline) { downloadModels() }))
        })

        // ---------- 2. online AI ----------
        root.addView(section("🌐", "Online AI (your own key)", cOnline) {
            hint("Better, more natural translations when there is internet. Off until you add a key.")
            addView(choice(listOf("offline" to "Off", "auto" to "Auto", "claude" to "Claude", "openai" to "OpenAI", "deepseek" to "DeepSeek"),
                prefs.mode, cOnline) { prefs.mode = it; showModeHint(); refreshKeys() })
            modeHint = hint(""); showModeHint()
            keyRow("claude", "Claude", prefs.claudeKey) { prefs.claudeKey = it }
            keyRow("openai", "OpenAI", prefs.openaiKey) { prefs.openaiKey = it }
            keyRow("deepseek", "DeepSeek", prefs.deepseekKey) { prefs.deepseekKey = it }
            addView(sw("Improve the offline result online", prefs.onlineEnhance) { prefs.onlineEnhance = it })
            // model names: rarely changed, so folded away
            val adv = vbox().apply { visibility = View.GONE }
            adv.field("Claude model", prefs.claudeModel) { prefs.claudeModel = it }
            adv.field("OpenAI model", prefs.openaiModel) { prefs.openaiModel = it }
            adv.field("DeepSeek model", prefs.deepseekModel) { prefs.deepseekModel = it }
            netTv = status()
            val testTv = status().apply { setTextIsSelectable(true); visibility = View.GONE }
            addView(buttons(
                small("Check network", cOnline) { checkNet(force = true) },
                small("Test keys", cOnline) {
                    saveAll(); testTv.visibility = View.VISIBLE; testTv.text = "⏳ testing…"
                    lifecycleScope.launch {
                        val o = Online(this@SettingsActivity)
                        val sb = StringBuilder()
                        for (p in listOf("claude", "openai", "deepseek")) { sb.append(o.test(p)).append('\n'); testTv.text = sb.trim() }
                    }
                },
                small("Models…", cOnline) { adv.visibility = if (adv.visibility == View.GONE) View.VISIBLE else View.GONE },
            ))
            addView(netTv); addView(testTv); addView(adv)
        })

        // ---------- 3. languages ----------
        root.addView(section("🔤", "Languages", cLang) {
            addView(status().apply { text = "English ⇄ 中文 (Simplified Chinese)"; setTextColor(pal.text) })
            hint("More languages are coming in the next version.")
            addView(sw("Show pinyin (拼音) with Chinese", prefs.showPinyin) { prefs.showPinyin = it })
        })

        // ---------- 4. personalise ----------
        root.addView(section("🎨", "Personalise", cLook) {
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                Themes.all.forEach { p ->
                    addView(swatch(p, p.id == pal.id) { if (p.id != prefs.theme) { saveAll(); prefs.theme = p.id; recreate() } },
                        LinearLayout.LayoutParams(0, -2, 1f))
                }
            })
        })

        // ---------- 5. screen translator ----------
        root.addView(section("⚡", "Screen translator · triggers", cScreen) {
            svcTv = status()
            svcBtn = small("Turn on", cScreen) {
                val st = KeepAlive.state(this@SettingsActivity)
                if (st == KeepAlive.State.STUCK) KeepAlive.showRepair(this@SettingsActivity, st) else openAccessibilitySetup()
            }
            addView(row(svcTv, svcBtn))
            batTv = status()
            addView(row(batTv, small("Keep it on", cScreen) { KeepAlive.showGuide(this@SettingsActivity) }))
            addView(sw("Keep running while asleep (quiet notification)", prefs.keepAlive) {
                prefs.keepAlive = it
                if (it && TranslatorService.instance != null) KeepAliveService.start(this@SettingsActivity)
                if (!it) KeepAliveService.stop(this@SettingsActivity)
            })
            repairTv = status()
            addView(row(repairTv, small("Why stopped?", cScreen) { showStops() }))
            addView(sw("Floating bubble", prefs.showBubble) { prefs.showBubble = it; TranslatorService.instance?.refreshBubble() })
            addView(sw("Double-press Volume Down", prefs.volumeTrigger) { prefs.volumeTrigger = it })
            hint("Or use the \"Translate screen\" Quick Settings tile. On screen: tap the bubble for pinyin, again for the translation, again to close. Pinch to zoom.")
        })

        // ---------- 6. updates (direct-download build only) ----------
        if (Updater.ENABLED) root.addView(section("⬇", "Updates", cUpdate) {
            addView(sw("Update automatically", prefs.autoUpdate) { prefs.autoUpdate = it })
            addView(sw("Only on Wi-Fi (~60 MB)", prefs.updateWifiOnly) { prefs.updateWifiOnly = it })
            Updater.clearStaleStatus(this@SettingsActivity)
            val st = status().apply { text = prefs.updateStatus.ifBlank { "Installed: v${BuildConfig.VERSION_NAME}" } }
            addView(row(st, small("Check now", cUpdate) {
                if (!packageManager.canRequestPackageInstalls()) {
                    st.text = "Allow \"Install unknown apps\" for roozTrans, then tap again."
                    startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:$packageName")))
                    return@small
                }
                st.text = "⏳ Checking…"
                lifecycleScope.launch { st.text = runCatching { Updater.checkAndInstall(this@SettingsActivity) }.getOrElse { "✗ ${it.message}" } }
            }))
        })

        // ---------- 7. about ----------
        root.addView(section("ℹ", "About · v${BuildConfig.VERSION_NAME}", cAbout) {
            hint("Free, no ads, no tracking. Machine translation can be wrong: don't rely on it for medical, legal or safety decisions.")
            addView(ChipGroup(context).apply {
                chipSpacingVertical = dp(2); chipSpacingHorizontal = dp(6)
                listOf("Setup guide" to { startActivity(Intent(this@SettingsActivity, WelcomeActivity::class.java)) },
                    "Contact us" to { openUrl(Links.CONTACT) },
                    "Terms" to { openUrl(Links.TERMS) },
                    "Privacy" to { openUrl(Links.PRIVACY) },
                    "Source code" to { openUrl(Links.SOURCE) },
                ).forEach { (t, f) -> addView(actionChip(t, cAbout, f)) }
            })
        })

        val scroll = ScrollView(this).apply { addView(root); setBackgroundColor(pal.bg) }
        ViewCompat.setOnApplyWindowInsetsListener(scroll) { v, ins ->
            val b = ins.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime()); v.setPadding(b.left, b.top, b.right, b.bottom); ins
        }
        setContentView(scroll)
        refreshKeys()
        if (savedInstanceState == null && intent.getBooleanExtra(EXTRA_ENABLE_A11Y, false)) {
            val st = KeepAlive.state(this)   // from the Quick Settings tile: stopped by the phone, or never turned on?
            if (st == KeepAlive.State.STUCK || (st == KeepAlive.State.OFF && prefs.a11yWasOn)) KeepAlive.showRepair(this, st)
            else openAccessibilitySetup()
        }
    }

    override fun onResume() {
        super.onResume()
        when (KeepAlive.state(this)) {
            KeepAlive.State.RUNNING -> { svcTv.text = "● On"; svcTv.setTextColor(cOk); svcBtn.text = "Settings" }
            KeepAlive.State.STUCK -> { svcTv.text = "● Stopped by the phone"; svcTv.setTextColor(cWarn); svcBtn.text = "Fix" }
            KeepAlive.State.OFF -> { svcTv.text = "○ Off"; svcTv.setTextColor(pal.muted); svcBtn.text = "Turn on" }
        }
        val rep = KeepAlive.canSelfRepair(this)
        repairTv.text = if (rep) "● Auto-repair on" else "○ Auto-repair off (one-time PC setup)"
        repairTv.setTextColor(if (rep) cOk else pal.muted)
        val bat = KeepAlive.ignoringBattery(this)
        batTv.text = if (bat) "● Allowed to keep running" else "● Battery saver may stop it"
        batTv.setTextColor(if (bat) cOk else cWarn)
        lifecycleScope.launch {
            val ok = runCatching { Offline.modelsReady() }.getOrDefault(false)
            modelsTv.text = if (ok) "● English/中文 packs ready" else "● Packs not downloaded: download before going to China"
            modelsTv.setTextColor(if (ok) cOk else cWarn)
        }
        checkNet(force = false)
    }

    /** When/why Android ended the app (diagnostics), plus how to turn on auto-repair. */
    private fun showStops() {
        val last = prefs.lastRepair.takeIf { it > 0 }?.let {
            "\nLast auto-repair: " + java.text.SimpleDateFormat("d MMM HH:mm", java.util.Locale.UK).format(java.util.Date(it)) } ?: ""
        val msg = "Recent stops of roozTrans:\n${KeepAlive.exitReasons(this)}$last\n\n" +
            (if (KeepAlive.canSelfRepair(this)) "Auto-repair is on: when the phone stops the screen translator, roozTrans turns it off and on again by itself."
            else "Auto-repair (optional): connect the phone to a computer with USB debugging on and run once:\n\n${KeepAlive.GRANT_CMD}\n\n" +
                "roozTrans can then turn the screen translator off and on again by itself when the phone stops it.")
        androidx.appcompat.app.AlertDialog.Builder(this).setTitle("Screen translator stops").setMessage(msg)
            .setPositiveButton("OK", null)
            .setNeutralButton("Copy") { _, _ ->
                (getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager)
                    .setPrimaryClip(android.content.ClipData.newPlainText("roozTrans", msg))
            }.show()
    }

    // ---------- building blocks ----------

    /** A card with a coloured stripe on the left and a coloured title. */
    private fun section(icon: String, title: String, color: Int, block: LinearLayout.() -> Unit): View {
        val body = vbox().apply {
            setPadding(dp(12), dp(8), dp(10), dp(8))
            addView(TextView(context).apply {
                text = "$icon  $title"; textSize = 15f; setTypeface(typeface, Typeface.BOLD); setTextColor(color)
                setPadding(0, 0, 0, dp(2))
            })
            block()
        }
        val r = dp(14).toFloat()
        val bg = GradientDrawable().apply { setColor(pal.surface); cornerRadius = r }
        val stripe = GradientDrawable().apply { setColor(color); cornerRadii = floatArrayOf(r, r, 0f, 0f, 0f, 0f, r, r) }
        body.background = LayerDrawable(arrayOf(bg, stripe)).apply {
            setLayerWidth(1, dp(4)); setLayerGravity(1, Gravity.START or Gravity.FILL_VERTICAL)
        }
        body.layoutParams = LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 0, 0, dp(8)) }
        return body
    }

    private fun LinearLayout.hint(s: String) = TextView(context).apply {
        text = s; textSize = 12.5f; setTextColor(pal.muted); setPadding(0, dp(1), 0, dp(3))
    }.also { addView(it) }

    private fun status() = TextView(this).apply { textSize = 13f; setTextColor(pal.text); setPadding(0, dp(2), 0, dp(2)) }

    /** Text on the left, a small button on the right. */
    private fun row(left: View, right: View) = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
        addView(left, LinearLayout.LayoutParams(0, -2, 1f)); addView(right)
    }

    private fun buttons(vararg b: View) = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        b.forEachIndexed { i, v -> addView(v, LinearLayout.LayoutParams(-2, -2).apply { marginEnd = if (i < b.size - 1) dp(6) else 0 }) }
    }

    /** Compact outlined button in the card's colour. */
    private fun small(s: String, color: Int, onClick: () -> Unit) =
        MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = s; isAllCaps = false; textSize = 13f; setTextColor(color)
            strokeColor = ColorStateList.valueOf(color); strokeWidth = dp(1)
            minHeight = dp(34); minimumHeight = dp(34); minWidth = 0; minimumWidth = 0
            insetTop = 0; insetBottom = 0; setPadding(dp(12), 0, dp(12), 0); cornerRadius = dp(17)
            setOnClickListener { onClick() }
        }

    private fun actionChip(s: String, color: Int, onClick: () -> Unit) = Chip(this).apply {
        text = s; textSize = 13f; setTextColor(color); isCheckable = false
        chipBackgroundColor = ColorStateList.valueOf(Color.TRANSPARENT)
        chipStrokeColor = ColorStateList.valueOf(color); chipStrokeWidth = dp(1).toFloat()
        chipMinHeight = dp(32).toFloat(); ensureAccessibleTouchTarget(dp(40))
        setOnClickListener { onClick() }
    }

    /** One-of-many choice as a row of chips; the selected one is filled with the card colour. */
    private fun choice(items: List<Pair<String, String>>, selected: String, color: Int, disabled: Set<String> = emptySet(), onPick: (String) -> Unit) =
        ChipGroup(this).apply {
            isSingleSelection = true; isSelectionRequired = true
            chipSpacingVertical = dp(2); chipSpacingHorizontal = dp(5)
            val states = arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf())
            items.forEach { (id, label) ->
                addView(Chip(context).apply {
                    text = label; textSize = 13f; isCheckable = true; isCheckedIconVisible = false
                    chipStartPadding = dp(2).toFloat(); chipEndPadding = dp(2).toFloat()
                    textStartPadding = dp(7).toFloat(); textEndPadding = dp(7).toFloat()
                    chipMinHeight = dp(32).toFloat(); ensureAccessibleTouchTarget(dp(40))
                    chipBackgroundColor = ColorStateList(states, intArrayOf(color, Color.TRANSPARENT))
                    setTextColor(ColorStateList(states, intArrayOf(if (pal.dark) pal.bg else Color.WHITE, pal.text)))
                    chipStrokeColor = ColorStateList.valueOf(color); chipStrokeWidth = dp(1).toFloat()
                    isChecked = id == selected; isEnabled = id !in disabled; if (!isEnabled) alpha = .45f
                    setOnCheckedChangeListener { _, on -> if (on) onPick(id) }
                })
            }
        }

    private fun sw(s: String, v: Boolean, on: (Boolean) -> Unit) = com.google.android.material.materialswitch.MaterialSwitch(this).apply {
        text = s; textSize = 14f; isChecked = v; setTextColor(pal.text)
        minHeight = dp(44); minimumHeight = dp(44); setPadding(0, 0, 0, 0); setOnCheckedChangeListener { _, c -> on(c) }
    }

    /** "Claude  [key……]  ●" — dot shows whether a key is set and whether this service is in use. */
    private fun LinearLayout.keyRow(id: String, name: String, value: String, save: (String) -> Unit) {
        val e = EditText(context).apply {
            setText(value); isSingleLine = true; textSize = 13f; hint = "API key"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            setPadding(dp(10), dp(6), dp(10), dp(6)); background = rounded(pal.bg, 10, pal.line)
            setOnFocusChangeListener { _, f -> if (!f) { save(text.toString()); refreshKeys() } }
        }
        val dot = TextView(context).apply { textSize = 12f; setPadding(dp(8), 0, 0, 0); minWidth = dp(64) }
        keyDots[id] = dot
        addView(LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, dp(3), 0, dp(3))
            addView(TextView(context).apply { text = name; textSize = 13f; setTextColor(pal.text); minWidth = dp(70) })
            addView(e, LinearLayout.LayoutParams(0, dp(36), 1f)); addView(dot)
        })
        saves += { save(e.text.toString()) }
    }

    private fun refreshKeys() {
        val keys = mapOf("claude" to prefs.claudeKey, "openai" to prefs.openaiKey, "deepseek" to prefs.deepseekKey)
        keyDots.forEach { (id, tv) ->
            val has = keys[id].orEmpty().isNotBlank()
            val used = has && (prefs.mode == "auto" || prefs.mode == id)
            tv.text = when { used -> "● in use"; has -> "● saved"; else -> "○ no key" }
            tv.setTextColor(when { used -> cOk; has -> pal.muted; else -> pal.muted })
        }
    }

    private fun showModeHint() {
        modeHint.text = when (prefs.mode) {
            "offline" -> "Online AI is off: everything is translated on the phone."
            "auto" -> "Auto: in mainland China → DeepSeek. Elsewhere → Claude, then OpenAI, then DeepSeek. Only services with a key are used."
            else -> "Always ${niceName(prefs.mode)} (needs its key below). Offline is used when it can't be reached."
        }
    }

    private fun LinearLayout.field(name: String, value: String, save: (String) -> Unit) {
        val e = EditText(context).apply {
            setText(value); isSingleLine = true; textSize = 13f
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            setPadding(dp(10), dp(6), dp(10), dp(6)); background = rounded(pal.bg, 10, pal.line)
        }
        addView(LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, dp(3), 0, dp(3))
            addView(TextView(context).apply { text = name; textSize = 13f; setTextColor(pal.text); minWidth = dp(110) })
            addView(e, LinearLayout.LayoutParams(0, dp(36), 1f))
        })
        saves += { save(e.text.toString()) }
    }

    /** A small round sample of a palette (page colour with its accent dot), ringed when selected. */
    private fun swatch(p: Palette, selected: Boolean, onClick: () -> Unit) = vbox {
        gravity = Gravity.CENTER_HORIZONTAL; setPadding(0, dp(4), 0, 0)
        addView(FrameLayout(context).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL; setColor(p.bg)
                setStroke(dp(if (selected) 3 else 1), if (selected) pal.accent else pal.line)
            }
            addView(View(context).apply {
                background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(p.accent) }
            }, FrameLayout.LayoutParams(dp(14), dp(14), Gravity.CENTER))
        }, LinearLayout.LayoutParams(dp(34), dp(34)))
        addView(TextView(context).apply {
            text = p.name.substringBefore(' '); textSize = 10f; maxLines = 1; gravity = Gravity.CENTER
            setTextColor(if (selected) pal.text else pal.muted); if (selected) setTypeface(typeface, Typeface.BOLD)
            setPadding(0, dp(2), 0, 0)
        })
        setOnClickListener { onClick() }
    }

    private fun downloadModels() {
        modelsTv.text = "⏳ Downloading (~60 MB)…"; modelsTv.setTextColor(pal.text)
        lifecycleScope.launch {
            runCatching { Offline.downloadModels() }
                .onSuccess { modelsTv.text = "● English/中文 packs ready"; modelsTv.setTextColor(cOk) }
                .onFailure { modelsTv.text = "✗ Download failed: ${it.message} (Google is blocked in China: use a VPN)"; modelsTv.setTextColor(cWarn) }
        }
    }

    private fun checkNet(force: Boolean) {
        netTv.text = "⏳ Checking network…"
        lifecycleScope.launch { netTv.text = Online(this@SettingsActivity).route(force).describe() }
    }

    companion object { const val EXTRA_ENABLE_A11Y = "enable_a11y" }

    private fun saveAll() = saves.forEach { it() }
    override fun onPause() { saveAll(); super.onPause() }
}
