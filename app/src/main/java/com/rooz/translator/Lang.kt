package com.rooz.translator

enum class Dir(val srcName: String, val dstName: String, val label: String) {
    EN_ZH("English", "Simplified Chinese", "EN → 中"),
    ZH_EN("Chinese", "English", "中 → EN");
}

object Lang {
    private fun isHan(c: Char) = Character.UnicodeScript.of(c.code) == Character.UnicodeScript.HAN
    private fun isLatin(c: Char) = c in 'a'..'z' || c in 'A'..'Z'

    /** Weighted counts: one Han character carries roughly a word's worth of meaning. */
    fun score(s: String): Pair<Int, Int> {
        var han = 0; var latin = 0
        for (c in s) { if (isHan(c)) han++ else if (isLatin(c)) latin++ }
        return han * 3 to latin
    }

    fun isChinese(s: String): Boolean { val (h, l) = score(s); return h > 0 && h >= l }
    fun hasLetters(s: String): Boolean { val (h, l) = score(s); return h + l > 0 }

    fun detect(s: String): Dir = if (isChinese(s)) Dir.ZH_EN else Dir.EN_ZH

    /** For a whole screen/photo: which language dominates. */
    fun dominant(texts: List<String>): Dir {
        var h = 0; var l = 0
        texts.forEach { val (a, b) = score(it); h += a; l += b }
        return if (h >= l && h > 0) Dir.ZH_EN else Dir.EN_ZH
    }

    /**
     * For a screen or photo: translate whatever is NOT in the user's language into it (an English reader on a mostly
     * English page with a few Chinese parts gets just those parts in English). Only if nothing on screen is foreign
     * does it go the other way, e.g. to show an English page in Chinese.
     */
    fun forScreen(texts: List<String>, myLang: String): Dir {
        val into = if (myLang == "zh") Dir.EN_ZH else Dir.ZH_EN
        val other = if (into == Dir.ZH_EN) Dir.EN_ZH else Dir.ZH_EN
        return when {
            texts.any { needs(it, into) } -> into
            texts.any { needs(it, other) } -> other
            else -> into
        }
    }

    /** Does this block belong to the source language of [dir] (and so need translating)? */
    fun needs(s: String, dir: Dir): Boolean =
        hasLetters(s) && (if (dir == Dir.ZH_EN) score(s).first > 0 else isChinese(s).not())
}
