package com.tangem.tangem_sdk_new.nfc

import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.tech.NfcV
import com.tangem.common.apdu.StatusWord
import com.tangem.common.extensions.toByteArray
import com.tangem.common.extensions.toHexString
import com.tangem.common.tlv.Tlv
import com.tangem.common.tlv.TlvMapper
import com.tangem.common.tlv.TlvTag
import java.io.ByteArrayOutputStream
import java.io.IOException

class SlixTagReader() {

    private lateinit var nfcV: NfcV

    fun transceive(nfcV: NfcV): SlixReadResult {
        this.nfcV = nfcV
        if (!nfcV.isConnected) {
            try {
                nfcV.connect()
            } catch (e: Exception) {
                return SlixReadResult.Failure(e)
            }
        }
        return try {
            runRead()
        } catch (e: Exception) {
            nfcV.close()
            SlixReadResult.Failure(e)
        }
    }

    private fun runRead(): SlixReadResult {
        val ndefMessage = runReadNDEF()
        val records: Array<NdefRecord> = ndefMessage.records
        for (record in records) {
            if (record.toUri() != null && record.toUri().toString() == "vnd.android.nfc://ext/tangem.com:wallet") {
                val payload = record.payload
                val status = StatusWord.ProcessCompleted.code.toByteArray()
                val data = payload.copyOfRange(2, payload.size) + status
                nfcV.close()
                return SlixReadResult.Success(data)
            }
        }
        return SlixReadResult.Failure(Exception("No relevant data was found."))
    }

    private fun runReadNDEF(): NdefMessage {
        val answerCC = readSingleBlock(0x00)

        if (answerCC.size != 4 || answerCC[0].toInt() != 0xE1 || ((answerCC[1].toInt() and 0xF0) != 0x40)) {

        } else {
            throw Exception("Failed! Invalid CC read " + answerCC.toHexString())
        }
        if ((answerCC[3].toInt() and 0x01) != 0x01) {
            throw Exception("Multiple block read unsupported!")
        }
        val areaSize = 8 * answerCC[2]
        val blocksCount = areaSize / 4

        val areaBuf = readMultipleBlocks(1, blocksCount)

        val tlvNdef = TlvMapper(Tlv.deserialize(areaBuf, true) ?: listOf())

        return NdefMessage(tlvNdef.map<ByteArray>(TlvTag.CardPublicKey))
    }

    private fun readSingleBlock(blockNo: Int): ByteArray {
        return performRead(0x20, blockNo)!!
    }

    private fun readMultipleBlocks(startBlock: Int, blocksCount: Int): ByteArray {
        val resultBuf = ByteArrayOutputStream()
        val maxBlocksAtOnce = 32
        var blocksRemaining = blocksCount
        var firstBlockToRead = startBlock
        while (blocksRemaining > 0) {
            val blocksToRead = if (blocksRemaining > maxBlocksAtOnce) {
                maxBlocksAtOnce
            } else {
                blocksRemaining
            }
            val blocks = performRead(0x23.toByte(), firstBlockToRead, blocksToRead - 1)
            blocksRemaining -= blocksToRead
            firstBlockToRead += blocksToRead
            resultBuf.write(blocks!!)
        }
        return resultBuf.toByteArray()
    }

    private fun performRead(cmd: Byte, p1: Int?, p2: Int? = null, params: ByteArray? = null): ByteArray? {
        val command: ByteArray
        val res: ByteArray?
        val os = ByteArrayOutputStream()
        os.write(REQ_FLAG.toInt())
        os.write(cmd.toInt())
        p1?.let { os.write(p1) }
        p2?.let { os.write(it) }
        params?.let { os.write(params, 0, params.size) }
        command = os.toByteArray()
        if (!nfcV.isConnected) throw IOException("Connection  lost")
        res = nfcV.transceive(command)
        val errorCode = res[0].toInt()
        if (errorCode != 0) {
            throw IOException("Error! Code: " + String.format("0x%02x", errorCode))
        }
        return res.copyOfRange(1, res.size)
    }

    companion object {
        // iso15693 flags
        private const val REQ_FLAG: Byte = 0x02
    }
}

sealed class SlixReadResult {
    data class Success(val data: ByteArray) : SlixReadResult()
    data class Failure(val exception: Exception) : SlixReadResult()
}