package com.airwear.aqi.tile

import android.content.Context
import androidx.compose.ui.graphics.toArgb
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ColorBuilders.argb
import androidx.wear.protolayout.DimensionBuilders.degrees
import androidx.wear.protolayout.DimensionBuilders.dp
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.DimensionBuilders.sp
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.LayoutElementBuilders.Arc
import androidx.wear.protolayout.LayoutElementBuilders.ArcLine
import androidx.wear.protolayout.LayoutElementBuilders.ArcSpacer
import androidx.wear.protolayout.LayoutElementBuilders.Box
import androidx.wear.protolayout.LayoutElementBuilders.Column
import androidx.wear.protolayout.LayoutElementBuilders.FontStyle
import androidx.wear.protolayout.LayoutElementBuilders.LayoutElement
import androidx.wear.protolayout.LayoutElementBuilders.Spacer
import androidx.wear.protolayout.LayoutElementBuilders.Text
import androidx.wear.protolayout.ModifiersBuilders.Background
import androidx.wear.protolayout.ModifiersBuilders.Clickable
import androidx.wear.protolayout.ModifiersBuilders.Corner
import androidx.wear.protolayout.ModifiersBuilders.Modifiers
import androidx.wear.protolayout.ModifiersBuilders.Padding
import androidx.wear.protolayout.ModifiersBuilders.Semantics
import com.airwear.aqi.MainActivity
import com.airwear.aqi.R
import com.airwear.aqi.domain.AqiCategory
import com.airwear.aqi.domain.AqiSnapshot
import com.airwear.aqi.ui.theme.AqiPalette

/**
 * ProtoLayout version of the Home screen: same gauge geometry (starts at
 * 1 o'clock, 1 AQI = 1°, 60° gap centred on 12), number, category pill, data
 * source + measurement time, and a refresh pill showing the last fetch.
 */
object AqiTileLayout {

    const val ID_OPEN = "open"
    const val ID_REFRESH = "refresh"

    private const val GAUGE_START = 30f       // 1 o'clock; ProtoLayout measures clockwise from 12
    private const val STROKE_DP = 5f
    private const val GAP_DEG = 5f

    private val ON_SURFACE = 0xFFE6E7EB.toInt()
    private val ON_SURFACE_VARIANT = 0xFFC2C6CF.toInt()
    private val SURFACE_HIGH = 0xFF2A2C31.toInt()
    private val ERROR = 0xFFFFB4AB.toInt()

    fun build(context: Context, snapshot: AqiSnapshot?, refreshing: Boolean, error: String?): LayoutElement {
        val root = Box.Builder()
            .setWidth(expand())
            .setHeight(expand())
            .setModifiers(
                Modifiers.Builder()
                    .setClickable(openApp(context))
                    .build(),
            )

        if (snapshot == null) {
            root.addContent(gauge(0f))
            root.addContent(
                Column.Builder()
                    .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
                    .addContent(text(context.getString(R.string.app_name), 16f, ON_SURFACE, bold = true))
                    .addContent(spacer(6f))
                    .addContent(text(error ?: context.getString(R.string.tile_no_data), 12f, ON_SURFACE_VARIANT, maxLines = 2))
                    .addContent(spacer(10f))
                    .addContent(refreshPill(context, context.getString(R.string.retry), refreshing, error != null))
                    .build(),
            )
            return root.build()
        }

        val category = snapshot.category
        val tones = AqiPalette.of(category)
        val primary = tones.primary.toArgb()
        val categoryLabel = context.getString(category.labelRes).let {
            if (it.length > 16) context.getString(category.shortLabelRes) else it
        }
        val clock = android.text.format.DateFormat.getTimeFormat(context)

        root.addContent(gauge(snapshot.aqiUs.toFloat()))
        root.addContent(
            Column.Builder()
                .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
                .setModifiers(
                    Modifiers.Builder()
                        .setSemantics(
                            Semantics.Builder()
                                .setContentDescription(
                                    context.getString(
                                        R.string.aqi_content_description,
                                        snapshot.aqiUs,
                                        context.getString(category.labelRes),
                                    ),
                                )
                                .build(),
                        )
                        .build(),
                )
                .addContent(text(snapshot.stationLabel, 13f, ON_SURFACE, maxLines = 1))
                .addContent(text(snapshot.aqiUs.toString(), 46f, primary, bold = true))
                .addContent(pill(categoryLabel, tones.primaryContainer.toArgb(), tones.onPrimaryContainer.toArgb()))
                .addContent(spacer(6f))
                .addContent(
                    text(
                        (if (snapshot.isStale()) "⚠ " else "") + context.getString(
                            R.string.source_measured,
                            context.getString(R.string.source_short),
                            clock.format(java.util.Date(snapshot.updatedAtEpochMs)),
                        ),
                        10f,
                        if (snapshot.isStale()) primary else ON_SURFACE_VARIANT,
                        maxLines = 1,
                    ),
                )
                .addContent(spacer(4f))
                .addContent(
                    refreshPill(
                        context,
                        when {
                            refreshing -> context.getString(R.string.refreshing)
                            error != null -> error
                            else -> context.getString(
                                R.string.updated_ago,
                                clock.format(java.util.Date(snapshot.sourceFetchedAtMs)),
                            )
                        },
                        refreshing,
                        error != null,
                    ),
                )
                .build(),
        )
        return root.build()
    }

    /** Two stacked arcs: dim EPA track, then the filled value on top. */
    private fun gauge(value: Float): LayoutElement {
        val bands = AqiCategory.scale.dropLast(1) // 0..300
        val v = value.coerceIn(0f, AqiCategory.SCALE_MAX.toFloat())

        val track = Arc.Builder()
            .setAnchorAngle(degrees(GAUGE_START))
            .setAnchorType(LayoutElementBuilders.ARC_ANCHOR_START)
        val fill = Arc.Builder()
            .setAnchorAngle(degrees(GAUGE_START))
            .setAnchorType(LayoutElementBuilders.ARC_ANCHOR_START)

        bands.forEach { band ->
            val from = if (band.rangeMin > 0) band.rangeMin - 1f else 0f
            val len = band.rangeMax - from - GAP_DEG
            val bandColor = band.color.toArgb()
            track.addContent(arcSpacer(GAP_DEG / 2f))
            track.addContent(arcLine(len, (bandColor and 0x00FFFFFF) or (0x38 shl 24)))
            track.addContent(arcSpacer(GAP_DEG / 2f))

            val filled = (v - from - GAP_DEG / 2f).coerceIn(0f, len)
            fill.addContent(arcSpacer(GAP_DEG / 2f))
            if (filled > 0.5f) fill.addContent(arcLine(filled, bandColor))
            if (len - filled > 0.5f) fill.addContent(arcSpacer(len - filled))
            fill.addContent(arcSpacer(GAP_DEG / 2f))
        }

        val pad = Modifiers.Builder()
            .setPadding(Padding.Builder().setAll(dp(3f)).build())
            .build()
        return Box.Builder()
            .setWidth(expand())
            .setHeight(expand())
            .setModifiers(pad)
            .addContent(track.build())
            .addContent(fill.build())
            .build()
    }

    private fun arcLine(lengthDeg: Float, color: Int) = ArcLine.Builder()
        .setLength(degrees(lengthDeg))
        .setThickness(dp(STROKE_DP))
        .setColor(argb(color))
        .setStrokeCap(LayoutElementBuilders.STROKE_CAP_ROUND)
        .build()

    private fun arcSpacer(lengthDeg: Float) = ArcSpacer.Builder()
        .setLength(degrees(lengthDeg))
        .setThickness(dp(STROKE_DP))
        .build()

    private fun text(
        value: String,
        sizeSp: Float,
        color: Int,
        bold: Boolean = false,
        maxLines: Int = 1,
    ) = Text.Builder()
        .setText(value)
        .setMaxLines(maxLines)
        .setFontStyle(
            FontStyle.Builder()
                .setSize(sp(sizeSp))
                .setColor(argb(color))
                .setWeight(
                    if (bold) LayoutElementBuilders.FONT_WEIGHT_BOLD else LayoutElementBuilders.FONT_WEIGHT_MEDIUM,
                )
                .build(),
        )
        .build()

    private fun spacer(heightDp: Float) = Spacer.Builder().setHeight(dp(heightDp)).build()

    private fun pill(label: String, container: Int, content: Int) = Box.Builder()
        .setModifiers(
            Modifiers.Builder()
                .setBackground(
                    Background.Builder()
                        .setColor(argb(container))
                        .setCorner(Corner.Builder().setRadius(dp(12f)).build())
                        .build(),
                )
                .setPadding(
                    Padding.Builder()
                        .setStart(dp(12f)).setEnd(dp(12f))
                        .setTop(dp(3f)).setBottom(dp(3f))
                        .build(),
                )
                .build(),
        )
        .addContent(text(label, 12f, content))
        .build()

    /** Small refresh pill; LoadAction re-requests the tile with id [ID_REFRESH]. */
    private fun refreshPill(context: Context, label: String, refreshing: Boolean, error: Boolean): LayoutElement {
        val prefix = if (refreshing) "… " else if (error) "⚠ " else "⟳ "
        return Box.Builder()
            .setModifiers(
                Modifiers.Builder()
                    .setClickable(
                        Clickable.Builder()
                            .setId(ID_REFRESH)
                            .setOnClick(ActionBuilders.LoadAction.Builder().build())
                            .build(),
                    )
                    .setSemantics(
                        Semantics.Builder()
                            .setContentDescription("${context.getString(R.string.refresh)}. $label")
                            .build(),
                    )
                    .setBackground(
                        Background.Builder()
                            .setColor(argb(if (error) 0x99_93000A.toInt() else SURFACE_HIGH))
                            .setCorner(Corner.Builder().setRadius(dp(12f)).build())
                            .build(),
                    )
                    .setPadding(
                        Padding.Builder()
                            .setStart(dp(10f)).setEnd(dp(10f))
                            .setTop(dp(5f)).setBottom(dp(5f))
                            .build(),
                    )
                    .build(),
            )
            .addContent(text(prefix + label, 10f, if (error) ERROR else ON_SURFACE, maxLines = 1))
            .build()
    }

    private fun openApp(context: Context) = Clickable.Builder()
        .setId(ID_OPEN)
        .setOnClick(
            ActionBuilders.LaunchAction.Builder()
                .setAndroidActivity(
                    ActionBuilders.AndroidActivity.Builder()
                        .setPackageName(context.packageName)
                        .setClassName(MainActivity::class.java.name)
                        .build(),
                )
                .build(),
        )
        .build()
}
