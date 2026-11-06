package com.airwear.aqi.tile

import android.content.Context
import android.util.Log
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.airwear.aqi.AqiApp
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.guava.future
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Air Level tile. Renders from the shared cache; tapping the refresh pill
 * re-requests the tile with [AqiTileLayout.ID_REFRESH], which runs a forced
 * (rate-limited) refresh before rendering.
 */
class AqiTileService : TileService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<TileBuilders.Tile> =
        scope.future {
            val repository = (application as AqiApp).repository
            var error: String? = null
            if (requestParams.currentState.lastClickableId == AqiTileLayout.ID_REFRESH) {
                val result = withTimeoutOrNull(REFRESH_TIMEOUT_MS) { repository.refresh(force = true) }
                error = when {
                    result == null -> null // still running in background; show cache
                    result.isFailure -> result.exceptionOrNull()?.message
                    else -> null
                }
                Log.i(TAG, "Tile refresh: ${error ?: "ok"}")
            }
            val snapshot = repository.cachedSnapshot.first()
            TileBuilders.Tile.Builder()
                .setResourcesVersion(RESOURCES_VERSION)
                .setFreshnessIntervalMillis(FRESHNESS_MS)
                .setTileTimeline(
                    TimelineBuilders.Timeline.fromLayoutElement(
                        AqiTileLayout.build(this@AqiTileService, snapshot, refreshing = false, error = error),
                    ),
                )
                .build()
        }

    override fun onTileResourcesRequest(
        requestParams: RequestBuilders.ResourcesRequest,
    ): ListenableFuture<ResourceBuilders.Resources> = scope.future {
        ResourceBuilders.Resources.Builder().setVersion(RESOURCES_VERSION).build()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "AirLevel"
        private const val RESOURCES_VERSION = "1"
        /** Re-render at least this often even without a push (relative data age). */
        private const val FRESHNESS_MS = 15 * 60_000L
        private const val REFRESH_TIMEOUT_MS = 12_000L

        /** Ask the system to re-request the tile (after new data is saved). */
        fun requestUpdate(context: Context) {
            runCatching { getUpdater(context).requestUpdate(AqiTileService::class.java) }
                .onFailure { Log.w(TAG, "Tile update request failed", it) }
        }
    }
}
