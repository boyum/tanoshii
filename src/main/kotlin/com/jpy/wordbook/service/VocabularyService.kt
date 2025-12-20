package com.jpy.wordbook.service

import com.jpy.wordbook.model.Word
import com.jpy.wordbook.repository.WordRepository
import io.micronaut.context.annotation.Value
import io.micronaut.context.event.ApplicationEventListener
import io.micronaut.runtime.server.event.ServerStartupEvent
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import java.io.File

@Singleton
class VocabularyService(
    private val wordRepository: WordRepository,
    @Value("\${vocabulary.path:known-words}") private val vocabularyPath: String
) : ApplicationEventListener<ServerStartupEvent> {

    private val logger = LoggerFactory.getLogger(VocabularyService::class.java)

    override fun onApplicationEvent(event: ServerStartupEvent) {
        val count = wordRepository.count()
        if (count == 0L) {
            logger.info("No words found in database, importing vocabulary...")
            val imported = importAllCsvFiles()
            if (imported == 0) {
                logger.info("No custom vocabulary found, loading default JLPT N5/N4 words...")
                loadDefaultVocabulary()
            }
        } else {
            logger.info("Found $count words in database, skipping import")
        }
    }

    fun importAllCsvFiles(): Int {
        val vocabDir = File(vocabularyPath)
        if (!vocabDir.exists() || !vocabDir.isDirectory) {
            logger.info("Custom vocabulary directory not found: $vocabularyPath")
            return 0
        }

        val csvFiles = vocabDir.listFiles { file -> file.extension == "csv" }
            ?.sortedBy { it.name }
            ?: return 0

        var totalImported = 0
        for (file in csvFiles) {
            val words = parseCsvFile(file)
            if (words.isNotEmpty()) {
                wordRepository.saveAll(words)
                totalImported += words.size
                logger.info("Imported ${words.size} words from ${file.name}")
            }
        }

        logger.info("Total words imported: $totalImported")
        return totalImported
    }

    private fun loadDefaultVocabulary() {
        val defaultWords = getDefaultJlptWords()
        wordRepository.saveAll(defaultWords)
        logger.info("Loaded ${defaultWords.size} default JLPT N5/N4 words")
    }

    private fun getDefaultJlptWords(): List<Word> {
        // JLPT N5 and N4 core vocabulary
        return listOf(
            // N5 - Basic nouns
            Word(japanese = "人", romaji = "hito", english = "person", category = "n5-nouns", categoryName = "N5 Nouns"),
            Word(japanese = "男", romaji = "otoko", english = "man", category = "n5-nouns", categoryName = "N5 Nouns"),
            Word(japanese = "女", romaji = "onna", english = "woman", category = "n5-nouns", categoryName = "N5 Nouns"),
            Word(japanese = "子供", romaji = "kodomo", english = "child", category = "n5-nouns", categoryName = "N5 Nouns"),
            Word(japanese = "友達", romaji = "tomodachi", english = "friend", category = "n5-nouns", categoryName = "N5 Nouns"),
            Word(japanese = "家族", romaji = "kazoku", english = "family", category = "n5-nouns", categoryName = "N5 Nouns"),
            Word(japanese = "母", romaji = "haha", english = "mother", category = "n5-nouns", categoryName = "N5 Nouns"),
            Word(japanese = "父", romaji = "chichi", english = "father", category = "n5-nouns", categoryName = "N5 Nouns"),
            Word(japanese = "先生", romaji = "sensei", english = "teacher", category = "n5-nouns", categoryName = "N5 Nouns"),
            Word(japanese = "学生", romaji = "gakusei", english = "student", category = "n5-nouns", categoryName = "N5 Nouns"),

            // N5 - Places
            Word(japanese = "家", romaji = "ie", english = "house", category = "n5-places", categoryName = "N5 Places"),
            Word(japanese = "学校", romaji = "gakkou", english = "school", category = "n5-places", categoryName = "N5 Places"),
            Word(japanese = "会社", romaji = "kaisha", english = "company", category = "n5-places", categoryName = "N5 Places"),
            Word(japanese = "駅", romaji = "eki", english = "station", category = "n5-places", categoryName = "N5 Places"),
            Word(japanese = "店", romaji = "mise", english = "shop", category = "n5-places", categoryName = "N5 Places"),
            Word(japanese = "病院", romaji = "byouin", english = "hospital", category = "n5-places", categoryName = "N5 Places"),
            Word(japanese = "銀行", romaji = "ginkou", english = "bank", category = "n5-places", categoryName = "N5 Places"),
            Word(japanese = "郵便局", romaji = "yuubinkyoku", english = "post office", category = "n5-places", categoryName = "N5 Places"),
            Word(japanese = "公園", romaji = "kouen", english = "park", category = "n5-places", categoryName = "N5 Places"),
            Word(japanese = "図書館", romaji = "toshokan", english = "library", category = "n5-places", categoryName = "N5 Places"),

            // N5 - Time
            Word(japanese = "今日", romaji = "kyou", english = "today", category = "n5-time", categoryName = "N5 Time"),
            Word(japanese = "明日", romaji = "ashita", english = "tomorrow", category = "n5-time", categoryName = "N5 Time"),
            Word(japanese = "昨日", romaji = "kinou", english = "yesterday", category = "n5-time", categoryName = "N5 Time"),
            Word(japanese = "今", romaji = "ima", english = "now", category = "n5-time", categoryName = "N5 Time"),
            Word(japanese = "朝", romaji = "asa", english = "morning", category = "n5-time", categoryName = "N5 Time"),
            Word(japanese = "昼", romaji = "hiru", english = "noon", category = "n5-time", categoryName = "N5 Time"),
            Word(japanese = "夜", romaji = "yoru", english = "night", category = "n5-time", categoryName = "N5 Time"),
            Word(japanese = "週末", romaji = "shuumatsu", english = "weekend", category = "n5-time", categoryName = "N5 Time"),
            Word(japanese = "毎日", romaji = "mainichi", english = "every day", category = "n5-time", categoryName = "N5 Time"),
            Word(japanese = "時間", romaji = "jikan", english = "time", category = "n5-time", categoryName = "N5 Time"),

            // N5 - Food & Drink
            Word(japanese = "水", romaji = "mizu", english = "water", category = "n5-food", categoryName = "N5 Food"),
            Word(japanese = "お茶", romaji = "ocha", english = "tea", category = "n5-food", categoryName = "N5 Food"),
            Word(japanese = "コーヒー", romaji = "koohii", english = "coffee", category = "n5-food", categoryName = "N5 Food"),
            Word(japanese = "ご飯", romaji = "gohan", english = "rice/meal", category = "n5-food", categoryName = "N5 Food"),
            Word(japanese = "パン", romaji = "pan", english = "bread", category = "n5-food", categoryName = "N5 Food"),
            Word(japanese = "肉", romaji = "niku", english = "meat", category = "n5-food", categoryName = "N5 Food"),
            Word(japanese = "魚", romaji = "sakana", english = "fish", category = "n5-food", categoryName = "N5 Food"),
            Word(japanese = "野菜", romaji = "yasai", english = "vegetables", category = "n5-food", categoryName = "N5 Food"),
            Word(japanese = "果物", romaji = "kudamono", english = "fruit", category = "n5-food", categoryName = "N5 Food"),
            Word(japanese = "卵", romaji = "tamago", english = "egg", category = "n5-food", categoryName = "N5 Food"),

            // N5 - Verbs
            Word(japanese = "食べる", romaji = "taberu", english = "to eat", category = "n5-verbs", categoryName = "N5 Verbs"),
            Word(japanese = "飲む", romaji = "nomu", english = "to drink", category = "n5-verbs", categoryName = "N5 Verbs"),
            Word(japanese = "行く", romaji = "iku", english = "to go", category = "n5-verbs", categoryName = "N5 Verbs"),
            Word(japanese = "来る", romaji = "kuru", english = "to come", category = "n5-verbs", categoryName = "N5 Verbs"),
            Word(japanese = "見る", romaji = "miru", english = "to see", category = "n5-verbs", categoryName = "N5 Verbs"),
            Word(japanese = "聞く", romaji = "kiku", english = "to listen", category = "n5-verbs", categoryName = "N5 Verbs"),
            Word(japanese = "話す", romaji = "hanasu", english = "to speak", category = "n5-verbs", categoryName = "N5 Verbs"),
            Word(japanese = "読む", romaji = "yomu", english = "to read", category = "n5-verbs", categoryName = "N5 Verbs"),
            Word(japanese = "書く", romaji = "kaku", english = "to write", category = "n5-verbs", categoryName = "N5 Verbs"),
            Word(japanese = "買う", romaji = "kau", english = "to buy", category = "n5-verbs", categoryName = "N5 Verbs"),
            Word(japanese = "する", romaji = "suru", english = "to do", category = "n5-verbs", categoryName = "N5 Verbs"),
            Word(japanese = "ある", romaji = "aru", english = "to exist (things)", category = "n5-verbs", categoryName = "N5 Verbs"),
            Word(japanese = "いる", romaji = "iru", english = "to exist (living)", category = "n5-verbs", categoryName = "N5 Verbs"),
            Word(japanese = "分かる", romaji = "wakaru", english = "to understand", category = "n5-verbs", categoryName = "N5 Verbs"),
            Word(japanese = "思う", romaji = "omou", english = "to think", category = "n5-verbs", categoryName = "N5 Verbs"),
            Word(japanese = "使う", romaji = "tsukau", english = "to use", category = "n5-verbs", categoryName = "N5 Verbs"),
            Word(japanese = "作る", romaji = "tsukuru", english = "to make", category = "n5-verbs", categoryName = "N5 Verbs"),
            Word(japanese = "待つ", romaji = "matsu", english = "to wait", category = "n5-verbs", categoryName = "N5 Verbs"),
            Word(japanese = "会う", romaji = "au", english = "to meet", category = "n5-verbs", categoryName = "N5 Verbs"),
            Word(japanese = "寝る", romaji = "neru", english = "to sleep", category = "n5-verbs", categoryName = "N5 Verbs"),

            // N5 - Adjectives
            Word(japanese = "大きい", romaji = "ookii", english = "big", category = "n5-adjectives", categoryName = "N5 Adjectives"),
            Word(japanese = "小さい", romaji = "chiisai", english = "small", category = "n5-adjectives", categoryName = "N5 Adjectives"),
            Word(japanese = "新しい", romaji = "atarashii", english = "new", category = "n5-adjectives", categoryName = "N5 Adjectives"),
            Word(japanese = "古い", romaji = "furui", english = "old", category = "n5-adjectives", categoryName = "N5 Adjectives"),
            Word(japanese = "良い", romaji = "yoi", english = "good", category = "n5-adjectives", categoryName = "N5 Adjectives"),
            Word(japanese = "悪い", romaji = "warui", english = "bad", category = "n5-adjectives", categoryName = "N5 Adjectives"),
            Word(japanese = "高い", romaji = "takai", english = "expensive/tall", category = "n5-adjectives", categoryName = "N5 Adjectives"),
            Word(japanese = "安い", romaji = "yasui", english = "cheap", category = "n5-adjectives", categoryName = "N5 Adjectives"),
            Word(japanese = "美味しい", romaji = "oishii", english = "delicious", category = "n5-adjectives", categoryName = "N5 Adjectives"),
            Word(japanese = "楽しい", romaji = "tanoshii", english = "fun", category = "n5-adjectives", categoryName = "N5 Adjectives"),
            Word(japanese = "難しい", romaji = "muzukashii", english = "difficult", category = "n5-adjectives", categoryName = "N5 Adjectives"),
            Word(japanese = "易しい", romaji = "yasashii", english = "easy", category = "n5-adjectives", categoryName = "N5 Adjectives"),
            Word(japanese = "暑い", romaji = "atsui", english = "hot (weather)", category = "n5-adjectives", categoryName = "N5 Adjectives"),
            Word(japanese = "寒い", romaji = "samui", english = "cold (weather)", category = "n5-adjectives", categoryName = "N5 Adjectives"),
            Word(japanese = "忙しい", romaji = "isogashii", english = "busy", category = "n5-adjectives", categoryName = "N5 Adjectives"),

            // N4 - Verbs
            Word(japanese = "起きる", romaji = "okiru", english = "to wake up", category = "n4-verbs", categoryName = "N4 Verbs"),
            Word(japanese = "開ける", romaji = "akeru", english = "to open", category = "n4-verbs", categoryName = "N4 Verbs"),
            Word(japanese = "閉める", romaji = "shimeru", english = "to close", category = "n4-verbs", categoryName = "N4 Verbs"),
            Word(japanese = "始める", romaji = "hajimeru", english = "to begin", category = "n4-verbs", categoryName = "N4 Verbs"),
            Word(japanese = "終わる", romaji = "owaru", english = "to end", category = "n4-verbs", categoryName = "N4 Verbs"),
            Word(japanese = "変わる", romaji = "kawaru", english = "to change", category = "n4-verbs", categoryName = "N4 Verbs"),
            Word(japanese = "決める", romaji = "kimeru", english = "to decide", category = "n4-verbs", categoryName = "N4 Verbs"),
            Word(japanese = "届く", romaji = "todoku", english = "to arrive", category = "n4-verbs", categoryName = "N4 Verbs"),
            Word(japanese = "届ける", romaji = "todokeru", english = "to deliver", category = "n4-verbs", categoryName = "N4 Verbs"),
            Word(japanese = "探す", romaji = "sagasu", english = "to search", category = "n4-verbs", categoryName = "N4 Verbs"),
            Word(japanese = "見つける", romaji = "mitsukeru", english = "to find", category = "n4-verbs", categoryName = "N4 Verbs"),
            Word(japanese = "忘れる", romaji = "wasureru", english = "to forget", category = "n4-verbs", categoryName = "N4 Verbs"),
            Word(japanese = "覚える", romaji = "oboeru", english = "to remember", category = "n4-verbs", categoryName = "N4 Verbs"),
            Word(japanese = "教える", romaji = "oshieru", english = "to teach", category = "n4-verbs", categoryName = "N4 Verbs"),
            Word(japanese = "習う", romaji = "narau", english = "to learn", category = "n4-verbs", categoryName = "N4 Verbs"),

            // N4 - Nouns
            Word(japanese = "天気", romaji = "tenki", english = "weather", category = "n4-nouns", categoryName = "N4 Nouns"),
            Word(japanese = "季節", romaji = "kisetsu", english = "season", category = "n4-nouns", categoryName = "N4 Nouns"),
            Word(japanese = "春", romaji = "haru", english = "spring", category = "n4-nouns", categoryName = "N4 Nouns"),
            Word(japanese = "夏", romaji = "natsu", english = "summer", category = "n4-nouns", categoryName = "N4 Nouns"),
            Word(japanese = "秋", romaji = "aki", english = "autumn", category = "n4-nouns", categoryName = "N4 Nouns"),
            Word(japanese = "冬", romaji = "fuyu", english = "winter", category = "n4-nouns", categoryName = "N4 Nouns"),
            Word(japanese = "趣味", romaji = "shumi", english = "hobby", category = "n4-nouns", categoryName = "N4 Nouns"),
            Word(japanese = "旅行", romaji = "ryokou", english = "travel", category = "n4-nouns", categoryName = "N4 Nouns"),
            Word(japanese = "予定", romaji = "yotei", english = "plan", category = "n4-nouns", categoryName = "N4 Nouns"),
            Word(japanese = "問題", romaji = "mondai", english = "problem", category = "n4-nouns", categoryName = "N4 Nouns"),
            Word(japanese = "答え", romaji = "kotae", english = "answer", category = "n4-nouns", categoryName = "N4 Nouns"),
            Word(japanese = "意味", romaji = "imi", english = "meaning", category = "n4-nouns", categoryName = "N4 Nouns"),
            Word(japanese = "理由", romaji = "riyuu", english = "reason", category = "n4-nouns", categoryName = "N4 Nouns"),
            Word(japanese = "経験", romaji = "keiken", english = "experience", category = "n4-nouns", categoryName = "N4 Nouns"),
            Word(japanese = "気持ち", romaji = "kimochi", english = "feeling", category = "n4-nouns", categoryName = "N4 Nouns"),

            // N4 - Adjectives
            Word(japanese = "便利", romaji = "benri", english = "convenient", category = "n4-adjectives", categoryName = "N4 Adjectives"),
            Word(japanese = "不便", romaji = "fuben", english = "inconvenient", category = "n4-adjectives", categoryName = "N4 Adjectives"),
            Word(japanese = "必要", romaji = "hitsuyou", english = "necessary", category = "n4-adjectives", categoryName = "N4 Adjectives"),
            Word(japanese = "大切", romaji = "taisetsu", english = "important", category = "n4-adjectives", categoryName = "N4 Adjectives"),
            Word(japanese = "簡単", romaji = "kantan", english = "simple", category = "n4-adjectives", categoryName = "N4 Adjectives"),
            Word(japanese = "複雑", romaji = "fukuzatsu", english = "complicated", category = "n4-adjectives", categoryName = "N4 Adjectives"),
            Word(japanese = "有名", romaji = "yuumei", english = "famous", category = "n4-adjectives", categoryName = "N4 Adjectives"),
            Word(japanese = "危険", romaji = "kiken", english = "dangerous", category = "n4-adjectives", categoryName = "N4 Adjectives"),
            Word(japanese = "安全", romaji = "anzen", english = "safe", category = "n4-adjectives", categoryName = "N4 Adjectives"),
            Word(japanese = "特別", romaji = "tokubetsu", english = "special", category = "n4-adjectives", categoryName = "N4 Adjectives")
        )
    }

    private fun parseCsvFile(file: File): List<Word> {
        val category = extractCategory(file.name)
        val categoryName = extractCategoryName(file.name)

        return file.readLines()
            .filter { it.isNotBlank() }
            .mapNotNull { line ->
                val parts = line.split(";")
                if (parts.size >= 3) {
                    Word(
                        japanese = parts[0].trim(),
                        romaji = parts[1].trim(),
                        english = parts[2].trim(),
                        category = category,
                        categoryName = categoryName
                    )
                } else {
                    logger.warn("Skipping invalid line in ${file.name}: $line")
                    null
                }
            }
    }

    private fun extractCategory(filename: String): String {
        // e.g., "010-food.csv" -> "010-food"
        return filename.removeSuffix(".csv")
    }

    private fun extractCategoryName(filename: String): String {
        // e.g., "010-food.csv" -> "Food"
        // e.g., "001-classroom-language-1.csv" -> "Classroom Language 1"
        return filename
            .removeSuffix(".csv")
            .replace(Regex("^\\d+-"), "") // Remove leading number prefix
            .replace("-", " ")
            .split(" ")
            .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
    }

    fun getRandomWords(count: Int): List<Word> = wordRepository.findRandomWords(count)

    fun getWordsByIds(ids: List<Long>): List<Word> = wordRepository.findByIds(ids)

    fun getAllCategories(): List<String> = wordRepository.getCategories()

    fun getWordCount(): Long = wordRepository.count()
}
