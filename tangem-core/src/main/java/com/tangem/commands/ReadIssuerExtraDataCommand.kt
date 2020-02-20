package com.tangem.commands

import com.tangem.common.CardEnvironment
import com.tangem.common.apdu.CommandApdu
import com.tangem.common.apdu.Instruction
import com.tangem.common.apdu.ResponseApdu
import com.tangem.common.tlv.TlvBuilder
import com.tangem.common.tlv.TlvMapper
import com.tangem.common.tlv.TlvTag
import com.tangem.tasks.TaskError

class ReadIssuerExtraDataResponse(

        /**
         * CID, Unique Tangem card ID number.
         */
        val cardId: String,

        /**
         * Size of all Issuer_Extra_Data field.
         */
        val size: Int?,

        /**
         * Data defined by issuer.
         */
        val issuerData: ByteArray,

        /**
         * Issuer’s signature of [issuerData] with Issuer Data Private Key (which is kept on card).
         * Issuer’s signature of SHA256-hashed [cardId] concatenated with [issuerData]:
         * SHA256([cardId] | [issuerData]).
         * When flag [SettingsMask.protectIssuerDataAgainstReplay] set in [SettingsMask] then signature of
         * SHA256-hashed CID Issuer_Data concatenated with and [issuerDataCounter]:
         * SHA256([cardId] | [issuerData] | [issuerDataCounter]).
         */
        val issuerDataSignature: ByteArray?,

        /**
         * An optional counter that protect issuer data against replay attack.
         * When flag [SettingsMask.protectIssuerDataAgainstReplay] set in [SettingsMask]
         * then this value is mandatory and must increase on each execution of [WriteIssuerDataCommand].
         */
        val issuerDataCounter: Int?
) : CommandResponse


/**
 * This command retrieves Issuer Extra Data field and its issuer’s signature.
 * Issuer Extra Data is never changed or parsed from within the Tangem COS. The issuer defines purpose of use,
 * format and payload of Issuer Data. . For example, this field may contain photo or
 * biometric information for ID card product. Because of the large size of Issuer_Extra_Data,
 * a series of these commands have to be executed to read the entire Issuer_Extra_Data.
 */
class ReadIssuerExtraDataCommand(
        verifier: IssuerDataVerifier = DefaultIssuerDataVerifier()
) : CommandSerializer<ReadIssuerExtraDataResponse>(), IssuerDataVerifier by verifier {

    var offset: Int = 0

    override fun serialize(cardEnvironment: CardEnvironment): CommandApdu {
        val tlvBuilder = TlvBuilder()
        tlvBuilder.append(TlvTag.Pin, cardEnvironment.pin1)
        tlvBuilder.append(TlvTag.CardId, cardEnvironment.cardId)
        tlvBuilder.append(TlvTag.Mode, IssuerDataMode.ExtraData)
        tlvBuilder.append(TlvTag.Offset, offset)
        return CommandApdu(Instruction.ReadIssuerData, tlvBuilder.serialize())
    }

    override fun deserialize(cardEnvironment: CardEnvironment, responseApdu: ResponseApdu): ReadIssuerExtraDataResponse? {
        val tlvData = responseApdu.getTlvData() ?: return null

        return try {
            val mapper = TlvMapper(tlvData)
            ReadIssuerExtraDataResponse(
                    cardId = mapper.map(TlvTag.CardId),
                    size = mapper.mapOptional(TlvTag.Size),
                    issuerData = mapper.mapOptional(TlvTag.IssuerData) ?: byteArrayOf(),
                    issuerDataSignature = mapper.mapOptional(TlvTag.IssuerDataSignature),
                    issuerDataCounter = mapper.mapOptional(TlvTag.IssuerDataCounter)
            )
        } catch (exception: Exception) {
            throw TaskError.SerializeCommandError()
        }
    }
}