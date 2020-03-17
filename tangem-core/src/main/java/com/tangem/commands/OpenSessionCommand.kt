package com.tangem.commands

import com.tangem.common.CardEnvironment
import com.tangem.common.apdu.CommandApdu
import com.tangem.common.apdu.Instruction
import com.tangem.common.apdu.ResponseApdu
import com.tangem.common.tlv.TlvBuilder
import com.tangem.common.tlv.TlvMapper
import com.tangem.common.tlv.TlvTag
import com.tangem.tasks.TaskError

class OpenSessionResponse(
        val sessionKeyB: ByteArray,
        val uid: ByteArray
) : CommandResponse


class OpenSessionCommand(private val sessionKeyA: ByteArray) : CommandSerializer<OpenSessionResponse>() {
    override fun serialize(cardEnvironment: CardEnvironment): CommandApdu {
        val tlvBuilder = TlvBuilder()
        tlvBuilder.append(TlvTag.SessionKeyA, sessionKeyA)
        return CommandApdu(
                Instruction.OpenSession, tlvBuilder.serialize(),
                encryptionMode = cardEnvironment.encryptionMode
        )
    }

    override fun deserialize(cardEnvironment: CardEnvironment, responseApdu: ResponseApdu): OpenSessionResponse? {
        val tlvData = responseApdu.getTlvData() ?: return null

        return try {
            val mapper = TlvMapper(tlvData)
            OpenSessionResponse(
                    sessionKeyB = mapper.map(TlvTag.SessionKeyB),
                    uid = mapper.map(TlvTag.Uid)
            )
        } catch (exception: Exception) {
            throw TaskError.SerializeCommandError()
        }
    }
}