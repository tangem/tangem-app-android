package com.tangem.commands

import com.tangem.CardEnvironment
import com.tangem.common.apdu.CommandApdu
import com.tangem.common.apdu.Instruction
import com.tangem.common.apdu.ResponseApdu
import com.tangem.common.extentions.calculateSha256
import com.tangem.common.extentions.hexToBytes
import com.tangem.common.tlv.Tlv
import com.tangem.common.tlv.TlvMapper
import com.tangem.common.tlv.TlvTag
import com.tangem.crypto.sign
import com.tangem.tasks.TaskError

class SignResponse(
        val cardId: String,
        val signature: ByteArray,
        val remainingSignatures: Int,
        val signedHashes: Int
) : CommandResponse


class SignCommand(private val hashes: Array<ByteArray>, private val cardId: String)
    : CommandSerializer<SignResponse>() {

    override val instruction = Instruction.Sign
    override val instructionCode = instruction.code

    private val hashSizes = if (hashes.isNotEmpty()) hashes.first().size else 0
    private val dataToSign = flattenHashes()

    private fun flattenHashes(): ByteArray {
        checkForErrors()
        return hashes.reduce { arr1, arr2 -> arr1 + arr2 }
    }

    private fun checkForErrors() {
        if (hashes.isEmpty()) throw TaskError.EmptyHashes()
        if (hashes.size > 10) throw  TaskError.TooMuchHashes()
        if (hashes.any { it.size != hashSizes }) throw TaskError.HashSizeMustBeEqual()
    }

    override fun serialize(cardEnvironment: CardEnvironment): CommandApdu {
        val tlvData = mutableListOf(
                Tlv(TlvTag.Pin, cardEnvironment.pin1.calculateSha256()),
                Tlv(TlvTag.Pin2, cardEnvironment.pin2.calculateSha256()),
                Tlv(TlvTag.CardId, cardId.hexToBytes()),
                Tlv(TlvTag.TransactionOutHashSize,byteArrayOf(hashSizes.toByte())),
                Tlv(TlvTag.TransactionOutHash, dataToSign)
        )

        addTerminalSignature(cardEnvironment, tlvData)

        return CommandApdu(instructionCode, tlvData)
    }

    private fun addTerminalSignature(cardEnvironment: CardEnvironment, tlvData: MutableList<Tlv>) {
        cardEnvironment.terminalKeys?.let {
            val signedData = dataToSign.sign(it.privateKey)
            tlvData.add(Tlv(TlvTag.TerminalTransactionSignature, signedData))
            tlvData.add(Tlv(TlvTag.TerminalPublicKey, it.publicKey))
        }
    }

    override fun deserialize(cardEnvironment: CardEnvironment, responseApdu: ResponseApdu): SignResponse? {
        val tlvData = responseApdu.getTlvData() ?: return null

        val tlvMapper = TlvMapper(tlvData)
        return SignResponse(
                cardId= tlvMapper.map(TlvTag.CardId),
                signature = tlvMapper.map(TlvTag.Signature),
                remainingSignatures = tlvMapper.map(TlvTag.RemainingSignatures),
                signedHashes = tlvMapper.map(TlvTag.SignedHashes)
        )
    }
}