package com.tangem.tasks

import com.tangem.commands.Card
import com.tangem.commands.SettingsMask
import com.tangem.commands.WriteIssuerDataCommand
import com.tangem.commands.WriteIssuerDataResponse
import com.tangem.commands.common.IssuerDataToVerify
import com.tangem.common.CardEnvironment
import com.tangem.common.CompletionResult

class WriteIssuerDataTask(
        private val issuerData: ByteArray,
        private val issuerDataSignature: ByteArray,
        private val issuerDataCounter: Int? = null,
        private val issuerPublicKey: ByteArray? = null
) : Task<WriteIssuerDataResponse>() {

    private lateinit var card: Card

    override fun onRun(
            cardEnvironment: CardEnvironment,
            currentCard: Card?,
            callback: (result: TaskEvent<WriteIssuerDataResponse>) -> Unit) {

        card = currentCard!!
        val command = WriteIssuerDataCommand(
                issuerData, issuerDataSignature, issuerDataCounter
        )
        if (!isCounterValid(issuerDataCounter)) {
            completeNfcSession(TaskError.MissingCounter())
            callback(TaskEvent.Completion(TaskError.MissingCounter()))
        } else if (!verifySignature(command, cardEnvironment.cardId!!)) {
            completeNfcSession(TaskError.VerificationFailed())
            callback(TaskEvent.Completion(TaskError.VerificationFailed()))
        }

        sendCommand(command, cardEnvironment) { result ->
            when (result) {
                is CompletionResult.Success -> {
                    completeNfcSession()
                    callback(TaskEvent.Event(result.data))
                    callback(TaskEvent.Completion())
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

    private fun verifySignature(command: WriteIssuerDataCommand, cardId: String): Boolean {
        return command.verify(
                issuerPublicKey ?: card.issuerPublicKey!!,
                issuerDataSignature,
                IssuerDataToVerify(cardId, issuerData, issuerDataCounter)
        )
    }
}