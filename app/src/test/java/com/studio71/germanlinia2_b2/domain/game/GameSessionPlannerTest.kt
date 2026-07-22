package com.studio71.germanlinia2_b2.domain.game

import com.studio71.germanlinia2_b2.data.local.VocabularyEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class GameSessionPlannerTest {

    @Test
    fun mixedCoverageSlice_uses70_30_whenPoolsHaveEnoughWords() {
        val words = (1..20).map { index -> testWord(index) }
        val seenOrMarked = (1..14).map { "w$it" }.toSet()

        val result = GameSessionPlanner.mixedCoverageSlice(
            allWords = words,
            seenOrMarkedIds = seenOrMarked,
            seenMarkedCursor = 0,
            newCursor = 0,
            size = 10,
            seenMarkedSharePercent = 70
        )

        val seenCount = result.batch.count { it.id in seenOrMarked }
        val newCount = result.batch.count { it.id !in seenOrMarked }

        assertEquals(10, result.batch.size)
        assertEquals(7, seenCount)
        assertEquals(3, newCount)
        assertEquals(7, result.nextSeenMarkedCursor)
        assertEquals(3, result.nextNewCursor)
    }

    @Test
    fun mixedCoverageSlice_fallsBackToSeenMarked_whenNewPoolIsTooSmall() {
        val words = (1..10).map { index -> testWord(index) }
        val seenOrMarked = (1..8).map { "w$it" }.toSet()

        val result = GameSessionPlanner.mixedCoverageSlice(
            allWords = words,
            seenOrMarkedIds = seenOrMarked,
            seenMarkedCursor = 0,
            newCursor = 0,
            size = 10,
            seenMarkedSharePercent = 70
        )

        val seenCount = result.batch.count { it.id in seenOrMarked }
        val newCount = result.batch.count { it.id !in seenOrMarked }

        assertEquals(10, result.batch.size)
        assertEquals(8, seenCount)
        assertEquals(2, newCount)
        assertEquals(0, result.nextSeenMarkedCursor)
        assertEquals(0, result.nextNewCursor)
    }

    private fun testWord(index: Int): VocabularyEntity {
        return VocabularyEntity(
            id = "w$index",
            word = "wort$index",
            sortKey = "wort$index",
            level = "A2",
            book = "A2",
            chapter = "1",
            pos = "n",
            english = "word$index",
            germanMeaning = "bedeutung$index",
            exampleDe = "Das ist wort$index.",
            orderIndex = index
        )
    }
}

