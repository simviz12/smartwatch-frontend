package com.pulso.core.breathing

enum class BreathingSessionState {
    IDLE,
    PREPARING,   // Start tap & countdown
    RUNNING,     // Cycling through timeline
    PAUSED,      // Paused by user or transient interruption
    INTERRUPTED, // Interrupted by phone call, system alert, etc.
    COMPLETED,   // Finished all cycles normally
    STOPPED      // Stopped early by user
}

data class BreathingSessionSnapshot(
    val state: BreathingSessionState,
    val currentCycle: Int,
    val totalCycles: Int,
    val currentSegmentType: TimelineSegmentType?,
    val segmentProgress: Float, // 0.0 to 1.0 within current segment
    val elapsedTimeMs: Long,
    val totalDurationMs: Long,
    val endReason: SessionEndReason? = null
)

class BreathingSessionStateMachine(
    val plan: BreathingPlan,
    val intensity: HapticIntensity = HapticIntensity.MEDIUM,
    val stepMs: Long = 100L
) {
    val timeline: List<HapticTimelineSegment> = BreathingTimelineGenerator.generateTimeline(plan, intensity, stepMs)
    val totalDurationMs: Long = timeline.sumOf { it.durationMs }

    private var _state: BreathingSessionState = BreathingSessionState.IDLE
    val state: BreathingSessionState get() = _state

    private var currentSegmentIndex: Int = 0
    private var segmentElapsedMs: Long = 0L
    private var totalElapsedMs: Long = 0L
    private var endReason: SessionEndReason? = null

    fun start() {
        if (_state == BreathingSessionState.IDLE) {
            _state = BreathingSessionState.RUNNING
            currentSegmentIndex = 0
            segmentElapsedMs = 0L
            totalElapsedMs = 0L
            endReason = null
        }
    }

    fun pause() {
        if (_state == BreathingSessionState.RUNNING) {
            _state = BreathingSessionState.PAUSED
        }
    }

    fun resume() {
        if (_state == BreathingSessionState.PAUSED) {
            _state = BreathingSessionState.RUNNING
        }
    }

    fun stop() {
        if (_state == BreathingSessionState.RUNNING || _state == BreathingSessionState.PAUSED) {
            _state = BreathingSessionState.STOPPED
            endReason = SessionEndReason.USER_STOPPED
        }
    }

    fun interrupt() {
        if (_state == BreathingSessionState.RUNNING || _state == BreathingSessionState.PAUSED) {
            _state = BreathingSessionState.INTERRUPTED
            endReason = SessionEndReason.INTERRUPTED
        }
    }

    /**
     * Advances time by [deltaMs]. Returns the current segment if running, or null if finished.
     */
    fun tick(deltaMs: Long): HapticTimelineSegment? {
        if (_state != BreathingSessionState.RUNNING) {
            return null
        }

        totalElapsedMs += deltaMs
        segmentElapsedMs += deltaMs

        while (currentSegmentIndex < timeline.size) {
            val currentSeg = timeline[currentSegmentIndex]
            if (segmentElapsedMs < currentSeg.durationMs) {
                return currentSeg
            }

            // Move to next segment
            segmentElapsedMs -= currentSeg.durationMs
            currentSegmentIndex++
        }

        // All segments finished
        _state = BreathingSessionState.COMPLETED
        endReason = SessionEndReason.COMPLETED
        return null
    }

    fun getCurrentSnapshot(): BreathingSessionSnapshot {
        val currentSeg = timeline.getOrNull(currentSegmentIndex)
        val progress = if (currentSeg != null && currentSeg.durationMs > 0) {
            (segmentElapsedMs.toFloat() / currentSeg.durationMs).coerceIn(0f, 1f)
        } else 1f

        val completedCycles = when {
            _state == BreathingSessionState.COMPLETED -> plan.cycles
            currentSeg != null -> (currentSeg.cycleIndex - 1).coerceAtLeast(0)
            else -> 0
        }

        return BreathingSessionSnapshot(
            state = _state,
            currentCycle = currentSeg?.cycleIndex?.coerceIn(1, plan.cycles) ?: plan.cycles,
            totalCycles = plan.cycles,
            currentSegmentType = currentSeg?.type,
            segmentProgress = progress,
            elapsedTimeMs = totalElapsedMs.coerceAtMost(totalDurationMs),
            totalDurationMs = totalDurationMs,
            endReason = endReason
        )
    }

    fun getCompletedCyclesCount(): Int {
        return when (_state) {
            BreathingSessionState.COMPLETED -> plan.cycles
            else -> {
                val currentSeg = timeline.getOrNull(currentSegmentIndex)
                if (currentSeg != null) (currentSeg.cycleIndex - 1).coerceAtLeast(0) else 0
            }
        }
    }
}
