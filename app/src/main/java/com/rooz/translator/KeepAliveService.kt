package com.rooz.translator

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder

/**
 * Why this exists: Android binds the accessibility service "foreground only while awake". Once the screen
 * has been off for a while the process drops to background priority and battery savers kill it — and a
 * killed accessibility service is treated as *crashed*: it is not restarted until the user toggles it.
 *
 * A real foreground service (quiet notification) in the same process keeps the process at foreground
 * priority while the phone sleeps. START_STICKY also brings us back if we are killed anyway, so we can
 * repair the service (when allowed, see [KeepAlive.selfRepair]) or tell the user straight away.
 */
class KeepAliveService : Service() {

    companion object {
        private const val CH_RUN = "running"
        private const val CH_ALERT = "stopped"
        private const val ID_RUN = 1
        private const val ID_ALERT = 2
        private const val ACTION_CAPTURE = "capture"

        fun start(ctx: Context) {
            if (!Prefs(ctx).keepAlive) return
            runCatching { ctx.startForegroundService(Intent(ctx, KeepAliveService::class.java)) }
                .onFailure { android.util.Log.w("roozTrans", "keep-alive not started", it) }
        }
        fun stop(ctx: Context) { runCatching { ctx.stopService(Intent(ctx, KeepAliveService::class.java)) } }

        private fun channels(ctx: Context) {
            val nm = ctx.getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(NotificationChannel(CH_RUN, "Screen translator running", NotificationManager.IMPORTANCE_MIN).apply {
                description = "Keeps the screen translator ready while the phone sleeps. You can hide this channel."
                setShowBadge(false)
            })
            nm.createNotificationChannel(NotificationChannel(CH_ALERT, "Screen translator stopped", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Tells you when the phone has stopped the screen translator."
            })
        }

        /** "Your phone stopped the screen translator — tap to fix" (no-op without notification permission). */
        fun notifyStopped(ctx: Context) {
            channels(ctx)
            val open = PendingIntent.getActivity(ctx, 1, Intent(ctx, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), PendingIntent.FLAG_IMMUTABLE)
            val n = Notification.Builder(ctx, CH_ALERT)
                .setSmallIcon(R.drawable.ic_bubble)
                .setContentTitle("Screen translator stopped")
                .setContentText("Your phone stopped it while sleeping. Tap to fix.")
                .setContentIntent(open).setAutoCancel(true).build()
            runCatching { ctx.getSystemService(NotificationManager::class.java).notify(ID_ALERT, n) }
        }
        fun clearStopped(ctx: Context) { runCatching { ctx.getSystemService(NotificationManager::class.java).cancel(ID_ALERT) } }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_CAPTURE) TranslatorService.instance?.captureFromTile()
        val ok = runCatching { goForeground() }.isSuccess   // can be refused when restarted in the background
        if (TranslatorService.instance == null) {
            // restarted after the process was killed: the accessibility service is now "crashed"
            val prefs = Prefs(this)
            if (prefs.a11yWasOn) {
                if (!KeepAlive.selfRepair(this)) notifyStopped(this)
            }
            if (!ok || !KeepAlive.canSelfRepair(this)) { stopSelf(); return START_NOT_STICKY }
        }
        return if (ok) START_STICKY else START_NOT_STICKY.also { stopSelf() }
    }

    private fun goForeground() {
        channels(this)
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), PendingIntent.FLAG_IMMUTABLE)
        val cap = PendingIntent.getService(this, 2, Intent(this, KeepAliveService::class.java).setAction(ACTION_CAPTURE),
            PendingIntent.FLAG_IMMUTABLE)
        val n = Notification.Builder(this, CH_RUN)
            .setSmallIcon(R.drawable.ic_bubble)
            .setContentTitle("Screen translator ready")
            .setContentText("Tap \"Translate screen\", the bubble, or double-press Volume Down")
            .setContentIntent(open)
            .addAction(Notification.Action.Builder(null, "Translate screen", cap).build())
            .setOngoing(true).setShowWhen(false)
            .build()
        if (Build.VERSION.SDK_INT >= 34) startForeground(ID_RUN, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        else startForeground(ID_RUN, n)
        // startForeground() does not re-show a notification that was hidden because notification permission was
        // missing when it was first posted (first run); posting it again by id does.
        runCatching { getSystemService(NotificationManager::class.java).notify(ID_RUN, n) }
    }
}
