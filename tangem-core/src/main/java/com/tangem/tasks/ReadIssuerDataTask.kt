package com.tangem.tasks

import com.tangem.commands.Card
import com.tangem.commands.ReadIssuerDataCommand
import com.tangem.commands.ReadIssuerDataResponse
import com.tangem.commands.common.IssuerDataToVerify
import com.tangem.common.CardEnvironment
import com.tangem.common.CompletionResult

class ReadIssuerDataTask(private val issuerPublicKey: ByteArray? = null) : Task<ReadIssuerDataResponse>() {

    override fun onRun(
            cardEnvironment: CardEnvironment,
            currentCard: Card?,
            callback: (result: TaskEvent<ReadIssuerDataResponse>) -> Unit) {

        val command = ReadIssuerDataCommand()

        sendCommand(command, cardEnvironment) { result ->
            when (result) {
                is CompletionResult.Success -> {
                    val data = result.data
                    if (data.issuerData.isEmpty()) {
                        completeNfcSession()
                        callback(TaskEvent.Event(result.data))
                        callback(TaskEvent.Completion())
                        return@sendCommand
                    }
                    val publicKey = issuerPublicKey ?: currentCard!!.issuerPublicKey!!
                    val issuerDataToVerify = IssuerDataToVerify(
                            cardEnvironment.cardId!!,
                            data.issuerData,
                            data.issuerDataCounter
                    )
                    if (command.verify(publicKey, data.issuerDataSignature, issuerDataToVerify)) {
                        completeNfcSession()
                        callback(TaskEvent.Event(result.data))
                        callback(TaskEvent.Completion())
                    } else {
                        completeNfcSession(TaskError.VerificationFailed())
                        callback(TaskEvent.Completion(TaskError.VerificationFailed()))
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
}