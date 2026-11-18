package com.rooz.translator

import android.content.Context
import android.content.SharedPreferences

/** All settings live in the app's private SharedPreferences (never leaves the phone). */
class Prefs(ctx: Context) {
    private val p: SharedPreferences = ctx.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var claudeKey by str("claude_key", "")
    var openaiKey by str("openai_key", "")
    var deepseekKey by str("deepseek_key", "")
    var claudeModel by str("claude_model", "claude-opus-5-5")
    var openaiModel by str("openai_model", "gpt-5-mini")
    var deepseekModel by str("deepseek_model", "deepseek-chat")

    /** auto | offline | claude | openai | deepseek */
    var mode by str("mode", "auto")
    var showBubble by bool("show_bubble", true)
    var volumeTrigger by bool("volume_trigger", true)
    /** user agreed to the accessibility-service disclosure */
    var a11yConsent by bool("a11y_consent", false)
    /** the service has connected at least once (so "not running" now means the phone stopped it) */
    var a11yWasOn by bool("a11y_was_on", false)
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

    var bubbleX by int("bubble_x", 0)
    var bubbleY by int("bubble_y", 400)

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
