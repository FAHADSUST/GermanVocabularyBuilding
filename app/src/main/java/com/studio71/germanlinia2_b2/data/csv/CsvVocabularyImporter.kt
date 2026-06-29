package com.studio71.germanlinia2_b2.data.csv

import android.content.Context
import com.studio71.germanlinia2_b2.data.local.VocabularyEntity

/**
 * Parses `assets/vocabulary.csv` into [VocabularyEntity] rows.
 * Handles quoted fields, embedded commas, and doubled quotes ("").
 */
object CsvVocabularyImporter {

    private const val ASSET_NAME = "vocabulary.csv"

    fun loadFromAssets(context: Context): List<VocabularyEntity> {
        context.assets.open(ASSET_NAME).bufferedReader(Charsets.UTF_8).use { reader ->
            val lines = reader.readText()
            return parse(lines)
        }
    }

    fun parse(text: String): List<VocabularyEntity> {
        val rows = splitRecords(text)
        if (rows.isEmpty()) return emptyList()

        val header = rows.first().map { it.trim() }
        val index = header.withIndex().associate { (i, name) -> name to i }

        fun MutableList<String>.col(name: String): String {
            val i = index[name] ?: return ""
            return if (i < size) this[i].trim() else ""
        }

        return rows.drop(1)
            .filter { it.any { cell -> cell.isNotBlank() } }
            .mapIndexedNotNull { rowIndex, raw ->
                val cells = raw.toMutableList()
                val id = cells.col("id")
                val word = cells.col("word")
                if (id.isBlank() || word.isBlank()) return@mapIndexedNotNull null
                VocabularyEntity(
                    id = id,
                    word = word,
                    article = cells.col("article"),
                    plural = cells.col("plural"),
                    sortKey = cells.col("sort_key").ifBlank { word.lowercase() },
                    level = cells.col("level"),
                    book = cells.col("book"),
                    chapter = cells.col("chapter"),
                    pos = cells.col("pos"),
                    english = cells.col("english"),
                    germanMeaning = cells.col("german_meaning"),
                    exampleDe = cells.col("example_de"),
                    synonyms = cells.col("synonyms"),
                    antonyms = cells.col("antonyms"),
                    memoryTrick = cells.col("memory_trick"),
                    memoryTips = cells.col("memory_tips"),
                    frequencyRank = cells.col("frequency_rank").toIntOrNull(),
                    grammarGroup = cells.col("grammar_group"),
                    preposition = cells.col("preposition"),
                    governCase = cells.col("govern_case"),
                    verbPresent3rd = cells.col("verb_present_3rd"),
                    verbPast = cells.col("verb_past"),
                    verbPerfect = cells.col("verb_perfect"),
                    verbAux = cells.col("verb_aux"),
                    adjComparative = cells.col("adj_comparative"),
                    adjSuperlative = cells.col("adj_superlative"),
                    tags = cells.col("tags"),
                    orderIndex = cells.col("order_index").toIntOrNull() ?: rowIndex
                )
            }
    }

    /** Splits the whole CSV text into records of fields, honoring quotes and newlines. */
    private fun splitRecords(text: String): List<List<String>> {
        val records = mutableListOf<List<String>>()
        var fields = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0
        val n = text.length

        fun endField() {
            fields.add(sb.toString())
            sb.setLength(0)
        }
        fun endRecord() {
            endField()
            records.add(fields)
            fields = mutableListOf()
        }

        while (i < n) {
            val c = text[i]
            when {
                inQuotes -> {
                    if (c == '"') {
                        if (i + 1 < n && text[i + 1] == '"') {
                            sb.append('"'); i++
                        } else {
                            inQuotes = false
                        }
                    } else sb.append(c)
                }
                c == '"' -> inQuotes = true
                c == ',' -> endField()
                c == '\r' -> { /* ignore, handle on \n */ }
                c == '\n' -> endRecord()
                else -> sb.append(c)
            }
            i++
        }
        // last record (no trailing newline)
        if (sb.isNotEmpty() || fields.isNotEmpty()) endRecord()
        return records
    }
}

