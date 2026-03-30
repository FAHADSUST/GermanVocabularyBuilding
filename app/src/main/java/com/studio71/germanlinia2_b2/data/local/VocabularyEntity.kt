package com.studio71.germanlinia2_b2.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One vocabulary item. Mirrors the columns of `assets/vocabulary.csv`
 * (see data/README_VOCAB_SCHEMA.md). This table holds only *static* content;
 * personal learning progress lives in [ProgressEntity].
 */
@Entity(tableName = "vocabulary")
data class VocabularyEntity(
    @PrimaryKey val id: String,
    val word: String,
    val article: String = "",
    val plural: String = "",
    val sortKey: String,
    val level: String,
    val book: String,
    val chapter: String,
    val pos: String,
    val english: String,
    val germanMeaning: String,
    val exampleDe: String,
    /** pipe-separated, e.g. "reden|sich unterhalten" */
    val synonyms: String = "",
    val antonyms: String = "",
    val memoryTrick: String = "",
    val frequencyRank: Int? = null,
    val grammarGroup: String = "",
    val preposition: String = "",
    val governCase: String = "",
    val verbPresent3rd: String = "",
    val verbPast: String = "",
    val verbPerfect: String = "",
    val verbAux: String = "",
    val adjComparative: String = "",
    val adjSuperlative: String = "",
    val tags: String = "",
    /** Position of the word in the original book/chapter order (for "Wie im Kapitel" sort). */
    val orderIndex: Int = 0
) {
    val displayWord: String
        get() = if (article.isBlank()) word else "$article $word"

    val synonymList: List<String> get() = splitPipes(synonyms)
    val antonymList: List<String> get() = splitPipes(antonyms)
    val tagList: List<String> get() = splitPipes(tags)

    val isVerb: Boolean get() = pos.equals("v", ignoreCase = true)
    val isAdjective: Boolean get() = pos.equals("adj", ignoreCase = true)
    val isNoun: Boolean get() = pos.equals("n", ignoreCase = true)

    companion object {
        fun splitPipes(value: String): List<String> =
            value.split('|').map { it.trim() }.filter { it.isNotEmpty() }
    }
}

