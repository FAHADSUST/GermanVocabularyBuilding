package com.studio71.germanlinia2_b2.ui.card

/**
 * Shared, in-memory ordered list of the currently displayed word ids.
 * The list screen fills it before opening the card so the card view can page
 * forward/backward through exactly the same (filtered + sorted) sequence.
 */
object CardDeck {
    @Volatile
    var ids: List<String> = emptyList()
        private set

    fun setDeck(ids: List<String>) {
        this.ids = ids
    }

    fun indexOf(id: String): Int = ids.indexOf(id)

    fun idAt(index: Int): String? = ids.getOrNull(index)

    val size: Int get() = ids.size
}

