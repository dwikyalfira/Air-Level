package com.airwear.aqi.ui.components

import android.text.format.DateUtils
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnItemScope
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.TransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import com.airwear.aqi.domain.AqiCategory

/**
 * Applies the MD3 list morph (height + container scale/fade) to a custom,
 * non-clickable surface inside a TransformingLazyColumn item.
 */
fun Modifier.morph(scope: TransformingLazyColumnItemScope, spec: TransformationSpec): Modifier =
    this
        .transformedHeight(scope, spec)
        .graphicsLayer {
            with(scope) {
                val progress = scrollProgress
                with(spec) { applyContainerTransformation(progress) }
            }
        }

/** Static MD3 card surface (Wear M3 Card is always clickable). */
@Composable
fun InfoSurface(
    modifier: Modifier = Modifier,
    container: Color = MaterialTheme.colorScheme.surfaceContainer,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(container)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        content = content,
    )
}

@Composable
fun SurfaceTitle(text: String, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Text(text = text, style = MaterialTheme.typography.labelSmall, color = color)
    Spacer(Modifier.height(2.dp))
}

/** Icon + text line used for health tips. */
@Composable
fun TipRow(icon: ImageVector, text: String, tint: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** Round AQI badge in the category colour (cities list). */
@Composable
fun AqiPill(aqi: Int, category: AqiCategory, size: Dp = 44.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(category.color),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = aqi.toString(),
            style = if (aqi >= 100) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge,
            color = Color(0xFF1A1C1E),
            maxLines = 1,
            overflow = TextOverflow.Clip,
        )
    }
}

/**
 * Expressive bezel gauge. 0–300 AQI maps to a 300° arc (1 AQI = 1°) that
 * starts at 1 o'clock and runs clockwise to 11 o'clock, leaving the top
 * free for TimeText. Each EPA band is its own segment: dim track, full
 * colour up to the current value, and a halo marker at the value.
 *
 * [value] (animated AQI) is read in the draw phase only, so the animation
 * redraws the ring without recomposing.
 */
@Composable
fun AqiGauge(
    value: () -> Float,
    modifier: Modifier = Modifier,
    stroke: Dp = 5.dp,
) {
    val bands = AqiCategory.scale.dropLast(1) // GOOD..VERY_UNHEALTHY = 0..300
    val markerFill = MaterialTheme.colorScheme.onSurface
    Canvas(modifier = modifier) {
        val strokePx = stroke.toPx()
        // Marker halo is wider than the stroke; keep it inside the screen.
        val inset = strokePx * 1.6f + 1.dp.toPx()
        val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
        val topLeft = Offset(inset, inset)
        val radius = arcSize.width / 2f
        val center = Offset(size.width / 2f, size.height / 2f)
        // Round caps extend strokePx/2 past each end, so the visible gap must
        // budget for both caps plus ~3dp of air.
        val gapDeg = Math.toDegrees(((strokePx + 3.dp.toPx()) / radius).toDouble()).toFloat()
        val raw = value()
        val markerColor = AqiCategory.fromAqi(raw.toInt()).color
        val value = raw.coerceIn(0f, AqiCategory.SCALE_MAX.toFloat())

        bands.forEach { band ->
            val from = band.rangeMin.toFloat().let { if (it > 0f) it - 1f else it }
            val to = band.rangeMax.toFloat()
            val start = GAUGE_START + from + gapDeg / 2f
            val sweep = (to - from) - gapDeg
            drawArc(
                color = band.color.copy(alpha = 0.22f),
                startAngle = start, sweepAngle = sweep, useCenter = false,
                topLeft = topLeft, size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round),
            )
            val filled = (value - from - gapDeg / 2f).coerceIn(0f, sweep)
            if (filled > 0f) {
                drawArc(
                    color = band.color,
                    startAngle = start, sweepAngle = filled, useCenter = false,
                    topLeft = topLeft, size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round),
                )
            }
        }

        val rad = Math.toRadians((GAUGE_START + value).toDouble())
        val marker = Offset(
            center.x + (radius * kotlin.math.cos(rad)).toFloat(),
            center.y + (radius * kotlin.math.sin(rad)).toFloat(),
        )
        drawCircle(color = markerColor.copy(alpha = 0.35f), radius = strokePx * 1.6f, center = marker)
        drawCircle(color = Color.Black, radius = strokePx * 1.15f, center = marker)
        drawCircle(color = markerFill, radius = strokePx * 0.8f, center = marker)
    }
}

/** 1 o'clock (Compose measures from 3 o'clock) — 60° gap centred on 12 for TimeText. */
private const val GAUGE_START = -60f

/** Localised "5 min. ago" / "5 mnt lalu". */
fun relativeTime(epochMs: Long, nowMs: Long = System.currentTimeMillis()): String {
    if (epochMs <= 0L) return "—"
    // Clamp future timestamps (station clock skew) to "0 min. ago".
    return DateUtils.getRelativeTimeSpanString(
        epochMs.coerceAtMost(nowMs), nowMs, DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE,
    ).toString()
}

/** Wall-clock time in the user's 12/24 h format. */
fun clockTime(context: android.content.Context, epochMs: Long): String =
    if (epochMs <= 0L) "—" else android.text.format.DateFormat.getTimeFormat(context).format(java.util.Date(epochMs))

val ListItemSpacing = Arrangement.spacedBy(6.dp)
