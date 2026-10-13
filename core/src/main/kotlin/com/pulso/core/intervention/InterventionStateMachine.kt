package com.pulso.core.intervention

data class AdaptiveSuggestion(
    val bucket: String,
    val consecutiveNotStressedCount: Int,
    val suggestedAction: String = "SUGGEST_RAISE_THRESHOLD"
)

class InterventionStateMachine(
    private val adaptiveThresholdK: Int = 3,
    private val adaptiveHelpEnabled: Boolean = false
) {
    // Tracks consecutive NOT_STRESSED counts per bucket
    private val notStressedStreaks = mutableMapOf<String, Int>()

    /**
     * Processes an outcome action for an intervention and determines if an adaptive
     * sensitivity recommendation should be surfaced to the user.
     */
    fun processFeedback(
        feedback: InterventionFeedback
    ): AdaptiveSuggestion? {
        val bucket = feedback.timeBucket ?: "UNKNOWN"

        if (feedback.action == InterventionAction.NOT_STRESSED) {
            val currentStreak = (notStressedStreaks[bucket] ?: 0) + 1
            notStressedStreaks[bucket] = currentStreak

            if (adaptiveHelpEnabled && currentStreak >= adaptiveThresholdK) {
                return AdaptiveSuggestion(
                    bucket = bucket,
                    consecutiveNotStressedCount = currentStreak
                )
            }
        } else if (feedback.action == InterventionAction.TALK || feedback.action == InterventionAction.BREATHE) {
            // User confirmed intervention was relevant; reset NOT_STRESSED streak for this bucket
            notStressedStreaks[bucket] = 0
        }

        return null
    }

    fun getStreak(bucket: String): Int = notStressedStreaks[bucket] ?: 0

    fun resetStreak(bucket: String) {
        notStressedStreaks[bucket] = 0
    }

    fun resetAll() {
        notStressedStreaks.clear()
    }
}
