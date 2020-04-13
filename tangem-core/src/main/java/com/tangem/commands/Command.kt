package com.tangem.commands

import com.tangem.*
import com.tangem.common.CompletionResult
import com.tangem.common.apdu.CommandApdu
import com.tangem.common.apdu.ResponseApdu
import com.tangem.common.apdu.StatusWord
import com.tangem.common.apdu.toSessionError
import com.tangem.common.extensions.toInt
import com.tangem.common.tlv.TlvTag

/**
 * Basic interface for a parsed response from [Command].
 */
interface CommandResponse

/**
 * Basic class for Tangem card commands
 */
abstract class Command<T : CommandResponse> : CardSessionRunnable<T> {

    /**
     * Serializes data into an array of [com.tangem.common.tlv.Tlv],
     * then creates [CommandApdu] with this data.
     * @param environment [SessionEnvironment] of the current card
     * @return command data converted to [CommandApdu] that allows to convert it to [ByteArray]
     * that can be sent to a Tangem card
     */
    abstract fun serialize(environment: SessionEnvironment): CommandApdu

    /**
     * Deserializes data received from a card and stored in [ResponseApdu]
     * into an array of [com.tangem.common.tlv.Tlv]. Then maps it into a [CommandResponse].
     * @param environment [SessionEnvironment] of the current card.
     * @param apdu received data.
     * @return Card response converted to a [CommandResponse] of a type [T]
     */
    abstract fun deserialize(environment: SessionEnvironment, apdu: ResponseApdu): T

    override fun run(session: CardSession, callback: (result: CompletionResult<T>) -> Unit) {
        Log.i("Command", "Sending ${this::class.java.simpleName}")
        transceive(session, callback)
    }

    fun transceive(session: CardSession, callback: (result: CompletionResult<T>) -> Unit) {
        try {
            val apdu = serialize(session.environment)
            transceiveApdu(apdu, session) { result ->
                when (result) {
                    is CompletionResult.Failure -> callback(CompletionResult.Failure(result.error))
                    is CompletionResult.Success -> {
                        val response = deserialize(session.environment, result.data)
                        callback(CompletionResult.Success(response))
                    }
                }
            }
        } catch (error: SessionError) {
            callback(CompletionResult.Failure(error))
        }
    }

    private fun transceiveApdu(apdu: CommandApdu, session: CardSession, callback: (result: CompletionResult<ResponseApdu>) -> Unit) {
        session.send(apdu) { result ->

            when (result) {
                is CompletionResult.Success -> {
                    val responseApdu = result.data
                    when (responseApdu.statusWord) {
                        StatusWord.ProcessCompleted, StatusWord.Pin1Changed, StatusWord.Pin2Changed, StatusWord.PinsChanged
                        -> callback(CompletionResult.Success(responseApdu))
                        StatusWord.NeedPause -> {
                            // NeedPause is returned from the card whenever security delay is triggered.
                            val remainingTime = deserializeSecurityDelay(responseApdu, session.environment)
                            if (remainingTime != null) {
                                session.viewDelegate.onSecurityDelay(
                                        remainingTime,
                                        session.environment.card?.pauseBeforePin2 ?: 0)
                            }
                            Log.i(this::class.simpleName!!, "Nfc command ${this::class.simpleName!!} " +
                                    "triggered security delay of $remainingTime milliseconds")
                            transceiveApdu(apdu, session, callback)
                        }
                        else -> {
                            val error = responseApdu.statusWord.toSessionError()
                            if (error != null && !tryHandleError(error)) {
                                callback(CompletionResult.Failure(error))
                            } else {
                                callback(CompletionResult.Failure(SessionError.UnknownError()))
                            }
                        }
                    }
                }
                is CompletionResult.Failure ->
                    if (result.error == SessionError.TagLost()) {
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
    private fun deserializeSecurityDelay(responseApdu: ResponseApdu, environment: SessionEnvironment): Int? {
        val tlv = responseApdu.getTlvData()
        return tlv?.find { it.tag == TlvTag.Pause }?.value?.toInt()
    }

    private fun tryHandleError(error: SessionError): Boolean {
        return false
    }

}