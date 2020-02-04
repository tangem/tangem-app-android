package com.tangem.commands

import com.tangem.common.CardEnvironment
import com.tangem.common.apdu.CommandApdu
import com.tangem.common.apdu.Instruction
import com.tangem.common.apdu.ResponseApdu
import com.tangem.common.extensions.calculateSha256
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.extensions.toByteArray
import com.tangem.common.tlv.Tlv
import com.tangem.common.tlv.TlvBuilder
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
 * @property cardId CID, Unique Tangem card ID number.
 * @property issuerData Data provided by issuer.
 * @property issuerDataSignature Issuer’s signature of [issuerData] with Issuer Data Private Key (which is kept on card).
 * @property issuerDataCounter An optional counter that protect issuer data against replay attack.
 */
class WriteIssuerDataCommand(
        private val cardId: String,
        private val issuerData: ByteArray,
        private val issuerDataSignature: ByteArray,
        private val issuerDataCounter: Int? = null
) : CommandSerializer<WriteIssuerDataResponse>() {

    override fun serialize(cardEnvironment: CardEnvironment): CommandApdu {
        val tlvBuilder = TlvBuilder()
        tlvBuilder.append(TlvTag.Pin, cardEnvironment.pin1)
        tlvBuilder.append(TlvTag.CardId, cardId)
        tlvBuilder.append(TlvTag.IssuerData, issuerData)
        tlvBuilder.append(TlvTag.IssuerDataSignature, issuerDataSignature)
        tlvBuilder.append(TlvTag.IssuerDataCounter, issuerDataCounter)

        return CommandApdu(Instruction.WriteIssuerData, tlvBuilder.serialize())
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