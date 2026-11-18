package com.rooz.translator

import android.icu.text.Transliterator

/**
 * Chinese characters → pinyin with tone marks (你好 → nǐ hǎo), fully offline.
 * Uses the ICU "Han-Latin" transliterator built into Android (nothing added to the APK), which knows one
 * reading per character. Characters with several readings (行, 了, 长, 重, 还 …) are fixed by [words]:
 * the longest matching common word wins, everything else goes character by character through ICU.
 */
object Pinyin {
    private val tr by lazy { Transliterator.getInstance("Han-Latin") }
    // Chinese punctuation → its plain equivalent, so pinyin lines read naturally
    private val punct = mapOf('，' to ",", '。' to ".", '！' to "!", '？' to "?", '：' to ":", '；' to ";",
        '、' to ",", '（' to "(", '）' to ")", '“' to "\"", '”' to "\"", '《' to "«", '》' to "»", '…' to "…")

    /** Common words whose characters have a different reading from ICU's default (travel/daily-life vocabulary). */
    private val words: Map<String, String> = """
        银行 yín háng|行长 háng zhǎng|行业 háng yè|一行 yì háng|同行 tóng háng|内行 nèi háng|外行 wài háng|商行 shāng háng|车行 chē háng
        行李 xíng li|自行车 zì xíng chē|旅行 lǚ xíng|行人 xíng rén|不行 bù xíng|可行 kě xíng|步行 bù xíng|银行卡 yín háng kǎ
        了解 liǎo jiě|不了 bù liǎo|受不了 shòu bù liǎo|好了 hǎo le|算了 suàn le|太好了 tài hǎo le|为了 wèi le|除了 chú le|到了 dào le|走了 zǒu le|来了 lái le|吃了 chī le|知道了 zhī dào le
        长大 zhǎng dà|长官 zhǎng guān|校长 xiào zhǎng|市长 shì zhǎng|部长 bù zhǎng|家长 jiā zhǎng|成长 chéng zhǎng|生长 shēng zhǎng|长城 cháng chéng|长江 cháng jiāng|长沙 cháng shā
        重庆 chóng qìng|重新 chóng xīn|重复 chóng fù|重要 zhòng yào|严重 yán zhòng|体重 tǐ zhòng
        还是 hái shi|还有 hái yǒu|还要 hái yào|还没 hái méi|还好 hái hǎo|还钱 huán qián|归还 guī huán|退还 tuì huán|还书 huán shū
        首都 shǒu dū|成都 chéng dū|都市 dū shì|京都 jīng dū
        音乐 yīn yuè|乐器 yuè qì|快乐 kuài lè|乐趣 lè qù|可乐 kě lè
        觉得 jué de|睡觉 shuì jiào|午觉 wǔ jiào|感觉 gǎn jué|觉醒 jué xǐng
        得到 dé dào|获得 huò dé|记得 jì de|觉得 jué de|值得 zhí de|晓得 xiǎo de|得去 děi qù|得走 děi zǒu
        便宜 pián yi|方便 fāng biàn|顺便 shùn biàn|随便 suí biàn|大便 dà biàn|小便 xiǎo biàn
        厦门 xià mén|大厦 dà shà|大夫 dài fu|地方 dì fang|朋友 péng you|东西 dōng xi|衣服 yī fu|事情 shì qing|名字 míng zi|时候 shí hou|先生 xiān sheng|喜欢 xǐ huan|谢谢 xiè xie|认识 rèn shi|知道 zhī dào|学生 xué sheng|休息 xiū xi|漂亮 piào liang|明白 míng bai|清楚 qīng chu|舒服 shū fu|麻烦 má fan|客气 kè qi|告诉 gào su|关系 guān xi|意思 yì si|消息 xiāo xi|热闹 rè nao|窗户 chuāng hu|豆腐 dòu fu|馒头 mán tou|包子 bāo zi|饺子 jiǎo zi|桌子 zhuō zi|椅子 yǐ zi|孩子 hái zi|儿子 ér zi|妻子 qī zi|妈妈 mā ma|爸爸 bà ba|哥哥 gē ge|姐姐 jiě jie|弟弟 dì di|妹妹 mèi mei|爷爷 yé ye|奶奶 nǎi nai
        着急 zháo jí|睡着 shuì zháo|看着 kàn zhe|拿着 ná zhe|等着 děng zhe|着火 zháo huǒ|着凉 zháo liáng
        为什么 wèi shén me|因为 yīn wèi|为了 wèi le|什么 shén me|怎么 zěn me|那么 nà me|这么 zhè me|多少 duō shao|少年 shào nián
        和平 hé píng|暖和 nuǎn huo|应该 yīng gāi|答应 dā ying|相应 xiāng yìng|回答 huí dá|答案 dá àn
        的确 dí què|目的 mù dì|的士 dí shì|打的 dǎ dí|一个 yí ge|这个 zhè ge|那个 nà ge|哪个 nǎ ge|几个 jǐ ge|两个 liǎng ge|一样 yí yàng|一定 yí dìng|一下 yí xià|一共 yí gòng|一半 yí bàn|一次 yí cì|一起 yì qǐ|一点 yì diǎn|一些 yì xiē|一天 yì tiān|一直 yì zhí|一般 yì bān
        不要 bú yào|不是 bú shì|不对 bú duì|不用 bú yòng|不会 bú huì|不客气 bú kè qi|不错 bú cuò|不过 bú guò|不见 bú jiàn|不去 bú qù|不在 bú zài|不认识 bú rèn shi
        干净 gān jìng|干杯 gān bēi|饼干 bǐng gān|干活 gàn huó|干什么 gàn shén me|能干 néng gàn
        数学 shù xué|数字 shù zì|数一数 shǔ yi shǔ|发现 fā xiàn|头发 tóu fa|理发 lǐ fà|发票 fā piào|出发 chū fā
        差不多 chà bu duō|出差 chū chāi|差别 chā bié|参加 cān jiā|人参 rén shēn|中奖 zhòng jiǎng|看中 kàn zhòng|调查 diào chá|空调 kōng tiáo|强调 qiáng diào
        银行行长 yín háng háng zhǎng|行不行 xíng bu xíng|好不好 hǎo bu hǎo|是不是 shì bu shì|对不起 duì bu qǐ|看不见 kàn bu jiàn|听不懂 tīng bu dǒng|来不及 lái bu jí
        乘车 chéng chē|乘客 chéng kè|地铁 dì tiě|车站 chē zhàn|火车站 huǒ chē zhàn|机场 jī chǎng|广场 guǎng chǎng|商场 shāng chǎng|市场 shì chǎng|停车场 tíng chē chǎng
        厕所 cè suǒ|卫生间 wèi shēng jiān|中间 zhōng jiān|房间 fáng jiān|时间 shí jiān|空间 kōng jiān|没空 méi kòng|有空 yǒu kòng|空儿 kòngr
        处理 chǔ lǐ|到处 dào chù|好处 hǎo chù|处方 chǔ fāng|传说 chuán shuō|自传 zì zhuàn|角色 jué sè|饺子馆 jiǎo zi guǎn|模样 mú yàng|模型 mó xíng
    """.trim().split('|', '\n').map { it.trim() }.filter { ' ' in it }
        .associate { it.substringBefore(' ') to it.substringAfter(' ') }
    private val maxWord = words.keys.maxOf { it.length }

    private fun isHan(c: Char) = Character.UnicodeScript.of(c.code) == Character.UnicodeScript.HAN
    fun has(s: String?) = s != null && s.any(::isHan)

    /** Pinyin for the whole string; non-Chinese parts (numbers, Latin words) are kept as they are. */
    fun of(s: String?): String? {
        if (s == null || !has(s)) return null
        val out = StringBuilder(s.length * 4)
        fun word(w: String) { if (out.isNotEmpty() && out.last() != ' ' && out.last() != '(' && out.last() != '"') out.append(' '); out.append(w) }
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (isHan(c)) {
                var hit: String? = null; var len = 0
                for (n in minOf(maxWord, s.length - i) downTo 2) { words[s.substring(i, i + n)]?.let { hit = it; len = n }; if (hit != null) break }
                if (hit != null) { word(hit!!); i += len; continue }
                word(synchronized(tr) { tr.transliterate(c.toString()) }); i++
                continue
            }
            val p = punct[c]
            when {
                p != null -> { out.append(p); if (c !in "（“《") out.append(' ') }   // no space after an opening bracket/quote
                c.isWhitespace() -> if (out.isNotEmpty() && out.last() != ' ') out.append(' ')
                else -> {   // Latin letters / digits: copy the whole run
                    val j = (i until s.length).firstOrNull { k -> isHan(s[k]) || s[k] in punct || s[k].isWhitespace() } ?: s.length
                    word(s.substring(i, j)); i = j; continue
                }
            }
            i++
        }
        // OCR often gives ASCII punctuation with spaces around it: no space before , . ! ? : ; )
        return out.toString().replace(Regex("\\s+"), " ").replace(Regex(" ([,.!?:;)])"), "$1").trim()
    }
}
