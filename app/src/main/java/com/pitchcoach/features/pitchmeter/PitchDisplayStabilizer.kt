package com.pitchcoach.features.pitchmeter

import com.pitchcoach.core.pitch.PitchAnalysisState
import kotlin.math.abs

class PitchDisplayStabilizer(
    private val holdAfterUnvoicedMs: Long = 1_400L,
    private val minFramesForNoteSwitch: Int = 2,
    private val minConfidenceForDisplay: Float = 0.5f,
    private val maxTrailSize: Int = 28,
) {
    private val centsTrail = ArrayDeque<Float?>()
    private var displayedAnalysis: PitchAnalysisState? = null
    private var lastVoicedAtMs: Long = Long.MIN_VALUE
    private var candidateMidi: Int? = null
    private var candidateFrameCount: Int = 0

    fun stabilize(raw: PitchAnalysisState): PitchDisplayFrame {
        val note = raw.note
        val isDisplayable = raw.isVoiced && note != null && raw.confidence >= minConfidenceForDisplay

        val frame = when {
            isDisplayable -> acceptOrHoldCandidate(raw)
            shouldHoldPrevious(raw) -> {
                val previous = displayedAnalysis!!
                PitchDisplayFrame(
                    analysis = previous.copy(
                        timestampMs = raw.timestampMs,
                        volumeRms = raw.volumeRms,
                        isSilent = false,
                        isVoiced = false,
                    ),
                    isHolding = true,
                )
            }
            else -> {
                displayedAnalysis = null
                candidateMidi = null
                candidateFrameCount = 0
                PitchDisplayFrame(analysis = raw, isHolding = false)
            }
        }

        appendTrail(frame.analysis)
        return frame.copy(centsTrail = centsTrail.toList())
    }

    fun reset() {
        centsTrail.clear()
        displayedAnalysis = null
        lastVoicedAtMs = Long.MIN_VALUE
        candidateMidi = null
        candidateFrameCount = 0
    }

    private fun acceptOrHoldCandidate(raw: PitchAnalysisState): PitchDisplayFrame {
        val rawMidi = raw.note!!.midi
        val currentMidi = displayedAnalysis?.note?.midi

        if (currentMidi == null || currentMidi == rawMidi) {
            candidateMidi = rawMidi
            candidateFrameCount = minFramesForNoteSwitch
            displayedAnalysis = raw
            lastVoicedAtMs = raw.timestampMs
            return PitchDisplayFrame(analysis = raw, isHolding = false)
        }

        if (abs(rawMidi - currentMidi) >= SEMITONES_PER_OCTAVE) {
            candidateMidi = rawMidi
            candidateFrameCount = minFramesForNoteSwitch
            displayedAnalysis = raw
            lastVoicedAtMs = raw.timestampMs
            return PitchDisplayFrame(analysis = raw, isHolding = false)
        }

        if (candidateMidi == rawMidi) {
            candidateFrameCount += 1
        } else {
            candidateMidi = rawMidi
            candidateFrameCount = 1
        }

        return if (candidateFrameCount >= minFramesForNoteSwitch) {
            displayedAnalysis = raw
            lastVoicedAtMs = raw.timestampMs
            PitchDisplayFrame(analysis = raw, isHolding = false)
        } else {
            val previous = displayedAnalysis!!
            PitchDisplayFrame(
                analysis = previous.copy(
                    timestampMs = raw.timestampMs,
                    volumeRms = raw.volumeRms,
                    stabilityScore = raw.stabilityScore,
                ),
                isHolding = true,
            )
        }
    }

    private fun shouldHoldPrevious(raw: PitchAnalysisState): Boolean {
        val previous = displayedAnalysis ?: return false
        if (previous.note == null) return false
        return raw.timestampMs - lastVoicedAtMs <= holdAfterUnvoicedMs
    }

    private fun appendTrail(analysis: PitchAnalysisState) {
        val cents = analysis.note?.cents?.takeUnless { analysis.isSilent }
        centsTrail.addLast(cents)
        while (centsTrail.size > maxTrailSize) {
            centsTrail.removeFirst()
        }
    }
}

data class PitchDisplayFrame(
    val analysis: PitchAnalysisState,
    val isHolding: Boolean,
    val centsTrail: List<Float?> = emptyList(),
)

private const val SEMITONES_PER_OCTAVE = 12
