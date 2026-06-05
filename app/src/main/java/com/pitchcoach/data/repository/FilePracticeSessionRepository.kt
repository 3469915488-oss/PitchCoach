package com.pitchcoach.data.repository

import com.pitchcoach.core.audio.AudioFrame
import com.pitchcoach.core.music.CentsCalculator
import com.pitchcoach.core.pitch.PitchAnalysisState
import com.pitchcoach.core.session.NoteEventBuilder
import com.pitchcoach.core.session.NoteEventSnapshot
import com.pitchcoach.core.session.PitchFrameSnapshot
import com.pitchcoach.core.session.PracticeSessionSummarizer
import com.pitchcoach.core.session.PracticeSessionSummary
import com.pitchcoach.data.database.NoteEventEntity
import com.pitchcoach.data.database.PracticeSessionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile
import java.util.UUID

class FilePracticeSessionRepository(
    private val rootDir: File,
    private val clock: () -> Long = { System.currentTimeMillis() },
) : PracticeSessionRepository {
    private val sessions = MutableStateFlow<List<PracticeSessionEntity>>(emptyList())

    init {
        rootDir.mkdirs()
        sessions.value = scanSavedSessions()
    }

    override suspend fun startSession(type: String): String = withContext(Dispatchers.IO) {
        rootDir.mkdirs()
        val id = UUID.randomUUID().toString()
        val now = clock()
        val summary = PracticeSessionSummary(
            sessionId = id,
            type = type,
            startedAt = now,
            endedAt = now,
            averageAbsCents = 0f,
            flatRate = 0f,
            sharpRate = 0f,
            stabilityScore = 0f,
            noteRangeLow = null,
            noteRangeHigh = null,
            totalFrameCount = 0,
            voicedFrameCount = 0,
        )
        sessionFile(id).writeText(renderSession(summary, emptyList(), emptyList()))
        id
    }

    override suspend fun appendFrames(sessionId: String, frames: List<PitchFrameSnapshot>) {
        if (frames.isEmpty()) return
        withContext(Dispatchers.IO) {
            sessionFile(sessionId).appendText(frames.joinToString(separator = "\n") { it.toCsvLine() } + "\n")
        }
    }

    override suspend fun appendAnalysisFrame(sessionId: String, state: PitchAnalysisState) {
        appendFrames(sessionId, listOf(state.toSnapshot()))
    }

    override suspend fun appendAudioFrame(sessionId: String, frame: AudioFrame) {
        if (frame.samples.isEmpty()) return
        withContext(Dispatchers.IO) {
            appendWavSamples(sessionAudioFile(sessionId), frame)
        }
    }

    override suspend fun finishSession(sessionId: String): PracticeSessionSummary = withContext(Dispatchers.IO) {
        finalizeWavFile(sessionId)
        val parsed = parseSession(sessionFile(sessionId))
        val summary = PracticeSessionSummarizer.summarize(
            sessionId = sessionId,
            type = parsed.type,
            startedAt = parsed.startedAt,
            endedAt = clock(),
            frames = parsed.frames,
        )
        val events = NoteEventBuilder.build(parsed.frames).mapIndexed { index, event ->
            event.toEntity(sessionId = sessionId, index = index)
        }

        sessionFile(sessionId).writeText(renderSession(summary, parsed.frames, events))
        sessions.value = scanSavedSessions()
        summary
    }

    override fun observeSessions(): Flow<List<PracticeSessionEntity>> = sessions

    override suspend fun deleteSession(sessionId: String) {
        withContext(Dispatchers.IO) {
            sessionFile(sessionId).delete()
            sessionAudioFile(sessionId).delete()
            sessions.value = scanSavedSessions()
        }
    }

    override suspend fun getAudioFilePath(sessionId: String): String? = withContext(Dispatchers.IO) {
        sessionAudioFile(sessionId)
            .takeIf { file -> file.exists() && file.length() > WAV_HEADER_BYTES }
            ?.absolutePath
    }

    override suspend fun getFrames(sessionId: String): List<PitchFrameSnapshot> = withContext(Dispatchers.IO) {
        parseSession(sessionFile(sessionId)).frames
    }

    override suspend fun getNoteEvents(sessionId: String): List<NoteEventEntity> = withContext(Dispatchers.IO) {
        parseSession(sessionFile(sessionId)).events
    }

    fun sessionFilePath(sessionId: String): String = sessionFile(sessionId).absolutePath

    private fun sessionFile(sessionId: String): File = File(rootDir, "$sessionId.pitchcoach.csv")

    private fun sessionAudioFile(sessionId: String): File = File(rootDir, "$sessionId.wav")

    private fun scanSavedSessions(): List<PracticeSessionEntity> {
        return rootDir
            .listFiles { file -> file.isFile && file.extension == "csv" && file.name.endsWith(".pitchcoach.csv") }
            .orEmpty()
            .mapNotNull { file -> runCatching { parseSession(file).toEntity() }.getOrNull() }
            .filter { session -> session.endedAt > session.startedAt || session.totalFrameCount > 0 }
            .sortedByDescending { it.startedAt }
    }

    private fun renderSession(
        summary: PracticeSessionSummary,
        frames: List<PitchFrameSnapshot>,
        events: List<NoteEventEntity>,
    ): String = buildString {
        appendLine("# PitchCoach Session v1")
        appendLine("# id=${summary.sessionId}")
        appendLine("# type=${summary.type}")
        appendLine("# startedAt=${summary.startedAt}")
        appendLine("# endedAt=${summary.endedAt}")
        appendLine("# averageAbsCents=${summary.averageAbsCents}")
        appendLine("# flatRate=${summary.flatRate}")
        appendLine("# sharpRate=${summary.sharpRate}")
        appendLine("# stabilityScore=${summary.stabilityScore}")
        appendLine("# noteRangeLow=${summary.noteRangeLow.orEmptyString()}")
        appendLine("# noteRangeHigh=${summary.noteRangeHigh.orEmptyString()}")
        appendLine("# totalFrameCount=${summary.totalFrameCount}")
        appendLine("# voicedFrameCount=${summary.voicedFrameCount}")
        appendLine("# Frames")
        appendLine("timestampMs,frequencyHz,midi,cents,confidence,volumeRms,isVoiced")
        frames.forEach { frame -> appendLine(frame.toCsvLine()) }
        if (summary.endedAt > summary.startedAt || events.isNotEmpty()) {
            appendLine("# Events")
            appendLine("id,targetMidi,actualAvgMidi,avgCents,maxAbsCents,durationMs,stableDurationMs,attackCents,sustainCents,releaseCents,problemTagsJson")
            events.forEach { event -> appendLine(event.toCsvLine()) }
        }
    }

    private fun parseSession(file: File): ParsedSession {
        require(file.exists()) { "Practice session file not found: ${file.absolutePath}" }

        val headers = mutableMapOf<String, String>()
        val frames = mutableListOf<PitchFrameSnapshot>()
        val events = mutableListOf<NoteEventEntity>()
        var section = Section.HEADER

        file.forEachLine { line ->
            when {
                line.isBlank() -> Unit
                line == "# Frames" -> section = Section.FRAMES
                line == "# Events" -> section = Section.EVENTS
                line.startsWith("# ") && section == Section.HEADER -> {
                    val raw = line.removePrefix("# ")
                    val separatorIndex = raw.indexOf('=')
                    if (separatorIndex > 0) {
                        headers[raw.substring(0, separatorIndex)] = raw.substring(separatorIndex + 1)
                    }
                }
                section == Section.FRAMES && line.startsWith("timestampMs,") -> Unit
                section == Section.EVENTS && line.startsWith("id,targetMidi,") -> Unit
                section == Section.FRAMES -> frames += parseFrame(line)
                section == Section.EVENTS -> events += parseEvent(line)
                else -> Unit
            }
        }

        return ParsedSession(
            id = headers.getValue("id"),
            type = headers.getValue("type"),
            startedAt = headers.getValue("startedAt").toLong(),
            endedAt = headers["endedAt"]?.toLongOrNull() ?: headers.getValue("startedAt").toLong(),
            averageAbsCents = headers["averageAbsCents"]?.toFloatOrNull() ?: 0f,
            flatRate = headers["flatRate"]?.toFloatOrNull() ?: 0f,
            sharpRate = headers["sharpRate"]?.toFloatOrNull() ?: 0f,
            stabilityScore = headers["stabilityScore"]?.toFloatOrNull() ?: 0f,
            noteRangeLow = headers["noteRangeLow"].toNullableInt(),
            noteRangeHigh = headers["noteRangeHigh"].toNullableInt(),
            totalFrameCount = headers["totalFrameCount"]?.toIntOrNull() ?: frames.size,
            voicedFrameCount = headers["voicedFrameCount"]?.toIntOrNull() ?: frames.count { it.isVoiced },
            frames = frames,
            events = events,
        )
    }

    private fun PitchFrameSnapshot.toCsvLine(): String {
        return listOf(
            timestampMs.toString(),
            frequencyHz.orEmptyString(),
            (midi ?: frequencyHz?.let(CentsCalculator::frequencyToMidi)).orEmptyString(),
            cents.orEmptyString(),
            confidence.toString(),
            volumeRms.toString(),
            isVoiced.toString(),
        ).joinToString(",")
    }

    private fun parseFrame(line: String): PitchFrameSnapshot {
        val parts = parseCsvLine(line)
        return PitchFrameSnapshot(
            timestampMs = parts[0].toLong(),
            frequencyHz = parts[1].toNullableFloat(),
            midi = parts[2].toNullableFloat(),
            cents = parts[3].toNullableFloat(),
            confidence = parts[4].toFloat(),
            volumeRms = parts[5].toFloat(),
            isVoiced = parts[6].toBoolean(),
        )
    }

    private fun NoteEventEntity.toCsvLine(): String {
        return listOf(
            csvCell(id),
            targetMidi.orEmptyString(),
            actualAvgMidi.orEmptyString(),
            avgCents.toString(),
            maxAbsCents.toString(),
            durationMs.toString(),
            stableDurationMs.toString(),
            attackCents.orEmptyString(),
            sustainCents.orEmptyString(),
            releaseCents.orEmptyString(),
            csvCell(problemTagsJson),
        ).joinToString(",")
    }

    private fun parseEvent(line: String): NoteEventEntity {
        val parts = parseCsvLine(line)
        return NoteEventEntity(
            id = parts[0],
            sessionId = parts[0].substringBefore("_note_"),
            targetMidi = parts[1].toNullableInt(),
            actualAvgMidi = parts[2].toNullableFloat(),
            avgCents = parts[3].toFloat(),
            maxAbsCents = parts[4].toFloat(),
            durationMs = parts[5].toLong(),
            stableDurationMs = parts[6].toLong(),
            attackCents = parts[7].toNullableFloat(),
            sustainCents = parts[8].toNullableFloat(),
            releaseCents = parts[9].toNullableFloat(),
            problemTagsJson = parts[10],
        )
    }

    private fun PitchAnalysisState.toSnapshot(): PitchFrameSnapshot {
        val note = note
        return PitchFrameSnapshot(
            timestampMs = timestampMs,
            frequencyHz = frequencyHz,
            midi = note?.let { it.midi + it.cents / 100f },
            cents = note?.cents,
            confidence = confidence,
            volumeRms = volumeRms,
            isVoiced = isVoiced,
        )
    }

    private fun NoteEventSnapshot.toEntity(sessionId: String, index: Int): NoteEventEntity {
        return NoteEventEntity(
            id = "${sessionId}_note_${index.toString().padStart(4, '0')}",
            sessionId = sessionId,
            targetMidi = targetMidi,
            actualAvgMidi = actualAvgMidi,
            avgCents = avgCents,
            maxAbsCents = maxAbsCents,
            durationMs = durationMs,
            stableDurationMs = stableDurationMs,
            attackCents = attackCents,
            sustainCents = sustainCents,
            releaseCents = releaseCents,
            problemTagsJson = problemTags.toJsonArrayString(),
        )
    }

    private fun List<String>.toJsonArrayString(): String {
        return joinToString(prefix = "[", postfix = "]") { value ->
            "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
        }
    }

    private fun ParsedSession.toEntity(): PracticeSessionEntity {
        return PracticeSessionEntity(
            id = id,
            type = type,
            startedAt = startedAt,
            endedAt = endedAt,
            averageAbsCents = averageAbsCents,
            flatRate = flatRate,
            sharpRate = sharpRate,
            stabilityScore = stabilityScore,
            noteRangeLow = noteRangeLow,
            noteRangeHigh = noteRangeHigh,
            totalFrameCount = totalFrameCount,
            voicedFrameCount = voicedFrameCount,
        )
    }

    private fun parseCsvLine(line: String): List<String> {
        val values = mutableListOf<String>()
        val current = StringBuilder()
        var quoted = false
        var index = 0
        while (index < line.length) {
            val char = line[index]
            when {
                char == '"' && quoted && index + 1 < line.length && line[index + 1] == '"' -> {
                    current.append('"')
                    index += 1
                }
                char == '"' -> quoted = !quoted
                char == ',' && !quoted -> {
                    values += current.toString()
                    current.clear()
                }
                else -> current.append(char)
            }
            index += 1
        }
        values += current.toString()
        return values
    }

    private fun csvCell(value: String): String {
        return if (value.any { it == ',' || it == '"' || it == '\n' }) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }

    private fun Float?.orEmptyString(): String = this?.toString().orEmpty()

    private fun Int?.orEmptyString(): String = this?.toString().orEmpty()

    private fun String?.toNullableFloat(): Float? = this?.takeIf { it.isNotBlank() }?.toFloat()

    private fun String?.toNullableInt(): Int? = this?.takeIf { it.isNotBlank() }?.toInt()

    private fun appendWavSamples(file: File, frame: AudioFrame) {
        RandomAccessFile(file, "rw").use { output ->
            if (output.length() == 0L) {
                writeWavHeader(output, frame.sampleRate, dataBytes = 0)
            }

            output.seek(output.length())
            frame.samples.forEach { sample ->
                val pcm = (sample.coerceIn(-1f, 1f) * Short.MAX_VALUE)
                    .toInt()
                    .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                output.write(pcm and 0xFF)
                output.write((pcm shr 8) and 0xFF)
            }
        }
    }

    private fun finalizeWavFile(sessionId: String) {
        val file = sessionAudioFile(sessionId)
        if (!file.exists() || file.length() <= WAV_HEADER_BYTES) return

        RandomAccessFile(file, "rw").use { output ->
            output.seek(24L)
            val sampleRate = readIntLittleEndian(output)
            val dataBytes = (output.length() - WAV_HEADER_BYTES).toInt()
            output.seek(0L)
            writeWavHeader(output, sampleRate, dataBytes)
        }
    }

    private fun writeWavHeader(output: RandomAccessFile, sampleRate: Int, dataBytes: Int) {
        output.writeBytes("RIFF")
        writeIntLittleEndian(output, (WAV_HEADER_BYTES - 8 + dataBytes).toInt())
        output.writeBytes("WAVE")
        output.writeBytes("fmt ")
        writeIntLittleEndian(output, 16)
        writeShortLittleEndian(output, 1)
        writeShortLittleEndian(output, 1)
        writeIntLittleEndian(output, sampleRate)
        writeIntLittleEndian(output, sampleRate * BYTES_PER_SAMPLE)
        writeShortLittleEndian(output, BYTES_PER_SAMPLE)
        writeShortLittleEndian(output, BITS_PER_SAMPLE)
        output.writeBytes("data")
        writeIntLittleEndian(output, dataBytes)
    }

    private fun writeIntLittleEndian(output: RandomAccessFile, value: Int) {
        output.write(value and 0xFF)
        output.write((value shr 8) and 0xFF)
        output.write((value shr 16) and 0xFF)
        output.write((value shr 24) and 0xFF)
    }

    private fun writeShortLittleEndian(output: RandomAccessFile, value: Int) {
        output.write(value and 0xFF)
        output.write((value shr 8) and 0xFF)
    }

    private fun readIntLittleEndian(input: RandomAccessFile): Int {
        val b0 = input.read()
        val b1 = input.read()
        val b2 = input.read()
        val b3 = input.read()
        return b0 or (b1 shl 8) or (b2 shl 16) or (b3 shl 24)
    }

    private enum class Section {
        HEADER,
        FRAMES,
        EVENTS,
    }

    private data class ParsedSession(
        val id: String,
        val type: String,
        val startedAt: Long,
        val endedAt: Long,
        val averageAbsCents: Float,
        val flatRate: Float,
        val sharpRate: Float,
        val stabilityScore: Float,
        val noteRangeLow: Int?,
        val noteRangeHigh: Int?,
        val totalFrameCount: Int,
        val voicedFrameCount: Int,
        val frames: List<PitchFrameSnapshot>,
        val events: List<NoteEventEntity>,
    )

    private companion object {
        private const val WAV_HEADER_BYTES = 44L
        private const val BYTES_PER_SAMPLE = 2
        private const val BITS_PER_SAMPLE = 16
    }
}
