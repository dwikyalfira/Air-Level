package com.airwear.aqi.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.airwear.aqi.domain.AqiCategory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

/** IQAir-style big number + face emoji. */
@Composable
fun AqiFaceNumber(
    aqi: Int,
    category: AqiCategory,
    modifier: Modifier = Modifier,
    numberSize: androidx.compose.ui.unit.TextUnit = 50.sp,
    faceSize: androidx.compose.ui.unit.TextUnit = 32.sp,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = aqi.toString(),
            fontSize = numberSize,
            fontWeight = FontWeight.Bold,
            color = category.color,
            lineHeight = numberSize * 1.05f,
        )
        Spacer(Modifier.width(6.dp))
        Text(text = category.face, fontSize = faceSize)
    }
}

/**
 * Full-circle EPA color scale (ref image 3).
 * Green → yellow → orange → red → purple around the bezel.
 */
@Composable
fun AqiScaleRing(
    aqi: Int,
    modifier: Modifier = Modifier,
    ringSize: Dp = 172.dp,
    stroke: Dp = 11.dp,
    content: @Composable () -> Unit,
) {
    val progress = aqi.coerceIn(0, 300) / 300f
    val tipColor = AqiCategory.fromAqi(aqi).color

    Box(modifier = modifier.size(ringSize), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = stroke.toPx()
            val inset = strokePx / 2f + 2.dp.toPx()
            val canvasW = this.size.width
            val canvasH = this.size.height
            val arcSize = Size(canvasW - inset * 2, canvasH - inset * 2)
            val topLeft = Offset(inset, inset)
            val max = 300f
            val stops = AqiCategory.scaleColors

            for (i in 0 until stops.size - 1) {
                val (startAqi, startColor) = stops[i]
                val (endAqi, endColor) = stops[i + 1]
                val startAngle = -90f + (startAqi / max) * 360f
                val sweep = ((endAqi - startAqi) / max) * 360f
                drawArc(
                    brush = Brush.sweepGradient(listOf(startColor, endColor)),
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Butt),
                )
            }

            // Tip marker at current AQI
            val tipDeg = -90f + progress * 360f
            val rad = Math.toRadians(tipDeg.toDouble())
            val cx = canvasW / 2f
            val cy = canvasH / 2f
            val r = (canvasW - inset * 2) / 2f
            drawCircle(
                color = tipColor,
                radius = strokePx * 0.65f,
                center = Offset(cx + (r * cos(rad)).toFloat(), cy + (r * sin(rad)).toFloat()),
            )
        }
        content()
    }
}

/** Simple solid progress ring (ref image 2). */
@Composable
fun AqiProgressRing(
    aqi: Int,
    category: AqiCategory,
    modifier: Modifier = Modifier,
    ringSize: Dp = 128.dp,
    stroke: Dp = 9.dp,
) {
    Box(modifier = modifier.size(ringSize), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = stroke.toPx()
            val inset = strokePx / 2f
            val canvasW = this.size.width
            val canvasH = this.size.height
            val arcSize = Size(canvasW - inset * 2, canvasH - inset * 2)
            val topLeft = Offset(inset, inset)
            val progress = aqi.coerceIn(0, 300) / 300f

            drawArc(
                color = Color.White.copy(alpha = 0.08f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round),
            )
            if (progress > 0f) {
                drawArc(
                    color = category.color,
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round),
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = aqi.toString(),
                fontSize = 44.sp,
                fontWeight = FontWeight.Bold,
                color = category.color,
            )
            Text(
                text = category.shortLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

/** Bottom sensor card (ref image 1). */
@Composable
fun SensorCard(
    station: String,
    sourceLine: String,
    accent: Color,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(if (compact) 22.dp else 28.dp))
            .background(accent.copy(alpha = 0.20f))
            .padding(
                horizontal = if (compact) 12.dp else 14.dp,
                vertical = if (compact) 8.dp else 12.dp,
            ),
    ) {
        Text(
            text = "Nearest sensor",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.70f),
        )
        Spacer(Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("📡", fontSize = if (compact) 13.sp else 15.sp)
            Spacer(Modifier.width(6.dp))
            Text(
                text = station,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = sourceLine,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.75f),
        )
    }
}

fun formatUpdatedAgo(epochMs: Long, nowMs: Long = System.currentTimeMillis()): String {
    if (epochMs <= 0L) return "—"
    val diff = (nowMs - epochMs).coerceAtLeast(0L)
    val mins = diff / 60_000L
    return when {
        diff < 60_000L -> "just now"
        mins < 60 -> "${mins} min. ago"
        mins < 24 * 60 -> "${mins / 60}h ago"
        else -> SimpleDateFormat("MMM d · HH:mm", Locale.getDefault()).format(Date(epochMs))
    }
}

fun formatClock(epochMs: Long): String {
    if (epochMs <= 0L) return "—"
    return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(epochMs))
}
