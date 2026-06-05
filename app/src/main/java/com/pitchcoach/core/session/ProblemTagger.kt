package com.pitchcoach.core.session

object ProblemTagger {
    fun tagsForNoteEvent(
        avgCents: Float,
        maxAbsCents: Float,
        durationMs: Long,
        stableDurationMs: Long,
        attackCents: Float?,
        sustainCents: Float?,
        releaseCents: Float?,
    ): List<String> {
        val tags = mutableListOf<String>()

        if (attackCents != null && attackCents < -35f) tags += "attack_flat"
        if (attackCents != null && attackCents > 35f) tags += "attack_sharp"
        if (releaseCents != null && sustainCents != null && releaseCents - sustainCents < -25f) {
            tags += "release_drop"
        }
        if (releaseCents != null && sustainCents != null && releaseCents - sustainCents > 25f) {
            tags += "release_rise"
        }
        if (durationMs > 0 && stableDurationMs.toFloat() / durationMs < 0.5f) {
            tags += "unstable_sustain"
        }
        if (maxAbsCents > 50f || kotlin.math.abs(avgCents) > 50f) {
            tags += "pitch_inaccurate"
        }

        return tags.distinct()
    }
}
