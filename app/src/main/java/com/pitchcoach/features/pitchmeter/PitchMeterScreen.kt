package com.pitchcoach.features.pitchmeter

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pitchcoach.design.components.LevelBar
import com.pitchcoach.design.components.PitchGauge

@Composable
fun PitchMeterScreen(
    uiState: PitchMeterUiState,
    history: List<SessionHistoryItemUiState>,
    hasAudioPermission: Boolean,
    onStart: () -> Unit,
    onPauseToggle: () -> Unit,
    onSave: () -> Unit,
    onDeleteSession: (String) -> Unit,
    onTogglePlayback: (String) -> Unit,
    onModeSelected: (PitchPracticeMode) -> Unit,
    onGuitarTuningSelected: (String) -> Unit,
    onGuitarStringSelected: (Int) -> Unit,
    onToggleReferenceTone: () -> Unit,
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        bottomBar = {
            StudioTabBar(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding),
        ) {
            if (selectedTab == 0) {
                MeterContent(
                    uiState = uiState,
                    hasAudioPermission = hasAudioPermission,
                    onStart = onStart,
                    onPauseToggle = onPauseToggle,
                    onSave = onSave,
                    onModeSelected = onModeSelected,
                    onGuitarTuningSelected = onGuitarTuningSelected,
                    onGuitarStringSelected = onGuitarStringSelected,
                    onToggleReferenceTone = onToggleReferenceTone,
                )
            } else {
                HistoryContent(
                    history = history,
                    onDeleteSession = onDeleteSession,
                    onTogglePlayback = onTogglePlayback,
                )
            }
        }
    }
}

@Composable
private fun MeterContent(
    uiState: PitchMeterUiState,
    hasAudioPermission: Boolean,
    onStart: () -> Unit,
    onPauseToggle: () -> Unit,
    onSave: () -> Unit,
    onModeSelected: (PitchPracticeMode) -> Unit,
    onGuitarTuningSelected: (String) -> Unit,
    onGuitarStringSelected: (Int) -> Unit,
    onToggleReferenceTone: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(26.dp),
    ) {
        item {
            StudioHeader(
                eyebrow = if (uiState.isListening) "LIVE TAKE" else "PITCH STUDIO",
                title = "PitchCoach",
                subtitle = if (uiState.practiceMode == PitchPracticeMode.GUITAR) {
                    "${uiState.selectedGuitarTuningName} · ${uiState.selectedGuitarTuningNotes}"
                } else if (uiState.isListening) {
                    "正在采集本次练习文件"
                } else {
                    "十二平均律参考 · 合格区间 ±${uiState.inTuneRangeCents.toInt()} cents"
                },
            )
        }
        item {
            PracticeModeSwitcher(
                selectedMode = uiState.practiceMode,
                onModeSelected = onModeSelected,
            )
        }
        if (uiState.practiceMode == PitchPracticeMode.GUITAR) {
            item {
                GuitarTuningControls(
                    options = uiState.guitarTunings,
                    selectedId = uiState.selectedGuitarTuningId,
                    onSelected = onGuitarTuningSelected,
                )
            }
        }
        if (uiState.practiceMode == PitchPracticeMode.GUITAR) {
            item {
                GuitarTunerPanel(
                    uiState = uiState,
                    hasAudioPermission = hasAudioPermission,
                    onStart = onStart,
                    onPauseToggle = onPauseToggle,
                    onSave = onSave,
                    onGuitarStringSelected = onGuitarStringSelected,
                    onToggleReferenceTone = onToggleReferenceTone,
                )
            }
        } else {
            item {
                PitchStage(
                    uiState = uiState,
                    hasAudioPermission = hasAudioPermission,
                    onStart = onStart,
                    onPauseToggle = onPauseToggle,
                    onSave = onSave,
                )
            }
            item {
                StudioMetrics(uiState = uiState)
            }
        }
        item {
            LastSummary(summary = uiState.lastSummaryText)
        }
        item {
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
private fun StudioHeader(
    eyebrow: String,
    title: String,
    subtitle: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = eyebrow,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PitchStage(
    uiState: PitchMeterUiState,
    hasAudioPermission: Boolean,
    onStart: () -> Unit,
    onPauseToggle: () -> Unit,
    onSave: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ListeningStatusChip(isListening = uiState.isListening, isPaused = uiState.isPaused)
        Text(
            text = uiState.noteLabel,
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            textAlign = TextAlign.Center,
        )
        Text(
            text = uiState.centsText,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            textAlign = TextAlign.Center,
        )
        Text(
            text = if (hasAudioPermission) uiState.directionText else "需要麦克风权限",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        PitchGauge(
            cents = uiState.centsValue,
            trail = uiState.centsTrail,
            inTuneRangeCents = uiState.inTuneRangeCents,
        )
        MeterControls(
            isListening = uiState.isListening,
            isPaused = uiState.isPaused,
            hasAudioPermission = hasAudioPermission,
            onStart = onStart,
            onPauseToggle = onPauseToggle,
            onSave = onSave,
        )
    }
}

@Composable
private fun GuitarTunerPanel(
    uiState: PitchMeterUiState,
    hasAudioPermission: Boolean,
    onStart: () -> Unit,
    onPauseToggle: () -> Unit,
    onSave: () -> Unit,
    onGuitarStringSelected: (Int) -> Unit,
    onToggleReferenceTone: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        ListeningStatusChip(isListening = uiState.isListening, isPaused = uiState.isPaused)
        Text(
            text = "第 ${uiState.selectedGuitarStringNumber} 弦",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = uiState.selectedGuitarStringLabel,
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            textAlign = TextAlign.Center,
        )
        Text(
            text = uiState.selectedGuitarStringFrequencyText,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
        Text(
            text = if (hasAudioPermission) uiState.directionText else "需要麦克风权限",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        PitchGauge(
            cents = uiState.centsValue,
            trail = uiState.centsTrail,
            inTuneRangeCents = uiState.inTuneRangeCents,
        )
        FilledTonalButton(
            onClick = onToggleReferenceTone,
            shape = CircleShape,
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
        ) {
            Icon(
                imageVector = if (uiState.isReferenceTonePlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = null,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = if (uiState.isReferenceTonePlaying) "停止标准音" else "播放标准音")
        }
        GuitarStringStrip(
            strings = uiState.guitarStrings,
            onSelected = onGuitarStringSelected,
        )
        MeterControls(
            isListening = uiState.isListening,
            isPaused = uiState.isPaused,
            hasAudioPermission = hasAudioPermission,
            onStart = onStart,
            onPauseToggle = onPauseToggle,
            onSave = onSave,
        )
    }
}

@Composable
private fun PracticeModeSwitcher(
    selectedMode: PitchPracticeMode,
    onModeSelected: (PitchPracticeMode) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ModePill(
            selected = selectedMode == PitchPracticeMode.CHROMATIC,
            title = "十二平均律",
            subtitle = "人声",
            onClick = { onModeSelected(PitchPracticeMode.CHROMATIC) },
            modifier = Modifier.weight(1f),
        )
        ModePill(
            selected = selectedMode == PitchPracticeMode.GUITAR,
            title = "吉他",
            subtitle = "调音",
            onClick = { onModeSelected(PitchPracticeMode.GUITAR) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ModePill(
    selected: Boolean,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = if (selected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
    val content = if (selected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurfaceVariant

    Button(
        onClick = onClick,
        modifier = modifier,
        shape = CircleShape,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = content,
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = title, style = MaterialTheme.typography.labelLarge)
            Text(text = subtitle, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun GuitarTuningControls(
    options: List<GuitarTuningOptionUiState>,
    selectedId: String,
    onSelected: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        options.forEach { option ->
            val selected = option.id == selectedId
            Button(
                onClick = { onSelected(option.id) },
                shape = CircleShape,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    },
                    contentColor = if (selected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
            ) {
                Text(text = option.shortName, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun GuitarStringStrip(
    strings: List<GuitarStringUiState>,
    onSelected: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.14f))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            strings.forEach { string ->
                GuitarStringIndicator(
                    string = string,
                    onClick = { onSelected(string.number) },
                )
            }
        }
    }
}

@Composable
private fun GuitarStringIndicator(
    string: GuitarStringUiState,
    onClick: () -> Unit,
) {
    val activeColor = when {
        string.cents == null -> MaterialTheme.colorScheme.onSurfaceVariant
        kotlin.math.abs(string.cents) <= 10f -> MaterialTheme.colorScheme.secondary
        string.cents < 0f -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }
    val contentColor = if (string.isSelected) activeColor else MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = string.number.toString(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Surface(
            onClick = onClick,
            modifier = Modifier
                .size(if (string.isSelected) 44.dp else 36.dp),
            shape = CircleShape,
            color = if (string.isSelected) {
                activeColor.copy(alpha = 0.18f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
            },
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = string.noteLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = contentColor,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun ListeningStatusChip(isListening: Boolean, isPaused: Boolean) {
    val color = when {
        isPaused -> MaterialTheme.colorScheme.tertiary
        isListening -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outline
    }
    Surface(
        shape = CircleShape,
        color = color.copy(alpha = 0.12f),
        contentColor = color,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(color),
            )
            Text(
                text = when {
                    isPaused -> "已暂停"
                    isListening -> "正在听"
                    else -> "准备就绪"
                },
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
private fun MeterControls(
    isListening: Boolean,
    isPaused: Boolean,
    hasAudioPermission: Boolean,
    onStart: () -> Unit,
    onPauseToggle: () -> Unit,
    onSave: () -> Unit,
) {
    if (!isListening) {
        Button(
            onClick = onStart,
            shape = CircleShape,
            contentPadding = PaddingValues(horizontal = 26.dp, vertical = 15.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.onBackground,
                contentColor = MaterialTheme.colorScheme.background,
            ),
        ) {
            Icon(Icons.Rounded.PlayArrow, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = if (hasAudioPermission) "开始" else "授权并开始")
        }
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FilledTonalButton(
                onClick = onPauseToggle,
                shape = CircleShape,
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
            ) {
                Icon(
                    imageVector = if (isPaused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                    contentDescription = null,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = if (isPaused) "继续" else "暂停")
            }
            Button(
                onClick = onSave,
                shape = CircleShape,
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Icon(Icons.Rounded.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "保存")
            }
        }
    }
}

@Composable
private fun StudioMetrics(uiState: PitchMeterUiState) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
        Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
            MetricValue(label = "频率", value = uiState.frequencyText, modifier = Modifier.weight(1f))
            MetricValue(label = "稳定度", value = "${uiState.stabilityPercent}%", modifier = Modifier.weight(1f))
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
        MeterLevelRow(
            label = "音量",
            value = "${uiState.volumePercent}%",
            icon = Icons.Rounded.Mic,
            percent = uiState.volumePercent,
            color = MaterialTheme.colorScheme.primary,
        )
        MeterLevelRow(
            label = "识别清晰度",
            value = "${uiState.confidencePercent}%",
            icon = Icons.Rounded.GraphicEq,
            percent = uiState.confidencePercent,
            color = MaterialTheme.colorScheme.secondary,
        )
    }
}

@Composable
private fun MetricValue(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
        )
    }
}

@Composable
private fun MeterLevelRow(
    label: String,
    value: String,
    icon: ImageVector,
    percent: Int,
    color: Color,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
                Text(text = label, style = MaterialTheme.typography.labelLarge)
            }
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        LevelBar(percent = percent, color = color)
    }
}

@Composable
private fun LastSummary(summary: String?) {
    AnimatedVisibility(visible = summary != null) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
            Text(
                text = summary.orEmpty(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}

@Composable
private fun HistoryContent(
    history: List<SessionHistoryItemUiState>,
    onDeleteSession: (String) -> Unit,
    onTogglePlayback: (String) -> Unit,
) {
    var pendingDelete by remember { mutableStateOf<SessionHistoryItemUiState?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            StudioHeader(
                eyebrow = "TAKE FILES",
                title = "记录",
                subtitle = "${history.size} 个已保存文件",
            )
        }
        if (history.isEmpty()) {
            item {
                EmptyHistory()
            }
        } else {
            items(history, key = { it.id }) { item ->
                HistoryItem(
                    item = item,
                    onTogglePlayback = { onTogglePlayback(item.id) },
                    onDelete = { pendingDelete = item },
                )
            }
        }
    }

    pendingDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除这次练习？") },
            text = { Text("删除后，这个训练文件和它的评估数据都会移除。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteSession(item.id)
                        pendingDelete = null
                    },
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text("取消")
                }
            },
        )
    }
}

@Composable
private fun EmptyHistory() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = Icons.Rounded.Mic,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(28.dp),
        )
        Text(
            text = "还没有练习文件",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = "保存一次 take 后，这里会显示它的评估结果。",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun HistoryItem(
    item: SessionHistoryItemUiState,
    onTogglePlayback: () -> Unit,
    onDelete: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = item.startedAtText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = item.durationText,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                IconButton(onClick = onTogglePlayback) {
                    Icon(
                        imageVector = if (item.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (item.isPlaying) "停止播放" else "播放录音",
                        tint = if (item.isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = "删除",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            HistoryMetric(label = "平均偏差", value = item.averageAbsCentsText, modifier = Modifier.weight(1f))
            HistoryMetric(label = "稳定度", value = item.stabilityText, modifier = Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            HistoryMetric(label = "音域", value = item.rangeText, modifier = Modifier.weight(1f))
            HistoryMetric(label = "有效帧", value = item.voicedFramesText, modifier = Modifier.weight(1f))
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.14f))
    }
}

@Composable
private fun HistoryMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
        )
    }
}

@Composable
private fun StudioTabBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.96f),
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            StudioTab(
                selected = selectedTab == 0,
                icon = Icons.Rounded.GraphicEq,
                contentDescription = "练习",
                onClick = { onTabSelected(0) },
                modifier = Modifier.weight(1f),
            )
            StudioTab(
                selected = selectedTab == 1,
                icon = Icons.Rounded.History,
                contentDescription = "记录",
                onClick = { onTabSelected(1) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun StudioTab(
    selected: Boolean,
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = if (selected) {
        MaterialTheme.colorScheme.onBackground
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
    }
    val content = if (selected) {
        MaterialTheme.colorScheme.background
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Button(
        onClick = onClick,
        modifier = modifier,
        shape = CircleShape,
        contentPadding = PaddingValues(vertical = 12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = content,
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
    ) {
        Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(20.dp))
    }
}
