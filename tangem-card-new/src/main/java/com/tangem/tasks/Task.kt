package com.tangem.tasks

import com.tangem.CardEnvironment
import com.tangem.CardManagerDelegate
import com.tangem.CardReader
import com.tangem.Log
import com.tangem.commands.CommandResponse
import com.tangem.commands.CommandSerializer
import com.tangem.common.CompletionResult
import com.tangem.enums.Status

abstract class Task<T> {

    var delegate: CardManagerDelegate? = null
    var reader: CardReader? = null

    fun run(cardEnvironment: CardEnvironment,
            callback: (result: TaskEvent<T>) -> Unit) {

        delegate?.openNfcPopup()
        reader?.setStartSession()
        Log.i(this::class.simpleName!!, "Nfc task is started")
        onRun(cardEnvironment, callback)

    }

    abstract fun onRun(cardEnvironment: CardEnvironment,
                       callback: (result: TaskEvent<T>) -> Unit)

    protected fun <T : CommandResponse> sendCommand(
            commandSerializer: CommandSerializer<T>,
            cardEnvironment: CardEnvironment,
            callback: (result: TaskEvent<T>) -> Unit) {

        Log.i(this::class.simpleName!!, "Nfc command ${commandSerializer::class.simpleName!!} is initiated")


        reader?.transceiveApdu(
                commandSerializer.serialize(cardEnvironment)) { result ->

            when (result) {
                is CompletionResult.Success -> {
                    val responseApdu = result.data
                    when (responseApdu.status) {
                        Status.ProcessCompleted, Status.Pin1Changed, Status.Pin2Changed, Status.PinsChanged
                        -> {
//                            reader.readingActive = false
//                            reader.setStartSession()
                            try {
                                val responseData = commandSerializer.deserialize(cardEnvironment, responseApdu)
                                Log.i(this::class.simpleName!!, "Nfc command ${commandSerializer::class.simpleName!!} is completed")
                                callback(TaskEvent.Event(responseData as T))
                                callback(TaskEvent.Completion())
                            } catch (error: TaskError) {
                                callback(TaskEvent.Completion(error))
                            }
                        }
                        Status.InvalidParams -> callback(TaskEvent.Completion(TaskError.InvalidParams()))
                        Status.ErrorProcessingCommand -> callback(TaskEvent.Completion(TaskError.ErrorProcessingCommand()))
                        Status.InvalidState -> callback(TaskEvent.Completion(TaskError.InvalidState()))

                        Status.InsNotSupported -> callback(TaskEvent.Completion(TaskError.InsNotSupported()))
                        Status.NeedEncryption -> callback(TaskEvent.Completion(TaskError.NeedEncryption()))
                        Status.NeedPause -> {
                            val remainingTime = commandSerializer.deserializeSecurityDelay(responseApdu, cardEnvironment)
                            if (remainingTime != null) delegate?.showSecurityDelay(remainingTime)
                            Log.i(this::class.simpleName!!, "Nfc command ${commandSerializer::class.simpleName!!} triggered security delay of $remainingTime milliseconds")
                            sendCommand(commandSerializer, cardEnvironment, callback)
                        }
                    }
                }
                is CompletionResult.Failure ->
                    if (result.error is TaskError.UserCancelledError) {
                        callback(TaskEvent.Completion(TaskError.UserCancelledError()))
                        reader?.readingActive = false
                    }

            }
        }
    }
}

sealed class TaskError(description: String? = null) : Exception(description) {
    class UnknownStatus(sw: Int) : TaskError()
    class MappingError : TaskError()
    class GenericError(description: String? = null) : TaskError(description)
    class UserCancelledError() : TaskError()
    class Busy(): TaskError()

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



