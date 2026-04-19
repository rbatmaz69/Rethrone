package com.example.androidlauncher.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class IconManagerTest {

    private lateinit var testFile: File
    private lateinit var manager: IconManager

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Before
    fun setup() {
        testFile = File.createTempFile("icon_manager_test", ".preferences_pb")

        val testDataStore = PreferenceDataStoreFactory.create(
            scope = testScope.backgroundScope,
            produceFile = { testFile }
        )

        manager = IconManager(testDataStore)
    }

    @After
    fun tearDown() {
        testFile.delete()
    }

    @Test
    fun `customIcons empty initially`() = testScope.runTest {
        val icons = manager.customIcons.first()
        assertTrue(icons.isEmpty())
    }

    @Test
    fun `setCustomIcon updates config and flow emits it`() = testScope.runTest {
        manager.setCustomIcon("com.pkg.a", "camera")
        val icons = manager.customIcons.first()
        assertEquals(1, icons.size)
        assertEquals("camera", icons["com.pkg.a"])
    }

    @Test
    fun `setCustomIcon with null removes the mapping`() = testScope.runTest {
        manager.setCustomIcon("com.pkg.a", "camera")
        manager.setCustomIcon("com.pkg.a", null)

        val icons = manager.customIcons.first()
        assertTrue(icons.isEmpty())
    }

    @Test
    fun `setAutoIconFallback updates fallback config`() = testScope.runTest {
        val fallback = AutoIconFallback(type = AutoIconFallbackType.ORIGINAL, reason = "configured_keep_original")
        manager.setAutoIconFallback("com.example.app", fallback)

        val fallbacks = manager.autoIconFallbacks.first()
        assertEquals(1, fallbacks.size)
        assertEquals(fallback, fallbacks["com.example.app"])
    }

    @Test
    fun `setAutoIconRule updates internal rule config`() = testScope.runTest {
        val rule = AutoIconRule(mode = AutoIconRuleMode.KEEP_ORIGINAL, reason = "user_rule")
        manager.setAutoIconRule("com.example.app", rule)

        val rules = manager.autoIconRules.first()
        assertEquals(1, rules.size)
        assertEquals(rule, rules["com.example.app"])
    }

    @Test
    fun `invalidatePackage removes fallback only`() = testScope.runTest {
        manager.setAutoIconFallback("com.pkg.a", AutoIconFallback(type = AutoIconFallbackType.ORIGINAL))
        manager.setAutoIconRule("com.pkg.a", AutoIconRule(mode = AutoIconRuleMode.KEEP_ORIGINAL))
        manager.setCustomIcon("com.pkg.a", "lucide_icon")

        manager.invalidatePackage("com.pkg.a", removeUserOverride = false)

        assertNull(manager.autoIconFallbacks.first()["com.pkg.a"])
        assertEquals(AutoIconRule(mode = AutoIconRuleMode.KEEP_ORIGINAL), manager.autoIconRules.first()["com.pkg.a"])
        assertEquals("lucide_icon", manager.customIcons.first()["com.pkg.a"])
    }

    @Test
    fun `invalidatePackage removes both fallback and overrides and rules`() = testScope.runTest {
        manager.setAutoIconFallback("com.pkg.a", AutoIconFallback(type = AutoIconFallbackType.ORIGINAL))
        manager.setAutoIconRule("com.pkg.a", AutoIconRule(mode = AutoIconRuleMode.KEEP_ORIGINAL))
        manager.setCustomIcon("com.pkg.a", "lucide_icon")

        manager.invalidatePackage("com.pkg.a", removeUserOverride = true)

        assertTrue(manager.autoIconFallbacks.first().isEmpty())
        assertTrue(manager.autoIconRules.first().isEmpty())
        assertTrue(manager.customIcons.first().isEmpty())
    }
}
