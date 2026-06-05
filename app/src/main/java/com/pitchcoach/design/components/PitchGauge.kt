package com.pitchcoach.design.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun PitchGauge(
    cents: Float?,
    trail: List<Float?> = emptyList(),
    inTuneRangeCents: Float = 10f,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val trackColor = colors.surfaceVariant
    val centerColor = colors.secondary
    val flatColor = colors.tertiary
    val sharpColor = colors.primary
    val markerColor = colors.outline

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(96.dp),
    ) {
        val trackStart = Offset(24.dp.toPx(), size.height * 0.58f)
        val trackEnd = Offset(size.width - 24.dp.toPx(), size.height * 0.58f)
        val centerX = size.width / 2f
        val halfWidth = (trackEnd.x - trackStart.x) / 2f

        drawLine(
            color = trackColor,
            start = trackStart,
            end = trackEnd,
            strokeWidth = 12.dp.toPx(),
            cap = StrokeCap.Round,
        )

        listOf(-50, -25, 0, 25, 50).forEach { mark ->
            val x = centerX + (mark / 50f) * halfWidth
            val height = if (mark == 0) 28.dp.toPx() else 16.dp.toPx()
            drawLine(
                color = markerColor.copy(alpha = if (mark == 0) 0.75f else 0.42f),
                start = Offset(x, trackStart.y - height / 2f),
                end = Offset(x, trackStart.y + height / 2f),
                strokeWidth = if (mark == 0) 2.dp.toPx() else 1.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }

        val zoneHalfWidth = (inTuneRangeCents.coerceIn(1f, 50f) / 50f) * halfWidth
        drawLine(
            color = centerColor.copy(alpha = 0.32f),
            start = Offset(centerX - zoneHalfWidth, trackStart.y),
            end = Offset(centerX + zoneHalfWidth, trackStart.y),
            strokeWidth = 16.dp.toPx(),
            cap = StrokeCap.Round,
        )

        val trailY = trackStart.y + 26.dp.toPx()
        val trailValues = trail.takeLast(24)
        trailValues.forEachIndexed { index, trailCents ->
            if (trailCents != null) {
                val progress = if (trailValues.size <= 1) 1f else index / (trailValues.size - 1f)
                val x = centerX + (trailCents.coerceIn(-50f, 50f) / 50f) * halfWidth
                drawCircle(
                    color = centerColor.copy(alpha = 0.14f + progress * 0.42f),
                    radius = 2.5.dp.toPx() + progress * 2.dp.toPx(),
                    center = Offset(x, trailY),
                )
            }
        }

        val normalized = cents?.coerceIn(-50f, 50f) ?: 0f
        val indicatorX = centerX + (normalized / 50f) * halfWidth
        val indicatorColor = when {
            cents == null -> markerColor
            kotlin.math.abs(cents) <= inTuneRangeCents -> centerColor
            cents < -inTuneRangeCents -> flatColor
            cents > inTuneRangeCents -> sharpColor
            else -> centerColor
        }

        drawLine(
            color = indicatorColor.copy(alpha = 0.35f),
            start = Offset(centerX, trackStart.y),
            end = Offset(indicatorX, trackStart.y),
            strokeWidth = 12.dp.toPx(),
            cap = StrokeCap.Round,
        )
        drawCircle(
            color = indicatorColor,
            radius = 14.dp.toPx(),
            center = Offset(indicatorX, trackStart.y),
        )
        drawCircle(
            color = colors.surface,
            radius = 8.dp.toPx(),
            center = Offset(indicatorX, trackStart.y),
            style = Stroke(width = 3.dp.toPx()),
        )

        val centsText = cents?.roundToInt()?.let { "%+d".format(it) } ?: "--"
        drawContext.canvas.nativeCanvas.apply {
            val paint = android.graphics.Paint().apply {
                isAntiAlias = true
                color = colors.onSurfaceVariant.copy(alpha = 0.72f).toArgb()
                textAlign = android.graphics.Paint.Align.CENTER
                textSize = 13.dp.toPx()
            }
            drawText("偏低", trackStart.x, 20.dp.toPx(), paint)
            drawText(centsText, centerX, 20.dp.toPx(), paint)
            drawText("偏高", trackEnd.x, 20.dp.toPx(), paint)
        }
    }
}
