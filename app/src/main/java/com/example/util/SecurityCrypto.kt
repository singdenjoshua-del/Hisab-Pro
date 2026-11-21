package com.example.util

import android.content.Context
import androidx.biometric.BiometricManager
import java.security.MessageDigest
import java.security.SecureRandom

object SecurityCrypto {
    private val random = SecureRandom()

    fun generateSaltHex(): String {
        val bytes = ByteArray(16)
        random.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun hashCredential(rawSecret: String, saltHex: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val input = "$saltHex:$rawSecret".toByteArray(Charsets.UTF_8)
        // Iterative stretching (10,000 rounds) to prevent brute-force
        var hash = digest.digest(input)
        repeat(9_999) {
            digest.reset()
            hash = digest.digest(hash)
        }
        return hash.joinToString("") { "%02x".format(it) }
    }

    fun verifyCredential(rawSecret: String, saltHex: String, expectedHash: String): Boolean {
        if (saltHex.isBlank() || expectedHash.isBlank()) return false
        val actual = hashCredential(rawSecret, saltHex)
        return MessageDigest.isEqual(
            actual.toByteArray(Charsets.UTF_8),
            expectedHash.toByteArray(Charsets.UTF_8)
        )
    }

    fun canUseBiometrics(context: Context): Boolean {
        val manager = BiometricManager.from(context)
        val result = manager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.BIOMETRIC_WEAK
        )
        return result == BiometricManager.BIOMETRIC_SUCCESS
    }
}
