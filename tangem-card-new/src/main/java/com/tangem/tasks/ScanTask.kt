package com.tangem.tasks

import com.tangem.CardEnvironment
import com.tangem.CardManagerDelegate
import com.tangem.CardReader
import com.tangem.commands.*
import com.tangem.crypto.generateRandomBytes
import com.tangem.crypto.verify

sealed class ScanEvent {
    data class OnReadEvent(val result: ReadCardResponse) : ScanEvent()
    data class OnVerifyEvent(val verified: Boolean) : ScanEvent()
    object OnUserCancelledEvent : ScanEvent()
    data class Failure(val error: TaskError) : ScanEvent()
}


class ScanTask(delegate: CardManagerDelegate? = null, reader: CardReader) : Task<ScanEvent>(delegate, reader) {

    private lateinit var readCardData: ReadCardResponse
    private lateinit var challenge: ByteArray
    private lateinit var curve: EllipticCurve
    private lateinit var walletPublickKey: ByteArray

    override fun onRun(cardEnvironment: CardEnvironment,
                       callback: (result: ScanEvent, cardEnvironment: CardEnvironment) -> Unit) {

        val readCommand = ReadCardCommand(cardEnvironment.pin1)
        sendCommand(readCommand, cardEnvironment) { readCommandEvent ->

            when (readCommandEvent) {
                is CommandEvent.Success -> {
                    readCardData = readCommandEvent.response as ReadCardResponse

                    callback(ScanEvent.OnReadEvent(readCardData), cardEnvironment)

                    if (readCardData.curve != null && readCardData.walletPublicKey != null) {
                        curve = readCardData.curve!!
                        walletPublickKey = readCardData.walletPublicKey!!
                    } else {
                        delegate?.closeNfcPopup()
                        callback(ScanEvent.Failure(TaskError.CardError()), cardEnvironment)
                    }

                    val checkWalletCommand = prepareCheckWalletCommand(cardEnvironment)

                    sendCommand(checkWalletCommand, cardEnvironment) { checkWalletEvent ->
                        delegate?.closeNfcPopup()

                        when (checkWalletEvent) {
                            is CommandEvent.Success -> {
                                val checkWalletResponse = checkWalletEvent.response as CheckWalletResponse
                                val verified = verify(walletPublickKey,
                                        challenge + checkWalletResponse.salt,
                                        checkWalletResponse.walletSignature,
                                        curve)
                                if (verified) {
                                    callback(ScanEvent.OnVerifyEvent(true), cardEnvironment)
                                } else {
                                    callback(ScanEvent.Failure(TaskError.VefificationFailed()), cardEnvironment)
                                }
                            }
                            is CommandEvent.Failure ->
                                callback(ScanEvent.Failure(checkWalletEvent.taskError), cardEnvironment)
                            is CommandEvent.UserCancellation ->
                                callback(ScanEvent.OnUserCancelledEvent, cardEnvironment)
                        }

                    }
                }
                is CommandEvent.Failure ->
                    callback(ScanEvent.Failure(readCommandEvent.taskError), cardEnvironment)
                is CommandEvent.UserCancellation ->
                    callback(ScanEvent.OnUserCancelledEvent, cardEnvironment)
            }
        }
    }

    private fun updateEnvironment(): CardEnvironment {
        //TODO: set logic of changing CardEnvironment when needed
        return CardEnvironment()
    }

    private fun prepareCheckWalletCommand(cardEnvironment: CardEnvironment): CheckWalletCommand {
        challenge = generateRandomBytes(16)
        return CheckWalletCommand(
                cardEnvironment.pin1,
                readCardData.cardId,
                challenge,
                byteArrayOf())
    }
}