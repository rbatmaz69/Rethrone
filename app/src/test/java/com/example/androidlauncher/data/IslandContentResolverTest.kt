package com.example.androidlauncher.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Prüft die reine Prioritäts-Logik der Dynamic Island
 * (Medien > Timer > Benachrichtigung > Laden) sowie den Leerlauf (null).
 */
class IslandContentResolverTest {

    private val timer = IslandContent.Timer(label = "Eier", remainingMs = 90_000)
    private val battery = IslandContent.Battery(level = 80, charging = true)
    private val notification = IslandContent.Notification("com.app", "Titel", "Text")
    private val media = IslandContent.Media("Song", "Artist", isPlaying = true)

    @Test
    fun `idle returns null when nothing active`() {
        assertNull(resolveIslandContent(IslandSources()))
    }

    @Test
    fun `battery shows when only source`() {
        assertEquals(battery, resolveIslandContent(IslandSources(battery = battery)))
    }

    @Test
    fun `notification beats battery`() {
        val result = resolveIslandContent(
            IslandSources(notification = notification, battery = battery)
        )
        assertEquals(notification, result)
    }

    @Test
    fun `timer beats notification and battery`() {
        val result = resolveIslandContent(
            IslandSources(timer = timer, notification = notification, battery = battery)
        )
        assertEquals(timer, result)
    }

    @Test
    fun `playing media wins over everything`() {
        val result = resolveIslandContent(
            IslandSources(
                media = media,
                timer = timer,
                notification = notification,
                battery = battery
            )
        )
        assertEquals(media, result)
    }

    @Test
    fun `paused media loses to timer`() {
        val pausedMedia = media.copy(isPlaying = false)
        val result = resolveIslandContent(
            IslandSources(media = pausedMedia, timer = timer)
        )
        assertEquals(timer, result)
    }

    @Test
    fun `paused media loses to notification`() {
        val pausedMedia = media.copy(isPlaying = false)
        val result = resolveIslandContent(
            IslandSources(media = pausedMedia, notification = notification)
        )
        assertEquals(notification, result)
    }

    @Test
    fun `paused media beats battery and shows when otherwise idle`() {
        val pausedMedia = media.copy(isPlaying = false)
        assertEquals(pausedMedia, resolveIslandContent(IslandSources(media = pausedMedia, battery = battery)))
        assertEquals(pausedMedia, resolveIslandContent(IslandSources(media = pausedMedia)))
    }
}
