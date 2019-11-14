package com.tangem.tasks

import com.tangem.CardEnvironment
import com.tangem.CardManagerDelegate
import com.tangem.CardReader
import com.tangem.Log
import com.tangem.commands.CommandResponse
import com.tangem.commands.CommandSerializer
import com.tangem.common.CompletionResult
import com.tangem.common.apdu.CommandApdu
import com.tangem.common.apdu.StatusWord

sealed class TaskError(description: String? = null) : Exception(description) {
    class UnknownStatus(sw: Int) : TaskError("Unknown StatusWord: $sw")
    class MappingError : TaskError()
    class GenericError(description: String? = null) : TaskError(description)
    class UserCancelledError() : TaskError()
    class Busy() : TaskError()
    class TagLost() : TaskError()

    class ErrorProcessingCommand : TaskError()
    class InvalidState : TaskError()
    class InsNotSupported : TaskError()
    class InvalidParams : TaskError()
    class NeedEncryption : TaskError()
    class NeedPause : TaskError()

    class VefificationFailed : TaskError()
    class CardError : TaskError()
    class ReaderError() : TaskError()
    class SerializeCommandError() : TaskError()

    class CardIsMissing() : TaskError()
    class EmptyHashes() : TaskError()
    class TooMuchHashes() : TaskError()
    class HashSizeMustBeEqual() : TaskError()
}

sealed class TaskEvent<T> {
    class Event<T>(val data: T) : TaskEvent<T>()
    class Completion<T>(val error: TaskError? = null) : TaskEvent<T>()
}

abstract class Task<T> {

    var delegate: CardManagerDelegate? = null
    var reader: CardReader? = null

    fun run(cardEnvironment: CardEnvironment,
            callback: (result: TaskEvent<T>) -> Unit) {
        delegate?.onNfcSessionStarted()
        reader?.startNfcSession()
        Log.i(this::class.simpleName!!, "Nfc task is started")
        onRun(cardEnvironment, callback)
    }

    protected fun completeNfcSession(withError: Boolean = false, taskError: TaskError? = null) {
        reader?.closeSession()
        if (withError) {
            delegate?.onError(taskError)
        } else {
            delegate?.onNfcSessionCompleted()
        }
    }

    protected abstract fun onRun(cardEnvironment: CardEnvironment,
                                 callback: (result: TaskEvent<T>) -> Unit)

    protected fun <T : CommandResponse> sendCommand(
            command: CommandSerializer<T>,
            cardEnvironment: CardEnvironment,
            callback: (result: CompletionResult<T>) -> Unit) {

        Log.i(this::class.simpleName!!, "Nfc command ${command::class.simpleName!!} is initiated")

        val commandApdu = command.serialize(cardEnvironment)
        sendRequest(command, commandApdu, cardEnvironment, callback)
    }

    private fun <T : CommandResponse> sendRequest(command: CommandSerializer<T>,
                                                  commandApdu: CommandApdu,
                                                  cardEnvironment: CardEnvironment,
                                                  callback: (result: CompletionResult<T>) -> Unit) {

        reader?.transceiveApdu(commandApdu) { result ->

            when (result) {
                is CompletionResult.Success -> {
                    val responseApdu = result.data
                    when (responseApdu.statusWord) {
                        StatusWord.ProcessCompleted, StatusWord.Pin1Changed, StatusWord.Pin2Changed, StatusWord.PinsChanged
                        -> {
                            try {
                                val responseData = command.deserialize(cardEnvironment, responseApdu)
                                Log.i(this::class.simpleName!!, "Nfc command ${command::class.simpleName!!} is completed")
                                callback(CompletionResult.Success(responseData as T))
                            } catch (error: TaskError) {
                                callback(CompletionResult.Failure(error))
                            }
                        }
                        StatusWord.InvalidParams -> callback(CompletionResult.Failure(TaskError.InvalidParams()))
                        StatusWord.Unknown -> callback(CompletionResult.Failure(TaskError.UnknownStatus(result.data.sw)))
                        StatusWord.ErrorProcessingCommand -> callback(CompletionResult.Failure(TaskError.ErrorProcessingCommand()))
                        StatusWord.InvalidState -> callback(CompletionResult.Failure(TaskError.InvalidState()))

                        StatusWord.InsNotSupported -> callback(CompletionResult.Failure(TaskError.InsNotSupported()))
                        StatusWord.NeedEncryption -> callback(CompletionResult.Failure(TaskError.NeedEncryption()))
                        StatusWord.NeedPause -> {
                            // When NeedPause is returned from the card whenever security delay is triggered.
                            val remainingTime = command.deserializeSecurityDelay(responseApdu, cardEnvironment)
                            if (remainingTime != null) delegate?.onSecurityDelay(remainingTime)
                            Log.i(this::class.simpleName!!, "Nfc command ${command::class.simpleName!!} triggered security delay of $remainingTime milliseconds")
                            sendRequest(command, commandApdu, cardEnvironment, callback)
                        }
                    }
                }
                is CompletionResult.Failure ->
                    if (result.error is TaskError.TagLost) {
                        delegate?.hideSecurityDelay()
                    } else if (result.error is TaskError.UserCancelledError) {
                        callback(CompletionResult.Failure(TaskError.UserCancelledError()))
                        reader?.readingActive = false
                    }
            }
        }
    }
}


