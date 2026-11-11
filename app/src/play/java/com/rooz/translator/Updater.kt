package com.rooz.translator

import android.content.Context

/** Google Play build: Play delivers updates, so the self-updater is a no-op. */
object Updater {
    const val ENABLED = false
    suspend fun autoCheck(ctx: Context) {}
    fun clearStaleStatus(ctx: Context) {}
    suspend fun checkAndInstall(ctx: Context, manual: Boolean = true): String = "Updates come from Google Play"
}
