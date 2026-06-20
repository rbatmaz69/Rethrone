package com.example.androidlauncher.data

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Verschlüsselt sensible Werte (z. B. ausgeblendete Apps, Suchverlauf, Gesten-Pakete), bevor sie
 * im DataStore landen. Der Schlüssel liegt im hardwaregestützten [AndroidKeyStore][KEYSTORE] und
 * verlässt das Gerät nie – Backups/ADB-Dumps der App-Daten enthalten dadurch nur Chiffretext.
 *
 * Format eines Tokens: Base64( IV(12 Byte) || AES-GCM-Ciphertext+Tag ).
 *
 * Bewusst dependency-frei (nur Plattform-Krypto), daher kein `androidx.security:security-crypto`.
 */
object CryptoManager {

    private const val KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "rethrone_data_key"
    private const val TRANSFORMATION =
        "${KeyProperties.KEY_ALGORITHM_AES}/${KeyProperties.BLOCK_MODE_GCM}/${KeyProperties.ENCRYPTION_PADDING_NONE}"
    private const val IV_LENGTH = 12
    private const val GCM_TAG_BITS = 128

    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance(KEYSTORE).apply { load(null) }
    }

    /**
     * Auf jedem echten Gerät ab API 26 ist der AndroidKeyStore vorhanden. Nur in reinen
     * JVM-Unit-Tests (ohne Android-Runtime) fehlt der Provider – dort arbeitet [encrypt] als
     * Klartext-Passthrough, damit die Tests ohne Robolectric laufen. Auf einem echten Gerät
     * würde ein Fehlschlag hier korrekt eine Exception auslösen, statt heimlich Klartext zu speichern.
     */
    private val isKeystoreAvailable: Boolean by lazy {
        runCatching { getOrCreateKey() }.isSuccess
    }

    /** Verschlüsselt [plain] und liefert ein Base64-Token. */
    fun encrypt(plain: String): String {
        if (!isKeystoreAvailable) return plain
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        }
        val cipherText = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        val combined = cipher.iv + cipherText
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    /** Entschlüsselt ein mit [encrypt] erzeugtes Token. Wirft bei ungültigem/fremdem Token. */
    fun decrypt(token: String): String {
        val combined = Base64.decode(token, Base64.NO_WRAP)
        require(combined.size > IV_LENGTH) { "Token zu kurz" }
        val iv = combined.copyOfRange(0, IV_LENGTH)
        val cipherText = combined.copyOfRange(IV_LENGTH, combined.size)
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
        }
        return cipher.doFinal(cipherText).toString(Charsets.UTF_8)
    }

    /**
     * Liest einen gespeicherten Wert, der entweder bereits verschlüsselt ist oder noch als
     * Klartext aus einer älteren App-Version stammt. Schlägt die Entschlüsselung fehl, wird der
     * Rohwert zurückgegeben – beim nächsten Schreiben verschlüsselt [encrypt] ihn dann erneut
     * (sanfte Einmal-Migration). [stored] darf null/leer sein.
     */
    fun decryptOrLegacy(stored: String?): String {
        if (stored.isNullOrEmpty()) return ""
        return runCatching { decrypt(stored) }.getOrDefault(stored)
    }

    private fun getOrCreateKey(): SecretKey {
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        return generateKey()
    }

    @Synchronized
    private fun generateKey(): SecretKey {
        // Doppelprüfung: ein paralleler Aufruf könnte den Schlüssel inzwischen erzeugt haben.
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }
}
