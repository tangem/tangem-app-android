package com.tangem.commands

import com.tangem.CardEnvironment
import com.tangem.common.apdu.CommandApdu
import com.tangem.common.apdu.Instruction
import com.tangem.common.apdu.ResponseApdu
import com.tangem.common.extensions.calculateSha256
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.extensions.toByteArray
import com.tangem.common.tlv.Tlv
import com.tangem.common.tlv.TlvMapper
import com.tangem.common.tlv.TlvTag
import com.tangem.tasks.TaskError

class WriteIssuerDataResponse(
        /**
         * CID, Unique Tangem card ID number.
         */
        val cardId: String
) : CommandResponse

/**
 * This command writes 512-byte Issuer Data field and its issuer’s signature.
 * Issuer Data is never changed or parsed from within the Tangem COS. The issuer defines purpose of use,
 * format and payload of Issuer Data. For example, this field may contain information about
 * wallet balance signed by the issuer or additional issuer’s attestation data.
 */
class WriteIssuerDataCommand(
        private val cardId: String,
        private val issuerData: ByteArray,
        private val issuerDataSignature: ByteArray,
        private val issuerDataCounter: Int? = null
) : CommandSerializer<WriteIssuerDataResponse>() {

    override fun serialize(cardEnvironment: CardEnvironment): CommandApdu {
        val tlvData = mutableListOf(
                Tlv(TlvTag.Pin, cardEnvironment.pin1.calculateSha256()),
                Tlv(TlvTag.CardId, cardId.hexToBytes()),
                Tlv(TlvTag.IssuerData, issuerData),
                Tlv(TlvTag.IssuerDataSignature, issuerDataSignature)
        )
        if (issuerDataCounter != null) {
            tlvData.add(Tlv(TlvTag.IssuerDataCounter, issuerDataCounter.toByteArray()))
        }

        return CommandApdu(Instruction.WriteIssuerData, tlvData)
    }

    override fun deserialize(cardEnvironment: CardEnvironment, responseApdu: ResponseApdu): WriteIssuerDataResponse? {
        val tlvData = responseApdu.getTlvData() ?: return null

        return try {
            val mapper = TlvMapper(tlvData)
            WriteIssuerDataResponse(
                    cardId = mapper.map(TlvTag.CardId)
            )
        } catch (exception: Exception) {
            throw TaskError.SerializeCommandError()
        }
    }
}