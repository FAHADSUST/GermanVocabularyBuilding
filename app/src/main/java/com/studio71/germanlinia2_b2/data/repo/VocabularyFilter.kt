package com.studio71.germanlinia2_b2.data.repo

/** Selected filters for the vocabulary list. Null means "no filter on this field". */
data class VocabularyFilter(
    val level: String? = null,
    val book: String? = null,
    val chapter: String? = null,
    val pos: String? = null,
    val grammarGroup: String? = null
)

enum class SortMode(val key: String, val label: String) {
    SOURCE("SOURCE", "Wie im Kapitel"),
    ALPHA("ALPHA", "A–Z (ohne Artikel)"),
    FREQUENCY("FREQUENCY", "Häufigkeit"),
    CHAPTER("CHAPTER", "Nach Kapitel")
}

