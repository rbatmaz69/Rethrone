package com.example.androidlauncher

private const val MAX_PENDING_RETURN_AGE_MS = 15 * 60 * 1000L
private const val OBSERVATION_CLOCK_SKEW_MS = 5_000L

data class ReturnResumeGuardState(
    val awaitingUserPresent: Boolean = false,
    val skipNextResume: Boolean = false
)

data class ReturnResumeDecision(
    val nextState: ReturnResumeGuardState,
    val shouldSuppress: Boolean
)

object ReturnResumeGuard {
    fun onScreenOff(
        state: ReturnResumeGuardState,
        launcherWasForeground: Boolean
    ): ReturnResumeGuardState {
        if (!launcherWasForeground) return state
        return state.copy(awaitingUserPresent = true, skipNextResume = false)
    }

    fun onUserPresent(state: ReturnResumeGuardState): ReturnResumeGuardState {
        if (!state.awaitingUserPresent) return state
        return state.copy(awaitingUserPresent = false, skipNextResume = true)
    }

    fun onResume(state: ReturnResumeGuardState): ReturnResumeDecision {
        return when {
            state.awaitingUserPresent -> ReturnResumeDecision(nextState = state, shouldSuppress = true)
            state.skipNextResume -> ReturnResumeDecision(
                nextState = state.copy(skipNextResume = false),
                shouldSuppress = true
            )
            else -> ReturnResumeDecision(nextState = state, shouldSuppress = false)
        }
    }
}

data class ReturnAnimationGateDecision(
    val returnAnimation: ReturnAnimation?,
    val reason: String,
    val matchedObservation: ForegroundAppObservation?
)

object ReturnAnimationGate {
    fun resolve(
        pendingReturnAnimation: ReturnAnimation?,
        pendingLaunchStartedAtMs: Long,
        observations: List<ForegroundAppObservation>
    ): ReturnAnimationGateDecision {
        val animation = pendingReturnAnimation
            ?: return ReturnAnimationGateDecision(null, "no-pending-animation", null)

        if (pendingLaunchStartedAtMs <= 0L) {
            return ReturnAnimationGateDecision(null, "invalid-pending-launch-time", null)
        }

        if (System.currentTimeMillis() - pendingLaunchStartedAtMs > MAX_PENDING_RETURN_AGE_MS) {
            return ReturnAnimationGateDecision(null, "stale-pending-animation", null)
        }

        if (observations.isEmpty()) {
            return ReturnAnimationGateDecision(null, "no-foreground-observations", null)
        }

        val matchedObservation = observations
            .asSequence()
            .filter { it.packageName == animation.launchedPackageName }
            .filter { it.observedAtMs > 0L }
            .maxByOrNull { it.observedAtMs }
            ?: return ReturnAnimationGateDecision(null, "no-matching-observation", null)

        if (matchedObservation.observedAtMs + OBSERVATION_CLOCK_SKEW_MS < pendingLaunchStartedAtMs) {
            return ReturnAnimationGateDecision(animation, "matched-observation-precedes-launch", matchedObservation)
        }

        return ReturnAnimationGateDecision(animation, "matched-observation", matchedObservation)
    }
}
