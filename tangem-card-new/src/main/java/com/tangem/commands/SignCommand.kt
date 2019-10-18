package com.tangem.commands

import com.tangem.CardEnvironment
import com.tangem.common.apdu.CommandApdu
import com.tangem.common.apdu.ResponseApdu
import com.tangem.enums.Instruction

class SignCommand(val signCommandData: SignCommandData) : CommandSerializer<SignCommandResponse>() {

    override val instruction = Instruction.Sign
    override val instructionCode = instruction.code

    override fun serialize(cardEnvironment: CardEnvironment): CommandApdu {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun deserialize(cardEnvironment: CardEnvironment, responseApdu: ResponseApdu): SignCommandResponse? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}

class SignCommandResponse(

) : CommandResponse


data class SignCommandData(
        val cid: String,
        val pin1: String,
        val pin2: String,
        val cvc: String,
        val transactionHash: TransactionHash,
        val issuerData: IssuerData,
        val terminalPublicKey: ByteArray? = null,
        val terminalTransactionSignature: ByteArray? = null

)

data class TransactionHash(
        val transactionHash: ByteArray,
        val transactionRaw: ByteArray,
        val hashName: String
)

data class IssuerData(
        val issuerData: ByteArray,
        val issuerDataCounter: Int,
        val issuerTransactionSignature: ByteArray,
        val issuerDataSignature: ByteArray
)