package com.tangem.commands

import com.tangem.*
import com.tangem.common.CompletionResult
import com.tangem.common.apdu.*
import com.tangem.common.extensions.toInt
import com.tangem.common.tlv.TlvTag


interface ApduSerializable<T : CommandResponse> {
    /**
     * Serializes data into an array of [com.tangem.common.tlv.Tlv],
     * then creates [CommandApdu] with this data.
     * @param environment [SessionEnvironment] of the current card
     * @return command data converted to [CommandApdu] that allows to convert it to [ByteArray]
     * that can be sent to a Tangem card
     */
    fun serialize(environment: SessionEnvironment): CommandApdu

    /**
     * Deserializes data received from a card and stored in [ResponseApdu]
     * into an array of [com.tangem.common.tlv.Tlv]. Then maps it into a [CommandResponse].
     * @param environment [SessionEnvironment] of the current card.
     * @param apdu received data.
     * @return Card response converted to a [CommandResponse] of a type [T]
     */
    fun deserialize(environment: SessionEnvironment, apdu: ResponseApdu): T
}

/**
 * Basic interface for a parsed response from [Command].
 */
interface CommandResponse

/**
 * Basic class for Tangem card commands
 */
abstract class Command<T : CommandResponse> : ApduSerializable<T>, CardSessionRunnable<T> {

    override val performPreflightRead: Boolean = true
    open val requiresPin2: Boolean = false

    override fun run(session: CardSession, callback: (result: CompletionResult<T>) -> Unit) {
        Log.i("Command", "Initializing ${this::class.java.simpleName}")
        transceive(session, callback)
    }

    open fun performPreCheck(card: Card): TangemSdkError? = null

    open fun mapError(card: Card?, error: TangemSdkError): TangemSdkError = error

    fun transceive(session: CardSession, callback: (result: CompletionResult<T>) -> Unit) {

        val card = session.environment.card
        if (session.environment.handleErrors && card != null) {
            performPreCheck(card)?.let { error ->
                callback(CompletionResult.Failure(error))
                return
            }
        }

        if (requiresPin2 && session.environment.isCurrentPin2Default()) {
            handlePin2(session, callback)
            return
        }

        val apdu = serialize(session.environment)
        transceiveApdu(apdu, session) { result ->
            when (result) {
                is CompletionResult.Failure -> {
                    if (session.environment.handleErrors) {
                        val error = mapError(session.environment.card, result.error)
                        if (error is TangemSdkError.Pin1Required) {
                            handlePin1(session, callback)
                            return@transceiveApdu
                        }
                        callback(CompletionResult.Failure(error))
                        return@transceiveApdu

                    }
                    callback(CompletionResult.Failure(result.error))
                }
                is CompletionResult.Success -> {
                    try {
                        val response = deserialize(session.environment, result.data)
                        callback(CompletionResult.Success(response))
                    } catch (error: TangemSdkError) {
                        callback(CompletionResult.Failure(error))
                    }
                }
            }
        }
    }

    private fun transceiveApdu(
        apdu: CommandApdu,
        session: CardSession,
        callback: (result: CompletionResult<ResponseApdu>) -> Unit
    ) {
        Log.i(this::class.simpleName!!, "transieve: ${Instruction.byCode(apdu.ins)}")

        session.send(apdu) { result ->
            when (result) {
                is CompletionResult.Success -> {
                    val responseApdu = result.data

                    when (responseApdu.statusWord) {
                        StatusWord.ProcessCompleted,
                        StatusWord.Pin1Changed, StatusWord.Pin2Changed, StatusWord.Pins12Changed,
                        StatusWord.Pin3Changed, StatusWord.Pins13Changed, StatusWord.Pins23Changed,
                        StatusWord.Pins123Changed -> {
                            callback(CompletionResult.Success(responseApdu))
                        }
                        StatusWord.NeedPause -> {
                            // NeedPause is returned from the card whenever security delay is triggered.
                            val remainingTime =
                                deserializeSecurityDelay(responseApdu)
                            if (remainingTime != null) {
                                session.viewDelegate.onSecurityDelay(
                                    remainingTime,
                                    session.environment.card?.pauseBeforePin2 ?: 0
                                )
                            }
                            Log.i(
                                this::class.simpleName!!,
                                "Nfc command ${this::class.simpleName!!} " +
                                        "triggered security delay of $remainingTime milliseconds"
                            )
                            transceiveApdu(apdu, session, callback)
                        }
                        StatusWord.NeedEncryption -> {
                            Log.i(this::class.simpleName!!, "Establishing encryption")
                            when (session.environment.encryptionMode) {
                                EncryptionMode.NONE -> {
                                    session.environment.encryptionKey = null
                                    session.environment.encryptionMode = EncryptionMode.FAST
                                }
                                EncryptionMode.FAST -> {
                                    session.environment.encryptionKey = null
                                    session.environment.encryptionMode = EncryptionMode.STRONG
                                }
                                EncryptionMode.STRONG -> {
                                    Log.e(this::class.simpleName!!, "Encryption doesn't work")
                                    callback(CompletionResult.Failure(TangemSdkError.NeedEncryption()))
                                    return@send
                                }
                            }
                            transceiveApdu(apdu, session, callback)
                        }
                        else -> {
                            val error = responseApdu.statusWord.toTangemSdkError()
                            if (error != null && !tryHandleError(error)) {
                                callback(CompletionResult.Failure(error))
                            } else {
                                callback(CompletionResult.Failure(TangemSdkError.UnknownError()))
                            }
                        }
                    }
                }
                is CompletionResult.Failure ->
                    if (result.error is TangemSdkError.TagLost) {
                        session.viewDelegate.onTagLost()
                    } else {
                        callback(CompletionResult.Failure(result.error))
                    }
            }
        }
    }

    /**
     * Helper method to parse security delay information received from a card.
     *
     * @return Remaining security delay in milliseconds.
     */
    private fun deserializeSecurityDelay(
        responseApdu: ResponseApdu
    ): Int? {
        val tlv = responseApdu.getTlvData()
        return tlv?.find { it.tag == TlvTag.Pause }?.value?.toInt()
    }

    private fun tryHandleError(error: TangemSdkError): Boolean {
        return false
    }

    private fun handlePin1(
        session: CardSession,
        callback: (result: CompletionResult<T>) -> Unit
    ) {
        if (!session.environment.isCurrentPin1Default()) {
            session.environment.setPin1(SessionEnvironment.DEFAULT_PIN)
            transceive(session, callback)
            return
        }
        session.pause()
        session.viewDelegate.onPinRequested { pin1 ->
            if (!pin1.isNullOrEmpty()) {
                session.environment.setPin1(pin1)
                session.resume()
                transceive(session, callback)
            } else {
                session.environment.setPin1(SessionEnvironment.DEFAULT_PIN)
                session.resume()
                transceive(session, callback)
            }
        }
    }

    private fun handlePin2(
        session: CardSession,
        callback: (result: CompletionResult<T>) -> Unit
    ) {
        val checkPinCommand = SetPinCommand(session.environment.pin1, session.environment.pin2)
        checkPinCommand.run(session) { result ->
            when (result) {
                is CompletionResult.Failure -> {
                    session.viewDelegate.onPinRequested { pin2 ->
                        if (!pin2.isNullOrEmpty()) {
                            session.environment.setPin2(pin2)
                            transceive(session, callback)
                        } else {
                            session.environment.setPin2(SessionEnvironment.DEFAULT_PIN2)
                            callback(CompletionResult.Failure(TangemSdkError.Pin2OrCvcRequired()))
                        }
                    }
                }
                is CompletionResult.Success -> {
                    transceive(session, callback)
                }
            }
        }
    }
}