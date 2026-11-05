package com.pulso.wear.tile

import android.content.Context
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ColorBuilders.argb
import androidx.wear.protolayout.DeviceParametersBuilders
import androidx.wear.protolayout.DimensionBuilders.degrees
import androidx.wear.protolayout.DimensionBuilders.dp
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.protolayout.material.ChipColors
import androidx.wear.protolayout.material.CompactChip
import androidx.wear.protolayout.material.Text
import androidx.wear.protolayout.material.Typography
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.pulso.wear.data.glance.GlanceLevel
import com.pulso.wear.data.glance.GlanceSnapshot
import com.pulso.wear.data.glance.GlanceStore
import com.pulso.wear.data.glance.label
import com.pulso.wear.presentation.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

private const val RESOURCES_VERSION = "1"

private const val PRIMARY = 0xFF8FEBD3.toInt()
private const val BUTTON = 0xFFF4F6F8.toInt()
private const val ON_PRIMARY = 0xFF0B0D10.toInt()
private const val WARN = 0xFFFFCF7A.toInt()
private const val HEART = 0xFFFF8A7D.toInt()
private const val TEXT = 0xFFF4F6F8.toInt()
private const val MUTED = 0xFF9198A4.toInt()

/** "Tile Pulso" from the design: current state, pulse/HRV and a one-tap breathing shortcut. */
class PulsoTileService : TileService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<TileBuilders.Tile> =
        CallbackToFutureAdapter.getFuture { completer ->
            scope.launch {
                try {
                    val snapshot = GlanceStore.snapshot(this@PulsoTileService)
                    completer.set(buildTile(this@PulsoTileService, snapshot, requestParams.deviceConfiguration))
                } catch (e: Exception) {
                    completer.setException(e)
                }
            }
            "pulso-tile"
        }

    override fun onTileResourcesRequest(requestParams: RequestBuilders.ResourcesRequest): ListenableFuture<ResourceBuilders.Resources> =
        Futures.immediateFuture(ResourceBuilders.Resources.Builder().setVersion(RESOURCES_VERSION).build())

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}

private fun levelColor(level: GlanceLevel): Int = when (level) {
    GlanceLevel.CALM -> PRIMARY
    GlanceLevel.ALERT -> WARN
    GlanceLevel.TENSION -> HEART
    GlanceLevel.UNKNOWN -> MUTED
}

private fun withAlpha(color: Int, alpha: Float): Int = ((alpha * 255).toInt() shl 24) or (color and 0x00FFFFFF)

internal fun buildTile(
    context: Context,
    snapshot: GlanceSnapshot,
    device: DeviceParametersBuilders.DeviceParameters
): TileBuilders.Tile {
    val openBreathing = ModifiersBuilders.Clickable.Builder()
        .setId("breathe")
        .setOnClick(
            ActionBuilders.LaunchAction.Builder()
                .setAndroidActivity(
                    ActionBuilders.AndroidActivity.Builder()
                        .setPackageName(context.packageName)
                        .setClassName(MainActivity::class.java.name)
                        .addKeyToExtraMapping("route", ActionBuilders.stringExtra("breathing"))
                        .build()
                )
                .build()
        )
        .build()
    val openApp = ModifiersBuilders.Clickable.Builder()
        .setId("open")
        .setOnClick(
            ActionBuilders.LaunchAction.Builder()
                .setAndroidActivity(
                    ActionBuilders.AndroidActivity.Builder()
                        .setPackageName(context.packageName)
                        .setClassName(MainActivity::class.java.name)
                        .build()
                )
                .build()
        )
        .build()

    // Tension gauge: calm / rising / tension segments; the active level is drawn at full strength.
    fun segment(color: Int, sweep: Float, active: Boolean) = LayoutElementBuilders.ArcLine.Builder()
        .setLength(degrees(sweep))
        .setThickness(dp(7f))
        .setColor(argb(if (active) color else withAlpha(color, 0.35f)))
        .build()
    fun gap() = LayoutElementBuilders.ArcSpacer.Builder().setLength(degrees(6f)).setThickness(dp(7f)).build()
    val gauge = LayoutElementBuilders.Arc.Builder()
        .setAnchorAngle(degrees(225f))
        .setAnchorType(LayoutElementBuilders.ARC_ANCHOR_START)
        .addContent(segment(PRIMARY, 130f, snapshot.level == GlanceLevel.CALM))
        .addContent(gap())
        .addContent(segment(WARN, 74f, snapshot.level == GlanceLevel.ALERT))
        .addContent(gap())
        .addContent(segment(HEART, 54f, snapshot.level == GlanceLevel.TENSION))
        .build()

    val metrics = buildString {
        append(snapshot.bpm?.let { "$it lpm" } ?: "-- lpm")
        snapshot.rmssd?.let { append(" · $it ms") }
    }

    val column = LayoutElementBuilders.Column.Builder()
        .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
        .addContent(Text.Builder(context, "PULSO").setTypography(Typography.TYPOGRAPHY_CAPTION2).setColor(argb(MUTED)).build())
        .addContent(LayoutElementBuilders.Spacer.Builder().setHeight(dp(6f)).build())
        .addContent(Text.Builder(context, "Ahora mismo").setTypography(Typography.TYPOGRAPHY_CAPTION1).setColor(argb(MUTED)).build())
        .addContent(Text.Builder(context, snapshot.level.label()).setTypography(Typography.TYPOGRAPHY_DISPLAY3).setColor(argb(levelColor(snapshot.level))).build())
        .addContent(Text.Builder(context, metrics).setTypography(Typography.TYPOGRAPHY_TITLE3).setColor(argb(TEXT)).build())
        .addContent(LayoutElementBuilders.Spacer.Builder().setHeight(dp(8f)).build())
        .addContent(
            CompactChip.Builder(context, "Respirar", openBreathing, device)
                .setChipColors(ChipColors(BUTTON, ON_PRIMARY))
                .build()
        )
        .build()

    val root = LayoutElementBuilders.Box.Builder()
        .setWidth(expand())
        .setHeight(expand())
        .setModifiers(ModifiersBuilders.Modifiers.Builder().setClickable(openApp).build())
        .addContent(gauge)
        .addContent(column)
        .build()

    return TileBuilders.Tile.Builder()
        .setResourcesVersion(RESOURCES_VERSION)
        .setFreshnessIntervalMillis(15 * 60_000L)
        .setTileTimeline(TimelineBuilders.Timeline.fromLayoutElement(root))
        .build()
}
