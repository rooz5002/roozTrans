package com.rooz.translator

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

/**
 * Why the screen translator "loses its permission" after the phone sleeps for a long time:
 * battery savers (Samsung Device care, Huawei App launch / PowerGenie) kill the app's process while the
 * screen is off. Android then marks the accessibility service as crashed: Samsung keeps it ticked but it
 * no longer runs ("Not working") until it is toggled off and on; Huawei/Honor untick it altogether.
 * An app cannot re-enable it by itself, so we (1) ask for the battery exemptions that stop the kill,
 * and (2) notice the broken state and walk the user through the 10-second fix.
 */
object KeepAlive {
    enum class State { RUNNING, STUCK, OFF }

    private fun component(ctx: Context) = ComponentName(ctx, TranslatorService::class.java)

    /** Ticked in Android's Accessibility settings? (says nothing about whether it actually runs) */
    fun enabledInSettings(ctx: Context): Boolean {
        val list = Settings.Secure.getString(ctx.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
        val me = component(ctx)
        return list.split(':').any { ComponentName.unflattenFromString(it) == me }
    }

    fun state(ctx: Context): State = when {
        TranslatorService.instance != null -> State.RUNNING
        enabledInSettings(ctx) -> State.STUCK      // ticked, but Android stopped it (Samsung "Not working")
        else -> State.OFF
    }

    fun ignoringBattery(ctx: Context) =
        (ctx.getSystemService(Context.POWER_SERVICE) as PowerManager).isIgnoringBatteryOptimizations(ctx.packageName)

    val maker: String get() = Build.MANUFACTURER.lowercase()
    val isSamsung get() = maker.contains("samsung")
    val isHuawei get() = maker.contains("huawei") || maker.contains("honor")

    /** One-line status for Settings / Welcome. */
    fun statusLine(ctx: Context) = if (ignoringBattery(ctx)) "✅ Battery: roozTrans is allowed to keep running"
        else "⚠ Battery saver may stop the screen translator while the phone sleeps"

    /** "Unrestricted" battery for this app. Direct (non-Play) build asks in one tap; Play build opens the list. */
    fun requestBatteryExemption(a: AppCompatActivity) {
        val canAsk = a.checkSelfPermission("android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS") == PackageManager.PERMISSION_GRANTED
        val tries = buildList {
            if (canAsk) add(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${a.packageName}")))
            add(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            add(appInfo(a))
        }
        start(a, tries)
    }

    private fun appInfo(c: Context) = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${c.packageName}"))

    /** Huawei / Honor "App launch" screen (name differs across EMUI / HarmonyOS / MagicOS versions). */
    private fun huaweiAppLaunch() = listOf(
        ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"),
        ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity"),
        ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity"),
        ComponentName("com.hihonor.systemmanager", "com.hihonor.systemmanager.startupmgr.ui.StartupNormalAppListActivity"),
    ).map { Intent().setComponent(it) }

    private fun start(a: AppCompatActivity, tries: List<Intent>) {
        for (i in tries) if (runCatching { a.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess) return
    }

    /** Phone-specific steps so the system stops killing the translator. */
    fun showGuide(a: AppCompatActivity) {
        val steps = when {
            isSamsung -> "Samsung:\n" +
                "1. Tap \"Battery\" below → choose Unrestricted.\n" +
                "2. Settings → Battery → Background usage limits: make sure roozTrans is NOT in \"Sleeping apps\" or " +
                "\"Deep sleeping apps\", and add it to \"Never sleeping apps\".\n" +
                "3. Optional: in Recent apps, tap the roozTrans icon → \"Keep open\"."
            isHuawei -> "Huawei / Honor:\n" +
                "1. Tap \"App launch\" below → find roozTrans → turn OFF \"Manage automatically\" → turn ON " +
                "Auto-launch, Secondary launch and Run in background → OK.\n" +
                "2. Tap \"Battery\" below and allow roozTrans to ignore battery optimisation.\n" +
                "3. In Recent apps, swipe the roozTrans card down to lock it."
            else -> "1. Tap \"Battery\" below and set roozTrans to Unrestricted / Don't optimise.\n" +
                "2. If your phone has an \"Auto-start\" or \"Background activity\" setting, allow it for roozTrans."
        }
        val b = AlertDialog.Builder(a)
            .setTitle("Keep the screen translator on")
            .setMessage("Some phones' battery savers stop the screen translator after the phone has slept for a while, " +
                "and you have to turn it on again. This stops that.\n\n$steps")
            .setPositiveButton("Battery") { _, _ -> requestBatteryExemption(a) }
            .setNegativeButton("Close", null)
        if (isHuawei) b.setNeutralButton("App launch") { _, _ -> start(a, huaweiAppLaunch() + appInfo(a)) }
        else b.setNeutralButton("App info") { _, _ -> start(a, listOf(appInfo(a))) }
        b.show()
    }

    /** The service is ticked but not running (or was switched off by the phone): explain the quick fix. */
    fun showRepair(a: AppCompatActivity, st: State) {
        val fix = if (st == State.STUCK)
            "Android has stopped it, although it still looks switched on.\n\n" +
            "Tap \"Accessibility\" below → Installed apps → roozTrans screen translator → turn it OFF, then ON again."
        else "Your phone switched it off.\n\nTap \"Accessibility\" below → Installed apps → roozTrans screen translator → On."
        AlertDialog.Builder(a)
            .setTitle("Screen translator stopped")
            .setMessage("$fix\n\nThis usually happens when a battery saver closes roozTrans while the phone sleeps. " +
                "After fixing it, tap \"Keep it on\" so it doesn't happen again.")
            .setPositiveButton("Accessibility") { _, _ -> start(a, listOf(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))) }
            .setNeutralButton("Keep it on") { _, _ -> showGuide(a) }
            .setNegativeButton("Later", null)
            .show()
    }
}
