package com.rooz.translator

import android.content.Context
import android.net.ConnectivityManager
import com.anthropic.client.AnthropicClient
import com.anthropic.client.okhttp.AnthropicOkHttpClient
import com.anthropic.models.messages.MessageCreateParams
import com.anthropic.models.messages.OutputConfig
import com.anthropic.models.messages.StopReason
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.time.Duration
import java.util.concurrent.TimeUnit

/** Picks an online AI service based on the country of the current IP, with fallback down the list. */
class Online(private val ctx: Context) {
    private val prefs = Prefs(ctx)

    companion object {
        private val http = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)   // blocked services in China usually die here
            .readTimeout(30, TimeUnit.SECONDS)
            .callTimeout(35, TimeUnit.SECONDS)
            .build()
        private val geoHttp = http.newBuilder().connectTimeout(3, TimeUnit.SECONDS).callTimeout(4, TimeUnit.SECONDS).build()
        private var cachedCountry: String? = null
        private var cachedAt = 0L
        private var cachedNet: String? = null
        private var claude: Pair<String, AnthropicClient>? = null
        private val JSON = "application/json".toMediaType()
    }

    data class Route(val country: String?, val providers: List<String>) {
        fun describe(): String {
            val c = when (country) { null -> "unknown"; "CN" -> "CN (mainland China)"; else -> country }
            return "IP country: $c → " + (if (providers.isEmpty()) "offline only (no API key set for this route)" else providers.joinToString(" → ") { niceName(it) })
        }
    }

    // ---------- where am I? ----------

    /** ISO country code of the current public IP, or null if no lookup succeeded. Cached 10 min per network. */
    suspend fun country(force: Boolean = false): String? {
        val net = (ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).activeNetwork?.toString()
        if (!force && cachedCountry != null && net == cachedNet && System.currentTimeMillis() - cachedAt < 10 * 60_000) return cachedCountry
        val found = lookupCountry()
        if (found != null) { cachedCountry = found; cachedAt = System.currentTimeMillis(); cachedNet = net }
        return found
    }

    private suspend fun lookupCountry(): String? = coroutineScope {
        val ch = Channel<String?>(4)
        // Several services in parallel; first answer wins. ipip.net is reachable inside China, the others outside.
        val jobs = listOf<suspend () -> String?>(
            { get("https://ipinfo.io/country")?.trim()?.takeIf { it.length == 2 } },
            { get("https://api.country.is/")?.let { JSONObject(it).optString("country") }?.takeIf { it.length == 2 } },
            { get("https://ipapi.co/country/")?.trim()?.takeIf { it.length == 2 } },
            { get("https://myip.ipip.net/")?.let { s ->
                if (s.contains("中国") && listOf("香港", "澳门", "台湾").none { s.contains(it) }) "CN"
                else if (s.contains("香港")) "HK" else null } },
        ).map { f -> async(Dispatchers.IO) { ch.send(runCatching { f() }.getOrNull()) } }
        var answer: String? = null
        withTimeoutOrNull(4500) {
            repeat(jobs.size) { val r = ch.receive(); if (r != null) { answer = r.uppercase(); return@withTimeoutOrNull } }
        }
        jobs.forEach { it.cancel() }
        answer
    }

    private fun get(url: String): String? =
        geoHttp.newCall(Request.Builder().url(url).header("User-Agent", "curl/8").build()).execute().use {
            if (it.isSuccessful) it.body?.string() else null
        }

    // ---------- which service? ----------

    private fun hasKey(p: String) = when (p) {
        "claude" -> prefs.claudeKey.isNotBlank(); "openai" -> prefs.openaiKey.isNotBlank()
        "deepseek" -> prefs.deepseekKey.isNotBlank(); else -> false
    }

    suspend fun route(force: Boolean = false): Route {
        val mode = prefs.mode
        if (mode == "offline") return Route(null, emptyList())
        // no API key at all: nothing could go online, so don't even look up the country (privacy)
        if (!force && listOf("claude", "openai", "deepseek").none(::hasKey)) return Route(null, emptyList())
        val c = country(force)
        val order = when {
            mode != "auto" -> listOf(mode)
            c == "CN" -> listOf("deepseek")                       // Claude/OpenAI are blocked in mainland China
            c == null -> listOf("deepseek", "claude", "openai")   // can't tell: try the one that works everywhere first
            else -> listOf("claude", "openai", "deepseek")
        }
        return Route(c, order.filter(::hasKey))
    }

    /** Translate a batch of strings online. Returns translations + the service that answered. Throws if all fail. */
    suspend fun translate(texts: List<String>, dir: Dir, route: Route? = null): Pair<List<String>, String> {
        val r = route ?: route()
        if (r.providers.isEmpty()) error("No online service available (offline mode, or no API key for this country)")
        var last: Throwable? = null
        for (p in r.providers) {
            try { return withContext(Dispatchers.IO) { callProvider(p, texts, dir) } to niceName(p) }
            catch (t: Throwable) { last = t }
        }
        throw last ?: IllegalStateException("all services failed")
    }

    suspend fun test(p: String): String = if (!hasKey(p)) "– ${niceName(p)}: no key set" else try {
        val t0 = System.currentTimeMillis()
        val out = withContext(Dispatchers.IO) { callProvider(p, listOf("Where is the nearest subway station?"), Dir.EN_ZH) }
        "✓ ${niceName(p)} (${System.currentTimeMillis() - t0} ms): ${out.first()}"
    } catch (t: Throwable) { "✗ ${niceName(p)}: ${t.message ?: t.javaClass.simpleName}" }

    // ---------- the three services ----------

    private fun systemPrompt(dir: Dir, n: Int) = """
        You are a translation engine inside a travel phone app. Translate each string in the JSON array from ${dir.srcName} to ${dir.dstName}.
        The strings were read by OCR from a phone screen or a photo (menus, signs, app screens, chat messages, tickets), so silently fix obvious OCR mistakes, keep numbers, prices, times and names, and keep the wording short and natural.
        If a string is already in ${dir.dstName}, or is only a number or symbol, return it unchanged.
        Reply with ONLY a JSON array of exactly $n strings in the same order, nothing else.
    """.trimIndent()

    private fun callProvider(p: String, texts: List<String>, dir: Dir): List<String> {
        val sys = systemPrompt(dir, texts.size)
        val user = JSONArray(texts).toString()
        val raw = when (p) {
            "claude" -> callClaude(sys, user)
            "openai" -> callOpenAiCompatible("https://api.openai.com/v1/chat/completions", prefs.openaiKey, prefs.openaiModel, sys, user)
            "deepseek" -> callOpenAiCompatible("https://api.deepseek.com/chat/completions", prefs.deepseekKey, prefs.deepseekModel, sys, user)
            else -> error("unknown provider $p")
        }
        return parseArray(raw, texts.size)
    }

    private fun callClaude(sys: String, user: String): String {
        val key = prefs.claudeKey
        val client = claude?.takeIf { it.first == key }?.second
            ?: AnthropicOkHttpClient.builder().apiKey(key).timeout(Duration.ofSeconds(30)).maxRetries(1).build()
                .also { claude = key to it }
        val params = MessageCreateParams.builder()
            .model(prefs.claudeModel)
            .maxTokens(8000L)
            .system(sys)
            .outputConfig(OutputConfig.builder().effort(OutputConfig.Effort.LOW).build())
            .addUserMessage(user)
            .build()
        val msg = client.messages().create(params)
        if (msg.stopReason().map { it == StopReason.REFUSAL }.orElse(false)) error("Claude declined this text")
        return msg.content().mapNotNull { b -> b.text().map { it.text() }.orElse(null) }.joinToString("")
    }

    private fun callOpenAiCompatible(url: String, key: String, model: String, sys: String, user: String): String {
        val body = JSONObject()
            .put("model", model)
            .put("messages", JSONArray()
                .put(JSONObject().put("role", "system").put("content", sys))
                .put(JSONObject().put("role", "user").put("content", user)))
        val req = Request.Builder().url(url)
            .header("Authorization", "Bearer $key")
            .post(body.toString().toRequestBody(JSON)).build()
        http.newCall(req).execute().use { resp ->
            val s = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) error("HTTP ${resp.code}: ${s.take(200)}")
            return JSONObject(s).getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content")
        }
    }

    private fun parseArray(raw: String, n: Int): List<String> {
        val a = raw.indexOf('['); val b = raw.lastIndexOf(']')
        if (a < 0 || b <= a) error("unexpected reply: ${raw.take(120)}")
        val arr = JSONArray(raw.substring(a, b + 1))
        if (arr.length() != n) error("got ${arr.length()} translations for $n texts")
        return List(n) { arr.optString(it) }
    }
}

fun niceName(p: String) = when (p) { "claude" -> "Claude"; "openai" -> "OpenAI"; "deepseek" -> "DeepSeek"; else -> p }
