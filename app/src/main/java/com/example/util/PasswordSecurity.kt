package com.example.util

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object PasswordSecurity {
    private const val ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val ITERATIONS = 100_000
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_BYTES = 16

    data class HashedPassword(
        val hashBase64: String,
        val saltBase64: String
    )

    fun hashPassword(password: String): HashedPassword {
        val salt = ByteArray(SALT_BYTES)
        SecureRandom().nextBytes(salt)
        val derived = deriveKey(password, salt)
        return HashedPassword(
            hashBase64 = Base64.encodeToString(derived, Base64.NO_WRAP),
            saltBase64 = Base64.encodeToString(salt, Base64.NO_WRAP)
        )
    }

    fun verifyPassword(password: String, expectedHashBase64: String, saltBase64: String): Boolean {
        if (password.isEmpty() || expectedHashBase64.isBlank() || saltBase64.isBlank()) return false
        return try {
            val salt = Base64.decode(saltBase64, Base64.NO_WRAP)
            val expected = Base64.decode(expectedHashBase64, Base64.NO_WRAP)
            val actual = deriveKey(password, salt)
            MessageDigest.isEqual(actual, expected)
        } catch (_: Exception) {
            false
        }
    }

    private fun deriveKey(password: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS)
        return try {
            SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }
}
