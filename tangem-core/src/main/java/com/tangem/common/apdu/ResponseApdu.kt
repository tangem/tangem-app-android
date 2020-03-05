package com.tangem.common.apdu

import com.tangem.common.tlv.Tlv

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
        return when {
            data.size <= 2 -> null
            else -> Tlv.deserialize(data.copyOf(data.size - 2))
        }
    }


    private fun decrypt(encryptionKey: ByteArray) {
        TODO("not implemented")
    }

}