package com.rooz.translator

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

/** Shown once on first launch (and from Settings → Setup guide): privacy, bubble, offline packs, updates. */
class WelcomeActivity : AppCompatActivity() {
    private lateinit var prefs: Prefs
    private lateinit var bubbleTv: TextView
    private lateinit var packsTv: TextView
    private var installTv: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        Themes.apply(this)
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)
        var step = 0
        val root = vbox(16) {
            addView(label("Welcome to roozTrans", 24f, bold = true))
            addView(label("English ⇄ 中文 — text, photos and your screen. Free, no ads.", 15f).apply {
                alpha = .75f; setPadding(0, dp(4), 0, dp(16))
            })

            addView(card {
                addView(label("🔒 Your privacy", 16f, bold = true))
                addView(label(
                    "• No account, no ads, no analytics, no tracking.\n" +
                    "• We (the developer) never receive or store anything you translate, nor any personal data.\n" +
                    "• Offline mode: everything stays on your phone.\n" +
                    "• Screenshots are read on the phone and then discarded, never uploaded or saved.\n" +
                    "• Online translation is OFF until you add your own API key. Then only the text is sent, " +
                    "directly to the AI service you chose (Claude, OpenAI or DeepSeek).\n" +
                    "• Google's ML Kit, which powers offline translation, may send Google basic diagnostics.", 14f
                ).apply { setPadding(0, dp(6), 0, 0) })
            })

            addView(card {
                addView(label("${++step}. Turn on the floating bubble", 16f, bold = true))
                addView(label("The bubble floats over any app. Tap it (or double-press Volume Down, or use the " +
                    "\"Translate screen\" Quick Settings tile) to translate what's on screen.", 14f).apply { alpha = .8f })
                bubbleTv = label("", 14f).apply { setPadding(0, dp(8), 0, 0) }; addView(bubbleTv)
                addView(button("Turn on screen translator") { openAccessibilitySetup() })
            })

            addView(card {
                addView(label("${++step}. Download offline languages", 16f, bold = true))
                addView(label("About 60 MB, once. Do it before travelling: Google's servers are blocked in mainland China.", 14f).apply { alpha = .8f })
                packsTv = label("", 14f).apply { setPadding(0, dp(8), 0, 0) }; addView(packsTv)
                addView(button("Download now", outlined = true) {
                    packsTv.text = "⏳ Downloading…"
                    lifecycleScope.launch {
                        runCatching { Offline.downloadModels() }
                            .onSuccess { packsTv.text = "✅ Offline English/中文 ready" }
                            .onFailure { packsTv.text = "✗ Failed: ${it.message}. Try again on another network, or later in Settings." }
                    }
                })
            })

            if (Updater.ENABLED) addView(card {
                addView(label("${++step}. Updates", 16f, bold = true))
                addView(label("roozTrans can keep itself up to date in the background. Only the app version is checked. Nothing about you is sent.", 14f).apply { alpha = .8f })
                addView(sw("Update automatically", prefs.autoUpdate) { prefs.autoUpdate = it })
                addView(sw("Only on Wi-Fi (updates are ~60 MB)", prefs.updateWifiOnly) { prefs.updateWifiOnly = it })
                installTv = label("", 14f).apply { setPadding(0, dp(4), 0, 0) }; addView(installTv)
                addView(button("Allow roozTrans to install its updates", outlined = true) {
                    startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:$packageName")))
                })
            })

            addView(label("By tapping Start you agree to the Terms of use. Machine translation can be wrong. " +
                "Don't rely on it for medical, legal or safety decisions.", 13f).apply { alpha = .7f; setPadding(0, dp(8), 0, dp(4)) })
            addView(hbox {
                weighted(button("Terms", outlined = true) { openUrl(Links.TERMS) }, marginEnd = 8)
                weighted(button("Privacy", outlined = true) { openUrl(Links.PRIVACY) })
            })
            addView(button("Start") { prefs.onboarded = true; finish() }.apply { setPadding(0, dp(14), 0, dp(14)) })
            addView(label("You can open this guide again from Settings.", 12f).apply { alpha = .6f; gravity = android.view.Gravity.CENTER })
        }
        val scroll = ScrollView(this).apply { addView(root) }
        ViewCompat.setOnApplyWindowInsetsListener(scroll) { v, ins ->
            val b = ins.getInsets(WindowInsetsCompat.Type.systemBars()); v.setPadding(b.left, b.top, b.right, b.bottom); ins
        }
        setContentView(scroll)
    }

    override fun onResume() {
        super.onResume()
        bubbleTv.text = if (TranslatorService.instance != null) "✅ Screen translator is on" else "⭕ Not on yet"
        installTv?.text = if (packageManager.canRequestPackageInstalls()) "✅ Updates can install" else "⭕ Not allowed yet (Android will also ask on the first update)"
        lifecycleScope.launch {
            packsTv.text = if (runCatching { Offline.modelsReady() }.getOrDefault(false)) "✅ Offline English/中文 ready" else "⭕ Not downloaded yet"
        }
    }

    private fun sw(s: String, v: Boolean, on: (Boolean) -> Unit) = SwitchCompat(this).apply {
        text = s; isChecked = v; setPadding(0, dp(6), 0, dp(6)); setOnCheckedChangeListener { _, c -> on(c) }
    }
}
