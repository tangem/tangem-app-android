package com.tangem.tasks

import com.tangem.CardEnvironment
import com.tangem.commands.Card
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
                       callback: (result: TaskEvent<ScanEvent>) -> Unit) {

        val readCommand = ReadCommand()
        sendCommand(readCommand, cardEnvironment) { readResult ->

            when (readResult) {

                is CompletionResult.Failure -> {
                    if (readResult.error !is TaskError.UserCancelledError) {
                        completeNfcSession(true, readResult.error)
                    }
                    callback(TaskEvent.Completion(readResult.error))
                }

                is CompletionResult.Success -> {
                    val card = readResult.data

                    callback(TaskEvent.Event(ScanEvent.OnReadEvent(card)))

                    if (card.curve == null || card.walletPublicKey == null) {
                        completeNfcSession(true)
                        callback(TaskEvent.Completion(TaskError.CardError()))
                        return@sendCommand
                    }

                    val challenge = CryptoUtils.generateRandomBytes(16)
                    val checkWalletCommand = CheckWalletCommand(
                            cardEnvironment.pin1,
                            card.cardId,
                            challenge)

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
                                val checkWalletResponse = result.data
                                val verified = CryptoUtils.verify(
                                        card.walletPublicKey,
                                        challenge + checkWalletResponse.salt,
                                        checkWalletResponse.walletSignature,
                                        card.curve)
                                if (verified) {
                                    callback(TaskEvent.Event(ScanEvent.OnVerifyEvent(true)))
                                    callback(TaskEvent.Completion())
                                } else {
                                    callback(TaskEvent.Completion(TaskError.VefificationFailed()))
                                }
                            }
                        }

                    }
                }
            }
        }
    }
}