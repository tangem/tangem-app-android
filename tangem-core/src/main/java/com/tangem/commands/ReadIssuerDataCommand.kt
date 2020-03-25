package com.tangem.commands

import com.tangem.commands.common.DefaultIssuerDataVerifier
import com.tangem.commands.common.IssuerDataMode
import com.tangem.commands.common.IssuerDataVerifier
import com.tangem.common.CardEnvironment
import com.tangem.common.apdu.CommandApdu
import com.tangem.common.apdu.Instruction
import com.tangem.common.apdu.ResponseApdu
import com.tangem.common.tlv.TlvBuilder
import com.tangem.common.tlv.TlvMapper
import com.tangem.common.tlv.TlvTag
import com.tangem.tasks.TaskError

class ReadIssuerDataResponse(

        /**
         * CID, Unique Tangem card ID number.
         */
        val cardId: String,

        /**
         * Data defined by issuer.
         */
        val issuerData: ByteArray,

        /**
         * Issuer’s signature of [issuerData] with Issuer Data Private Key (which is kept on card).
         * Issuer’s signature of SHA256-hashed [cardId] concatenated with [issuerData]:
         * SHA256([cardId] | [issuerData]).
         * When flag [Settings.ProtectIssuerDataAgainstReplay] set in [SettingsMask] then signature of
         * SHA256-hashed CID Issuer_Data concatenated with and [issuerDataCounter]:
         * SHA256([cardId] | [issuerData] | [issuerDataCounter]).
         */
        val issuerDataSignature: ByteArray,

        /**
         * An optional counter that protect issuer data against replay attack.
         * When flag [Settings.ProtectIssuerDataAgainstReplay] set in [SettingsMask]
         * then this value is mandatory and must increase on each execution of [WriteIssuerDataCommand].
         */
        val issuerDataCounter: Int?
) : CommandResponse


/**
 * This command returns 512-byte Issuer Data field and its issuer’s signature.
 * Issuer Data is never changed or parsed from within the Tangem COS. The issuer defines purpose of use,
 * format and payload of Issuer Data. For example, this field may contain information about
 * wallet balance signed by the issuer or additional issuer’s attestation data.
 * @property cardId CID, Unique Tangem card ID number.
 */
class ReadIssuerDataCommand(
        verifier: IssuerDataVerifier = DefaultIssuerDataVerifier()
) : CommandSerializer<ReadIssuerDataResponse>(),
        IssuerDataVerifier by verifier {

    override fun serialize(cardEnvironment: CardEnvironment): CommandApdu {
        val tlvBuilder = TlvBuilder()
        tlvBuilder.append(TlvTag.Pin, cardEnvironment.pin1)
        tlvBuilder.append(TlvTag.CardId, cardEnvironment.cardId)
        tlvBuilder.append(TlvTag.Mode, IssuerDataMode.ReadData)
        return CommandApdu(
                Instruction.ReadIssuerData, tlvBuilder.serialize(),
                cardEnvironment.encryptionMode, cardEnvironment.encryptionKey
        )
    }

    override fun deserialize(cardEnvironment: CardEnvironment, responseApdu: ResponseApdu): ReadIssuerDataResponse? {
        val tlvData = responseApdu.getTlvData(cardEnvironment.encryptionKey) ?: return null

        return try {
            val mapper = TlvMapper(tlvData)
            ReadIssuerDataResponse(
                    cardId = mapper.map(TlvTag.CardId),
                    issuerData = mapper.map(TlvTag.IssuerData),
                    issuerDataSignature = mapper.map(TlvTag.IssuerDataSignature),
                    issuerDataCounter = mapper.mapOptional(TlvTag.IssuerDataCounter)
            )
        } catch (exception: Exception) {
            throw TaskError.SerializeCommandError()
        }
    }
}