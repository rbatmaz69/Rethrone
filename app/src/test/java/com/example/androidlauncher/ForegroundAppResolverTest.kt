package com.example.androidlauncher

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import android.provider.Settings
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import io.mockk.mockkConstructor
import io.mockk.unmockkConstructor

class ForegroundAppResolverTest {

    private lateinit var context: Context
    private lateinit var appOpsManager: AppOpsManager
    private lateinit var usageStatsManager: UsageStatsManager

    @Before
    fun setUp() {
        context = mockk<Context>(relaxed = true)
        appOpsManager = mockk<AppOpsManager>(relaxed = true)
        usageStatsManager = mockk<UsageStatsManager>(relaxed = true)

        every { context.getSystemService(Context.APP_OPS_SERVICE) } returns appOpsManager
        every { context.getSystemService(Context.USAGE_STATS_SERVICE) } returns usageStatsManager
        every { context.packageName } returns "com.example.androidlauncher"

        mockkStatic(Process::class)
        every { Process.myUid() } returns 12345
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun testOpenUsageAccessSettings() {
        ForegroundAppResolver.openUsageAccessSettings(context)
        verify {
            context.startActivity(any())
        }
    }

    private fun mockAppOpsManager(mode: Int) {
        every {
            appOpsManager.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                12345,
                "com.example.androidlauncher"
            )
        } returns mode
        every {
            appOpsManager.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                12345,
                "com.example.androidlauncher"
            )
        } returns mode
    }

    @Test
    fun testHasUsageAccess_Granted() {
        mockAppOpsManager(AppOpsManager.MODE_ALLOWED)
        val result = ForegroundAppResolver.hasUsageAccess(context)
        assertTrue(result)
    }

    @Test
    fun testHasUsageAccess_Denied() {
        mockAppOpsManager(AppOpsManager.MODE_IGNORED)
        val result = ForegroundAppResolver.hasUsageAccess(context)
        assertFalse(result)
    }

    @Test
    fun testGetRecentForegroundObservation_NoAccess() {
        mockAppOpsManager(AppOpsManager.MODE_IGNORED)
        val result = ForegroundAppResolver.getRecentForegroundObservation(context)
        assertNull(result)
    }

    @Test
    fun testGetRecentForegroundObservation_NoUsageStatsManager() {
        mockAppOpsManager(AppOpsManager.MODE_ALLOWED)
        every { context.getSystemService(Context.USAGE_STATS_SERVICE) } returns null

        val result = ForegroundAppResolver.getRecentForegroundObservation(context)
        assertNull(result)
    }

    @Test
    fun testGetRecentForegroundObservation_StatsCandidate() {
        mockAppOpsManager(AppOpsManager.MODE_ALLOWED)

        // Events returns empty
        val events = mockk<UsageEvents>()
        every { events.hasNextEvent() } returns false
        every { usageStatsManager.queryEvents(any(), any()) } returns events

        // Stats
        val stat = mockk<UsageStats>()
        every { stat.packageName } returns "com.other.app"
        every { stat.lastTimeUsed } returns 1000L

        every { usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, any(), any()) } returns listOf(stat)

        val result = ForegroundAppResolver.getRecentForegroundObservation(context)
        assertNotNull(result)
        assertEquals("com.other.app", result?.packageName)
        assertEquals(1000L, result?.observedAtMs)
        assertEquals("usage-stats", result?.source)
    }
}
