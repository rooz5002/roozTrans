package com.rooz.translator

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.TileService

/** Quick Settings tile: pull down the shade, tap "Translate screen". */
class CaptureTileService : TileService() {
    override fun onClick() {
        val svc = TranslatorService.instance
        if (svc != null) { svc.captureFromTile(); return }
        // screen translator is off: go through the in-app disclosure, not straight to Android settings
        val i = Intent(this, SettingsActivity::class.java).putExtra(SettingsActivity.EXTRA_ENABLE_A11Y, true)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (Build.VERSION.SDK_INT >= 34)
            startActivityAndCollapse(PendingIntent.getActivity(this, 0, i, PendingIntent.FLAG_IMMUTABLE))
        else @Suppress("DEPRECATION") startActivityAndCollapse(i)
    }
}
