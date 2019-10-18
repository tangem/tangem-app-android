package com.tangem.data

import com.tangem.enums.Instruction

class CommandApdu(
        val tlvList: List<Tlv>,
        val instruction: Instruction,
        val p1: Int = 0x00,
        val p2: Int = 0x00,
        val encryptionKey: ByteArray? = null,
        val rawInstruction: ByteArray = byteArrayOf()) {

    val cla = ISO_CLA


    fun serialize(): ByteArray {

        var length = 4 // CLA, INS, P1, P2

        val data = if (tlvList.isNotEmpty()) {
            tlvList.toBytes()
        } else {
            byteArrayOf()
        }

        val lc = data.size

        if (data.isNotEmpty()) {
            length += 1 // LC
            if (lc >= 256)
                length += 2
            length += data.size // DATA
        }

        val apdu = ByteArray(length)

        var index = 0
        apdu[index] = cla
        index++
        apdu[index] = instruction.code.toByte()
        index++
        apdu[index] = p1.toByte()
        index++
        apdu[index] = p2.toByte()
        index++
        if (lc != 0) {
            if (lc < 256) {
                apdu[index] = lc.toByte()
                index++
            } else {
                apdu[index] = 0
                index++
                apdu[index] = (lc shr 8).toByte()
                index++
                apdu[index] = (lc and 0xFF).toByte()
                index++
            }

            System.arraycopy(data, 0, apdu, index, data.size)
            index += data.size
        }
        return apdu


    }


    private fun encrypt() {

    }

    companion object {
        const val ISO_CLA = 0x00.toByte()
    }
}