package com.studio71.germanlinia2_b2.data.repo

/** UI-friendly snapshot for one word's meaning image. */
data class WordImageState(
    val imageUrl: String? = null,
    val isLoading: Boolean = false,
    val canRetry: Boolean = false,
    val message: String = ""
)

