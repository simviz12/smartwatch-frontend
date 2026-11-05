package com.pulso.wear.data.glance

import android.content.Context
import android.util.Log
import androidx.wear.tiles.TileService
import com.pulso.wear.tile.PulsoTileService

/** Asks the system to redraw surfaces that live outside the app (the Pulso tile). */
object GlanceRefresher {
    fun requestRefresh(context: Context) {
        try {
            TileService.getUpdater(context.applicationContext).requestUpdate(PulsoTileService::class.java)
        } catch (e: Exception) {
            Log.w("GlanceRefresher", "Tile refresh request failed: ${e.message}")
        }
        com.pulso.wear.complication.PulsoComplicationService.requestUpdateAll(context.applicationContext)
    }
}
