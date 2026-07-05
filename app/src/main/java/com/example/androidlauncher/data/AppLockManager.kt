package com.example.androidlauncher.data

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Krypto- und Sitzungslogik der App-Sperre.
 *
 * - Das PIN/Muster wird **nie** im Klartext gespeichert, sondern als gesalzener
 *   PBKDF2-Hash (Token-Format: `Base64(salt):Base64(hash)`). Das Token selbst wird
 *   vom [ThemeManager] zusätzlich über den [CryptoManager] (AES-GCM) abgelegt.
 * - Die Entsperr-Sitzung merkt sich prozessweit (als Hilt-Singleton, siehe
 *   `DataModule`), welche Pakete aktuell freigeschaltet sind, damit nicht bei jedem
 *   Fensterwechsel erneut abgefragt wird, solange die App im Vordergrund bleibt.
 *   Service und Sperr-Activity laufen im selben Prozess.
 * - Die reinen Hash-Funktionen liegen im Companion, damit UI-Code ohne Instanz
 *   (z. B. `AppLockMenu`) Tokens erzeugen/prüfen kann.
 */
class AppLockManager {

    // --- Entsperr-Sitzung (prozessweit über das Hilt-Singleton, In-Memory) ---

    private val unlockedPackages = mutableSetOf<String>()

    @Synchronized
    fun markUnlocked(packageName: String) {
        unlockedPackages.add(packageName)
    }

    @Synchronized
    fun isUnlocked(packageName: String): Boolean = packageName in unlockedPackages

    /**
     * Behält nur das aktuelle Vordergrund-Paket als entsperrt; alle anderen werden
     * wieder gesperrt (Aufruf bei jedem Vordergrundwechsel durch den Service).
     */
    @Synchronized
    fun retainOnly(packageName: String?) {
        unlockedPackages.retainAll { it == packageName }
    }

    /** Sperrt alles wieder (z. B. bei Bildschirm aus). */
    @Synchronized
    fun lockAll() {
        unlockedPackages.clear()
    }

    companion object {

        private const val ITERATIONS = 120_000
        private const val KEY_LENGTH_BITS = 256
        private const val SALT_LENGTH = 16

        private val secureRandom = SecureRandom()

        /** Erzeugt aus einem PIN/Muster ein speicherbares Token `Base64(salt):Base64(hash)`. */
        fun hashSecret(input: String): String {
            val salt = ByteArray(SALT_LENGTH).also { secureRandom.nextBytes(it) }
            val hash = pbkdf2(input, salt)
            return "${Base64.encodeToString(salt, Base64.NO_WRAP)}:${Base64.encodeToString(hash, Base64.NO_WRAP)}"
        }

        /** Prüft eine Eingabe gegen ein zuvor mit [hashSecret] erzeugtes Token. */
        fun verify(input: String, token: String): Boolean {
            val parts = token.split(":")
            if (parts.size != 2) return false
            val salt = runCatching { Base64.decode(parts[0], Base64.NO_WRAP) }.getOrNull() ?: return false
            val expected = runCatching { Base64.decode(parts[1], Base64.NO_WRAP) }.getOrNull() ?: return false
            val actual = pbkdf2(input, salt)
            return constantTimeEquals(expected, actual)
        }

        private fun pbkdf2(input: String, salt: ByteArray): ByteArray {
            val spec = PBEKeySpec(input.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS)
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            return factory.generateSecret(spec).encoded
        }

        private fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean {
            if (a.size != b.size) return false
            var result = 0
            for (i in a.indices) result = result or (a[i].toInt() xor b[i].toInt())
            return result == 0
        }
    }
}
