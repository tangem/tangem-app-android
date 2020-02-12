package com.tangem.tasks

import com.tangem.common.CardEnvironment
import com.tangem.CardManagerDelegate
import com.tangem.CardReader
import com.tangem.Log
import com.tangem.commands.Card
import com.tangem.commands.CommandResponse
import com.tangem.commands.CommandSerializer
import com.tangem.commands.ReadCommand
import com.tangem.common.CompletionResult
import com.tangem.common.apdu.CommandApdu
import com.tangem.common.apdu.StatusWord

/**
 * An error class that represent typical errors that may occur when performing Tangem SDK tasks.
 * Errors are propagated back to the caller in callbacks.
 */
sealed class TaskError(val code: Int): Exception() {

    //Errors in serializing APDU
    class SerializeCommandError: TaskError(1001)
    class EncodingError: TaskError(1002)
    class MissingTag: TaskError(1003)
    class WrongType: TaskError(1004)
    class ConvertError: TaskError(1005)

    //Card errors
    class UnknownStatus: TaskError(2001)
    class ErrorProcessingCommand: TaskError(2002)
    class MissingPreflightRead: TaskError(2003)
    class InvalidState: TaskError(2004)
    class InsNotSupported: TaskError(2005)
    class InvalidParams: TaskError(2006)
    class NeedEncryption: TaskError(2007)

    //Scan errors
    class VerificationFailed: TaskError(3000)
    class CardError: TaskError(3001)
    class WrongCard: TaskError(3002)
    class TooMuchHashesInOneTransaction: TaskError(3003)
    class EmptyHashes: TaskError(3004)
    class HashSizeMustBeEqual: TaskError(3005)

    class Busy: TaskError(4000)
    class UserCancelled: TaskError(4001)
    class UnsupportedDevice: TaskError(4002)

    //NFC error
    class NfcReaderError: TaskError(5002)
    class TagLost: TaskError(5003)

    class UnknownError: TaskError(6000)
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
    var performPreflightRead: Boolean = true
    var securityDelayDuration: Int = 0

    /**
     * This method should be called to run the [Task] and perform all its operations.
     *
     * @param cardEnvironment Relevant current version of a card environment
     * @param callback It will be triggered during the performance of the [Task]
     */
    fun run(cardEnvironment: CardEnvironment,
            callback: (result: TaskEvent<T>) -> Unit) {
        delegate?.onNfcSessionStarted(cardEnvironment.cardId)
        reader?.openSession()
        Log.i(this::class.simpleName!!, "Nfc task is started")

        if (performPreflightRead) {
            runWithPreflightRead(cardEnvironment, callback)
        } else {
            onRun(cardEnvironment, null, callback)
        }
    }

    /**
     * Should be called on [Task] completion, whether it was successful or with failure.
     *
     * @param withError True when there is an error
     * @param taskError The error to be shown by [CardManagerDelegate]
     */
    protected fun completeNfcSession(taskError: TaskError? = null) {
        reader?.closeSession()
        if (taskError != null) {
            delegate?.onError(taskError)
        } else {
            delegate?.onNfcSessionCompleted()
        }
    }

    /**
     * In this method the individual Tasks' logic should be implemented.
     */
    protected abstract fun onRun(cardEnvironment: CardEnvironment,
                                 currentCard: Card?,
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
                        StatusWord.Unknown -> {
                            Log.e(this::class.simpleName!!, "Unknown status error: ${result.data.sw}")
                            callback(CompletionResult.Failure(TaskError.UnknownStatus()))
                        }
                        StatusWord.ErrorProcessingCommand -> callback(CompletionResult.Failure(TaskError.ErrorProcessingCommand()))
                        StatusWord.InvalidState -> callback(CompletionResult.Failure(TaskError.InvalidState()))

                        StatusWord.InsNotSupported -> callback(CompletionResult.Failure(TaskError.InsNotSupported()))
                        StatusWord.NeedEncryption -> callback(CompletionResult.Failure(TaskError.NeedEncryption()))
                        StatusWord.NeedPause -> {
                            // When NeedPause is returned from the card whenever security delay is triggered.
                            val remainingTime = command.deserializeSecurityDelay(responseApdu, cardEnvironment)
                            if (remainingTime != null) delegate?.onSecurityDelay(remainingTime, securityDelayDuration)
                            Log.i(this::class.simpleName!!, "Nfc command ${command::class.simpleName!!} triggered security delay of $remainingTime milliseconds")
                            sendRequest(command, commandApdu, cardEnvironment, callback)
                        }
                    }
                }
                is CompletionResult.Failure ->
                    if (result.error == TaskError.TagLost()) {
                        delegate?.onTagLost()
                    } else if (result.error is TaskError.UserCancelled) {
                        callback(CompletionResult.Failure(TaskError.UserCancelled()))
                        reader?.closeSession()
                    }
            }
        }
    }

    private fun runWithPreflightRead(
            environment: CardEnvironment, callback: (result: TaskEvent<T>) -> Unit) {
        sendCommand(ReadCommand(), environment) { readResult ->
            when (readResult) {
                is CompletionResult.Failure -> {
                    completeNfcSession(readResult.error)
                    callback(TaskEvent.Completion(readResult.error))
                }
                is CompletionResult.Success -> {

                    val receivedCardId = readResult.data.cardId
                    securityDelayDuration = readResult.data.pauseBeforePin2 ?: 0

                    if (environment.cardId != null && environment.cardId != receivedCardId) {
                        completeNfcSession(TaskError.WrongCard())
                        callback(TaskEvent.Completion(TaskError.WrongCard()))
                        return@sendCommand
                    }

                    val newEnvironment = environment.copy(cardId = receivedCardId)
                    onRun(newEnvironment, readResult.data, callback)
                }
            }


        }

    }
}



