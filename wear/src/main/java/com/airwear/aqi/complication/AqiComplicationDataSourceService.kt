package com.airwear.aqi.complication

import android.app.PendingIntent
import android.content.Intent
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.LongTextComplicationData
import androidx.wear.watchface.complications.data.NoDataComplicationData
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.RangedValueComplicationData
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import com.airwear.aqi.AqiApp
import com.airwear.aqi.MainActivity
import com.airwear.aqi.R
import com.airwear.aqi.domain.AqiCategory
import kotlinx.coroutines.flow.first

/**
 * Watch-face complication data source.
 *
 * Manifest must declare (official strings — do not rename):
 *  permission: android.permission.BIND_COMPLICATION_DATA_SOURCE
 *  action:     android.support.wearable.complications.ACTION_COMPLICATION_DATA_SOURCE
 *
 * Reads the cache only; [ComplicationUpdater] is poked whenever the
 * repository saves a new reading.
 */
class AqiComplicationDataSourceService : SuspendingComplicationDataSourceService() {

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        // Sample values shown in the watch face customize UI only.
        val aqi = 54
        val category = AqiCategory.fromAqi(aqi)
        return build(type, aqi, category, city = "Jakarta")
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        val snapshot = (application as? AqiApp)?.repository?.cachedSnapshot?.first()
            ?: return NoDataComplicationData()
        return build(request.complicationType, snapshot.aqiUs, snapshot.category, snapshot.city)
    }

    private fun build(
        type: ComplicationType,
        aqi: Int,
        category: AqiCategory,
        city: String?,
    ): ComplicationData? = when (type) {
        ComplicationType.SHORT_TEXT -> shortText(aqi, category)
        ComplicationType.RANGED_VALUE -> rangedValue(aqi, category)
        ComplicationType.LONG_TEXT -> longText(aqi, category, city)
        else -> null
    }

    private fun tapPendingIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun description(aqi: Int, category: AqiCategory) =
        PlainComplicationText.Builder(getString(R.string.aqi_content_description, aqi, getString(category.labelRes))).build()

    private fun shortText(aqi: Int, category: AqiCategory) =
        ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder(aqi.toString()).build(),
            contentDescription = description(aqi, category),
        )
            .setTitle(PlainComplicationText.Builder(getString(category.shortLabelRes)).build())
            .setTapAction(tapPendingIntent())
            .build()

    /** Same 0–300 scale as the in-app ring (300+ is pinned to full). */
    private fun rangedValue(aqi: Int, category: AqiCategory) =
        RangedValueComplicationData.Builder(
            value = aqi.coerceIn(0, RANGE_MAX).toFloat(),
            min = 0f,
            max = RANGE_MAX.toFloat(),
            contentDescription = description(aqi, category),
        )
            .setText(PlainComplicationText.Builder(aqi.toString()).build())
            .setTitle(PlainComplicationText.Builder(getString(category.shortLabelRes)).build())
            .setTapAction(tapPendingIntent())
            .build()

    private fun longText(aqi: Int, category: AqiCategory, city: String?): LongTextComplicationData {
        val body = buildString {
            append("AQI $aqi · ${getString(category.labelRes)}")
            if (!city.isNullOrBlank()) append(" · $city")
        }
        return LongTextComplicationData.Builder(
            text = PlainComplicationText.Builder(body).build(),
            contentDescription = PlainComplicationText.Builder(body).build(),
        )
            .setTitle(PlainComplicationText.Builder(getString(R.string.app_name)).build())
            .setTapAction(tapPendingIntent())
            .build()
    }

    private companion object {
        const val RANGE_MAX = 300
    }
}
