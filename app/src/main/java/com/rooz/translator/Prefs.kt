package com.rooz.translator

import android.content.Context
import android.content.SharedPreferences

/**
 * All settings live in the app's private SharedPreferences. Ordinary settings ("settings") may be included in
 * Android's backup to the user's Google account; API keys live in a separate file ("keys") that is excluded from
 * cloud backup (res/xml/backup_rules.xml, data_extraction_rules.xml).
 */
class Prefs(ctx: Context) {
    private val p: SharedPreferences = ctx.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val k: SharedPreferences = ctx.applicationContext.getSharedPreferences(KEYS_FILE, Context.MODE_PRIVATE)

    init { migrateKeys() }

    /** Up to 1.8.2 the keys were in "settings": move them once. */
    private fun migrateKeys() {
        val old = KEY_NAMES.filter { p.contains(it) }
        if (old.isEmpty()) return
        val e = k.edit()
        old.forEach { n ->
            val v = p.getString(n, "").orEmpty()
            if (v.isNotBlank() || k.getString(n, "").isNullOrBlank()) e.putString(n, v)
        }
        e.commit()
        p.edit().apply { old.forEach { remove(it) } }.apply()
    }

    var claudeKey by key("claude_key")
    var openaiKey by key("openai_key")
    var deepseekKey by key("deepseek_key")
    var claudeModel by str("claude_model", "claude-opus-5-5")
    var openaiModel by str("openai_model", "gpt-5-mini")
    var deepseekModel by str("deepseek_model", "deepseek-chat")

    /** offline method: fast (ML Kit, on phone) | quality (on-phone AI model, next version) */
    var offlineEngine by str("offline_engine", "fast")
    /** auto | offline | claude | openai | deepseek */
    var mode by str("mode", "auto")
    var showBubble by bool("show_bubble", true)
    var volumeTrigger by bool("volume_trigger", true)
    /** user agreed to the accessibility-service disclosure */
    var a11yConsent by bool("a11y_consent", false)
    /** the service has connected at least once (so "not running" now means the phone stopped it) */
    var a11yWasOn by bool("a11y_was_on", false)
    /** run a quiet foreground notification so the phone doesn't kill the screen translator while asleep */
    var keepAlive by bool("keep_alive", true)
    var notifAsked by bool("notif_asked", false)
    var lastRepair by long("last_repair", 0L)
    /** show pinyin next to Chinese (text screen) and as the first step of the screen overlay */
    var showPinyin by bool("show_pinyin", true)
    /** first-run welcome / setup guide finished */
    var onboarded by bool("onboarded", false)
    /** After the instant offline result, also fetch a better online one. */
    var onlineEnhance by bool("online_enhance", true)

    var autoUpdate by bool("auto_update", true)
    var updateWifiOnly by bool("update_wifi_only", true)
    var updateStatus by str("update_status", "")
    var lastUpdateCheck by long("last_update_check", 0L)
    /** in-flight / downloaded update (system DownloadManager) */
    var updateDownloadId by long("update_dl_id", -1L)
    var pendingUpdateCode by int("pending_update_code", 0)
    var pendingUpdateName by str("pending_update_name", "")
    var pendingUpdateSha by str("pending_update_sha", "")
    var updateRetries by int("update_retries", 0)
    /** versionCode that last ran; a change clears the stale update status */
    var lastRunVersion by int("last_run_version", 0)

    /** colour palette id, see Themes */
    var theme by str("theme", "clay")
    /** 0 = auto-detect, 1 = EN → 中, 2 = 中 → EN */
    var dirMode by int("dir_mode", 0)
    /** The language the user reads: screens and photos are translated INTO it. "en" or "zh"; default from the phone's language. */
    var myLang by str("my_lang", if (java.util.Locale.getDefault().language == "zh") "zh" else "en")

    var bubbleX by int("bubble_x", 0)
    var bubbleY by int("bubble_y", 400)

    private fun key(n: String) = object : kotlin.properties.ReadWriteProperty<Any?, String> {
        override fun getValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>) = k.getString(n, "") ?: ""
        override fun setValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>, value: String) =
            k.edit().putString(n, value.trim()).apply()
    }

    companion object {
        const val KEYS_FILE = "keys"
        private val KEY_NAMES = listOf("claude_key", "openai_key", "deepseek_key")
    }

    private fun str(k: String, d: String) = object : kotlin.properties.ReadWriteProperty<Any?, String> {
        override fun getValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>) = p.getString(k, d) ?: d
        override fun setValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>, value: String) =
            p.edit().putString(k, value.trim()).apply()
    }
    private fun bool(k: String, d: Boolean) = object : kotlin.properties.ReadWriteProperty<Any?, Boolean> {
        override fun getValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>) = p.getBoolean(k, d)
        override fun setValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>, value: Boolean) =
            p.edit().putBoolean(k, value).apply()
    }
    private fun long(k: String, d: Long) = object : kotlin.properties.ReadWriteProperty<Any?, Long> {
        override fun getValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>) = p.getLong(k, d)
        override fun setValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>, value: Long) =
            p.edit().putLong(k, value).apply()
    }
    private fun int(k: String, d: Int) = object : kotlin.properties.ReadWriteProperty<Any?, Int> {
        override fun getValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>) = p.getInt(k, d)
        override fun setValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>, value: Int) =
            p.edit().putInt(k, value).apply()
    }
}
