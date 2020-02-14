package com.tangem.tasks

import com.tangem.commands.*
import com.tangem.common.CardEnvironment
import com.tangem.common.CompletionResult
import com.tangem.common.tlv.TlvEncoder
import com.tangem.common.tlv.TlvTag
import com.tangem.crypto.CryptoUtils
import java.io.ByteArrayOutputStream

/**
 * This task performs [WriteIssuerExtraDataCommand] repeatedly until the issuer extra data is fully
 * written on the card.
 * @param issuerData Data provided by issuer.
 * @param startingSignature Issuer’s signature with Issuer Data Private Key of [cardId],
 * [issuerDataCounter] (if flags Protect_Issuer_Data_Against_Replay and
 * Restrict_Overwrite_Issuer_Extra_Data are set in [SettingsMask]) and size of [issuerData].
 * @param finalizingSignature Issuer’s signature with Issuer Data Private Key of [cardId],
 * [issuerData] and [issuerDataCounter] (the latter one only if flags Protect_Issuer_Data_Against_Replay
 * andRestrict_Overwrite_Issuer_Extra_Data are set in [SettingsMask]).
 * @param issuerDataCounter An optional counter that protect issuer data against replay attack.
 */
internal class WriteIssuerExtraDataTask(
        private val issuerData: ByteArray,
        private val startingSignature: ByteArray,
        private val finalizingSignature: ByteArray,
        private val issuerDataCounter: Int? = null
) : Task<WriteIssuerDataResponse>() {

    private lateinit var card: Card

    override fun onRun(cardEnvironment: CardEnvironment,
                       currentCard: Card?,
                       callback: (result: TaskEvent<WriteIssuerDataResponse>) -> Unit) {

        card = currentCard!!
        if (!isCounterValid(issuerDataCounter)) {
            completeNfcSession(TaskError.MissingCounter())
            callback(TaskEvent.Completion(TaskError.MissingCounter()))
        } else if (!verifySignatures()) {
            completeNfcSession(TaskError.VerificationFailed())
            callback(TaskEvent.Completion(TaskError.VerificationFailed()))
        }
        val command = WriteIssuerExtraDataCommand(
                issuerData, startingSignature, finalizingSignature, issuerDataCounter
        )
        writeIssuerData(command, cardEnvironment, callback)
    }

    private fun writeIssuerData(
            command: WriteIssuerExtraDataCommand,
            cardEnvironment: CardEnvironment,
            callback: (result: TaskEvent<WriteIssuerDataResponse>) -> Unit) {

        if (command.mode == IssuerDataMode.WriteExtraData) {
            delegate?.onDelay(issuerData.size, command.offset, WriteIssuerExtraDataCommand.SINGLE_WRITE_SIZE)
        }
        sendCommand(command, cardEnvironment) { result ->
            when (result) {

                is CompletionResult.Success -> {
                    when (command.mode) {
                        IssuerDataMode.ExtraData -> {
                            command.mode = IssuerDataMode.WriteExtraData
                            writeIssuerData(command, cardEnvironment, callback)
                            return@sendCommand
                        }
                        IssuerDataMode.WriteExtraData -> {
                            command.offset += WriteIssuerExtraDataCommand.SINGLE_WRITE_SIZE
                            if (command.offset >= issuerData.size) {
                                command.mode = IssuerDataMode.FinalizeExtraData
                            }
                            writeIssuerData(command, cardEnvironment, callback)
                            return@sendCommand
                        }
                        IssuerDataMode.FinalizeExtraData -> {
                            completeNfcSession()
                            callback(TaskEvent.Event(result.data))
                            callback(TaskEvent.Completion())
                        }
                    }
                }
                is CompletionResult.Failure -> {
                    if (result.error !is TaskError.UserCancelled) {
                        completeNfcSession(result.error)
                    }
                    callback(TaskEvent.Completion(result.error))
                }
            }
        }
    }

    private fun isCounterValid(issuerDataCounter: Int?): Boolean =
            if (isCounterRequired()) issuerDataCounter != null else true

    private fun isCounterRequired(): Boolean =
            card.settingsMask?.contains(SettingsMask.protectIssuerDataAgainstReplay) != false

    private fun verifySignatures(): Boolean = verifyFirstSignature() && verifyFinalizingSignature()

    private fun verifyFirstSignature(): Boolean {
        val tlvEncoder = TlvEncoder()
        val dataToVerify = ByteArrayOutputStream()
        dataToVerify.write(tlvEncoder.encodeValue(TlvTag.CardId, card.cardId))
        if (isCounterRequired()) {
            dataToVerify.write(tlvEncoder.encodeValue(TlvTag.IssuerDataCounter, issuerDataCounter))
        }
        dataToVerify.write(tlvEncoder.encodeValue(TlvTag.Size, issuerData.size))

        return CryptoUtils.verify(
                card.issuerPublicKey!!, dataToVerify.toByteArray(), startingSignature
        )
    }

    private fun verifyFinalizingSignature(): Boolean {
        val tlvEncoder = TlvEncoder()
        val dataToVerify = ByteArrayOutputStream()
        dataToVerify.write(tlvEncoder.encodeValue(TlvTag.CardId, card.cardId))
        dataToVerify.write(tlvEncoder.encodeValue(TlvTag.IssuerData, issuerData))
        if (isCounterRequired()) {
            dataToVerify.write(tlvEncoder.encodeValue(TlvTag.IssuerDataCounter, issuerDataCounter))
        }
        return CryptoUtils.verify(
                card.issuerPublicKey!!, dataToVerify.toByteArray(), finalizingSignature
        )
    }
}