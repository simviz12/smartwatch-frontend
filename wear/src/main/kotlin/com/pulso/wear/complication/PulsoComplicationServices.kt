package com.pulso.wear.complication

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import com.pulso.wear.data.glance.GlanceSnapshot
import com.pulso.wear.data.glance.GlanceStore
import com.pulso.wear.data.glance.label
import com.pulso.wear.presentation.MainActivity

/**
 * Pulso data for the Pulso watch face (Watch Face Format) and any other face:
 * pulse, HRV and the current tension state, each as a short-text complication.
 */
abstract class PulsoComplicationService : SuspendingComplicationDataSourceService() {

    protected abstract fun text(snapshot: GlanceSnapshot): String
    protected abstract fun title(snapshot: GlanceSnapshot): String
    protected abstract fun description(snapshot: GlanceSnapshot): String
    protected open val route: String? = null

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        if (type != ComplicationType.SHORT_TEXT) return null
        val demo = GlanceSnapshot(72, 48, System.currentTimeMillis(), com.pulso.wear.data.glance.GlanceLevel.CALM, IntArray(48))
        return build(demo, null)
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        if (request.complicationType != ComplicationType.SHORT_TEXT) return null
        return build(GlanceStore.snapshot(this), openApp())
    }

    private fun build(snapshot: GlanceSnapshot, tap: PendingIntent?): ComplicationData =
        ShortTextComplicationData.Builder(
            PlainComplicationText.Builder(text(snapshot)).build(),
            PlainComplicationText.Builder(description(snapshot)).build()
        )
            .setTitle(PlainComplicationText.Builder(title(snapshot)).build())
            .setTapAction(tap)
            .build()

    private fun openApp(): PendingIntent = PendingIntent.getActivity(
        this,
        javaClass.name.hashCode(),
        Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            route?.let { putExtra("route", it) }
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    companion object {
        /** Pushes fresh values to every face showing Pulso complications. */
        fun requestUpdateAll(context: Context) {
            listOf(PulseComplicationService::class.java, HrvComplicationService::class.java, StateComplicationService::class.java).forEach {
                runCatching {
                    ComplicationDataSourceUpdateRequester.create(context, ComponentName(context, it)).requestUpdateAll()
                }
            }
        }
    }
}

class PulseComplicationService : PulsoComplicationService() {
    override fun text(snapshot: GlanceSnapshot) = snapshot.bpm?.toString() ?: "--"
    override fun title(snapshot: GlanceSnapshot) = "lpm"
    override fun description(snapshot: GlanceSnapshot) = snapshot.bpm?.let { "Pulso $it latidos por minuto" } ?: "Sin lectura de pulso"
}

class HrvComplicationService : PulsoComplicationService() {
    override fun text(snapshot: GlanceSnapshot) = snapshot.rmssd?.toString() ?: "--"
    override fun title(snapshot: GlanceSnapshot) = "ms HRV"
    override fun description(snapshot: GlanceSnapshot) = snapshot.rmssd?.let { "Variabilidad $it milisegundos" } ?: "Sin lectura de HRV"
}

class StateComplicationService : PulsoComplicationService() {
    override val route: String = "breathing"
    override fun text(snapshot: GlanceSnapshot) = snapshot.level.label()
    override fun title(snapshot: GlanceSnapshot) = "Pulso"
    override fun description(snapshot: GlanceSnapshot) = "Estado: ${snapshot.level.label()}"
}
