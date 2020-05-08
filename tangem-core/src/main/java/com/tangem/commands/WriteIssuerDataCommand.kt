package com.tangem.commands

import com.tangem.CardSession
import com.tangem.SessionEnvironment
import com.tangem.TangemSdkError
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
        private val issuerData: ByteArray,
        private val issuerDataSignature: ByteArray,
        private val issuerDataCounter: Int? = null,
        private val issuerPublicKey: ByteArray? = null,
        verifier: IssuerDataVerifier = DefaultIssuerDataVerifier()
) : Command<WriteIssuerDataResponse>(), IssuerDataVerifier by verifier {

    override fun performPreCheck(session: CardSession, callback: (result: CompletionResult<WriteIssuerDataResponse>) -> Unit): Boolean {
        val card = session.environment.card
        if (card == null) {
            callback(CompletionResult.Failure(TangemSdkError.MissingPreflightRead()))
            return true
        }
        val publicKey = issuerPublicKey ?: card.issuerPublicKey
        if (publicKey == null) {
            callback(CompletionResult.Failure(TangemSdkError.MissingIssuerPubicKey()))
            return true
        }
        if (session.environment.card?.status == CardStatus.NotPersonalized) {
            callback(CompletionResult.Failure(TangemSdkError.NotPersonalized()))
            return true
        }
        if (session.environment.card?.isActivated == true) {
            callback(CompletionResult.Failure(TangemSdkError.NotActivated()))
            return true
        }
        if (issuerData.size > MAX_SIZE) {
            callback(CompletionResult.Failure(TangemSdkError.DataSizeTooLarge()))
            return true
        }
        if (!isCounterValid(issuerDataCounter, card)) {
            callback(CompletionResult.Failure(TangemSdkError.MissingCounter()))
            return true
        }
        if (!verifySignature(publicKey, card.cardId)) {
            callback(CompletionResult.Failure(TangemSdkError.VerificationFailed()))
            return true
        }
        return false
    }

    override fun performAfterCheck(session: CardSession,
                                   result: CompletionResult<WriteIssuerDataResponse>,
                                   callback: (result: CompletionResult<WriteIssuerDataResponse>) -> Unit
    ): Boolean {
        when (result) {
            is CompletionResult.Failure -> {
                if (result.error is TangemSdkError.InvalidParams &&
                        isCounterRequired(session.environment.card)) {
                    callback(CompletionResult.Failure(TangemSdkError.DataCannotBeWritten()))
                    return true
                }
                return false
            }
            else -> return false
        }
    }

    private fun isCounterValid(issuerDataCounter: Int?, card: Card): Boolean =
            if (isCounterRequired(card)) issuerDataCounter != null else true

    private fun isCounterRequired(card: Card?): Boolean =
            card?.settingsMask?.contains(Settings.ProtectIssuerDataAgainstReplay) != false

    private fun verifySignature(publicKey: ByteArray, cardId: String): Boolean {
        return verify(
                publicKey,
                issuerDataSignature,
                IssuerDataToVerify(cardId, issuerData, issuerDataCounter)
        )
    }

    override fun serialize(environment: SessionEnvironment): CommandApdu {
        val tlvBuilder = TlvBuilder()
        tlvBuilder.append(TlvTag.Pin, environment.pin1)
        tlvBuilder.append(TlvTag.CardId, environment.card?.cardId)
        tlvBuilder.append(TlvTag.Mode, IssuerDataMode.WriteData)
        tlvBuilder.append(TlvTag.IssuerData, issuerData)
        tlvBuilder.append(TlvTag.IssuerDataSignature, issuerDataSignature)
        tlvBuilder.append(TlvTag.IssuerDataCounter, issuerDataCounter)

        return CommandApdu(
                Instruction.WriteIssuerData, tlvBuilder.serialize(),
                environment.encryptionMode, environment.encryptionKey
        )
    }

    override fun deserialize(environment: SessionEnvironment, apdu: ResponseApdu): WriteIssuerDataResponse {
        val tlvData = apdu.getTlvData(environment.encryptionKey)
                ?: throw TangemSdkError.DeserializeApduFailed()

        val decoder = TlvDecoder(tlvData)
        return WriteIssuerDataResponse(
                cardId = decoder.decode(TlvTag.CardId)
        )
    }

    companion object {
        const val MAX_SIZE = 512
    }
}