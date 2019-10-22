package com.tangem.commands

import com.tangem.CardEnvironment
import com.tangem.common.apdu.CommandApdu
import com.tangem.common.apdu.ResponseApdu
import com.tangem.enums.Instruction
import com.tangem.tasks.TaskError

//interface TlvMappable {
//    fun responseFromTlv(tlvList: List<Tlv>): CommandResponse?
//}

interface CommandResponse
//    : TlvMappable {
//
//}


abstract class CommandSerializer<T : CommandResponse>() {

    abstract val instruction: Instruction
    abstract val instructionCode: Int

    abstract fun serialize(cardEnvironment: CardEnvironment): CommandApdu
    abstract fun deserialize(cardEnvironment: CardEnvironment, responseApdu: ResponseApdu): T?
}

sealed class CommandEvent {
    class Success(val response: Any) : CommandEvent()
    class Failure(val taskError: TaskError) : CommandEvent()
    class UserCancellation : CommandEvent()
}

//class CheckWalletResponse(
//        val cardId: String,
//        val salt: ByteArray,
//        val walletSignature: ByteArray
//) : CommandResponse, TlvMappable by CheckWalletResponse {
//
//    companion object : TlvMappable {
//        override fun responseFromTlv(tlvList: List<Tlv>): CommandResponse? {
//            val mapper = TlvMapper(tlvList)
//
//            return try {
//                CheckWalletResponse(
//                        mapper.map(TlvTag.CardId),
//                        mapper.map(TlvTag.Salt),
//                        mapper.map(TlvTag.Signature)
//                )
//            } catch (exception: TlvMapperException) {
//                null
//            }
//        }
//
//    }
//}





