package com.airwear.aqi.complication

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.LongTextComplicationData
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
 * getPreviewData MUST return non-null for supported types or some watch faces
 * hide this provider from the customization picker.
 */
class AqiComplicationDataSourceService : SuspendingComplicationDataSourceService() {

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        // Sample values shown in the watch face customize UI
        val aqi = 54
        val category = AqiCategory.fromAqi(aqi)
        return when (type) {
            ComplicationType.SHORT_TEXT -> shortText(aqi, category)
            ComplicationType.RANGED_VALUE -> rangedValue(aqi, category)
            ComplicationType.LONG_TEXT -> longText(aqi, category, city = "Sample City")
            else -> shortText(aqi, category)
        }
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        // Live data when cache exists; otherwise fall back to preview so the face still draws
        val snapshot = (application as? AqiApp)?.repository?.cachedSnapshot?.first()
        val aqi = snapshot?.aqiUs ?: 54
        val category = snapshot?.category ?: AqiCategory.fromAqi(aqi)
        val city = snapshot?.city

        return when (request.complicationType) {
            ComplicationType.SHORT_TEXT -> shortText(aqi, category)
            ComplicationType.RANGED_VALUE -> rangedValue(aqi, category)
            ComplicationType.LONG_TEXT -> longText(aqi, category, city)
            else -> shortText(aqi, category)
        }
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

    private fun launcherIcon(): Icon =
        Icon.createWithResource(this, R.drawable.ic_launcher)

    private fun shortText(aqi: Int, category: AqiCategory) =
        ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder(aqi.toString()).build(),
            contentDescription = PlainComplicationText
                .Builder("AQI $aqi ${category.label}")
                .build(),
        )
            .setTitle(PlainComplicationText.Builder(category.shortLabel).build())
            .setTapAction(tapPendingIntent())
            .build()

    /** AQI scale 0–500 (EPA upper bound used by many faces). */
    private fun rangedValue(aqi: Int, category: AqiCategory) =
        RangedValueComplicationData.Builder(
            value = aqi.coerceIn(0, 500).toFloat(),
            min = 0f,
            max = 500f,
            contentDescription = PlainComplicationText
                .Builder("AQI $aqi ${category.label}")
                .build(),
        )
            .setText(PlainComplicationText.Builder(aqi.toString()).build())
            .setTitle(PlainComplicationText.Builder(category.shortLabel).build())
            .setTapAction(tapPendingIntent())
            .build()

    private fun longText(aqi: Int, category: AqiCategory, city: String?): LongTextComplicationData {
        val body = buildString {
            append("AQI $aqi · ${category.label}")
            if (!city.isNullOrBlank()) append(" · $city")
        }
        return LongTextComplicationData.Builder(
            text = PlainComplicationText.Builder(body).build(),
            contentDescription = PlainComplicationText.Builder(body).build(),
        )
            .setTitle(PlainComplicationText.Builder("AirWear").build())
            .setTapAction(tapPendingIntent())
            .build()
    }

    companion object {
        fun forceUpdateAll(@Suppress("UNUSED_PARAMETER") context: Context) = Unit
    }
}
