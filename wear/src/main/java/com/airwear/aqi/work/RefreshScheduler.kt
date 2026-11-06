package com.airwear.aqi.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object RefreshScheduler {

    /** WorkManager's floor for periodic work. */
    private const val MIN_MINUTES = 15L

    /**
     * (Re)schedule the periodic AQI refresh. Uses UPDATE so a changed interval
     * takes effect immediately while an unchanged one keeps its timing.
     */
    fun schedule(context: Context, minutes: Int) {
        val period = minutes.toLong().coerceAtLeast(MIN_MINUTES)
        val flex = (period / 3).coerceAtLeast(5L)
        val request = PeriodicWorkRequestBuilder<AqiRefreshWorker>(
            period, TimeUnit.MINUTES,
            flex, TimeUnit.MINUTES,
        )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            AqiRefreshWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }
}
