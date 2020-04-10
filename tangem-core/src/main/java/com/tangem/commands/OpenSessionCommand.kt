package com.tangem.commands

import com.tangem.SessionEnvironment
import com.tangem.SessionError
import com.tangem.common.apdu.CommandApdu
import com.tangem.common.apdu.Instruction
import com.tangem.common.apdu.ResponseApdu
import com.tangem.common.tlv.TlvBuilder
import com.tangem.common.tlv.TlvDecoder
import com.tangem.common.tlv.TlvTag

class OpenSessionResponse(
        val sessionKeyB: ByteArray,
        val uid: ByteArray
) : CommandResponse

/**
 * In case of encrypted communication, App should setup a session before calling any further command.
 * [OpenSessionCommand] generates secret session_key that is used by both host and card
 * to encrypt and decrypt commands’ payload.

 */
class OpenSessionCommand(private val sessionKeyA: ByteArray) : Command<OpenSessionResponse>() {
    override fun serialize(environment: SessionEnvironment): CommandApdu {
        val tlvBuilder = TlvBuilder()
        tlvBuilder.append(TlvTag.SessionKeyA, sessionKeyA)
        return CommandApdu(
                Instruction.OpenSession, tlvBuilder.serialize(),
                encryptionMode = environment.encryptionMode
        )
    }

    override fun deserialize(environment: SessionEnvironment, apdu: ResponseApdu): OpenSessionResponse {
        val tlvData = apdu.getTlvData()
                ?: throw SessionError.DeserializeApduFailed()

        val decoder = TlvDecoder(tlvData)
        return OpenSessionResponse(
                sessionKeyB = decoder.decode(TlvTag.SessionKeyB),
                uid = decoder.decode(TlvTag.Uid)
        )
    }
}