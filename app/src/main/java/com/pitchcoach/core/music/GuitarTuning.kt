package com.pitchcoach.core.music

import kotlin.math.abs

data class GuitarStringTarget(
    val number: Int,
    val noteName: String,
    val octave: Int,
    val midi: Int,
) {
    val displayName: String = "$noteName$octave"
    val frequencyHz: Float = CentsCalculator.frequencyForMidi(midi)
}

data class GuitarTuning(
    val id: String,
    val name: String,
    val shortName: String,
    val strings: List<GuitarStringTarget>,
) {
    val noteSequence: String = strings.joinToString(" ") { it.displayName }
}

data class GuitarTuningMatch(
    val target: GuitarStringTarget,
    val cents: Float,
)

object GuitarTunings {
    val standard = GuitarTuning(
        id = "standard",
        name = "标准音",
        shortName = "E 标准",
        strings = strings("E", 2, 40, "A", 2, 45, "D", 3, 50, "G", 3, 55, "B", 3, 59, "E", 4, 64),
    )

    val halfStepDown = GuitarTuning(
        id = "half_step_down",
        name = "降半音",
        shortName = "Eb",
        strings = strings("Eb", 2, 39, "Ab", 2, 44, "Db", 3, 49, "Gb", 3, 54, "Bb", 3, 58, "Eb", 4, 63),
    )

    val dropD = GuitarTuning(
        id = "drop_d",
        name = "Drop D",
        shortName = "Drop D",
        strings = strings("D", 2, 38, "A", 2, 45, "D", 3, 50, "G", 3, 55, "B", 3, 59, "E", 4, 64),
    )

    val dStandard = GuitarTuning(
        id = "d_standard",
        name = "D 标准",
        shortName = "D 标准",
        strings = strings("D", 2, 38, "G", 2, 43, "C", 3, 48, "F", 3, 53, "A", 3, 57, "D", 4, 62),
    )

    val dadgad = GuitarTuning(
        id = "dadgad",
        name = "DADGAD",
        shortName = "DADGAD",
        strings = strings("D", 2, 38, "A", 2, 45, "D", 3, 50, "G", 3, 55, "A", 3, 57, "D", 4, 62),
    )

    val openG = GuitarTuning(
        id = "open_g",
        name = "Open G",
        shortName = "Open G",
        strings = strings("D", 2, 38, "G", 2, 43, "D", 3, 50, "G", 3, 55, "B", 3, 59, "D", 4, 62),
    )

    val openD = GuitarTuning(
        id = "open_d",
        name = "Open D",
        shortName = "Open D",
        strings = strings("D", 2, 38, "A", 2, 45, "D", 3, 50, "F#", 3, 54, "A", 3, 57, "D", 4, 62),
    )

    val all: List<GuitarTuning> = listOf(
        standard,
        halfStepDown,
        dropD,
        dStandard,
        dadgad,
        openG,
        openD,
    )

    fun find(id: String): GuitarTuning = all.firstOrNull { it.id == id } ?: standard

    fun match(frequencyHz: Float, tuning: GuitarTuning): GuitarTuningMatch? {
        if (frequencyHz <= 0f || !frequencyHz.isFinite()) return null
        return tuning.strings
            .mapNotNull { target ->
                CentsCalculator.centsBetween(frequencyHz, target.frequencyHz)
                    ?.let { cents -> GuitarTuningMatch(target = target, cents = cents) }
            }
            .minByOrNull { match -> abs(match.cents) }
    }

    private fun strings(
        s6: String,
        o6: Int,
        m6: Int,
        s5: String,
        o5: Int,
        m5: Int,
        s4: String,
        o4: Int,
        m4: Int,
        s3: String,
        o3: Int,
        m3: Int,
        s2: String,
        o2: Int,
        m2: Int,
        s1: String,
        o1: Int,
        m1: Int,
    ): List<GuitarStringTarget> {
        return listOf(
            GuitarStringTarget(6, s6, o6, m6),
            GuitarStringTarget(5, s5, o5, m5),
            GuitarStringTarget(4, s4, o4, m4),
            GuitarStringTarget(3, s3, o3, m3),
            GuitarStringTarget(2, s2, o2, m2),
            GuitarStringTarget(1, s1, o1, m1),
        )
    }
}
