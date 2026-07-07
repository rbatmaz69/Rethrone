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
    fun `swipe gesture target packages are re-encrypted on import`() {
        // U1: Die pro Richtung gewählte OPEN_APP-Ziel-App liegt CryptoManager-verschlüsselt
        // im Store und muss beim Export/Import wie die anderen Paket-Keys behandelt werden.
        val swipeKeys = setOf(
            "swipe_up_app_package",
            "swipe_down_app_package",
            "swipe_left_app_package",
            "swipe_right_app_package",
        )
        assertTrue(BackupSpec.ENCRYPTED_SETTINGS_KEYS.containsAll(swipeKeys))
    }

    @Test
    fun `all four stores are covered`() {
        assertEquals(
            listOf("settings", "favorites", "folders", "icon_mappings"),
            BackupSpec.STORE_NAMES,
        )
    }
}
