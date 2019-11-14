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

/**
 * An error class that represent typical errors that may occur when performing Tangem SDK tasks.
 * Errors are propagated back to the caller in callbacks.
 */
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

/**
 * Events that are are sent in callbacks from [Task].
 */
sealed class TaskEvent<T> {

    /**
     * A callback that is triggered by a Task.
     */
    class Event<T>(val data: T) : TaskEvent<T>()

    /**
     * A callback that is triggered when a [Task] is completed.
     *
     * @param error is null if it's a successful completion of a [Task]
     */
    class Completion<T>(val error: TaskError? = null) : TaskEvent<T>()
}

/**
 * Allows to perform a group of commands interacting between the card and the application.
 * A task opens an NFC session, sends commands to the card and receives its responses,
 * repeats the commands if needed, and closes session after receiving the last answer.
 */
abstract class Task<T> {

    var delegate: CardManagerDelegate? = null
    var reader: CardReader? = null

    /**
     * This method should be called to run the [Task] and perform all its operations.
     *
     * @param cardEnvironment Relevant current version of a card environment
     * @param callback It will be triggered during the performance of the [Task]
     */
    fun run(cardEnvironment: CardEnvironment,
            callback: (result: TaskEvent<T>) -> Unit) {
        delegate?.onNfcSessionStarted()
        reader?.openSession()
        Log.i(this::class.simpleName!!, "Nfc task is started")
        onRun(cardEnvironment, callback)
    }

    /**
     * Should be called on [Task] completion, whether it was successful or with failure.
     *
     * @param withError True when there is an error
     * @param taskError The error to be shown by [CardManagerDelegate]
     */
    protected fun completeNfcSession(withError: Boolean = false, taskError: TaskError? = null) {
        reader?.closeSession()
        if (withError) {
            delegate?.onError(taskError)
        } else {
            delegate?.onNfcSessionCompleted()
        }
    }

    /**
     * In this method the individual Tasks' logic should be implemented.
     */
    protected abstract fun onRun(cardEnvironment: CardEnvironment,
                                 callback: (result: TaskEvent<T>) -> Unit)

    /**
     * This method should be called by Tasks in their [onRun] method wherever
     * they need to communicate with the Tangem Card by launching commands.
     */
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
                        delegate?.onTagLost()
                    } else if (result.error is TaskError.UserCancelledError) {
                        callback(CompletionResult.Failure(TaskError.UserCancelledError()))
                        reader?.closeSession()
                    }
            }
        }
    }
}


