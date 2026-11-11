package com.rooz.translator

import android.app.DownloadManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.net.ConnectivityManager
import android.os.Build
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/**
 * Self-update from your own server (deploy.properties → updateUrl).
 * First self-update needs one "Update" tap; after that roozTrans is its own installer of record
 * and Android 12+ lets it update silently.
 */
object Updater {
    /** Off when no update server is configured (e.g. someone building from the public source). */
    val ENABLED = BuildConfig.UPDATE_URL.isNotBlank()
    private val URL = BuildConfig.UPDATE_URL
    private const val CHECK_EVERY_MS = 6 * 3600_000L
    private val http = OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).readTimeout(60, TimeUnit.SECONDS).build()
    private val lock = Mutex()
    private const val APK_NAME = "roozTrans-update.apk"

    data class Info(val code: Int, val name: String, val sha256: String, val size: Long)

    /** Cheap to call often: only hits the network every 6 h, and only if auto-update is on. */
    suspend fun autoCheck(ctx: Context) {
        if (!ENABLED) return
        val p = Prefs(ctx)
        clearStaleStatus(ctx)
        // an update that finished downloading while we were away gets installed now, in the foreground
        runCatching { if (installPendingIfReady(ctx)) return }
        if (!p.autoUpdate || System.currentTimeMillis() - p.lastUpdateCheck < CHECK_EVERY_MS) return
        if (p.updateWifiOnly && isMetered(ctx)) return
        p.lastUpdateCheck = System.currentTimeMillis()
        runCatching { checkAndInstall(ctx, manual = false) }.onFailure { p.updateStatus = "Last check failed: ${it.message}" }
    }

    /**
     * Returns a human-readable status. The APK itself is fetched by Android's DownloadManager,
     * so it survives the app going to the background / screen locking (OkHttp got
     * "Software caused connection abort" on Samsung), shows progress, and retries on drops.
     */
    suspend fun checkAndInstall(ctx: Context, manual: Boolean = true): String = lock.withLock {
        val p = Prefs(ctx)
        val info = withContext(Dispatchers.IO) { fetchInfo() }
        if (info.code <= BuildConfig.VERSION_CODE) {
            cleanup(ctx)
            return "Up to date (v${BuildConfig.VERSION_NAME})".also { p.updateStatus = it }
        }
        if (!ctx.packageManager.canRequestPackageInstalls())
            return "v${info.name} available — allow \"Install unknown apps\" for roozTrans first".also { p.updateStatus = it }
        // already downloaded and intact?
        if (p.pendingUpdateCode == info.code && p.pendingUpdateSha.equals(info.sha256, true) &&
            withContext(Dispatchers.IO) { verified(ctx, info.sha256) }) {
            withContext(Dispatchers.IO) { install(ctx, apkFile(ctx)) }
            return "Installing v${info.name}…".also { p.updateStatus = it }
        }
        val dm = ctx.getSystemService(DownloadManager::class.java)
        if (p.pendingUpdateCode == info.code && p.pendingUpdateSha.equals(info.sha256, true) && isRunning(dm, p.updateDownloadId))
            return "Downloading v${info.name}… (see notifications)"
        if (p.updateDownloadId != -1L) dm.remove(p.updateDownloadId)
        apkFile(ctx).delete()
        p.pendingUpdateCode = info.code; p.pendingUpdateName = info.name; p.pendingUpdateSha = info.sha256
        p.updateRetries = 0
        enqueue(ctx, info.name, allowMetered = manual || !p.updateWifiOnly)
        "Downloading v${info.name}… (see notifications — it installs when done)".also { p.updateStatus = it }
    }

    private fun enqueue(ctx: Context, name: String, allowMetered: Boolean) {
        val p = Prefs(ctx)
        val req = DownloadManager.Request(Uri.parse("$URL?t=${BuildConfig.UPDATE_TOKEN}&get=1"))
            .setTitle("roozTrans v$name")
            .setDescription("Downloading update")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            .setDestinationInExternalFilesDir(ctx, null, APK_NAME)
            .setAllowedOverMetered(allowMetered)
            .setAllowedOverRoaming(false)
        p.updateDownloadId = ctx.getSystemService(DownloadManager::class.java).enqueue(req)
    }

    /** Called by UpdateDownloadReceiver when the system download finishes. */
    fun onDownloadComplete(ctx: Context, id: Long) {
        val p = Prefs(ctx)
        if (id != p.updateDownloadId) return
        val dm = ctx.getSystemService(DownloadManager::class.java)
        val (status, reason) = dm.query(DownloadManager.Query().setFilterById(id)).use { c ->
            if (!c.moveToFirst()) return
            c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)) to c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
        }
        if (status != DownloadManager.STATUS_SUCCESSFUL) {
            // dropped connection etc.: start again a few times before giving up
            if (p.updateRetries < 3) {
                p.updateRetries = p.updateRetries + 1
                dm.remove(id); apkFile(ctx).delete()
                p.updateStatus = "Connection dropped (code $reason) — retrying (${p.updateRetries}/3)…"
                enqueue(ctx, p.pendingUpdateName, allowMetered = !p.updateWifiOnly)
                return
            }
            p.updateStatus = "Download failed (code $reason) — tap Check again"; p.updateDownloadId = -1L; return
        }
        if (!verified(ctx, p.pendingUpdateSha)) {
            apkFile(ctx).delete(); p.updateDownloadId = -1L
            p.updateStatus = "Download corrupted (checksum mismatch) — tap Check again"; return
        }
        p.updateStatus = "Installing v${p.pendingUpdateName}…"
        install(ctx, apkFile(ctx))
    }

    /** If a verified newer APK is already on disk, install it. */
    private suspend fun installPendingIfReady(ctx: Context): Boolean {
        val p = Prefs(ctx)
        if (p.pendingUpdateCode <= BuildConfig.VERSION_CODE) { cleanup(ctx); return false }
        val dm = ctx.getSystemService(DownloadManager::class.java)
        if (isRunning(dm, p.updateDownloadId) || !apkFile(ctx).exists()) return false
        if (!withContext(Dispatchers.IO) { verified(ctx, p.pendingUpdateSha) }) return false
        withContext(Dispatchers.IO) { install(ctx, apkFile(ctx)) }
        return true
    }

    /** After an update (or manual reinstall) the saved status ("Up to date (v1.4)") is stale. */
    fun clearStaleStatus(ctx: Context) {
        val p = Prefs(ctx)
        if (p.lastRunVersion != BuildConfig.VERSION_CODE) { p.updateStatus = ""; p.lastRunVersion = BuildConfig.VERSION_CODE }
    }

    private fun cleanup(ctx: Context) {
        val p = Prefs(ctx)
        if (p.pendingUpdateCode == 0 && p.updateDownloadId == -1L) return
        apkFile(ctx).delete(); File(ctx.cacheDir, "update.apk").delete()
        p.pendingUpdateCode = 0; p.updateDownloadId = -1L
    }

    private fun isRunning(dm: DownloadManager, id: Long): Boolean {
        if (id == -1L) return false
        dm.query(DownloadManager.Query().setFilterById(id)).use { c ->
            if (!c.moveToFirst()) return false
            val st = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            return st == DownloadManager.STATUS_PENDING || st == DownloadManager.STATUS_RUNNING || st == DownloadManager.STATUS_PAUSED
        }
    }

    private fun apkFile(ctx: Context) = File(ctx.getExternalFilesDir(null), APK_NAME)

    private fun verified(ctx: Context, sha: String): Boolean {
        val f = apkFile(ctx); if (!f.exists() || sha.isEmpty()) return false
        val md = MessageDigest.getInstance("SHA-256")
        f.inputStream().use { i -> val b = ByteArray(256 * 1024); while (true) { val n = i.read(b); if (n < 0) break; md.update(b, 0, n) } }
        return md.digest().joinToString("") { "%02x".format(it) }.equals(sha, ignoreCase = true)
    }

    private fun fetchInfo(): Info {
        val req = Request.Builder().url("$URL?t=${BuildConfig.UPDATE_TOKEN}").build()
        http.newCall(req).execute().use { r ->
            if (!r.isSuccessful) error("server said ${r.code}")
            val j = JSONObject(r.body!!.string())
            return Info(j.getInt("versionCode"), j.getString("versionName"), j.getString("sha256"), j.getLong("size"))
        }
    }

    private fun install(ctx: Context, apk: File) {
        val pi = ctx.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            setAppPackageName(ctx.packageName)
            setSize(apk.length())
            if (Build.VERSION.SDK_INT >= 31) setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
            if (Build.VERSION.SDK_INT >= 34) setRequestUpdateOwnership(true)
        }
        val id = pi.createSession(params)
        pi.openSession(id).use { s ->
            s.openWrite("base.apk", 0, apk.length()).use { out -> apk.inputStream().use { it.copyTo(out) }; s.fsync(out) }
            val cb = PendingIntent.getBroadcast(ctx, id, Intent(ctx, UpdateReceiver::class.java),
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            s.commit(cb.intentSender)
        }
    }

    private fun isMetered(ctx: Context) =
        (ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).isActiveNetworkMetered
}

/** System DownloadManager says the update APK is done: verify + install off the main thread. */
class UpdateDownloadReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
        val app = ctx.applicationContext
        if (id == -1L || id != Prefs(app).updateDownloadId) return
        val pending = goAsync()
        Thread {
            try { runCatching { Updater.onDownloadComplete(app, id) }.onFailure { Prefs(app).updateStatus = "Update failed: ${it.message}" } }
            finally { pending.finish() }
        }.start()
    }
}

/** Gets the installer's answer. First time round Android wants the user to confirm. */
class UpdateReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        when (val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -999)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val confirm = if (Build.VERSION.SDK_INT >= 33) intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                              else @Suppress("DEPRECATION") intent.getParcelableExtra(Intent.EXTRA_INTENT)
                confirm?.let { runCatching { ctx.startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } }
            }
            PackageInstaller.STATUS_SUCCESS -> Prefs(ctx).updateStatus = "Updated"
            else -> {
                val msg = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE) ?: "status $status"
                Prefs(ctx).updateStatus = "Update failed: $msg"
                Toast.makeText(ctx, "roozTrans update failed: $msg", Toast.LENGTH_LONG).show()
            }
        }
    }
}
