package com.pitchcoach.features.pitchmeter

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.media.MediaPlayer
import androidx.annotation.RequiresPermission
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.pitchcoach.R
import com.pitchcoach.core.audio.AudioRecordAudioRecorder
import com.pitchcoach.core.audio.AudioRecorder
import com.pitchcoach.core.music.GuitarTunings
import com.pitchcoach.core.music.NoteEstimate
import com.pitchcoach.core.music.TargetPitchNormalizer
import com.pitchcoach.core.pitch.PitchAnalyzer
import com.pitchcoach.core.pitch.PitchAnalysisState
import com.pitchcoach.core.pitch.PitchDirection
import com.pitchcoach.core.session.PitchFrameSnapshot
import com.pitchcoach.data.repository.FilePracticeSessionRepository
import com.pitchcoach.data.repository.PracticeSessionRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class PitchMeterViewModel(
    private val audioRecorder: AudioRecorder,
    private val analyzer: PitchAnalyzer,
    private val repository: PracticeSessionRepository? = null,
    private val guitarReferenceTonePlayer: GuitarReferenceTonePlayer,
) : ViewModel() {
    private val displayStabilizer = PitchDisplayStabilizer()
    private val pendingFrames = mutableListOf<PitchFrameSnapshot>()
    private var practiceMode: PitchPracticeMode = PitchPracticeMode.CHROMATIC
    private var selectedGuitarTuningId: String = GuitarTunings.standard.id
    private var selectedGuitarStringNumber: Int = 6
    private var mediaPlayer: MediaPlayer? = null
    private var referenceToneJob: Job? = null
    private val playingSessionId = MutableStateFlow<String?>(null)

    private val _uiState = MutableStateFlow(baseUiState())
    val uiState: StateFlow<PitchMeterUiState> = _uiState.asStateFlow()
    val historyState: StateFlow<List<SessionHistoryItemUiState>> =
        repository
            ?.observeSessions()
            ?.combine(playingSessionId) { sessions, playingId ->
                sessions.map { session ->
                    SessionHistoryItemUiState.fromEntity(
                        entity = session,
                        isPlaying = session.id == playingId,
                    )
                }
            }
            ?.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())
            ?: MutableStateFlow<List<SessionHistoryItemUiState>>(emptyList())

    private var meteringJob: Job? = null
    private var activeSessionId: String? = null
    private var isPaused: Boolean = false
    private var lastUiEmitAtMs: Long? = null
    private var lastFrameFlushAtMs: Long? = null

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun startMetering(recordSession: Boolean = true) {
        if (meteringJob?.isActive == true) return

        displayStabilizer.reset()
        pendingFrames.clear()
        isPaused = false
        lastUiEmitAtMs = null
        lastFrameFlushAtMs = null

        meteringJob = viewModelScope.launch {
            val sessionId = if (recordSession) repository?.startSession("pitch_meter") else null
            activeSessionId = sessionId

            try {
                audioRecorder
                    .start()
                    .catch { throwable ->
                        _uiState.value = PitchMeterUiState(
                            directionText = throwable.message ?: "麦克风暂时不可用",
                            isListening = false,
                        )
                        activeSessionId = null
                    }
                    .collect { frame ->
                        if (isPaused) {
                            return@collect
                        }
                        val analysis = analyzer.analyze(frame)
                        val displayFrame = displayStabilizer.stabilize(analysis)
                        emitUiIfNeeded(displayFrame)
                        if (sessionId != null) {
                            repository?.appendAudioFrame(sessionId, frame)
                            appendAnalysisFrameBuffered(sessionId, analysis)
                        }
                    }
            } finally {
                if (sessionId != null) {
                    withContext(NonCancellable) {
                        flushPendingFrames(sessionId)
                    }
                }
            }
        }
    }

    fun togglePause() {
        if (meteringJob?.isActive != true) return

        isPaused = !isPaused
        _uiState.value = _uiState.value.copy(
            isPaused = isPaused,
            directionText = if (isPaused) {
                "已暂停，当前音高已保留"
            } else {
                "继续采集"
            },
        )
    }

    fun selectPracticeMode(mode: PitchPracticeMode) {
        if (practiceMode == mode) return

        practiceMode = mode
        displayStabilizer.reset()
        stopReferenceTone()
        _uiState.value = baseUiState().copy(
            isListening = _uiState.value.isListening,
            isPaused = _uiState.value.isPaused,
            lastSummaryText = _uiState.value.lastSummaryText,
        )
    }

    fun selectGuitarTuning(tuningId: String) {
        selectedGuitarTuningId = GuitarTunings.find(tuningId).id
        displayStabilizer.reset()
        stopReferenceTone()
        _uiState.value = baseUiState().copy(
            isListening = _uiState.value.isListening,
            isPaused = _uiState.value.isPaused,
            lastSummaryText = _uiState.value.lastSummaryText,
        )
    }

    fun selectGuitarString(stringNumber: Int) {
        selectedGuitarStringNumber = stringNumber.coerceIn(1, 6)
        displayStabilizer.reset()
        stopReferenceTone()
        _uiState.value = baseUiState().copy(
            isListening = _uiState.value.isListening,
            isPaused = _uiState.value.isPaused,
            lastSummaryText = _uiState.value.lastSummaryText,
        )
    }

    fun toggleReferenceTone() {
        if (referenceToneJob?.isActive == true) {
            stopReferenceTone()
            _uiState.value = _uiState.value.copy(isReferenceTonePlaying = false)
            return
        }

        val frequency = selectedGuitarTarget().frequencyHz
        referenceToneJob = viewModelScope.launch(Dispatchers.Default) {
            guitarReferenceTonePlayer.play(frequency)
        }
        _uiState.value = _uiState.value.copy(isReferenceTonePlaying = true)
    }

    fun deleteSession(sessionId: String) {
        if (sessionId == activeSessionId) return

        viewModelScope.launch {
            if (playingSessionId.value == sessionId) {
                stopPlayback()
            }
            repository?.deleteSession(sessionId)
        }
    }

    fun togglePlayback(sessionId: String) {
        if (playingSessionId.value == sessionId) {
            stopPlayback()
            return
        }

        viewModelScope.launch {
            stopPlayback()
            val audioPath = repository?.getAudioFilePath(sessionId)
            if (audioPath == null) {
                _uiState.value = _uiState.value.copy(
                    lastSummaryText = "这条记录没有原声文件，保存新的练习后可以回放。",
                )
                return@launch
            }

            val player = MediaPlayer().apply {
                setDataSource(audioPath)
                setOnCompletionListener { stopPlayback() }
                setOnErrorListener { _, _, _ ->
                    stopPlayback()
                    true
                }
                prepare()
                start()
            }
            mediaPlayer = player
            playingSessionId.value = sessionId
        }
    }

    fun stopMetering() {
        val sessionId = activeSessionId
        activeSessionId = null
        isPaused = false
        val job = meteringJob
        meteringJob = null
        audioRecorder.stop()

        if (sessionId != null || job != null) {
            viewModelScope.launch {
                job?.cancelAndJoin()
                val summary = if (sessionId != null) {
                    repository?.finishSession(sessionId)
                } else {
                    null
                }
                _uiState.value = _uiState.value.copy(
                    isListening = false,
                    isPaused = false,
                    lastSummaryText = summary?.let {
                        "已保存文件 · 平均偏差 %.0f cents，稳定度 %.0f%%，有效帧 %d".format(
                            it.averageAbsCents,
                            it.stabilityScore,
                            it.voicedFrameCount,
                        )
                    },
                )
            }
        } else {
            _uiState.value = _uiState.value.copy(isListening = false, isPaused = false)
        }
    }

    override fun onCleared() {
        stopReferenceTone()
        stopPlayback()
        stopMetering()
        super.onCleared()
    }

    private fun stopPlayback() {
        mediaPlayer?.let { player ->
            runCatching {
                if (player.isPlaying) player.stop()
                player.release()
            }
        }
        mediaPlayer = null
        playingSessionId.value = null
    }

    private fun emitUiIfNeeded(displayFrame: PitchDisplayFrame) {
        val timestampMs = displayFrame.analysis.timestampMs
        val lastEmit = lastUiEmitAtMs
        if (lastEmit != null && timestampMs - lastEmit < UI_UPDATE_INTERVAL_MS) {
            return
        }

        lastUiEmitAtMs = timestampMs
        _uiState.value = PitchMeterUiState.fromAnalysis(
            state = displayAnalysis(displayFrame.analysis),
            isListening = true,
            isPaused = false,
            isHoldingPitch = displayFrame.isHolding,
            practiceMode = practiceMode,
            inTuneRangeCents = IN_TUNE_RANGE_CENTS,
            noteLabelOverride = guitarNoteLabel(displayFrame.analysis),
            centsValueOverride = guitarCents(displayFrame.analysis),
            directionTextOverride = guitarDirectionText(displayFrame.analysis),
            selectedGuitarTuningId = selectedGuitarTuningId,
            selectedGuitarTuningName = selectedGuitarTuning().name,
            selectedGuitarTuningNotes = selectedGuitarTuning().noteSequence,
            selectedGuitarStringNumber = selectedGuitarTarget().number,
            selectedGuitarStringLabel = selectedGuitarTarget().displayName,
            selectedGuitarStringFrequencyText = "%.1f Hz".format(selectedGuitarTarget().frequencyHz),
            isReferenceTonePlaying = referenceToneJob?.isActive == true,
            guitarTunings = guitarTuningOptions(),
            guitarStrings = guitarStringStates(displayFrame.analysis),
            centsTrail = if (practiceMode == PitchPracticeMode.GUITAR) {
                listOf(guitarCents(displayFrame.analysis))
            } else {
                displayFrame.centsTrail
            },
        )
    }

    private fun displayAnalysis(state: PitchAnalysisState): PitchAnalysisState {
        if (practiceMode != PitchPracticeMode.GUITAR) return state

        val frequency = state.frequencyHz ?: return state.copy(note = null, direction = PitchDirection.UNKNOWN)
        val target = selectedGuitarTarget()
        val cents = TargetPitchNormalizer.centsToTargetOctave(frequency, target.frequencyHz)
            ?: return state.copy(note = null, direction = PitchDirection.UNKNOWN)

        return state.copy(
            note = NoteEstimate(
                noteName = target.noteName,
                octave = target.octave,
                midi = target.midi,
                cents = cents,
                frequencyHz = target.frequencyHz,
            ),
            direction = directionForCents(cents),
            isVoiced = state.isVoiced,
        )
    }

    private fun guitarNoteLabel(state: PitchAnalysisState): String? {
        if (practiceMode != PitchPracticeMode.GUITAR) return null
        return selectedGuitarTarget().displayName
    }

    private fun guitarCents(state: PitchAnalysisState): Float? {
        if (practiceMode != PitchPracticeMode.GUITAR) return null
        return state.frequencyHz?.let { frequency ->
            TargetPitchNormalizer.centsToTargetOctave(
                actualFrequencyHz = frequency,
                targetFrequencyHz = selectedGuitarTarget().frequencyHz,
            )
        }
    }

    private fun guitarDirectionText(state: PitchAnalysisState): String? {
        if (practiceMode != PitchPracticeMode.GUITAR) return null
        val cents = guitarCents(state) ?: return "拨一根弦"
        return when {
            kotlin.math.abs(cents) <= IN_TUNE_RANGE_CENTS -> "在合格区间内"
            cents < 0f -> "偏低，拧紧一点"
            else -> "偏高，放松一点"
        }
    }

    private fun guitarStringStates(state: PitchAnalysisState? = null): List<GuitarStringUiState> {
        val tuning = selectedGuitarTuning()
        val selectedCents = guitarCents(state ?: return tuning.strings.map { target ->
            GuitarStringUiState(
                number = target.number,
                noteLabel = target.displayName,
                frequencyText = "%.1f Hz".format(target.frequencyHz),
                cents = null,
                isActive = target.number == selectedGuitarStringNumber,
                isSelected = target.number == selectedGuitarStringNumber,
            )
        })
        return tuning.strings.map { target ->
            GuitarStringUiState(
                number = target.number,
                noteLabel = target.displayName,
                frequencyText = "%.1f Hz".format(target.frequencyHz),
                cents = if (target.number == selectedGuitarStringNumber) selectedCents else null,
                isActive = target.number == selectedGuitarStringNumber,
                isSelected = target.number == selectedGuitarStringNumber,
            )
        }
    }

    private fun directionForCents(cents: Float): PitchDirection = when {
        kotlin.math.abs(cents) <= IN_TUNE_RANGE_CENTS -> PitchDirection.IN_TUNE
        cents < 0f -> PitchDirection.FLAT
        else -> PitchDirection.SHARP
    }

    private fun baseUiState(): PitchMeterUiState {
        val tuning = selectedGuitarTuning()
        return PitchMeterUiState(
            practiceMode = practiceMode,
            inTuneRangeCents = IN_TUNE_RANGE_CENTS,
            selectedGuitarTuningId = tuning.id,
            selectedGuitarTuningName = tuning.name,
            selectedGuitarTuningNotes = tuning.noteSequence,
            selectedGuitarStringNumber = selectedGuitarTarget().number,
            selectedGuitarStringLabel = selectedGuitarTarget().displayName,
            selectedGuitarStringFrequencyText = "%.1f Hz".format(selectedGuitarTarget().frequencyHz),
            isReferenceTonePlaying = referenceToneJob?.isActive == true,
            guitarTunings = guitarTuningOptions(),
            guitarStrings = guitarStringStates(),
        )
    }

    private fun selectedGuitarTuning() = GuitarTunings.find(selectedGuitarTuningId)

    private fun selectedGuitarTarget() = selectedGuitarTuning()
        .strings
        .firstOrNull { string -> string.number == selectedGuitarStringNumber }
        ?: selectedGuitarTuning().strings.first()

    private fun guitarTuningOptions(): List<GuitarTuningOptionUiState> {
        return GuitarTunings.all.map { tuning ->
            GuitarTuningOptionUiState(
                id = tuning.id,
                name = tuning.name,
                shortName = tuning.shortName,
            )
        }
    }

    private fun stopReferenceTone() {
        referenceToneJob?.cancel()
        referenceToneJob = null
    }

    private suspend fun appendAnalysisFrameBuffered(sessionId: String, state: PitchAnalysisState) {
        pendingFrames.add(state.toSnapshot())

        val lastFlush = lastFrameFlushAtMs
        val shouldFlush = pendingFrames.size >= FRAME_BATCH_SIZE ||
            (lastFlush != null && state.timestampMs - lastFlush >= FRAME_FLUSH_INTERVAL_MS)

        if (lastFlush == null) {
            lastFrameFlushAtMs = state.timestampMs
        } else if (shouldFlush) {
            flushPendingFrames(sessionId)
        }
    }

    private suspend fun flushPendingFrames(sessionId: String) {
        if (pendingFrames.isEmpty()) return

        val frames = pendingFrames.toList()
        pendingFrames.clear()
        repository?.appendFrames(sessionId, frames)
        lastFrameFlushAtMs = frames.lastOrNull()?.timestampMs
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

    companion object {
        private const val UI_UPDATE_INTERVAL_MS = 160L
        private const val FRAME_BATCH_SIZE = 24
        private const val FRAME_FLUSH_INTERVAL_MS = 1_000L
        private const val IN_TUNE_RANGE_CENTS = 10f

        @SuppressLint("MissingPermission")
        fun factory(context: Context): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val appContext = context.applicationContext
                val sessionDir = File(appContext.filesDir, "practice-sessions")
                PitchMeterViewModel(
                    audioRecorder = AudioRecordAudioRecorder(),
                    analyzer = PitchAnalyzer(),
                    repository = FilePracticeSessionRepository(sessionDir),
                    guitarReferenceTonePlayer = GuitarReferenceTonePlayer(
                        sample = GuitarReferenceSample.fromRawResource(
                            context = appContext,
                            resourceId = R.raw.acoustic_guitar_gc,
                        ),
                    ),
                )
            }
        }
    }
}
