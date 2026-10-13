package com.pulso.wear.data.baseline

import android.content.Context
import android.util.Log
import com.pulso.core.baseline.Baseline
import com.pulso.core.baseline.BaselineEngine
import com.pulso.wear.data.local.PulsoWearDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class WearBaselineManager private constructor(private val context: Context) {

    private val sharedPrefs = context.getSharedPreferences("pulso_baseline_prefs", Context.MODE_PRIVATE)
    private val database = PulsoWearDatabase.getInstance(context)
    private val json = Json { ignoreUnknownKeys = true }

    private val _currentBaseline = MutableStateFlow<Baseline?>(loadPersistedBaseline())
    val currentBaseline: StateFlow<Baseline?> = _currentBaseline.asStateFlow()

    private fun loadPersistedBaseline(): Baseline? {
        val rawJson = sharedPrefs.getString(KEY_BASELINE_JSON, null) ?: return null
        return try {
            json.decodeFromString<Baseline>(rawJson)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading persisted baseline", e)
            null
        }
    }

    private fun persistBaseline(baseline: Baseline) {
        val rawJson = json.encodeToString(baseline)
        sharedPrefs.edit()
            .putString(KEY_BASELINE_JSON, rawJson)
            .putLong(KEY_UPDATED_AT, baseline.updatedAt)
            .apply()
        _currentBaseline.value = baseline
    }

    suspend fun recomputeBaseline(): Baseline = withContext(Dispatchers.IO) {
        val windows = database.windowDao().getAllWindows().map { it.toDomain() }
        Log.i(TAG, "Recomputing baseline with ${windows.size} historical windows")

        val newBaseline = BaselineEngine.update(
            windows = windows,
            currentTimeMs = System.currentTimeMillis()
        )
        persistBaseline(newBaseline)
        Log.i(TAG, "Baseline recomputed successfully")
        newBaseline
    }

    suspend fun resetBaseline(): Baseline = withContext(Dispatchers.IO) {
        Log.w(TAG, "Resetting baseline upon command")
        sharedPrefs.edit().remove(KEY_BASELINE_JSON).remove(KEY_UPDATED_AT).apply()
        val emptyBaseline = BaselineEngine.update(emptyList())
        persistBaseline(emptyBaseline)
        emptyBaseline
    }

    companion object {
        private const val TAG = "PulsoBaselineManager"
        private const val KEY_BASELINE_JSON = "baseline_json"
        private const val KEY_UPDATED_AT = "baseline_updated_at"

        @Volatile
        private var INSTANCE: WearBaselineManager? = null

        fun getInstance(context: Context): WearBaselineManager {
            return INSTANCE ?: synchronized(this) {
                val instance = WearBaselineManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
