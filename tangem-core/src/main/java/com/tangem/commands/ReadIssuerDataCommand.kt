package com.tangem.commands

import com.tangem.CardSession
import com.tangem.SessionEnvironment
import com.tangem.SessionError
import com.tangem.commands.common.DefaultIssuerDataVerifier
import com.tangem.commands.common.IssuerDataMode
import com.tangem.commands.common.IssuerDataToVerify
import com.tangem.commands.common.IssuerDataVerifier
import com.tangem.common.CompletionResult
import com.tangem.common.apdu.CommandApdu
import com.tangem.common.apdu.Instruction
import com.tangem.common.apdu.ResponseApdu
import com.tangem.common.tlv.TlvBuilder
import com.tangem.common.tlv.TlvDecoder
import com.tangem.common.tlv.TlvTag

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
        val issuerPublicKey: ByteArray? = null,
        verifier: IssuerDataVerifier = DefaultIssuerDataVerifier()
) : Command<ReadIssuerDataResponse>(), IssuerDataVerifier by verifier {

    override fun run(session: CardSession, callback: (result: CompletionResult<ReadIssuerDataResponse>) -> Unit) {
        val card = session.environment.card
        if (card == null) {
            callback(CompletionResult.Failure(SessionError.MissingPreflightRead()))
            return
        }
        val publicKey = issuerPublicKey ?: card.issuerPublicKey
        if (publicKey == null) {
            callback(CompletionResult.Failure(SessionError.MissingIssuerPubicKey()))
            return
        }
        super.run(session) { result ->
            when (result) {
                is CompletionResult.Failure -> callback(result)
                is CompletionResult.Success -> {
                    if (result.data.issuerData.isEmpty()) {
                        callback(result)
                        return@run
                    }
                    val issuerDataToVerify = IssuerDataToVerify(
                            card.cardId, result.data.issuerData, result.data.issuerDataCounter
                    )
                    if (verify(publicKey, result.data.issuerDataSignature, issuerDataToVerify)) {
                        callback(result)
                    } else {
                        callback(CompletionResult.Failure(SessionError.VerificationFailed()))
                    }
                }
            }
        }
    }

    override fun serialize(environment: SessionEnvironment): CommandApdu {
        val tlvBuilder = TlvBuilder()
        tlvBuilder.append(TlvTag.Pin, environment.pin1)
        tlvBuilder.append(TlvTag.CardId, environment.card?.cardId)
        tlvBuilder.append(TlvTag.Mode, IssuerDataMode.ReadData)
        return CommandApdu(
                Instruction.ReadIssuerData, tlvBuilder.serialize(),
                environment.encryptionMode, environment.encryptionKey
        )
    }

    override fun deserialize(environment: SessionEnvironment, apdu: ResponseApdu): ReadIssuerDataResponse {
        val tlvData = apdu.getTlvData(environment.encryptionKey)
                ?: throw SessionError.DeserializeApduFailed()

        val decoder = TlvDecoder(tlvData)
        return ReadIssuerDataResponse(
                cardId = decoder.decode(TlvTag.CardId),
                issuerData = decoder.decode(TlvTag.IssuerData),
                issuerDataSignature = decoder.decode(TlvTag.IssuerDataSignature),
                issuerDataCounter = decoder.decodeOptional(TlvTag.IssuerDataCounter)
        )
    }
}