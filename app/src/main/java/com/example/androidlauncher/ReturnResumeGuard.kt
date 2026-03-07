package com.example.androidlauncher

/**
 * Unterdrückt Return-Animationen für Resume-Zyklen, die nur durch Sperren/Entsperren
 * des Geräts entstehen und keine echte Rücknavigation aus einer App darstellen.
 */
data class ReturnResumeGuardState(
    val awaitingUserPresent: Boolean = false,
    val skipNextResume: Boolean = false
)

data class ReturnResumeDecision(
    val shouldSuppress: Boolean,
    val nextState: ReturnResumeGuardState
)

object ReturnResumeGuard {
    fun onScreenOff(
        state: ReturnResumeGuardState,
        launcherWasForeground: Boolean
    ): ReturnResumeGuardState {
        if (!launcherWasForeground) return state
        return state.copy(awaitingUserPresent = true)
    }

    fun onUserPresent(state: ReturnResumeGuardState): ReturnResumeGuardState {
        if (!state.awaitingUserPresent) return state
        return state.copy(awaitingUserPresent = false, skipNextResume = true)
    }

    fun onResume(state: ReturnResumeGuardState): ReturnResumeDecision {
        return when {
            state.awaitingUserPresent -> ReturnResumeDecision(
                shouldSuppress = true,
                nextState = state
            )
            state.skipNextResume -> ReturnResumeDecision(
                shouldSuppress = true,
                nextState = state.copy(skipNextResume = false)
            )
            else -> ReturnResumeDecision(
                shouldSuppress = false,
                nextState = state
            )
        }
    }
}

