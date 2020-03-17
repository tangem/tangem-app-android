package com.tangem.common.apdu

import com.tangem.common.extensions.calculateCrc16
import com.tangem.common.tlv.Tlv
import com.tangem.crypto.decrypt
import java.io.ByteArrayInputStream

/**
 * Stores response data from the card and parses it to [Tlv] and [StatusWord].
 *
 * @property data Raw response from the card.
 * @property sw Status word code, reflecting the status of the response.
 * @property statusWord Parsed status word.
 */
class ResponseApdu(private val data: ByteArray) {

    private val sw1: Int = 0x00FF and data[data.size - 2].toInt()
    private val sw2: Int = 0x00FF and data[data.size - 1].toInt()

    val sw: Int = sw1 shl 8 or sw2

    val statusWord: StatusWord = StatusWord.byCode(sw)

    /**
     * Converts raw response data to the list of TLVs.
     *
     * @param encryptionKey key to decrypt response.
     * (Encryption / decryption functionality is not implemented yet.)
     */
    fun getTlvData(encryptionKey: ByteArray? = null): List<Tlv>? {
        return if (data.size <= 2) {
            null
        } else {
            val responseData = data.copyOf(data.size - 2)
            return if (encryptionKey != null) {
                if (data.size >= 18) {
                    val decryptedData = decrypt(responseData, encryptionKey)
                    Tlv.deserialize(decryptedData)
                } else {
                    null
                }
            } else {
                Tlv.deserialize(responseData)
            }
        }
    }

    private fun decrypt(responseData: ByteArray, encryptionKey: ByteArray): ByteArray {
        val decryptedData: ByteArray = responseData.decrypt(encryptionKey)

        val inputStream = ByteArrayInputStream(decryptedData)
        val baLength = ByteArray(2)
        inputStream.read(baLength)
        val length = (baLength[0].toInt() and 0xFF) * 256 + (baLength[1].toInt() and 0xFF)
        if (length > decryptedData.size - 4) throw Exception("Can't decrypt - data size invalid")
        val baCRC = ByteArray(2)
        inputStream.read(baCRC)
        val answerData = ByteArray(length)
        inputStream.read(answerData)
        val crc: ByteArray = answerData.calculateCrc16()
        if (!baCRC.contentEquals(crc)) throw Exception("Can't decrypt - crc invalid")

        return answerData
    }

}