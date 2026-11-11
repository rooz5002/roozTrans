package com.rooz.translator

import android.content.Context

/**
 * Offline first (instant, always works), then an online upgrade if allowed and reachable.
 * [onResult] may be called twice: once with the on-device result, once with the online one.
 */
class Engine(ctx: Context) {
    private val prefs = Prefs(ctx)
    val online = Online(ctx)

    suspend fun translate(
        texts: List<String>, dir: Dir, offlineOnly: Boolean = false,
        onResult: (List<String>, String) -> Unit, onError: (String) -> Unit,
    ) {
        if (texts.isEmpty()) return
        var gotOffline = false
        val ready = runCatching { Offline.modelsReady() }.getOrDefault(false)
        if (ready) {
            runCatching { Offline.translate(texts, dir) }
                .onSuccess { onResult(it, "Offline · on phone"); gotOffline = true }
                .onFailure { onError("Offline: ${it.message}") }
        }
        val wantOnline = !offlineOnly && prefs.mode != "offline" && (prefs.onlineEnhance || !gotOffline)
        if (!wantOnline) {
            if (!ready) onError("Offline language packs not downloaded yet — open roozTrans and tap Download (needs internet outside China).")
            return
        }
        runCatching { online.translate(texts, dir) }
            .onSuccess { (t, who) -> onResult(t, "$who ✓") }
            .onFailure { if (!gotOffline) onError("Online failed (${it.message}) and no offline packs.") }
    }
}
