package com.airwear.aqi.complication

import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester

/** Asks every watch face showing our complication to pull fresh data. */
object ComplicationUpdater {
    fun requestUpdateAll(context: Context) {
        runCatching {
            ComplicationDataSourceUpdateRequester
                .create(
                    context = context.applicationContext,
                    complicationDataSourceComponent = ComponentName(
                        context.applicationContext,
                        AqiComplicationDataSourceService::class.java,
                    ),
                )
                .requestUpdateAll()
        }.onFailure { Log.w("AirWear", "Complication update request failed", it) }
    }
}
