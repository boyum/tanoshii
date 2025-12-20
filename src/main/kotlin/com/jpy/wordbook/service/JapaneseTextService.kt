package com.jpy.wordbook.service

import com.atilika.kuromoji.ipadic.Token
import com.atilika.kuromoji.ipadic.Tokenizer
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory

data class WordToken(
    val surface: String,
    val romaji: String,
    val furiganaHtml: String,  // HTML with ruby tags for kanji
    val meaning: String? = null  // English meaning if available
)

data class SentenceWithTokens(
    val tokens: List<WordToken>
)

@Singleton
class JapaneseTextService {
    private val logger = LoggerFactory.getLogger(JapaneseTextService::class.java)
    private val tokenizer = Tokenizer()

    // Comprehensive fallback dictionary for common words, particles, and grammar
    private val commonWordsDictionary = mapOf(
        // Particles
        "は" to "topic marker (wa)",
        "が" to "subject marker (ga)",
        "を" to "object marker (wo/o)",
        "に" to "at, to, in (ni)",
        "で" to "at, by, with (de)",
        "と" to "and, with (to)",
        "も" to "also, too (mo)",
        "の" to "possessive (no)",
        "へ" to "to, toward (e)",
        "から" to "from, since",
        "まで" to "until, to",
        "より" to "than, from",
        "や" to "and (non-exhaustive)",
        "か" to "question marker",
        "ね" to "sentence-final (confirmation)",
        "よ" to "sentence-final (emphasis)",
        "な" to "sentence-final (don't)",
        "ば" to "if, when",
        "けど" to "but, however",
        "けれど" to "but, however",
        "けれども" to "but, however",
        "だけ" to "only, just",
        "しか" to "only (with negative)",
        "ほど" to "extent, degree",
        "ぐらい" to "about, approximately",
        "くらい" to "about, approximately",
        "など" to "et cetera",
        "さえ" to "even",
        "こそ" to "emphasis",
        "ずつ" to "each, apiece",
        "ながら" to "while doing",
        "たり" to "and/or (listing)",

        // Copula and auxiliaries
        "だ" to "is, are (da)",
        "です" to "is, are (polite)",
        "である" to "is, are (formal)",
        "でした" to "was, were (polite)",
        "じゃない" to "is not",
        "ではない" to "is not (formal)",
        "だった" to "was, were",
        "ます" to "polite verb ending",
        "ました" to "polite past verb",
        "ません" to "polite negative",
        "ませんでした" to "polite past negative",

        // Common verbs (base forms)
        "する" to "to do",
        "いる" to "to be (animate)",
        "ある" to "to be (inanimate), to have",
        "なる" to "to become",
        "くる" to "to come",
        "いく" to "to go",
        "見る" to "to see, to look",
        "聞く" to "to hear, to listen, to ask",
        "言う" to "to say",
        "思う" to "to think",
        "知る" to "to know",
        "わかる" to "to understand",
        "できる" to "can do, to be able",
        "たべる" to "to eat",
        "のむ" to "to drink",
        "かく" to "to write",
        "よむ" to "to read",
        "はなす" to "to speak",
        "おしえる" to "to teach",
        "ならう" to "to learn",
        "つかう" to "to use",
        "つくる" to "to make",
        "はじめる" to "to begin",
        "おわる" to "to end",

        // Common adjectives
        "いい" to "good",
        "よい" to "good",
        "わるい" to "bad",
        "おおきい" to "big",
        "ちいさい" to "small",
        "あたらしい" to "new",
        "ふるい" to "old",
        "たかい" to "expensive, high, tall",
        "やすい" to "cheap",
        "おいしい" to "delicious",
        "まずい" to "bad-tasting",
        "たのしい" to "fun, enjoyable",
        "むずかしい" to "difficult",
        "やさしい" to "easy, kind",
        "あつい" to "hot",
        "さむい" to "cold",
        "あたたかい" to "warm",
        "すずしい" to "cool",
        "いそがしい" to "busy",

        // Na-adjectives
        "きれい" to "pretty, clean",
        "しずか" to "quiet",
        "にぎやか" to "lively",
        "べんり" to "convenient",
        "ふべん" to "inconvenient",
        "すき" to "like, favorite",
        "きらい" to "dislike",
        "じょうず" to "skillful, good at",
        "へた" to "poor at, unskillful",
        "げんき" to "healthy, energetic",
        "ひま" to "free (time)",
        "たいせつ" to "important",
        "ひつよう" to "necessary",
        "かんたん" to "simple, easy",
        "ふくざつ" to "complex",
        "ゆうめい" to "famous",
        "しんせつ" to "kind",
        "しあわせ" to "happy",

        // Pronouns
        "私" to "I, me",
        "わたし" to "I, me",
        "僕" to "I, me (masculine)",
        "ぼく" to "I, me (masculine)",
        "俺" to "I, me (very masculine)",
        "おれ" to "I, me (very masculine)",
        "あなた" to "you",
        "彼" to "he",
        "かれ" to "he",
        "彼女" to "she",
        "かのじょ" to "she",
        "これ" to "this",
        "それ" to "that",
        "あれ" to "that (over there)",
        "どれ" to "which",
        "ここ" to "here",
        "そこ" to "there",
        "あそこ" to "over there",
        "どこ" to "where",
        "こちら" to "this way, here (polite)",
        "そちら" to "that way, there (polite)",
        "あちら" to "that way, over there (polite)",
        "どちら" to "which way, where (polite)",

        // Question words
        "何" to "what",
        "なに" to "what",
        "なん" to "what",
        "誰" to "who",
        "だれ" to "who",
        "いつ" to "when",
        "どこ" to "where",
        "なぜ" to "why",
        "どう" to "how",
        "どうして" to "why, how come",
        "どんな" to "what kind of",
        "いくつ" to "how many, how old",
        "いくら" to "how much",

        // Numbers
        "一" to "one",
        "二" to "two",
        "三" to "three",
        "四" to "four",
        "五" to "five",
        "六" to "six",
        "七" to "seven",
        "八" to "eight",
        "九" to "nine",
        "十" to "ten",
        "百" to "hundred",
        "千" to "thousand",
        "万" to "ten thousand",

        // Conjunctions
        "そして" to "and then",
        "それから" to "and then, after that",
        "だから" to "therefore, so",
        "でも" to "but, however",
        "しかし" to "however",
        "または" to "or",
        "あるいは" to "or, alternatively",
        "もし" to "if",
        "それで" to "so, therefore",
        "すると" to "then, thereupon",
        "ところが" to "however, but",
        "つまり" to "in other words",
        "たとえば" to "for example",

        // Adverbs
        "とても" to "very",
        "すごく" to "very, extremely",
        "ちょっと" to "a little",
        "もう" to "already, anymore",
        "まだ" to "still, not yet",
        "また" to "again, also",
        "もっと" to "more",
        "いちばん" to "most, best",
        "たくさん" to "many, much, a lot",
        "ぜんぶ" to "all",
        "みんな" to "everyone, all",
        "よく" to "well, often",
        "すぐ" to "soon, immediately",
        "ゆっくり" to "slowly",
        "はやく" to "quickly, early",
        "いつも" to "always",
        "ときどき" to "sometimes",
        "たまに" to "occasionally",
        "ぜんぜん" to "not at all (with negative)",
        "あまり" to "not very (with negative)",
        "けっして" to "never (with negative)",
        "もちろん" to "of course",
        "たぶん" to "probably",
        "きっと" to "surely, certainly",
        "ぜひ" to "by all means",
        "やっと" to "finally",
        "まず" to "first of all",
        "さいご" to "finally, lastly",

        // Common te-form patterns
        "て" to "(te-form)",
        "で" to "(te-form)",
        "た" to "(past tense)",
        "だ" to "(past tense)",
        "ない" to "not",
        "なかった" to "did not",
        "たい" to "want to",
        "たかった" to "wanted to"
    )

    /**
     * Convert Japanese text to HTML with ruby annotations for kanji.
     * Example: "食べる" -> "<ruby>食<rt>た</rt></ruby>べる"
     */
    fun toFurigana(text: String): String {
        val tokens = tokenizer.tokenize(text)
        return tokens.joinToString("") { token ->
            convertTokenToFurigana(token)
        }
    }

    /**
     * Split Japanese text into sentences and tokenize each word with its romaji.
     * Returns a list of sentences, where each sentence contains a list of word tokens.
     * If vocabularyMap is provided, will add English meanings where tokens match vocabulary.
     */
    fun toSentencesWithTokens(text: String, vocabularyMap: Map<String, String> = emptyMap()): List<SentenceWithTokens> {
        val sentences = text.split("。").filter { it.isNotBlank() }
        return sentences.map { sentence ->
            val withPeriod = "$sentence。"
            SentenceWithTokens(tokenizeToWords(withPeriod, vocabularyMap))
        }
    }

    /**
     * Tokenize text into words with their romaji readings and furigana.
     * If vocabularyMap is provided, will add English meanings where tokens match vocabulary.
     */
    private fun tokenizeToWords(text: String, vocabularyMap: Map<String, String> = emptyMap()): List<WordToken> {
        val tokens = tokenizer.tokenize(text)
        val wordTokens = mutableListOf<WordToken>()

        for ((index, token) in tokens.withIndex()) {
            val reading = token.reading
            var romaji = when {
                reading == null || reading == "*" || reading.isEmpty() -> {
                    if (isLatin(token.surface)) {
                        token.surface
                    } else {
                        katakanaToRomaji(token.surface)
                    }
                }
                else -> katakanaToRomaji(reading)
            }

            // Clean up romaji (remove punctuation marks from romaji display)
            val cleanRomaji = when (romaji) {
                ".", ",", "?", "!" -> ""
                else -> romaji
            }

            // Generate furigana HTML for this token
            val furiganaHtml = convertTokenToFurigana(token)

            // Look up meaning in vocabulary map first, then fallback dictionary
            // Try: surface form in vocab -> base form in vocab -> surface in common -> base in common
            val meaning = vocabularyMap[token.surface]
                ?: token.baseForm?.let { vocabularyMap[it] }
                ?: commonWordsDictionary[token.surface]
                ?: token.baseForm?.let { commonWordsDictionary[it] }

            if (cleanRomaji.isNotEmpty() || token.surface.isNotBlank()) {
                wordTokens.add(WordToken(token.surface, cleanRomaji, furiganaHtml, meaning))
            }
        }

        return wordTokens
    }

    /**
     * Split Japanese text into sentences (by 。) and return both Japanese and romaji for each.
     * Returns a list of pairs where first is Japanese sentence and second is romaji.
     */
    fun toSentencesWithRomaji(text: String): List<Pair<String, String>> {
        val sentences = text.split("。").filter { it.isNotBlank() }
        return sentences.map { sentence ->
            val withPeriod = "$sentence。"
            Pair(withPeriod, toRomaji(withPeriod))
        }
    }

    /**
     * Convert Japanese text to romaji with spaces between words.
     * Example: "食べる" -> "taberu", "私は食べる" -> "watashi wa taberu"
     */
    fun toRomaji(text: String): String {
        val tokens = tokenizer.tokenize(text)
        val result = StringBuilder()

        for ((index, token) in tokens.withIndex()) {
            val reading = token.reading
            var romaji = when {
                // If reading is null, empty, or "*", use the surface form
                reading == null || reading == "*" || reading.isEmpty() -> {
                    // If surface is already latin, keep it as-is
                    if (isLatin(token.surface)) {
                        token.surface
                    } else {
                        // Try to convert surface (might be katakana)
                        katakanaToRomaji(token.surface)
                    }
                }
                else -> katakanaToRomaji(reading)
            }

            // Handle small kana combinations across token boundaries
            // If this token starts with a small kana vowel (ya, yu, yo) and previous ends with consonant
            if (index > 0 && result.isNotEmpty() && romaji.isNotEmpty()) {
                val surface = token.surface
                if (surface.isNotEmpty() && isSmallYKana(surface[0])) {
                    val lastChar = result.last()
                    // If previous token ended with 'i' from shi, chi, ni, hi, etc.
                    // we need to combine: shi + yo = sho, chi + yo = cho, etc.
                    if (lastChar == 'i') {
                        result.deleteCharAt(result.length - 1)
                        // Convert ya/yu/yo to a/u/o for combination
                        romaji = when (romaji.firstOrNull()) {
                            'y' -> romaji.drop(1) // ya->a, yu->u, yo->o
                            else -> romaji
                        }
                    }
                }
            }

            // Decide whether to add a space before this token
            if (index > 0 && romaji.isNotEmpty()) {
                val surface = token.surface
                // Don't add space if this is a small kana (combining with previous)
                val isSmallKana = surface.isNotEmpty() && isSmallYKana(surface[0])

                if (!isSmallKana) {
                    val pos = token.partOfSpeechLevel1 ?: ""
                    val prevToken = tokens[index - 1]
                    val prevPos = prevToken.partOfSpeechLevel1 ?: ""

                    // Add space before content words (nouns, verbs, adjectives, adverbs)
                    // Don't add space before/after particles and auxiliary elements
                    val isContentWord = pos in listOf("名詞", "動詞", "形容詞", "副詞", "接続詞", "感動詞")
                    val prevWasParticle = prevPos in listOf("助詞", "助動詞")
                    val isParticle = pos in listOf("助詞", "助動詞", "記号")

                    if (isContentWord || (prevWasParticle && !isParticle)) {
                        result.append(" ")
                    }
                }
            }

            result.append(romaji)
        }

        return result.toString()
            .replace("  ", " ")  // Clean up double spaces
            .replace(" .", ".")
            .replace(" ,", ",")
            .replace(" ?", "?")
            .replace(" !", "!")
            .replace(" -", "-")
            .replace("- ", "-")
            .trim()
    }

    private fun isSmallYKana(char: Char): Boolean {
        return char in listOf('ゃ', 'ゅ', 'ょ', 'ャ', 'ュ', 'ョ', 'ぁ', 'ぃ', 'ぅ', 'ぇ', 'ぉ', 'ァ', 'ィ', 'ゥ', 'ェ', 'ォ')
    }

    private fun isLatin(text: String): Boolean {
        return text.all { char ->
            char.code in 0x0000..0x007F ||  // Basic Latin
            char.code in 0x0080..0x00FF ||  // Latin-1 Supplement
            char == '-' || char == ' '
        }
    }

    private fun convertTokenToFurigana(token: Token): String {
        val surface = token.surface
        val reading = token.reading

        // If no reading available or same as surface, wrap with empty furigana for consistent height
        if (reading == null || reading == "*" || reading == surface) {
            return "<ruby>$surface<rt>&nbsp;</rt></ruby>"
        }

        // Check if surface contains kanji
        if (!containsKanji(surface)) {
            // No kanji, but wrap with empty furigana for consistent height
            return "<ruby>$surface<rt>&nbsp;</rt></ruby>"
        }

        // If entire surface is kanji, wrap the whole thing
        if (surface.all { isKanji(it) }) {
            val hiraganaReading = katakanaToHiragana(reading)
            return "<ruby>$surface<rt>$hiraganaReading</rt></ruby>"
        }

        // Mixed kanji and kana - try to match and wrap only kanji parts
        return wrapKanjiWithFurigana(surface, reading)
    }

    private fun wrapKanjiWithFurigana(surface: String, reading: String): String {
        // Wrap kanji with furigana and kana with empty ruby for consistent height
        val result = StringBuilder()
        val hiraganaReading = katakanaToHiragana(reading)

        var kanjiStart = -1
        var kanaStart = -1
        var i = 0

        while (i < surface.length) {
            val char = surface[i]
            if (isKanji(char)) {
                // Flush any pending kana
                if (kanaStart != -1) {
                    val kanaPart = surface.substring(kanaStart, i)
                    result.append("<ruby>$kanaPart<rt>&nbsp;</rt></ruby>")
                    kanaStart = -1
                }
                if (kanjiStart == -1) kanjiStart = i
            } else {
                if (kanjiStart != -1) {
                    // End of kanji sequence - try to extract reading
                    val kanjiPart = surface.substring(kanjiStart, i)
                    val kanjiReading = extractKanjiReading(surface, hiraganaReading, kanjiStart, i)
                    result.append("<ruby>$kanjiPart<rt>$kanjiReading</rt></ruby>")
                    kanjiStart = -1
                }
                if (kanaStart == -1) kanaStart = i
            }
            i++
        }

        // Handle trailing kanji
        if (kanjiStart != -1) {
            val kanjiPart = surface.substring(kanjiStart)
            val kanjiReading = extractKanjiReading(surface, hiraganaReading, kanjiStart, surface.length)
            result.append("<ruby>$kanjiPart<rt>$kanjiReading</rt></ruby>")
        }

        // Handle trailing kana
        if (kanaStart != -1) {
            val kanaPart = surface.substring(kanaStart)
            result.append("<ruby>$kanaPart<rt>&nbsp;</rt></ruby>")
        }

        return result.toString()
    }

    private fun extractKanjiReading(surface: String, reading: String, kanjiStart: Int, kanjiEnd: Int): String {
        // Try to match kana suffix to find where kanji reading ends
        val suffix = surface.substring(kanjiEnd)
        val hiraganaSuffix = katakanaToHiragana(suffix)

        if (hiraganaSuffix.isNotEmpty() && reading.endsWith(hiraganaSuffix)) {
            val readingWithoutSuffix = reading.dropLast(hiraganaSuffix.length)
            // Also try to remove prefix if there's kana before kanji
            val prefix = surface.substring(0, kanjiStart)
            val hiraganaPrefix = katakanaToHiragana(prefix)
            if (hiraganaPrefix.isNotEmpty() && readingWithoutSuffix.startsWith(hiraganaPrefix)) {
                return readingWithoutSuffix.drop(hiraganaPrefix.length)
            }
            return readingWithoutSuffix
        }

        // Fallback: return full reading
        return reading
    }

    private fun containsKanji(text: String): Boolean = text.any { isKanji(it) }

    private fun isKanji(char: Char): Boolean {
        val code = char.code
        // CJK Unified Ideographs and extensions
        return (code in 0x4E00..0x9FFF) ||
               (code in 0x3400..0x4DBF) ||
               (code in 0x20000..0x2A6DF)
    }

    private fun katakanaToHiragana(text: String): String {
        return text.map { char ->
            when {
                // Standard katakana (ァ-ヶ) -> hiragana
                char.code in 0x30A1..0x30F6 -> (char.code - 0x60).toChar()
                // Small katakana vowels that might be outside main range
                char == 'ヮ' -> 'ゎ'
                else -> char
            }
        }.joinToString("")
    }

    private fun katakanaToRomaji(text: String): String {
        // First convert katakana to hiragana, then to romaji
        val hiragana = katakanaToHiragana(text)
        return hiraganaToRomaji(hiragana)
    }

    private fun isKatakana(char: Char): Boolean {
        return char.code in 0x30A0..0x30FF
    }

    private fun hiraganaToRomaji(text: String): String {
        val result = StringBuilder()
        var i = 0

        while (i < text.length) {
            val char = text[i]

            // Check for small tsu (っ or ッ) - doubles next consonant
            if ((char == 'っ' || char == 'ッ') && i + 1 < text.length) {
                val nextChar = text[i + 1]
                val nextRomaji = getSingleCharRomaji(if (nextChar.code in 0x30A1..0x30F6) (nextChar.code - 0x60).toChar() else nextChar)
                if (nextRomaji.isNotEmpty() && nextRomaji[0].isLetter()) {
                    result.append(nextRomaji[0])
                }
                i++
                continue
            }

            // Check for long vowel mark (ー) - extends previous vowel
            if (char == 'ー') {
                if (result.isNotEmpty()) {
                    val lastChar = result.last()
                    val extendedVowel = when (lastChar) {
                        'a', 'i', 'u', 'e', 'o' -> lastChar
                        else -> 'u' // default extension
                    }
                    result.append(extendedVowel)
                }
                i++
                continue
            }

            // Check for two-character combinations (きゃ, しゅ, etc.)
            if (i + 1 < text.length) {
                val combo = "${text[i]}${text[i + 1]}"
                val comboRomaji = getTwoCharRomaji(combo)
                if (comboRomaji != null) {
                    result.append(comboRomaji)
                    i += 2
                    continue
                }
            }

            // Single character
            result.append(getSingleCharRomaji(char))
            i++
        }

        return result.toString()
    }

    private fun getSingleCharRomaji(char: Char): String {
        return when (char) {
            'あ' -> "a"; 'い' -> "i"; 'う' -> "u"; 'え' -> "e"; 'お' -> "o"
            'か' -> "ka"; 'き' -> "ki"; 'く' -> "ku"; 'け' -> "ke"; 'こ' -> "ko"
            'さ' -> "sa"; 'し' -> "shi"; 'す' -> "su"; 'せ' -> "se"; 'そ' -> "so"
            'た' -> "ta"; 'ち' -> "chi"; 'つ' -> "tsu"; 'て' -> "te"; 'と' -> "to"
            'な' -> "na"; 'に' -> "ni"; 'ぬ' -> "nu"; 'ね' -> "ne"; 'の' -> "no"
            'は' -> "ha"; 'ひ' -> "hi"; 'ふ' -> "fu"; 'へ' -> "he"; 'ほ' -> "ho"
            'ま' -> "ma"; 'み' -> "mi"; 'む' -> "mu"; 'め' -> "me"; 'も' -> "mo"
            'や' -> "ya"; 'ゆ' -> "yu"; 'よ' -> "yo"
            'ら' -> "ra"; 'り' -> "ri"; 'る' -> "ru"; 'れ' -> "re"; 'ろ' -> "ro"
            'わ' -> "wa"; 'を' -> "wo"; 'ん' -> "n"
            'が' -> "ga"; 'ぎ' -> "gi"; 'ぐ' -> "gu"; 'げ' -> "ge"; 'ご' -> "go"
            'ざ' -> "za"; 'じ' -> "ji"; 'ず' -> "zu"; 'ぜ' -> "ze"; 'ぞ' -> "zo"
            'だ' -> "da"; 'ぢ' -> "ji"; 'づ' -> "zu"; 'で' -> "de"; 'ど' -> "do"
            'ば' -> "ba"; 'び' -> "bi"; 'ぶ' -> "bu"; 'べ' -> "be"; 'ぼ' -> "bo"
            'ぱ' -> "pa"; 'ぴ' -> "pi"; 'ぷ' -> "pu"; 'ぺ' -> "pe"; 'ぽ' -> "po"
            // Small kana (used in combinations)
            'ゃ' -> "ya"; 'ゅ' -> "yu"; 'ょ' -> "yo"
            'ぁ' -> "a"; 'ぃ' -> "i"; 'ぅ' -> "u"; 'ぇ' -> "e"; 'ぉ' -> "o"
            'っ' -> "" // handled separately
            'ゎ' -> "wa"
            '。' -> "."; '、' -> ","; '？' -> "?"; '！' -> "!"
            ' ', '　' -> " "
            else -> char.toString()
        }
    }

    private fun getTwoCharRomaji(combo: String): String? {
        return when (combo) {
            // Standard combinations
            "きゃ" -> "kya"; "きゅ" -> "kyu"; "きょ" -> "kyo"
            "しゃ" -> "sha"; "しゅ" -> "shu"; "しょ" -> "sho"
            "ちゃ" -> "cha"; "ちゅ" -> "chu"; "ちょ" -> "cho"
            "にゃ" -> "nya"; "にゅ" -> "nyu"; "にょ" -> "nyo"
            "ひゃ" -> "hya"; "ひゅ" -> "hyu"; "ひょ" -> "hyo"
            "みゃ" -> "mya"; "みゅ" -> "myu"; "みょ" -> "myo"
            "りゃ" -> "rya"; "りゅ" -> "ryu"; "りょ" -> "ryo"
            "ぎゃ" -> "gya"; "ぎゅ" -> "gyu"; "ぎょ" -> "gyo"
            "じゃ" -> "ja"; "じゅ" -> "ju"; "じょ" -> "jo"
            "びゃ" -> "bya"; "びゅ" -> "byu"; "びょ" -> "byo"
            "ぴゃ" -> "pya"; "ぴゅ" -> "pyu"; "ぴょ" -> "pyo"
            // Foreign loanword combinations (using small vowels)
            "うぃ" -> "wi"; "うぇ" -> "we"; "うぉ" -> "wo"
            "ゔぁ" -> "va"; "ゔぃ" -> "vi"; "ゔ" -> "vu"; "ゔぇ" -> "ve"; "ゔぉ" -> "vo"
            "てぃ" -> "ti"; "でぃ" -> "di"
            "とぅ" -> "tu"; "どぅ" -> "du"
            "しぇ" -> "she"; "じぇ" -> "je"; "ちぇ" -> "che"
            "ふぁ" -> "fa"; "ふぃ" -> "fi"; "ふぇ" -> "fe"; "ふぉ" -> "fo"
            "つぁ" -> "tsa"; "つぃ" -> "tsi"; "つぇ" -> "tse"; "つぉ" -> "tso"
            else -> null
        }
    }
}
