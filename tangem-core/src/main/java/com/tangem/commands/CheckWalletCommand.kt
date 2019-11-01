package com.tangem.commands

import com.tangem.CardEnvironment
import com.tangem.common.apdu.CommandApdu
import com.tangem.common.apdu.ResponseApdu
import com.tangem.common.extentions.calculateSha256
import com.tangem.common.extentions.hexToBytes
import com.tangem.common.tlv.Tlv
import com.tangem.common.tlv.TlvMapper
import com.tangem.common.tlv.TlvTag
import com.tangem.enums.Instruction
import com.tangem.tasks.TaskError

class CheckWalletResponse(
        val cardId: String,
        val salt: ByteArray,
        val walletSignature: ByteArray
) : CommandResponse


class CheckWalletCommand(
        val pin1: String, val cid: String,
        val challenge: ByteArray, val publicKeyChallenge: ByteArray) : CommandSerializer<CheckWalletResponse>() {

    override val instruction = Instruction.CheckWallet
    override val instructionCode = instruction.code

    override fun serialize(cardEnvironment: CardEnvironment): CommandApdu {
        val tlvData = listOf(
                Tlv(TlvTag.Pin, cardEnvironment.pin1.calculateSha256()),
                Tlv(TlvTag.CardId, cid.hexToBytes()),
                Tlv(TlvTag.Challenge, challenge)
        )

        return CommandApdu(instructionCode, tlvData)
    }

    override fun deserialize(cardEnvironment: CardEnvironment, responseApdu: ResponseApdu): CheckWalletResponse? {
        val tlvData = responseApdu.getTlvData() ?: return null

        return try {
            val mapper = TlvMapper(tlvData)
            CheckWalletResponse(
                    cardId = mapper.map(TlvTag.CardId),
                    salt = mapper.map(TlvTag.Salt),
                    walletSignature = mapper.map(TlvTag.Signature)
            )
        } catch (exception: Exception) {
            throw TaskError.SerializeCommandError()
        }
    }


}