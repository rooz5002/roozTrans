package com.rooz.translator

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

/**
 * Turning on the screen translator (floating bubble), shared by Welcome and Settings.
 * Google Play "prominent disclosure": explain what the accessibility service does and get a clear
 * yes before sending the user to Android's Accessibility settings.
 */
fun AppCompatActivity.openAccessibilitySetup() {
    val prefs = Prefs(this)
    if (prefs.a11yConsent) { showAccessibilitySteps(); return }
    AlertDialog.Builder(this)
        .setTitle("roozTrans uses the Accessibility service")
        .setMessage(getString(R.string.a11y_disclosure))
        .setPositiveButton("Agree") { _, _ -> prefs.a11yConsent = true; showAccessibilitySteps() }
        .setNegativeButton("No thanks", null)
        .setCancelable(false)
        .show()
}

private fun AppCompatActivity.showAccessibilitySteps() {
    AlertDialog.Builder(this)
        .setTitle("Turn on screen translator")
        .setMessage("1. Tap \"Accessibility\" below → roozTrans screen translator → On → Allow. " +
            "(On some phones it is under Installed apps or Downloaded apps.)\n\n" +
            "If it is greyed out, or you see \"Restricted setting\" / \"App was denied access\":\n" +
            "2. Tap \"App info\" below → ⋮ (top right) → Allow restricted settings → confirm.\n" +
            "3. Come back and do step 1 again.\n\n" +
            "Samsung: if ⋮ has no such option, turn off Settings → Security and privacy → Auto Blocker first.\n\n" +
            "When it's on, the bubble appears at the edge of the screen. Drag it anywhere; tap it to translate.")
        .setPositiveButton("Accessibility") { _, _ -> KeepAlive.openA11y(this) }
        .setNeutralButton("App info") { _, _ ->
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
        }
        .show()
}

fun AppCompatActivity.openUrl(u: String) { runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(u))) } }

object Links {
    const val SOURCE = "https://github.com/rooz5002/roozTrans"
    // hosted on our own site so they keep working even if the repo goes private (Play needs a public privacy URL)
    const val TERMS = "https://rooz.com/rooztrans/terms.html"
    const val PRIVACY = "https://rooz.com/rooztrans/privacy.html"
    /** web contact form with the roozTrans topic preselected (mails support@rooz.com, no address published) */
    const val CONTACT = "https://rooz.com/?contact=roozTrans"
}
