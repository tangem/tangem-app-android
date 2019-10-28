package com.tangem.tasks

import com.tangem.CardEnvironment
import com.tangem.commands.CheckWalletCommand
import com.tangem.commands.EllipticCurve
import com.tangem.commands.ReadCardCommand
import com.tangem.commands.ReadCardResponse
import com.tangem.crypto.generateRandomBytes
import com.tangem.crypto.verify

sealed class ScanEvent {
    data class OnReadEvent(val result: ReadCardResponse) : ScanEvent()
    data class OnVerifyEvent(val verified: Boolean) : ScanEvent()
}


internal class ScanTask : Task<ScanEvent>() {

    private lateinit var readCardData: ReadCardResponse
    private lateinit var challenge: ByteArray
    private lateinit var curve: EllipticCurve
    private lateinit var walletPublickKey: ByteArray

    override fun onRun(cardEnvironment: CardEnvironment,
                       callback: (result: TaskEvent<ScanEvent>) -> Unit) {

        val readCommand = ReadCardCommand()
        sendCommand(readCommand, cardEnvironment) { readEvent ->

            when (readEvent) {
                is TaskEvent.Event -> {
                    readCardData = readEvent.data

                    callback(TaskEvent.Event(ScanEvent.OnReadEvent(readCardData)))

                    if (readCardData.curve != null && readCardData.walletPublicKey != null) {
                        curve = readCardData.curve!!
                        walletPublickKey = readCardData.walletPublicKey!!
                    } else {
                        delegate?.closeNfcPopup()
                        callback(TaskEvent.Completion(TaskError.CardError()))
                    }

                    val checkWalletCommand = prepareCheckWalletCommand(cardEnvironment)

                    sendCommand(checkWalletCommand, cardEnvironment) { checkWalletEvent ->
                        delegate?.closeNfcPopup()

                        when (checkWalletEvent) {
                            is TaskEvent.Event -> {
                                val checkWalletResponse = checkWalletEvent.data
                                val verified = verify(walletPublickKey,
                                        challenge + checkWalletResponse.salt,
                                        checkWalletResponse.walletSignature,
                                        curve)
                                if (verified) {
                                    callback(TaskEvent.Event(ScanEvent.OnVerifyEvent(true)))
                                    callback(TaskEvent.Completion())
                                } else {
                                    callback(TaskEvent.Completion(TaskError.VefificationFailed()))
                                }
                            }
                            is TaskEvent.Completion ->
                                if (checkWalletEvent.error != null)
                                    callback(TaskEvent.Completion(checkWalletEvent.error))
                        }

                    }
                }
                is TaskEvent.Completion ->
                    if (readEvent.error != null) callback(TaskEvent.Completion(readEvent.error))
            }
        }
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