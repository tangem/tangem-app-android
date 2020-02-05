package com.tangem.tasks

import com.tangem.common.CardEnvironment
import com.tangem.commands.Card
import com.tangem.commands.CardStatus
import com.tangem.commands.CheckWalletCommand
import com.tangem.commands.ReadCommand
import com.tangem.common.CompletionResult
import com.tangem.crypto.CryptoUtils

/**
 * Events that [ScanTask] returns on completion of its commands.
 */
sealed class ScanEvent {

    /**
     * Contains data from a Tangem card after successful completion of [ReadCommand].
     */
    data class OnReadEvent(val card: Card) : ScanEvent()

    /**
     * Shows whether the Tangem card was verified on completion of [CheckWalletCommand].
     */
    data class OnVerifyEvent(val isGenuine: Boolean) : ScanEvent()
}

/**
 * Task that allows to read Tangem card and verify its private key.
 *
 * It performs two commands, [ReadCommand] and [CheckWalletCommand], subsequently.
 */
internal class ScanTask : Task<ScanEvent>() {

    override fun onRun(cardEnvironment: CardEnvironment,
                       currentCard: Card?,
                       callback: (result: TaskEvent<ScanEvent>) -> Unit) {

        if (currentCard != null) callback(TaskEvent.Event(ScanEvent.OnReadEvent(currentCard)))

        if (currentCard == null) {
            completeNfcSession(true, TaskError.MissingPreflightRead())
            callback(TaskEvent.Completion(TaskError.MissingPreflightRead()))

        } else if (currentCard.status != CardStatus.Loaded) {
            completeNfcSession()
            callback(TaskEvent.Completion())

        } else if (currentCard.curve == null || currentCard.walletPublicKey == null) {
            completeNfcSession(true, TaskError.CardError())
            callback(TaskEvent.Completion(TaskError.CardError()))

        } else {

            val checkWalletCommand = CheckWalletCommand()

            sendCommand(checkWalletCommand, cardEnvironment) { result ->
                when (result) {
                    is CompletionResult.Failure -> {
                        if (result.error !is TaskError.UserCancelledError) {
                            completeNfcSession(true, result.error)
                        }
                        callback(TaskEvent.Completion(result.error))
                    }
                    is CompletionResult.Success -> {
                        completeNfcSession()
                        val verified = result.data.verify(
                                currentCard.curve,
                                currentCard.walletPublicKey,
                                checkWalletCommand.challenge
                        )
                        callback(TaskEvent.Event(ScanEvent.OnVerifyEvent(verified)))
                        callback(TaskEvent.Completion())
                    }
                }

            }
        }
    }
}