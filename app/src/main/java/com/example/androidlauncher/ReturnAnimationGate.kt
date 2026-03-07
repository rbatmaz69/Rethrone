package com.example.androidlauncher

private const val RETURN_CANDIDATE_FRESHNESS_WINDOW_MS = 2_500L

data class ForegroundAppObservation(
    val packageName: String,
    val observedAtMs: Long,
    val source: String
)

data class ReturnAnimationGateDecision(
    val returnAnimation: ReturnAnimation?,
    val matchedObservation: ForegroundAppObservation?,
    val reason: String
)

object ReturnAnimationGate {
    fun resolve(
        pendingReturnAnimation: ReturnAnimation?,
        pendingLaunchStartedAtMs: Long,
        observations: List<ForegroundAppObservation>,
        nowMs: Long = System.currentTimeMillis()
    ): ReturnAnimationGateDecision {
        if (pendingReturnAnimation == null) {
            return ReturnAnimationGateDecision(null, null, "no-pending-animation")
        }
        if (pendingLaunchStartedAtMs <= 0L) {
            return ReturnAnimationGateDecision(null, null, "missing-launch-timestamp")
        }
        if (observations.isEmpty()) {
            return ReturnAnimationGateDecision(null, null, "no-foreground-observation")
        }

        observations.forEach { observation ->
            if (observation.packageName != pendingReturnAnimation.launchedPackageName) {
                return@forEach
            }
            if (observation.observedAtMs < pendingLaunchStartedAtMs) {
                return@forEach
            }
            val ageMs = nowMs - observation.observedAtMs
            if (ageMs !in 0..RETURN_CANDIDATE_FRESHNESS_WINDOW_MS) {
                return@forEach
            }
            return ReturnAnimationGateDecision(
                returnAnimation = pendingReturnAnimation,
                matchedObservation = observation,
                reason = "confirmed-return"
            )
        }

        return ReturnAnimationGateDecision(null, null, "no-confirmed-return")
    }
}

