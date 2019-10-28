package com.tangem.commands

import com.tangem.CardEnvironment
import com.tangem.common.apdu.CommandApdu
import com.tangem.common.apdu.ResponseApdu
import com.tangem.common.extentions.toInt
import com.tangem.common.tlv.TlvTag
import com.tangem.enums.Instruction

interface CommandResponse


abstract class CommandSerializer<T : CommandResponse> {

    abstract val instruction: Instruction
    abstract val instructionCode: Int

    abstract fun serialize(cardEnvironment: CardEnvironment): CommandApdu
    abstract fun deserialize(cardEnvironment: CardEnvironment, responseApdu: ResponseApdu): T?

    fun deserializeSecurityDelay(responseApdu: ResponseApdu, cardEnvironment: CardEnvironment): Int? {
        val tlv = responseApdu.getTlvData(cardEnvironment.encryptionKey)
        return tlv?.find { it.tag == TlvTag.Pause }?.value?.toInt()
    }
}
