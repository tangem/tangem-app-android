package com.tangem.tasks

import com.tangem.commands.*
import com.tangem.common.CardEnvironment
import com.tangem.common.CompletionResult
import com.tangem.common.tlv.TlvEncoder
import com.tangem.common.tlv.TlvTag
import com.tangem.crypto.CryptoUtils
import java.io.ByteArrayOutputStream

/**
 * This task performs [ReadIssuerExtraDataCommand] repeatedly until
 * the issuer extra data is fully retrieved.
 */
internal class ReadIssuerExtraDataTask : Task<ReadIssuerExtraDataResponse>() {

    private val issuerData = ByteArrayOutputStream()
    private var issuerDataSize = 0
    private lateinit var card: Card

    override fun onRun(
            cardEnvironment: CardEnvironment,
            currentCard: Card?,
            callback: (result: TaskEvent<ReadIssuerExtraDataResponse>) -> Unit
    ) {
        card = currentCard!!
        val command = ReadIssuerExtraDataCommand()
        readIssuerData(command, cardEnvironment, callback)
    }

    private fun readIssuerData(
            command: ReadIssuerExtraDataCommand,
            cardEnvironment: CardEnvironment,
            callback: (result: TaskEvent<ReadIssuerExtraDataResponse>) -> Unit) {

        if (issuerDataSize != 0) {
            delegate?.onDelay(
                    issuerDataSize, command.offset, WriteIssuerExtraDataCommand.SINGLE_WRITE_SIZE
            )
        }

        sendCommand(command, cardEnvironment) { result ->
            when (result) {

                is CompletionResult.Success -> {
                    if (result.data.size != null) {
                        if (result.data.size == 0) {
                            completeNfcSession(TaskError.NoData())
                            callback(TaskEvent.Completion(TaskError.NoData()))
                            return@sendCommand
                        }
                        issuerDataSize = result.data.size
                    }
                    issuerData.write(result.data.issuerData)
                    if (result.data.issuerDataSignature == null) {
                        command.offset = issuerData.size()
                        readIssuerData(command, cardEnvironment, callback)
                    } else {
                        completeTask(result.data, callback)
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

    private fun completeTask(data: ReadIssuerExtraDataResponse,
                             callback: (result: TaskEvent<ReadIssuerExtraDataResponse>) -> Unit) {
        if (isIssuerDataValid(data.issuerDataCounter, data.issuerDataSignature!!)) {
            completeNfcSession()
            val finalResult = ReadIssuerExtraDataResponse(
                    data.cardId,
                    issuerDataSize,
                    issuerData.toByteArray(),
                    data.issuerDataSignature,
                    data.issuerDataCounter
            )
            callback(TaskEvent.Event(finalResult))
            callback(TaskEvent.Completion())
        } else {
            completeNfcSession(TaskError.VerificationFailed())
            callback(TaskEvent.Completion(TaskError.VerificationFailed()))
        }
    }

    private fun isIssuerDataValid(issuerDataCounter: Int?, signature: ByteArray): Boolean {
        val tlvEncoder = TlvEncoder()
        val dataToVerify = ByteArrayOutputStream()
        dataToVerify.write(tlvEncoder.encodeValue(TlvTag.CardId, card.cardId))
        dataToVerify.write(issuerData.toByteArray())
        if (counterIncludedInSignature(issuerDataCounter)) {
            dataToVerify.write(tlvEncoder.encodeValue(TlvTag.IssuerDataCounter, issuerDataCounter))
        }
        return CryptoUtils.verify(card.issuerPublicKey!!, dataToVerify.toByteArray(), signature)
    }

    private fun counterIncludedInSignature(issuerDataCounter: Int?): Boolean {
        return issuerDataCounter != null &&
                card.settingsMask?.contains(SettingsMask.protectIssuerDataAgainstReplay) != false
    }

}