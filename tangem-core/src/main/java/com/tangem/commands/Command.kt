package com.tangem.commands

import com.tangem.*
import com.tangem.common.CompletionResult
import com.tangem.common.PinCode
import com.tangem.common.apdu.*
import com.tangem.common.extensions.toInt
import com.tangem.common.tlv.TlvTag
import com.tangem.tasks.PinType


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
    override val requiresPin2: Boolean = false

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
        if (requiresPin2 && session.environment.pin2?.isDefault == true) {
            handlePin2(session, callback)
            return
        }
        transceiveInternal(session, callback)
    }

    private fun transceiveInternal(session: CardSession, callback: (result: CompletionResult<T>) -> Unit) {
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
                        if (error is TangemSdkError.Pin2OrCvcRequired) {
                            handlePin2(session, callback)
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
                            if (error != null) {
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

    private fun handlePin1(
            session: CardSession,
            callback: (result: CompletionResult<T>) -> Unit
    ) {
        session.pause()
        session.viewDelegate.onPinRequested(PinType.Pin1) { pin1 ->
            session.environment.pin1 = PinCode(pin1)
            session.resume()
            transceive(session, callback)
        }
    }

    private fun handlePin2(
            session: CardSession,
            callback: (result: CompletionResult<T>) -> Unit
    ) {
        val currentPin1 = session.environment.pin1?.value ?: run {
            callback(CompletionResult.Failure(TangemSdkError.Pin1Required()))
            return
        }
        val currentPin2 = session.environment.pin2?.value ?: run {
            callback(CompletionResult.Failure(TangemSdkError.Pin2OrCvcRequired()))
            return
        }

        val checkPinCommand = SetPinCommand(currentPin1, currentPin2)
        checkPinCommand.run(session) { result ->
            when (result) {
                is CompletionResult.Failure -> {
                    session.environment.pin2 = null
                    getPin2FromDelegate(session, callback)
                }
                is CompletionResult.Success -> {
                    transceiveInternal(session, callback)
                }
            }
        }
    }

    private fun getPin2FromDelegate(session: CardSession,
                                    callback: (result: CompletionResult<T>) -> Unit) {
        session.viewDelegate.onPinRequested(PinType.Pin2) { pin2 ->
            session.environment.pin2 = PinCode(pin2)
            transceive(session, callback)
        }
    }
}