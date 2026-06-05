package com.pitchcoach.core.pitch

import com.pitchcoach.core.audio.AudioFrame
import com.pitchcoach.core.audio.SilenceDetector
import com.pitchcoach.core.music.NoteMapper
import kotlin.math.abs

class PitchAnalyzer(
    private val detector: PitchDetector = WindowedPitchDetector(),
    private val smoother: PitchSmoother = PitchSmoother(),
    private val silenceDetector: SilenceDetector = SilenceDetector(),
) {
    fun analyze(frame: AudioFrame): PitchAnalysisState {
        if (silenceDetector.isSilent(frame)) {
            detector.reset()
            smoother.reset()
            return PitchAnalysisState(
                timestampMs = frame.timestampMs,
                frequencyHz = null,
                note = null,
                confidence = 0f,
                volumeRms = frame.rms,
                isSilent = true,
                isVoiced = false,
                direction = PitchDirection.SILENT,
                stabilityScore = 0f,
            )
        }

        val detected = detector.detect(frame)
        val smoothed = smoother.smooth(detected)
        val note = smoothed.frequencyHz?.let(NoteMapper::estimate)
        val direction = when {
            !smoothed.isVoiced || note == null -> PitchDirection.UNKNOWN
            abs(note.cents) <= IN_TUNE_CENTS -> PitchDirection.IN_TUNE
            note.cents < 0f -> PitchDirection.FLAT
            else -> PitchDirection.SHARP
        }

        return PitchAnalysisState(
            timestampMs = frame.timestampMs,
            frequencyHz = smoothed.frequencyHz,
            note = note,
            confidence = smoothed.confidence,
            volumeRms = frame.rms,
            isSilent = false,
            isVoiced = smoothed.isVoiced && note != null,
            direction = direction,
            stabilityScore = smoother.stabilityScore(),
        )
    }

    companion object {
        const val IN_TUNE_CENTS = 10f
    }
}
