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
import java.io.ByteArrayOutputStream

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
         * When flag [Settings.ProtectIssuerDataAgainstReplay] set in [SettingsMask] then signature of
         * SHA256-hashed CID Issuer_Data concatenated with and [issuerDataCounter]:
         * SHA256([cardId] | [issuerData] | [issuerDataCounter]).
         */
        val issuerDataSignature: ByteArray?,

        /**
         * An optional counter that protects issuer data against replay attack.
         * When flag [Settings.ProtectIssuerDataAgainstReplay] set in [SettingsMask]
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
        private val issuerPublicKey: ByteArray? = null,
        verifier: IssuerDataVerifier = DefaultIssuerDataVerifier()
) : Command<ReadIssuerExtraDataResponse>(), IssuerDataVerifier by verifier {

    private val issuerData = ByteArrayOutputStream()
    private var offset: Int = 0
    private var issuerDataSize: Int = 0

    override fun run(session: CardSession, callback: (result: CompletionResult<ReadIssuerExtraDataResponse>) -> Unit) {
        val card = session.environment.card
        if (card == null) {
            callback(CompletionResult.Failure(TangemSdkError.MissingPreflightRead()))
            return
        }
        val publicKey = issuerPublicKey ?: card.issuerPublicKey
        if (publicKey == null) {
            callback(CompletionResult.Failure(TangemSdkError.MissingIssuerPubicKey()))
            return
        }
        if (session.environment.card?.status == CardStatus.NotPersonalized) {
            callback(CompletionResult.Failure(TangemSdkError.NotPersonalized()))
            return
        }

        readIssuerData(session, card.cardId, publicKey, callback)
    }


    private fun readIssuerData(
            session: CardSession,
            cardId: String, publicKey: ByteArray,
            callback: (result: CompletionResult<ReadIssuerExtraDataResponse>) -> Unit) {

        if (issuerDataSize != 0) {
            session.viewDelegate.onDelay(
                    issuerDataSize, offset, WriteIssuerExtraDataCommand.SINGLE_WRITE_SIZE
            )
        }

        transceive(session) { result ->
            when (result) {
                is CompletionResult.Success -> {
                    if (result.data.size != null) {
                        if (result.data.size == 0) {
                            callback(CompletionResult.Success(result.data))
                            return@transceive
                        }
                        issuerDataSize = result.data.size
                    }
                    issuerData.write(result.data.issuerData)
                    if (result.data.issuerDataSignature == null) {
                        offset = issuerData.size()
                        readIssuerData(session, cardId, publicKey, callback)
                    } else {
                        completeTask(result.data, cardId, publicKey, callback)
                    }
                }
                is CompletionResult.Failure -> {
                    callback(CompletionResult.Failure(result.error))
                }
            }
        }
    }

    private fun completeTask(data: ReadIssuerExtraDataResponse,
                             cardId: String, publicKey: ByteArray,
                             callback: (result: CompletionResult<ReadIssuerExtraDataResponse>) -> Unit) {
        val dataToVerify = IssuerDataToVerify(
                cardId,
                issuerData.toByteArray(),
                data.issuerDataCounter
        )
        if (verify(publicKey, data.issuerDataSignature!!, dataToVerify)) {
            val finalResult = ReadIssuerExtraDataResponse(
                    data.cardId,
                    issuerDataSize,
                    issuerData.toByteArray(),
                    data.issuerDataSignature,
                    data.issuerDataCounter
            )
            callback(CompletionResult.Success(finalResult))
        } else {
            callback(CompletionResult.Failure(TangemSdkError.VerificationFailed()))
        }
    }

    override fun serialize(environment: SessionEnvironment): CommandApdu {
        val tlvBuilder = TlvBuilder()
        tlvBuilder.append(TlvTag.Pin, environment.pin1)
        tlvBuilder.append(TlvTag.CardId, environment.card?.cardId)
        tlvBuilder.append(TlvTag.Mode, IssuerDataMode.ReadExtraData)
        tlvBuilder.append(TlvTag.Offset, offset)
        return CommandApdu(
                Instruction.ReadIssuerData, tlvBuilder.serialize(),
                environment.encryptionMode, environment.encryptionKey
        )
    }

    override fun deserialize(environment: SessionEnvironment, apdu: ResponseApdu): ReadIssuerExtraDataResponse {
        val tlvData = apdu.getTlvData(environment.encryptionKey)
                ?: throw TangemSdkError.DeserializeApduFailed()


        val decoder = TlvDecoder(tlvData)
        return ReadIssuerExtraDataResponse(
                cardId = decoder.decode(TlvTag.CardId),
                size = decoder.decodeOptional(TlvTag.Size),
                issuerData = decoder.decodeOptional(TlvTag.IssuerData) ?: byteArrayOf(),
                issuerDataSignature = decoder.decodeOptional(TlvTag.IssuerDataSignature),
                issuerDataCounter = decoder.decodeOptional(TlvTag.IssuerDataCounter)
        )
    }

    companion object {
        /**
         * This mode value specifies that this command retrieves Issuer EXTRA data from the card
         * (with value 0 the command will get instead simple Issuer Data from the card).
         */
        const val EXTRA_DATA_MODE = 1
    }
}