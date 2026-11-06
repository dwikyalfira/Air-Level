package com.airwear.aqi.work

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.airwear.aqi.AqiApp
import com.airwear.aqi.data.DailyQuotaExceededException

class AqiRefreshWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? AqiApp ?: return Result.failure()
        val result = app.repository.refresh()
        val error = result.exceptionOrNull()
        Log.i(TAG, "Periodic refresh: ${if (error == null) "ok" else "failed: ${error.message}"}")
        return when {
            error == null -> Result.success()
            // No point retrying until the quota resets; next period will try again.
            error is DailyQuotaExceededException -> Result.success()
            runAttemptCount >= MAX_RETRIES -> Result.success()
            else -> Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "aqi_refresh"
        private const val TAG = "AirWear"
        private const val MAX_RETRIES = 3
    }
}
