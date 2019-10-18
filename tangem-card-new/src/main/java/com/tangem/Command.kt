package com.tangem

import com.tangem.data.CommandApdu
import com.tangem.data.ResponseApduParsed
import com.tangem.data.Tlv
import com.tangem.enums.Instruction
import com.tangem.enums.TlvTag

abstract class Command {

    abstract val instruction: Instruction
    abstract val instructionCode: Int

    abstract fun serialize(cardEnvironment: CardEnvironment): CommandApdu

    abstract fun deserialize(responseApduParsed: ResponseApduParsed): TaskResult
}


class ReadCardCommand(val pin1: String) : Command() {

    override val instruction = Instruction.Read
    override val instructionCode = instruction.code

    override fun serialize(cardEnvironment: CardEnvironment): CommandApdu {

        return CommandApdu(
                listOf(Tlv(TlvTag.Pin, TlvTag.Pin.code, pin1.toByteArray())), instruction)
    }

    override fun deserialize(responseApduParsed: ResponseApduParsed): TaskResult {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }


}

class CheckWalletCommand(
        val pin1: String, val cid: String,
        val challenge: ByteArray, val publicKeyChallenge: ByteArray) : Command() {

    override val instruction = Instruction.CheckWallet
    override val instructionCode = instruction.code

    override fun serialize(cardEnvironment: CardEnvironment): CommandApdu {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun deserialize(responseApduParsed: ResponseApduParsed): TaskResult {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }


}

class SignCommand(val signCommandData: SignCommandData) : Command() {

    override val instruction = Instruction.Sign
    override val instructionCode = instruction.code

    override fun serialize(cardEnvironment: CardEnvironment): CommandApdu {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun deserialize(responseApduParsed: ResponseApduParsed): TaskResult {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }


}



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

sealed class CommandData {
    data class CheckWalletResponse(
            val cid: String,
            val salt: ByteArray,
            val walletSignature: ByteArray
    )
}

sealed class CommandResult{

    data class Success(val data: CommandData) : CommandResult()
    data class Error(val error: CardError) : CommandResult()

}