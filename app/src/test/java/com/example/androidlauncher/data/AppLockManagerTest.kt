package com.example.androidlauncher.data

import android.util.Base64
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
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

    // Frische Instanz pro Test; in Produktion ist der Manager ein Hilt-Singleton.
    private lateinit var manager: AppLockManager

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
        manager = AppLockManager()
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    // --- Entsperr-Sitzung ---

    @Test
    fun markUnlocked_makesPackageUnlocked() {
        manager.markUnlocked("com.whatsapp")
        assertTrue(manager.isUnlocked("com.whatsapp"))
    }

    @Test
    fun unknownPackage_isNotUnlocked() {
        assertFalse(manager.isUnlocked("com.unknown"))
    }

    @Test
    fun retainOnly_keepsOnlyCurrentForegroundPackage() {
        manager.markUnlocked("com.a")
        manager.markUnlocked("com.b")

        manager.retainOnly("com.a")

        assertTrue(manager.isUnlocked("com.a"))
        assertFalse(manager.isUnlocked("com.b"))
    }

    @Test
    fun retainOnly_null_locksEverything() {
        manager.markUnlocked("com.a")
        manager.markUnlocked("com.b")

        manager.retainOnly(null)

        assertFalse(manager.isUnlocked("com.a"))
        assertFalse(manager.isUnlocked("com.b"))
    }

    @Test
    fun lockAll_clearsAllUnlockedPackages() {
        manager.markUnlocked("com.a")
        manager.markUnlocked("com.b")

        manager.lockAll()

        assertFalse(manager.isUnlocked("com.a"))
        assertFalse(manager.isUnlocked("com.b"))
    }

    @Test
    fun separateInstances_doNotShareSessionState() {
        // Absicherung der Umstellung von `object` auf Hilt-Singleton: Sitzungen
        // leben in der Instanz, nicht mehr in globalem statischem Zustand.
        val other = AppLockManager()
        manager.markUnlocked("com.a")

        assertFalse(other.isUnlocked("com.a"))
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
