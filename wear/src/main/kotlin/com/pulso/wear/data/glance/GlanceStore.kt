package com.pulso.wear.data.glance

import android.content.Context
import com.pulso.wear.data.local.PulsoWearDatabase
import java.util.Calendar

/** Tension level shown on the tile and watch face. Always paired with a text label. */
enum class GlanceLevel { CALM, ALERT, TENSION, UNKNOWN }

data class GlanceSnapshot(
    val bpm: Int?,
    val rmssd: Int?,
    val readingAt: Long?,
    val level: GlanceLevel,
    /** 48 half-hour slots of today: 0 none, 1 rising, 2 tension. */
    val slots: IntArray
)

/**
 * Lightweight store of the latest reading so surfaces outside the app (tile, watch face)
 * can show it even after measurement windows are synced and purged from Room.
 */
object GlanceStore {
    private const val PREFS = "pulso_glance"
    private const val KEY_BPM = "bpm"
    private const val KEY_RMSSD = "rmssd"
    private const val KEY_AT = "at"
    private const val RECENT_MS = 30 * 60_000L
    private const val KEY_DEMO = "demo_mode"

    /** Demo mode lets the tile/watch face be previewed without readings. Real readings always win. */
    fun isDemo(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_DEMO, false)

    fun setDemo(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_DEMO, enabled).apply()
        GlanceRefresher.requestRefresh(context)
    }

    private fun demoSnapshot(now: Long): GlanceSnapshot {
        val slots = IntArray(48)
        // A calm day with one rising episode mid-morning and one tension spike before noon.
        slots[15] = 1; slots[22] = 1; slots[23] = 2
        return GlanceSnapshot(bpm = 72, rmssd = 48, readingAt = now, level = GlanceLevel.CALM, slots = slots)
    }

    fun recordReading(context: Context, meanHr: Double, rmssd: Double?, at: Long) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putInt(KEY_BPM, meanHr.toInt())
            .putInt(KEY_RMSSD, rmssd?.toInt() ?: -1)
            .putLong(KEY_AT, at)
            .apply()
        GlanceRefresher.requestRefresh(context)
    }

    suspend fun snapshot(context: Context, now: Long = System.currentTimeMillis()): GlanceSnapshot {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val at = prefs.getLong(KEY_AT, 0L).takeIf { it > 0 }
        val hasRecentReal = at != null && now - at < RECENT_MS
        if (!hasRecentReal && prefs.getBoolean(KEY_DEMO, false)) return demoSnapshot(now)
        val bpm = prefs.getInt(KEY_BPM, -1).takeIf { it > 0 && at != null }
        val rmssd = prefs.getInt(KEY_RMSSD, -1).takeIf { it > 0 && at != null }

        val dayStart = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val events = runCatching { PulsoWearDatabase.getInstance(context).stressEventDao().getRecentEvents(50) }.getOrDefault(emptyList())
        val slots = IntArray(48)
        events.filter { it.time >= dayStart }.forEach { e ->
            val slot = ((e.time - dayStart) / (30 * 60_000L)).toInt().coerceIn(0, 47)
            slots[slot] = maxOf(slots[slot], if (e.confidence >= 0.75) 2 else 1)
        }
        val lastEvent = events.maxOfOrNull { it.time }
        val level = when {
            lastEvent != null && now - lastEvent < RECENT_MS -> {
                if ((events.first { it.time == lastEvent }.confidence) >= 0.75) GlanceLevel.TENSION else GlanceLevel.ALERT
            }
            at != null && now - at < RECENT_MS -> GlanceLevel.CALM
            else -> GlanceLevel.UNKNOWN
        }
        return GlanceSnapshot(bpm, rmssd, at, level, slots)
    }
}

fun GlanceLevel.label(): String = when (this) {
    GlanceLevel.CALM -> "Calma"
    GlanceLevel.ALERT -> "Atento"
    GlanceLevel.TENSION -> "Tensión"
    GlanceLevel.UNKNOWN -> "Sin lectura"
}
