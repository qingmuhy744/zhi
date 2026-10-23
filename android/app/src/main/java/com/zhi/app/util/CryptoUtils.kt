package com.zhi.app.util

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object CryptoUtils {

    private const val AES_GCM_TAG_LENGTH = 128
    private const val GCM_IV_LENGTH = 12
    private const val CBC_IV_LENGTH = 16

    /**
     * Generates a 32-character random alphanumeric AES key
     */
    fun generateRandomKey(length: Int = 32): String {
        val chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        val random = SecureRandom()
        val sb = StringBuilder(length)
        repeat(length) {
            sb.append(chars[random.nextInt(chars.length)])
        }
        return sb.toString()
    }

    /**
     * Derives a 256-bit AES key from the passphrase using SHA-256
     */
    private fun deriveKey(keyPassphrase: String): SecretKeySpec {
        val digest = MessageDigest.getInstance("SHA-256")
        val keyBytes = digest.digest(keyPassphrase.toByteArray(Charsets.UTF_8))
        return SecretKeySpec(keyBytes, "AES")
    }

    /**
     * Encrypts plaintext using AES-256-GCM (with prepended 12-byte IV).
     * Output format: Base64(IV + CiphertextWithTag)
     */
    fun encryptGCM(plaintext: String, keyPassphrase: String): String {
        val secretKey = deriveKey(keyPassphrase)
        val iv = ByteArray(GCM_IV_LENGTH)
        SecureRandom().nextBytes(iv)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(AES_GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec)

        val encrypted = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        val combined = ByteArray(iv.size + encrypted.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(encrypted, 0, combined, iv.size, encrypted.size)

        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    /**
     * Decrypts ciphertext (supporting both AES-GCM and AES-CBC).
     * Automatically attempts:
     * 1. AES-256-GCM with prepended 12-byte IV
     * 2. AES-256-GCM with separate IV parameter
     * 3. AES-256-CBC with prepended 16-byte IV
     * 4. AES-256-CBC with separate IV parameter or zero IV
     */
    fun decrypt(ciphertextBase64: String, keyPassphrase: String, customIv: String? = null): String {
        val secretKey = deriveKey(keyPassphrase)
        val data = Base64.decode(ciphertextBase64.trim(), Base64.DEFAULT)

        // 1. Try AES-GCM with custom IV if provided
        if (!customIv.isNullOrEmpty()) {
            try {
                val ivBytes = if (customIv.length == GCM_IV_LENGTH) {
                    customIv.toByteArray(Charsets.UTF_8)
                } else {
                    Base64.decode(customIv, Base64.DEFAULT)
                }
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                val spec = GCMParameterSpec(AES_GCM_TAG_LENGTH, ivBytes)
                cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
                return String(cipher.doFinal(data), Charsets.UTF_8)
            } catch (_: Exception) {}

            // Try AES-CBC with custom IV
            try {
                val ivBytes = Base64.decode(customIv, Base64.DEFAULT)
                val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
                cipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(ivBytes))
                return String(cipher.doFinal(data), Charsets.UTF_8)
            } catch (_: Exception) {}
        }

        // 2. Try AES-GCM with prepended 12-byte IV
        if (data.size > GCM_IV_LENGTH) {
            try {
                val iv = ByteArray(GCM_IV_LENGTH)
                System.arraycopy(data, 0, iv, 0, GCM_IV_LENGTH)
                val encrypted = ByteArray(data.size - GCM_IV_LENGTH)
                System.arraycopy(data, GCM_IV_LENGTH, encrypted, 0, encrypted.size)

                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                val spec = GCMParameterSpec(AES_GCM_TAG_LENGTH, iv)
                cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
                return String(cipher.doFinal(encrypted), Charsets.UTF_8)
            } catch (_: Exception) {}
        }

        // 3. Try AES-CBC with prepended 16-byte IV
        if (data.size > CBC_IV_LENGTH) {
            try {
                val iv = ByteArray(CBC_IV_LENGTH)
                System.arraycopy(data, 0, iv, 0, CBC_IV_LENGTH)
                val encrypted = ByteArray(data.size - CBC_IV_LENGTH)
                System.arraycopy(data, CBC_IV_LENGTH, encrypted, 0, encrypted.size)

                val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
                cipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(iv))
                return String(cipher.doFinal(encrypted), Charsets.UTF_8)
            } catch (_: Exception) {}
        }

        // 4. Try AES-CBC with zero IV (Bark fallback)
        try {
            val iv = ByteArray(CBC_IV_LENGTH)
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(iv))
            return String(cipher.doFinal(data), Charsets.UTF_8)
        } catch (e: Exception) {
            throw IllegalArgumentException("Decryption failed. Please verify the AES Key.", e)
        }
    }
}
