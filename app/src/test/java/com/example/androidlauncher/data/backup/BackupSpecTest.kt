package com.example.androidlauncher.data.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupSpecTest {

    @Test
    fun `credentials and device-local keys are excluded`() {
        val expected = setOf(
            "lock_secret",
            "lock_type",
            "lock_biometric_enabled",
            "locked_apps",
            "custom_wallpaper_uri",
            "hosted_widgets",
        )
        assertEquals(expected, BackupSpec.EXCLUDED_SETTINGS_KEYS)
    }

    @Test
    fun `encrypted keys are never simultaneously excluded`() {
        // Ein Key, der beides wäre, würde beim Export entschlüsselt UND ausgeschlossen –
        // die Spec muss die Mengen disjunkt halten.
        assertTrue(BackupSpec.ENCRYPTED_SETTINGS_KEYS.intersect(BackupSpec.EXCLUDED_SETTINGS_KEYS).isEmpty())
    }

    @Test
    fun `all four stores are covered`() {
        assertEquals(
            listOf("settings", "favorites", "folders", "icon_mappings"),
            BackupSpec.STORE_NAMES,
        )
    }
}
