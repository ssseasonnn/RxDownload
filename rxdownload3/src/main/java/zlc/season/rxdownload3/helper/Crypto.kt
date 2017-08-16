package zlc.season.rxdownload3.helper

import java.io.UnsupportedEncodingException
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException


object Crypto {
    private val MD5 = "MD5"
    private val DIGITS_LOWER = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f')
    private val DIGITS_UPPER = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F')

    fun md5(text: String): String {
        return String(encodeHex(md5Bytes(getRawBytes(text))))
    }

    fun getRawBytes(text: String): ByteArray {
        try {
            return text.toByteArray(Charsets.UTF_8)
        } catch (e: UnsupportedEncodingException) {
            return text.toByteArray()
        }
    }

    fun md5(data: ByteArray): String {
        return String(encodeHex(md5Bytes(data)))
    }

    fun encodeHex(data: ByteArray, toLowerCase: Boolean = true): CharArray {
        return encodeHex(data, if (toLowerCase) DIGITS_LOWER else DIGITS_UPPER)
    }

    fun encodeHex(data: ByteArray, toDigits: CharArray): CharArray {
        val l = data.size
        val out = CharArray(l shl 1)
        var i = 0
        var j = 0
        while (i < l) {
            out[j++] = toDigits[(240 and data[i].toInt()).ushr(4)]
            out[j++] = toDigits[15 and data[i].toInt()]
            i++
        }
        return out
    }

    fun md5Bytes(data: ByteArray): ByteArray {
        return getDigest(MD5).digest(data)
    }

    fun getDigest(algorithm: String): MessageDigest {
        try {
            return MessageDigest.getInstance(algorithm)
        } catch (e: NoSuchAlgorithmException) {
            throw IllegalArgumentException(e)
        }

    }
}