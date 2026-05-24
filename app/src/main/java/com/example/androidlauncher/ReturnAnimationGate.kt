package com.example.androidlauncher

private const val MAX_PENDING_RETURN_AGE_MS = 15 * 60 * 1000L

data class ReturnAnimationGateDecision(
    val returnAnimation: ReturnAnimation?,
    val reason: String,
    val matchedObservation: ForegroundAppObservation?
)

object ReturnAnimationGate {
    fun resolve(
        pendingReturnAnimation: ReturnAnimation?,
        pendingLaunchStartedAtMs: Long,
        storedAnimations: Map<String, ReturnAnimation>,
        observations: List<ForegroundAppObservation>
    ): ReturnAnimationGateDecision {
        if (observations.isEmpty()) {
            return ReturnAnimationGateDecision(null, "no-foreground-observations", null)
        }

        // 1. Find the most recent foreground observation
        val latestObservation = observations.maxByOrNull { it.observedAtMs }
            ?: return ReturnAnimationGateDecision(null, "no-foreground-observations", null)

        // 2. Try to match it against any known origin (prefer pending if it matches)
        val matchedAnimation = if (pendingReturnAnimation != null && latestObservation.packageName == pendingReturnAnimation.launchedPackageName) {
            pendingReturnAnimation
        } else {
            storedAnimations[latestObservation.packageName]
        } ?: return ReturnAnimationGateDecision(null, "no-matching-animation", latestObservation)

        // 3. Stale check (15 minutes)
        val launchTime = if (matchedAnimation === pendingReturnAnimation) pendingLaunchStartedAtMs else matchedAnimation.launchedAtMs
        if (launchTime > 0L && System.currentTimeMillis() - launchTime > MAX_PENDING_RETURN_AGE_MS) {
            return ReturnAnimationGateDecision(null, "stale-animation", latestObservation)
        }

        return ReturnAnimationGateDecision(matchedAnimation, "matched-observation", latestObservation)
    }
}
