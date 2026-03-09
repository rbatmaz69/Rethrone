package com.example.androidlauncher

data class ForegroundAppObservation(
    val packageName: String,
    val observedAtMs: Long,
    val source: String
)

