package com.pitchcoach.core.session

import kotlin.math.abs
import kotlin.math.roundToInt

object NoteEventBuilder {
    fun build(
        frames: List<PitchFrameSnapshot>,
        stableCentsThreshold: Float = 35f,
        maxGapMs: Long = 250L,
    ): List<NoteEventSnapshot> {
        val voicedFrames = frames
            .filter { it.isVoiced && it.midi != null && it.cents != null }
            .sortedBy { it.timestampMs }
        if (voicedFrames.isEmpty()) return emptyList()

        val groups = mutableListOf<MutableList<PitchFrameSnapshot>>()
        var current = mutableListOf<PitchFrameSnapshot>()

        for (frame in voicedFrames) {
            val last = current.lastOrNull()
            val lastMidi = last?.midi?.roundToInt()
            val frameMidi = frame.midi?.roundToInt()
            val sameNote = last == null ||
                lastMidi == frameMidi
            val closeEnough = last == null || frame.timestampMs - last.timestampMs <= maxGapMs

            if (sameNote && closeEnough) {
                current += frame
            } else {
                if (current.isNotEmpty()) groups += current
                current = mutableListOf(frame)
            }
        }
        if (current.isNotEmpty()) groups += current

        return groups.mapNotNull { group -> buildEvent(group, stableCentsThreshold) }
    }

    private fun buildEvent(
        frames: List<PitchFrameSnapshot>,
        stableCentsThreshold: Float,
    ): NoteEventSnapshot? {
        val centsValues = frames.mapNotNull { it.cents }
        val midiValues = frames.mapNotNull { it.midi }
        if (centsValues.isEmpty() || midiValues.isEmpty()) return null

        val durationMs = durationMs(frames)
        val frameDurationMs = frameDurationMs(frames)
        val stableDurationMs = centsValues.count { abs(it) <= stableCentsThreshold } * frameDurationMs
        val attackCents = segmentAverage(centsValues, 0f, 0.2f)
        val sustainCents = segmentAverage(centsValues, 0.2f, 0.8f)
        val releaseCents = segmentAverage(centsValues, 0.8f, 1f)
        val avgCents = centsValues.average().toFloat()
        val maxAbsCents = centsValues.maxOf { abs(it) }

        val tags = ProblemTagger.tagsForNoteEvent(
            avgCents = avgCents,
            maxAbsCents = maxAbsCents,
            durationMs = durationMs,
            stableDurationMs = stableDurationMs,
            attackCents = attackCents,
            sustainCents = sustainCents,
            releaseCents = releaseCents,
        )

        return NoteEventSnapshot(
            targetMidi = null,
            actualAvgMidi = midiValues.average().toFloat(),
            avgCents = avgCents,
            maxAbsCents = maxAbsCents,
            durationMs = durationMs,
            stableDurationMs = stableDurationMs,
            attackCents = attackCents,
            sustainCents = sustainCents,
            releaseCents = releaseCents,
            problemTags = tags,
        )
    }

    private fun durationMs(frames: List<PitchFrameSnapshot>): Long {
        if (frames.isEmpty()) return 0L
        return (frames.last().timestampMs - frames.first().timestampMs).coerceAtLeast(frameDurationMs(frames))
    }

    private fun frameDurationMs(frames: List<PitchFrameSnapshot>): Long {
        if (frames.size < 2) return 0L
        val deltas = frames.zipWithNext { a, b -> b.timestampMs - a.timestampMs }
            .filter { it > 0L }
        return deltas.minOrNull() ?: 0L
    }

    private fun segmentAverage(values: List<Float>, startFraction: Float, endFraction: Float): Float? {
        if (values.isEmpty()) return null
        val start = (values.size * startFraction).toInt().coerceIn(0, values.lastIndex)
        val endExclusive = (values.size * endFraction).toInt().coerceIn(start + 1, values.size)
        return values.subList(start, endExclusive).average().toFloat()
    }
}
