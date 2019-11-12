package com.tangem.tasks

import com.tangem.CardEnvironment
import com.tangem.commands.Card
import com.tangem.commands.CheckWalletCommand
import com.tangem.commands.EllipticCurve
import com.tangem.commands.ReadCardCommand
import com.tangem.common.CompletionResult
import com.tangem.crypto.generateRandomBytes
import com.tangem.crypto.verify

sealed class ScanEvent {
    data class OnReadEvent(val card: Card) : ScanEvent()
    data class OnVerifyEvent(val isGenuine: Boolean) : ScanEvent()
}


internal class ScanTask : Task<ScanEvent>() {

    private lateinit var cardData: Card
    private lateinit var challenge: ByteArray
    private lateinit var curve: EllipticCurve
    private lateinit var walletPublickKey: ByteArray

    override fun onRun(cardEnvironment: CardEnvironment,
                       callback: (result: TaskEvent<ScanEvent>) -> Unit) {

        val readCommand = ReadCardCommand()
        sendCommand(readCommand, cardEnvironment) { readEvent ->

            when (readEvent) {
                is CompletionResult.Success -> {
                    cardData = readEvent.data

                    callback(TaskEvent.Event(ScanEvent.OnReadEvent(cardData)))

                    if (cardData.curve != null && cardData.walletPublicKey != null) {
                        curve = cardData.curve!!
                        walletPublickKey = cardData.walletPublicKey!!
                    } else {
                        onTaskCompleted(true)
                        callback(TaskEvent.Completion(TaskError.CardError()))
                    }

                    val checkWalletCommand = prepareCheckWalletCommand(cardEnvironment)

                    sendCommand(checkWalletCommand, cardEnvironment) { checkWalletEvent ->
                        when (checkWalletEvent) {
                            is CompletionResult.Success -> {
                                val checkWalletResponse = checkWalletEvent.data
                                val verified = verify(walletPublickKey,
                                        challenge + checkWalletResponse.salt,
                                        checkWalletResponse.walletSignature,
                                        curve)
                                if (verified) {
                                    onTaskCompleted()
                                    callback(TaskEvent.Completion())
                                    callback(TaskEvent.Event(ScanEvent.OnVerifyEvent(true)))
                                } else {
                                    onTaskCompleted(true)
                                    callback(TaskEvent.Completion(TaskError.VefificationFailed()))
                                }
                            }
                            is CompletionResult.Failure -> {
                                if (checkWalletEvent.error !is TaskError.UserCancelledError) {
                                    onTaskCompleted(true, checkWalletEvent.error)
                                }
                                callback(TaskEvent.Completion(checkWalletEvent.error))
                            }
                        }

                    }
                }
                is CompletionResult.Failure -> {
                    if (readEvent.error !is TaskError.UserCancelledError) {
                        onTaskCompleted(true, readEvent.error)
                    }
                    callback(TaskEvent.Completion(readEvent.error))
                }
            }
        }
    }

    private fun prepareCheckWalletCommand(cardEnvironment: CardEnvironment): CheckWalletCommand {
        challenge = generateRandomBytes(16)
        return CheckWalletCommand(
                cardEnvironment.pin1,
                cardData.cardId,
                challenge,
                byteArrayOf())
    }
}