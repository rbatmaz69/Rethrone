package com.example.androidlauncher

private const val MAX_PENDING_RETURN_AGE_MS = 15 * 60 * 1000L

// Engeres Fenster für den berechtigungsunabhängigen Fallback: Liegt keine
// Foreground-Observation vor (Accessibility/Usage-Access aus), fehlt uns das
// Signal, von welcher App der Nutzer zurückkehrt. Dann vertrauen wir der beim
// Start gesetzten pending-Animation, aber nur für einen frischen Direkt-Roundtrip.
private const val MAX_PENDING_FALLBACK_AGE_MS = 5 * 60 * 1000L

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
            // Kein Foreground-Signal verfügbar (keine Berechtigung oder keine
            // Daten). Statt die Rückkehr-Animation komplett auszulassen, fallen
            // wir auf die pending-Animation zurück, sofern sie frisch ist.
            // Tradeoff: Öffnet der Nutzer aus App A heraus App B und kehrt dann
            // zurück, schrumpft das Overlay auf A statt B. Akzeptabel für
            // maximale Zuverlässigkeit im Normalfall; mit Berechtigungen
            // korrigiert das der Observation-Pfad unten.
            val pendingIsFresh = pendingLaunchStartedAtMs > 0L &&
                System.currentTimeMillis() - pendingLaunchStartedAtMs <= MAX_PENDING_FALLBACK_AGE_MS
            if (pendingReturnAnimation != null && pendingIsFresh) {
                return ReturnAnimationGateDecision(pendingReturnAnimation, "pending-fallback-no-observations", null)
            }
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
