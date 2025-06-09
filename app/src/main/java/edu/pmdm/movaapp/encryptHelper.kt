package edu.pmdm.movaapp

import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object encryptHelper {
    private const val secretKey = "1234567890123456" // 16 caracteres (clave segura)
    private const val initVector = "abcdefghijklmnop" // 16 caracteres (IV)

    private fun getCipher(mode: Int): Cipher {
        val ivSpec = IvParameterSpec(initVector.toByteArray(Charsets.UTF_8))
        val keySpec = SecretKeySpec(secretKey.toByteArray(Charsets.UTF_8), "AES")
        return Cipher.getInstance("AES/CBC/PKCS5PADDING").apply {
            init(mode, keySpec, ivSpec)
        }
    }

    fun encrypt(value: String): String {
        val cipher = getCipher(Cipher.ENCRYPT_MODE)
        val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(encrypted, Base64.DEFAULT)
    }

    fun decrypt(encrypted: String): String {
        val cipher = getCipher(Cipher.DECRYPT_MODE)
        val original = cipher.doFinal(Base64.decode(encrypted, Base64.DEFAULT))
        return String(original, Charsets.UTF_8)
    }
}