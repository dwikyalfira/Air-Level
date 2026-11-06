package com.airwear.aqi.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.airwear.aqi.AqiApp
import com.airwear.aqi.complication.AqiComplicationDataSourceService

class AqiRefreshWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? AqiApp ?: return Result.failure()
        val result = app.repository.refresh()
        result.onSuccess {
            AqiComplicationDataSourceService.forceUpdateAll(applicationContext)
        }
        // Retry on failure with backoff via WorkManager policy
        return if (result.isSuccess) Result.success() else Result.retry()
    }

    companion object {
        const val WORK_NAME = "aqi_refresh"
    }
}
