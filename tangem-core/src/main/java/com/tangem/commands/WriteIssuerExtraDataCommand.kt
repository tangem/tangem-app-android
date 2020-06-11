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

/**
 * This command writes Issuer Extra Data field and its issuer’s signature.
 * Issuer Extra Data is never changed or parsed from within the Tangem COS.
 * The issuer defines purpose of use, format and payload of Issuer Data.
 * For example, this field may contain a photo or biometric information for ID card products.
 * Because of the large size of Issuer_Extra_Data, a series of these commands have to be executed
 * to write entire Issuer_Extra_Data.
 * @param issuerData Data provided by issuer.
 * @param startingSignature Issuer’s signature with Issuer Data Private Key of [cardId],
 * [issuerDataCounter] (if flags Protect_Issuer_Data_Against_Replay and
 * Restrict_Overwrite_Issuer_Extra_Data are set in [SettingsMask]) and size of [issuerData].
 * @param finalizingSignature Issuer’s signature with Issuer Data Private Key of [cardId],
 * [issuerData] and [issuerDataCounter] (the latter one only if flags Protect_Issuer_Data_Against_Replay
 * andRestrict_Overwrite_Issuer_Extra_Data are set in [SettingsMask]).
 * @param issuerDataCounter An optional counter that protect issuer data against replay attack.
 */
class WriteIssuerExtraDataCommand(
    private val issuerData: ByteArray,
    private val startingSignature: ByteArray,
    private val finalizingSignature: ByteArray,
    private val issuerDataCounter: Int? = null,
    private val issuerPublicKey: ByteArray? = null,
    verifier: IssuerDataVerifier = DefaultIssuerDataVerifier()
) : Command<WriteIssuerDataResponse>(), IssuerDataVerifier by verifier {

    var mode: IssuerDataMode = IssuerDataMode.InitializeWritingExtraData
    var offset: Int = 0

    override fun run(
        session: CardSession,
        callback: (result: CompletionResult<WriteIssuerDataResponse>) -> Unit
    ) {
        writeIssuerData(session, callback)
    }

    override fun performPreCheck(card: Card): TangemSdkError? {
        val publicKey = issuerPublicKey ?: card.issuerPublicKey
        ?: return TangemSdkError.MissingIssuerPubicKey()

        if (card.status == CardStatus.NotPersonalized) {
            return TangemSdkError.NotPersonalized()
        }
        if (card.isActivated) {
            return TangemSdkError.NotActivated()
        }
        if (issuerData.size > MAX_SIZE) {
            return TangemSdkError.DataSizeTooLarge()
        }
        if (!isCounterValid(issuerDataCounter, card)) {
            return TangemSdkError.MissingCounter()
        }
        if (!verifySignatures(publicKey, card.cardId)) {
            return TangemSdkError.VerificationFailed()
        }
        return null
    }

    override fun mapError(card: Card?, error: TangemSdkError): TangemSdkError {
        if (error is TangemSdkError.InvalidParams && isCounterRequired(card)) {
            return TangemSdkError.DataCannotBeWritten()
        }
        if (error is TangemSdkError.InvalidState &&
            card?.settingsMask?.contains(Settings.ProtectIssuerDataAgainstReplay) == true) {
            return TangemSdkError.OverwritingDataIsProhibited()
        }
        return error
    }

    private fun isCounterValid(issuerDataCounter: Int?, card: Card): Boolean =
        if (isCounterRequired(card)) issuerDataCounter != null else true

    private fun isCounterRequired(card: Card?): Boolean =
        card?.settingsMask?.contains(Settings.ProtectIssuerDataAgainstReplay) == true

    private fun verifySignatures(publicKey: ByteArray, cardId: String): Boolean {

        val firstData = IssuerDataToVerify(cardId, null, issuerDataCounter, issuerData.size)
        val secondData = IssuerDataToVerify(cardId, issuerData, issuerDataCounter)

        return verify(publicKey, startingSignature, firstData) &&
                verify(publicKey, finalizingSignature, secondData)
    }

    private fun writeIssuerData(
        session: CardSession,
        callback: (result: CompletionResult<WriteIssuerDataResponse>) -> Unit
    ) {

        if (mode == IssuerDataMode.WriteExtraData) {
            session.viewDelegate.onDelay(
                issuerData.size,
                offset,
                SINGLE_WRITE_SIZE
            )
        }
        transceive(session) { result ->
            when (result) {
                is CompletionResult.Success -> {
                    when (mode) {
                        IssuerDataMode.InitializeWritingExtraData -> {
                            mode = IssuerDataMode.WriteExtraData
                            writeIssuerData(session, callback)
                            return@transceive
                        }
                        IssuerDataMode.WriteExtraData -> {
                            offset += WriteIssuerExtraDataCommand.SINGLE_WRITE_SIZE
                            if (offset >= issuerData.size) {
                                mode = IssuerDataMode.FinalizeExtraData
                            }
                            writeIssuerData(session, callback)
                            return@transceive
                        }
                        IssuerDataMode.FinalizeExtraData -> {
                            callback(CompletionResult.Success(result.data))
                        }
                    }
                }
                is CompletionResult.Failure -> {
                    if (session.environment.handleErrors) {
                        mapError(session.environment.card, result.error)?.let {
                            callback(CompletionResult.Failure(it))
                        }
                    }
                    callback(CompletionResult.Failure(result.error))
                }
            }
        }


    }

    override fun serialize(environment: SessionEnvironment): CommandApdu {
        val tlvBuilder = TlvBuilder()

        tlvBuilder.append(TlvTag.Pin, environment.pin1)
        tlvBuilder.append(TlvTag.CardId, environment.card?.cardId)
        tlvBuilder.append(TlvTag.Mode, mode)

        when (mode) {
            IssuerDataMode.InitializeWritingExtraData -> {
                tlvBuilder.append(TlvTag.Size, issuerData.size)
                tlvBuilder.append(TlvTag.IssuerDataSignature, startingSignature)
                tlvBuilder.append(TlvTag.IssuerDataCounter, issuerDataCounter)
            }
            IssuerDataMode.WriteExtraData -> {
                tlvBuilder.append(TlvTag.IssuerData, getDataToWrite())
                tlvBuilder.append(TlvTag.Offset, offset)
            }
            IssuerDataMode.FinalizeExtraData -> {
                tlvBuilder.append(TlvTag.IssuerDataSignature, finalizingSignature)
            }
        }
        return CommandApdu(Instruction.WriteIssuerData, tlvBuilder.serialize())
    }

    private fun getDataToWrite(): ByteArray =
        issuerData.copyOfRange(offset, offset + calculatePartSize())

    private fun calculatePartSize(): Int {
        val bytesLeft = issuerData.size - offset
        return if (bytesLeft < SINGLE_WRITE_SIZE) bytesLeft else SINGLE_WRITE_SIZE
    }

    override fun deserialize(
        environment: SessionEnvironment,
        apdu: ResponseApdu
    ): WriteIssuerDataResponse {
        val tlvData = apdu.getTlvData() ?: throw TangemSdkError.DeserializeApduFailed()

        return WriteIssuerDataResponse(
            cardId = TlvDecoder(tlvData).decode(TlvTag.CardId)
        )
    }

    companion object {
        const val SINGLE_WRITE_SIZE = 1524
        const val MAX_SIZE = 32 * 1024
    }
}