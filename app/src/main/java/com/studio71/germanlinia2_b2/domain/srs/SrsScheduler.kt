package com.studio71.germanlinia2_b2.domain.srs

import com.studio71.germanlinia2_b2.data.local.ProgressEntity

/**
 * Spaced-repetition scheduler.
 *
 * A word learned on day 0 is shown again after 1, 2, 3, 7, 15 and 30 days
 * (offsets relative to the first-learned day, as requested). A late review is spaced
 * from the day it happens, not from the first-learned day.
 * If the user fails a review, the word steps back one interval.
 * Passing the final (30-day) review finishes the plan and the word leaves the pipeline.
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

    /**
     * Advance the schedule after a successful review.
     * The next review is [today] plus the gap between the two plan steps, so a late review
     * is not due again right away. An on-time review lands on the same day as the plain
     * offset from the first-learned day.
     */
    fun onReviewSuccess(current: ProgressEntity, today: Long): ProgressEntity {
        val index = current.intervalIndex.coerceIn(0, INTERVALS_DAYS.lastIndex)
        val nextIndex = (index + 1).coerceAtMost(INTERVALS_DAYS.lastIndex)
        val gap = (INTERVALS_DAYS[nextIndex] - INTERVALS_DAYS[index]).coerceAtLeast(1)
        return current.copy(
            lastReviewed = today,
            intervalIndex = nextIndex,
            nextDue = today + gap,
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

    /** True at the last plan step: passing this review finishes the word. */
    fun isFinalStage(progress: ProgressEntity): Boolean =
        progress.intervalIndex >= INTERVALS_DAYS.lastIndex
}

