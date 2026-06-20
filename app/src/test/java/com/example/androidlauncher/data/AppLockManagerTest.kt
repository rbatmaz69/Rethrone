package com.example.androidlauncher.data

import android.util.Base64
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.slot
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Base64 as JavaBase64

/**
 * Unit-Tests für [AppLockManager]: Entsperr-Sitzung (reine Logik) und das
 * PBKDF2-Hashing/`verify`. `android.util.Base64` liefert in Plain-JVM-Tests `null`,
 * daher wird es statisch an `java.util.Base64` delegiert.
 */
class AppLockManagerTest {

    @Before
    fun setUp() {
        // android.util.Base64 -> java.util.Base64 (NO_WRAP entspricht dem RFC4648-Encoder/Decoder).
        mockkStatic(Base64::class)
        val bytesSlot = slot<ByteArray>()
        every { Base64.encodeToString(capture(bytesSlot), any()) } answers {
            JavaBase64.getEncoder().withoutPadding().encodeToString(bytesSlot.captured)
        }
        val strSlot = slot<String>()
        every { Base64.decode(capture(strSlot), any()) } answers {
            JavaBase64.getDecoder().decode(strSlot.captured)
        }
        // Geteilten Singleton-Zustand zwischen Tests zurücksetzen.
        AppLockManager.lockAll()
    }

    @After
    fun tearDown() {
        AppLockManager.lockAll()
        unmockkAll()
    }

    // --- Entsperr-Sitzung ---

    @Test
    fun markUnlocked_makesPackageUnlocked() {
        AppLockManager.markUnlocked("com.whatsapp")
        assertTrue(AppLockManager.isUnlocked("com.whatsapp"))
    }

    @Test
    fun unknownPackage_isNotUnlocked() {
        assertFalse(AppLockManager.isUnlocked("com.unknown"))
    }

    @Test
    fun retainOnly_keepsOnlyCurrentForegroundPackage() {
        AppLockManager.markUnlocked("com.a")
        AppLockManager.markUnlocked("com.b")

        AppLockManager.retainOnly("com.a")

        assertTrue(AppLockManager.isUnlocked("com.a"))
        assertFalse(AppLockManager.isUnlocked("com.b"))
    }

    @Test
    fun retainOnly_null_locksEverything() {
        AppLockManager.markUnlocked("com.a")
        AppLockManager.markUnlocked("com.b")

        AppLockManager.retainOnly(null)

        assertFalse(AppLockManager.isUnlocked("com.a"))
        assertFalse(AppLockManager.isUnlocked("com.b"))
    }

    @Test
    fun lockAll_clearsAllUnlockedPackages() {
        AppLockManager.markUnlocked("com.a")
        AppLockManager.markUnlocked("com.b")

        AppLockManager.lockAll()

        assertFalse(AppLockManager.isUnlocked("com.a"))
        assertFalse(AppLockManager.isUnlocked("com.b"))
    }

    // --- Hashing / verify ---

    @Test
    fun verify_acceptsCorrectSecret() {
        val token = AppLockManager.hashSecret("1234")
        assertTrue(AppLockManager.verify("1234", token))
    }

    @Test
    fun verify_rejectsWrongSecret() {
        val token = AppLockManager.hashSecret("1234")
        assertFalse(AppLockManager.verify("0000", token))
    }

    @Test
    fun hashSecret_usesRandomSalt_butBothVerify() {
        val a = AppLockManager.hashSecret("1234")
        val b = AppLockManager.hashSecret("1234")

        // Unterschiedliches Salt -> unterschiedliche Tokens trotz gleicher Eingabe.
        assertNotEquals(a, b)
        assertTrue(AppLockManager.verify("1234", a))
        assertTrue(AppLockManager.verify("1234", b))
    }

    @Test
    fun verify_rejectsMalformedToken() {
        assertFalse(AppLockManager.verify("1234", ""))
        assertFalse(AppLockManager.verify("1234", "no-colon-token"))
    }

    @Test
    fun verify_worksForPatternStyleInput() {
        // Muster werden als komma-getrennte Knotenfolge übergeben.
        val token = AppLockManager.hashSecret("0,4,8,5")
        assertTrue(AppLockManager.verify("0,4,8,5", token))
        assertFalse(AppLockManager.verify("0,4,8", token))
    }
}
