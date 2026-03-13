package com.studio71.germanlinia2_b2.domain.srs

import com.studio71.germanlinia2_b2.data.local.ProgressEntity

/**
 * Spaced-repetition scheduler.
 *
 * A word learned on day 0 is shown again after 1, 2, 3, 7, 15 and 30 days
 * (offsets relative to the first-learned day, as requested).
 * If the user fails a review, the word steps back one interval.
 */
object SrsScheduler {

    /** Days after [ProgressEntity.firstLearned] when a word becomes due again. */
    val INTERVALS_DAYS = intArrayOf(1, 2, 3, 7, 15, 30)

    /** Create progress for a freshly learned word. */
    fun onLearned(wordId: String, today: Long): ProgressEntity = ProgressEntity(
        wordId = wordId,
        firstLearned = today,
        lastReviewed = today,
        nextDue = today + INTERVALS_DAYS[0],
        intervalIndex = 0,
        box = 1
    )

    /** Advance the schedule after a successful review. */
    fun onReviewSuccess(current: ProgressEntity, today: Long): ProgressEntity {
        val nextIndex = (current.intervalIndex + 1).coerceAtMost(INTERVALS_DAYS.lastIndex)
        val offset = INTERVALS_DAYS[nextIndex]
        return current.copy(
            lastReviewed = today,
            intervalIndex = nextIndex,
            nextDue = current.firstLearned + offset,
            box = current.box + 1
        )
    }

    /** Step back after a failed review (review again tomorrow). */
    fun onReviewFail(current: ProgressEntity, today: Long): ProgressEntity {
        val prevIndex = (current.intervalIndex - 1).coerceAtLeast(0)
        return current.copy(
            lastReviewed = today,
            intervalIndex = prevIndex,
            nextDue = today + 1,
            box = (current.box - 1).coerceAtLeast(1)
        )
    }

    /** True when the word has graduated through the whole schedule. */
    fun isMastered(progress: ProgressEntity): Boolean =
        progress.intervalIndex >= INTERVALS_DAYS.lastIndex
}

