package cn.vvbbnn00.picture_data_hider.utils

import android.annotation.SuppressLint
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec


class AESHelper {

    companion object {
        /**
         * Generate a key for AES encryption
         */
        fun generateKey(): ByteArray {
            val key: ByteArray = ByteArray(16)
            val random = SecureRandom()
            random.nextBytes(key)

            return key
        }

        /**
         * Encrypt data with AES
         */
        @SuppressLint("GetInstance")
        fun encrypt(data: ByteArray, key: ByteArray): ByteArray {
            val skeySpec = SecretKeySpec(key, "AES")
            val cipher = Cipher.getInstance("AES")
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec)
            return cipher.doFinal(data)
        }

        /**
         * Decrypt data with AES
         */
        @SuppressLint("GetInstance")
        fun decrypt(data: ByteArray, key: ByteArray): ByteArray {
            val skeySpec = SecretKeySpec(key, "AES")
            val cipher = Cipher.getInstance("AES")
            cipher.init(Cipher.DECRYPT_MODE, skeySpec)
            return cipher.doFinal(data)
        }


        /**
         * Convert byte array to hex string
         */
        fun bytesToHex(bytes: ByteArray): String {
            val hexArray = "0123456789ABCDEF".toCharArray()
            val hexChars = CharArray(bytes.size * 2)

            for (i in bytes.indices) {
                val v = bytes[i].toInt() and 0xFF
                hexChars[i * 2] = hexArray[v ushr 4]
                hexChars[i * 2 + 1] = hexArray[v and 0x0F]
            }

            return String(hexChars)
        }

        /**
         * Convert hex string to byte array
         */
        fun hexToBytes(hexString: String): ByteArray {
            val len = hexString.length
            val data = ByteArray(len / 2)

            for (i in 0 until len step 2) {
                data[i / 2] = ((Character.digit(hexString[i], 16) shl 4)
                        + Character.digit(hexString[i + 1], 16)).toByte()
            }

            return data
        }

    }
}