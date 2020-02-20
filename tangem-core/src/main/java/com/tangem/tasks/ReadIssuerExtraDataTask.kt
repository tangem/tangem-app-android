package com.tangem.tasks

import com.tangem.commands.*
import com.tangem.common.CardEnvironment
import com.tangem.common.CompletionResult
import java.io.ByteArrayOutputStream

/**
 * This task performs [ReadIssuerExtraDataCommand] repeatedly until
 * the issuer extra data is fully retrieved.
 */
internal class ReadIssuerExtraDataTask(
        private val issuerPublicKey: ByteArray?) : Task<ReadIssuerExtraDataResponse>() {

    private val issuerData = ByteArrayOutputStream()
    private var issuerDataSize = 0
    private lateinit var card: Card
    private lateinit var cardEnvironment: CardEnvironment

    override fun onRun(
            cardEnvironment: CardEnvironment,
            currentCard: Card?,
            callback: (result: TaskEvent<ReadIssuerExtraDataResponse>) -> Unit
    ) {
        card = currentCard!!
        this.cardEnvironment = cardEnvironment
        val command = ReadIssuerExtraDataCommand()
        readIssuerData(command, callback)
    }

    private fun readIssuerData(
            command: ReadIssuerExtraDataCommand,
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
                            completeNfcSession()
                            callback(TaskEvent.Event(result.data))
                            callback(TaskEvent.Completion())
                            return@sendCommand
                        }
                        issuerDataSize = result.data.size
                    }
                    issuerData.write(result.data.issuerData)
                    if (result.data.issuerDataSignature == null) {
                        command.offset = issuerData.size()
                        readIssuerData(command, callback)
                    } else {
                        completeTask(result.data, command, callback)
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

    private fun completeTask(data: ReadIssuerExtraDataResponse, command: ReadIssuerExtraDataCommand,
                             callback: (result: TaskEvent<ReadIssuerExtraDataResponse>) -> Unit) {
        val publicKey = issuerPublicKey ?: card.issuerPublicKey!!
        val dataToVerify = IssuerDataToVerify(
                cardEnvironment.cardId!!,
                issuerData.toByteArray(),
                data.issuerDataCounter
        )
        if (command.verify(publicKey, data.issuerDataSignature!!, dataToVerify)) {
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
}