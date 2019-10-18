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
    class OnUserCancelledEvent() : ScanEvent()
    data class Failure(val error: TaskError) : ScanEvent()
    data class Succcess(val result: Any) : ScanEvent()
}

//
//sealed class ScanResult {
//    data class ReadResult(val card: Card) : ScanResult()
//    data class VerifyResult(val verified: Boolean) : ScanResult()
//    data class Failure(val error: TaskError) : ScanResult()
//}


class ScanTask<out: ScanEvent>(delegate: CardManagerDelegate? = null, reader: CardReader) : Task<ScanEvent>(delegate, reader) {

    private lateinit var readCardData: ReadCardResponse
    private lateinit var challenge: ByteArray
    private lateinit var curve: EllipticCurve
    private lateinit var walletPublickKey: ByteArray

    override fun onRun(cardEnvironment: CardEnvironment,
                       callback: (result: ScanEvent, cardEnvironment: CardEnvironment) -> Unit) {

        val readCommand = ReadCardCommand(cardEnvironment.pin1)
        sendCommand(readCommand, cardEnvironment) {

            if (it is CommandEvent.Success) {
                readCardData = it.response as ReadCardResponse

                callback(ScanEvent.OnReadEvent(readCardData), cardEnvironment)

                if (readCardData.curve != null && readCardData.walletPublicKey != null) {
                    curve = readCardData.curve!!
                    walletPublickKey = readCardData.walletPublicKey!!
                } else {
                    callback(ScanEvent.Failure(TaskError.CardError()), cardEnvironment)
                }

                val checkWalletCommand = prepareCheckWalletCommand(cardEnvironment)

                sendCommand(checkWalletCommand, cardEnvironment) { checkWalletResult ->
                    delegate?.closeNfcPopup()

                    if (checkWalletResult is CommandEvent.Success) {
                        val checkWalletResponse = checkWalletResult.response as CheckWalletResponse
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


                }
            } else {
                //TODO: Handle error
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