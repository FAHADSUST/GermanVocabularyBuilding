package com.studio71.germanlinia2_b2.domain.game

import com.studio71.germanlinia2_b2.data.local.VocabularyEntity
import com.studio71.germanlinia2_b2.data.settings.AppSettings

enum class GameMode(val routeValue: String, val title: String) {
    MATCHING("matching", "Matching"),
    CLOZE("cloze", "Fill in the gap"),
    RECENT("recent", "Recent 50"),
    REVERSE_RECALL("reverse", "Reverse recall");

    companion object {
        fun fromRoute(value: String?): GameMode =
            entries.firstOrNull { it.routeValue.equals(value, ignoreCase = true) } ?: MATCHING
    }
}

object GameSessionPlanner {

    data class MixedCoverageSlice(
        val batch: List<VocabularyEntity>,
        val nextSeenMarkedCursor: Int,
        val nextNewCursor: Int
    )

    fun enabledModes(settings: AppSettings): List<GameMode> {
        val base = buildList {
            if (settings.gameMatchingEnabled) add(GameMode.MATCHING)
            if (settings.gameClozeEnabled) add(GameMode.CLOZE)
            if (settings.gameRecentEnabled) add(GameMode.RECENT)
            if (settings.gameReverseRecallEnabled) add(GameMode.REVERSE_RECALL)
        }
        return if (base.isEmpty()) listOf(GameMode.MATCHING, GameMode.CLOZE, GameMode.RECENT) else base
    }

    fun chooseNextMode(settings: AppSettings): Pair<GameMode, Int> {
        val modes = enabledModes(settings)
        val index = settings.nextGameModeIndex.coerceAtLeast(0)
        val mode = modes[index % modes.size]
        return mode to (index + 1)
    }

    /**
     * Picks one game batch while keeping long-term coverage in both pools:
     * - seen/marked words (priority share)
     * - new words (remaining share)
     */
    fun mixedCoverageSlice(
        allWords: List<VocabularyEntity>,
        seenOrMarkedIds: Set<String>,
        seenMarkedCursor: Int,
        newCursor: Int,
        size: Int,
        seenMarkedSharePercent: Int = 70
    ): MixedCoverageSlice {
        if (allWords.isEmpty()) {
            return MixedCoverageSlice(
                batch = emptyList(),
                nextSeenMarkedCursor = 0,
                nextNewCursor = 0
            )
        }

        val targetSize = size.coerceIn(1, allWords.size)
        val share = seenMarkedSharePercent.coerceIn(0, 100)
        val priorityPool = allWords.filter { it.id in seenOrMarkedIds }
        val newPool = allWords.filterNot { it.id in seenOrMarkedIds }

        var priorityCount = (targetSize * share) / 100
        var newCount = targetSize - priorityCount

        priorityCount = priorityCount.coerceAtMost(priorityPool.size)
        newCount = newCount.coerceAtMost(newPool.size)

        var remaining = targetSize - priorityCount - newCount
        if (remaining > 0) {
            val addPriority = (priorityPool.size - priorityCount).coerceAtLeast(0).coerceAtMost(remaining)
            priorityCount += addPriority
            remaining -= addPriority
        }
        if (remaining > 0) {
            val addNew = (newPool.size - newCount).coerceAtLeast(0).coerceAtMost(remaining)
            newCount += addNew
        }

        val (prioritySlice, nextPriorityCursor) = coverageSliceOrEmpty(priorityPool, seenMarkedCursor, priorityCount)
        val (newSlice, nextNewCursor) = coverageSliceOrEmpty(newPool, newCursor, newCount)

        val batch = (prioritySlice + newSlice)
        return MixedCoverageSlice(
            batch = batch,
            nextSeenMarkedCursor = nextPriorityCursor,
            nextNewCursor = nextNewCursor
        )
    }

    /**
     * Returns a source-order slice with wrap-around and the next cursor.
     * This guarantees that repeated sessions eventually cover all words.
     */
    fun coverageSlice(
        allWords: List<VocabularyEntity>,
        cursor: Int,
        size: Int
    ): Pair<List<VocabularyEntity>, Int> {
        if (allWords.isEmpty()) return emptyList<VocabularyEntity>() to 0
        val normalizedSize = size.coerceIn(1, allWords.size)
        val start = ((cursor % allWords.size) + allWords.size) % allWords.size
        val out = ArrayList<VocabularyEntity>(normalizedSize)
        repeat(normalizedSize) { offset ->
            out += allWords[(start + offset) % allWords.size]
        }
        val nextCursor = (start + normalizedSize) % allWords.size
        return out to nextCursor
    }

    private fun coverageSliceOrEmpty(
        allWords: List<VocabularyEntity>,
        cursor: Int,
        size: Int
    ): Pair<List<VocabularyEntity>, Int> {
        if (allWords.isEmpty() || size <= 0) return emptyList<VocabularyEntity>() to cursor.coerceAtLeast(0)
        return coverageSlice(allWords, cursor, size)
    }
}

