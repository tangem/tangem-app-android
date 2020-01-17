package com.tangem.tangem_sdk.android.reader

import android.nfc.NdefMessage
import android.nfc.NdefRecord
import com.tangem.tangem_card.reader.NfcReader
import com.tangem.tangem_card.reader.TLV
import com.tangem.tangem_card.reader.TLVList
import com.tangem.tangem_card.util.Util
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.*

class ReadSlixTagTask(val nfcReader: NfcReader) {

    private var lastErrorCode: Int = 0

    fun read(): ReadResult {
        if (!nfcReader.isConnected) {
            try {
                nfcReader.connect()
            } catch (e: Exception) {
                return ReadResult.Failure(e)
            }
        }
        return try {
            runRead()
        } catch (e: Exception) {
            nfcReader.ignoreTag()
            ReadResult.Failure(e)
        }
    }

    private fun runRead(): ReadResult {
        val ndefMessage = runReadNDEF()
        val records: Array<NdefRecord> = ndefMessage.records
        for (record in records) {
            if (record.toUri() != null && record.toUri().toString() == "vnd.android.nfc://ext/tangem.com:wallet") {
                val payload = record.payload
                val tlvNDEF = TLVList.fromBytes(Arrays.copyOfRange(payload, 2, payload.size))
                nfcReader.ignoreTag()
                return ReadResult.Success(tlvNDEF)
            }
        }
        return ReadResult.Failure(Exception("No parcelable Tlv has been found."))
    }

    private fun runReadNDEF(): NdefMessage {
        val answerCC = readSingleBlock(0x00)

        if (answerCC.size != 4 || answerCC[0].toInt() != 0xE1 || ((answerCC[1].toInt() and 0xF0) != 0x40)) {

        } else {
            throw Exception("Failed! Invalid CC read " + Util.bytesToHex(answerCC))
        }
        if ((answerCC[3].toInt() and 0x01) != 0x01) {
            throw Exception("Multiple block read unsupported!")
        }
        val areaSize = 8 * answerCC[2]
        val blocksCount = areaSize / 4

        val areaBuf = readMultipleBlocks(1, blocksCount)

        val tlvNDEF = TLVList.tryFromBytes(areaBuf)

        return NdefMessage(tlvNDEF.getTLV(TLV.Tag.TAG_CardPublicKey).Value)
    }

    private fun readSingleBlock(blockNo: Int): ByteArray {
        return doTransceive(0x20, blockNo)!!
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
            val blocks = doTransceive(0x23.toByte(), firstBlockToRead, blocksToRead - 1)
            blocksRemaining -= blocksToRead
            firstBlockToRead += blocksToRead
            resultBuf.write(blocks!!)
        }
        return resultBuf.toByteArray()
    }

    private fun doTransceive(cmd: Byte, p1: Int?, p2: Int? = null, params: ByteArray? = null): ByteArray? {
        val command: ByteArray
        val res: ByteArray?
        lastErrorCode = -1
        val os = ByteArrayOutputStream()
        os.write(REQ_FLAG.toInt())
        os.write(cmd.toInt())
        p1?.let { os.write(p1) }
        p2?.let { os.write(it) }
        params?.let { os.write(params, 0, params.size) }
        command = os.toByteArray()
        if (!nfcReader.isConnected) throw IOException("Connection  lost")
        res = nfcReader.transceive(command)
        lastErrorCode = res[0].toInt()
        if (lastErrorCode != 0) {
            throw IOException("Error! Code: " + String.format("0x%02x", lastErrorCode))
        }
        return res.copyOfRange(1, res.size)
    }

    companion object{
        // iso15693 flags
        private const val REQ_FLAG: Byte = 0x02
    }
}

sealed class ReadResult {
    data class Success(val tlvs: TLVList) : ReadResult()
    data class Failure(val exception: Exception) : ReadResult()
}
