package com.tangem.tasks

import com.tangem.CardManagerDelegate
import com.tangem.CardReader
import com.tangem.Log
import com.tangem.commands.*
import com.tangem.common.CardEnvironment
import com.tangem.common.CompletionResult
import com.tangem.common.EncryptionMode
import com.tangem.common.apdu.CommandApdu
import com.tangem.common.apdu.StatusWord
import com.tangem.common.extensions.calculateSha256
import com.tangem.crypto.EncryptionHelper
import com.tangem.crypto.FastEncryptionHelper
import com.tangem.crypto.StrongEncryptionHelper
import com.tangem.crypto.pbkdf2Hash

/**
 * An error class that represent typical errors that may occur when performing Tangem SDK tasks.
 * Errors are propagated back to the caller in callbacks.
 */
sealed class TaskError(val code: Int) : Exception() {

    //Errors in serializing APDU
    /**
     * This error is returned when there [CommandSerializer] cannot deserialize [com.tangem.common.tlv.Tlv]
     * (this error is a wrapper around internal [com.tangem.common.tlv.TlvMapper] errors).
     */
    class SerializeCommandError : TaskError(1001)

    class EncodingError : TaskError(1002)
    class MissingTag : TaskError(1003)
    class WrongType : TaskError(1004)
    class ConvertError : TaskError(1005)

    /**
     * This error is returned when unknown [StatusWord] is received from a card.
     */
    class UnknownStatus : TaskError(2001)

    /**
     * This error is returned when a card's reply is [StatusWord.ErrorProcessingCommand].
     * The card sends this status in case of internal card error.
     */
    class ErrorProcessingCommand : TaskError(2002)

    /**
     * This error is returned when a task (such as [ScanTask]) requires that [ReadCommand]
     * is executed before performing other commands.
     */
    class MissingPreflightRead : TaskError(2003)

    /**
     * This error is returned when a card's reply is [StatusWord.InvalidState].
     * The card sends this status when command can not be executed in the current state of a card.
     */
    class InvalidState : TaskError(2004)

    /**
     * This error is returned when a card's reply is [StatusWord.InsNotSupported].
     * The card sends this status when the card cannot process the [com.tangem.common.apdu.Instruction].
     */
    class InsNotSupported : TaskError(2005)

    /**
     * This error is returned when a card's reply is [StatusWord.InvalidParams].
     * The card sends this status when there are wrong or not sufficient parameters in TLV request,
     * or wrong PIN1/PIN2.
     * The error may be caused, for example, by wrong parameters of the [Task], [CommandSerializer],
     * mapping or serialization errors.
     */
    class InvalidParams : TaskError(2006)

    /**
     * This error is returned when a card's reply is [StatusWord.NeedEncryption]
     * and the encryption was not established by TangemSdk.
     */
    class NeedEncryption : TaskError(2007)

    //Scan errors
    /**
     * This error is returned when a [Task] checks unsuccessfully either
     * a card's ability to sign with its private key, or the validity of issuer data.
     */
    class VerificationFailed : TaskError(3000)

    /**
     * This error is returned when a [ScanTask] returns a [Card] without some of the essential fields.
     */
    class CardError : TaskError(3001)

    /**
     * This error is returned when a [Task] expects a user to use a particular card,
     * and a user tries to use a different card.
     */
    class WrongCard : TaskError(3002)

    /**
     * Tangem cards can sign currently up to 10 hashes during one [com.tangem.commands.SignCommand].
     * This error is returned when a [com.tangem.commands.SignCommand] receives more than 10 hashes to sign.
     */
    class TooMuchHashesInOneTransaction : TaskError(3003)

    /**
     * This error is returned when a [com.tangem.commands.SignCommand]
     * receives only empty hashes for signature.
     */
    class EmptyHashes : TaskError(3004)

    /**
     * This error is returned when a [com.tangem.commands.SignCommand]
     * receives hashes of different lengths for signature.
     */
    class HashSizeMustBeEqual : TaskError(3005)

    /**
     * This error is returned when [com.tangem.CardManager] was called with a new [Task],
     * while a previous [Task] is still in progress.
     */
    class Busy : TaskError(4000)

    /**
     * This error is returned when a user manually closes NFC Reading Bottom Sheet Dialog.
     */
    class UserCancelled : TaskError(4001)

    //NFC errors
    class NfcReaderError : TaskError(5002)

    /**
     * This error is returned when Android  NFC reader loses a tag
     * (e.g. a user detaches card from the phone's NFC module) while the NFC session is in progress.
     */
    class TagLost : TaskError(5003)

    class UnknownError : TaskError(6000)

    //Issuer Data Errors
    /**
     * This error is returned when [ReadIssuerDataTask] or [ReadIssuerExtraDataTask] expects a counter
     * (when the card's requires it), but the counter is missing.
     */
    class MissingCounter : TaskError(7001)
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

        when (cardEnvironment.encryptionMode) {
            EncryptionMode.NONE -> {
                val commandApdu = command.serialize(cardEnvironment)
                sendRequest(command, commandApdu, cardEnvironment, callback)
            }
            EncryptionMode.FAST, EncryptionMode.STRONG -> {
                if (cardEnvironment.encryptionKey != null ) {
                    val commandApdu = command.serialize(cardEnvironment)
                    sendRequest(command, commandApdu, cardEnvironment, callback)
                    return
                }
                val encryptionHelper: EncryptionHelper =
                        if (cardEnvironment.encryptionMode == EncryptionMode.STRONG) {
                            StrongEncryptionHelper()
                        } else {
                            FastEncryptionHelper()
                        }
                val openSessionCommand = OpenSessionCommand(encryptionHelper.keyA)
                val openSessionApdu = openSessionCommand.serialize(cardEnvironment)
                sendRequest(openSessionCommand, openSessionApdu, cardEnvironment) { result ->
                    when (result) {
                        is CompletionResult.Success -> {
                            val uid = result.data.uid
                            val protocolKey = cardEnvironment.pin1.calculateSha256().pbkdf2Hash(uid, 50)
                            val secret = encryptionHelper.generateSecret(result.data.sessionKeyB)
                            val sessionKey = (secret + protocolKey).calculateSha256()
                            cardEnvironment.encryptionKey = sessionKey

                            sendCommand(command, cardEnvironment, callback)
                        }
                        is CompletionResult.Failure -> callback(CompletionResult.Failure(result.error))
                    }
                }
            }
        }
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
                        StatusWord.NeedEncryption -> {
                            when (cardEnvironment.encryptionMode) {
                                EncryptionMode.NONE -> {
                                    cardEnvironment.encryptionKey = null
                                    cardEnvironment.encryptionMode = EncryptionMode.FAST
                                }
                                EncryptionMode.FAST -> {
                                    cardEnvironment.encryptionKey = null
                                    cardEnvironment.encryptionMode = EncryptionMode.STRONG
                                }
                                EncryptionMode.STRONG -> {
                                    Log.e(this::class.simpleName!!, "Encryption doesn't work")
                                    callback(CompletionResult.Failure(TaskError.NeedEncryption()))
                                    return@transceiveApdu
                                }
                            }
                            sendCommand(command, cardEnvironment, callback)
                        }
                        StatusWord.NeedPause -> {
                            // NeedPause is returned from the card whenever security delay is triggered.
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


