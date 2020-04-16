package com.tangem

import com.tangem.commands.Card
import com.tangem.commands.CommandResponse
import com.tangem.commands.OpenSessionCommand
import com.tangem.commands.ReadCommand
import com.tangem.common.CompletionResult
import com.tangem.common.apdu.CommandApdu
import com.tangem.common.apdu.ResponseApdu
import com.tangem.common.extensions.calculateSha256
import com.tangem.crypto.EncryptionHelper
import com.tangem.crypto.FastEncryptionHelper
import com.tangem.crypto.StrongEncryptionHelper
import com.tangem.crypto.pbkdf2Hash

/**
 * Basic interface for running tasks and [com.tangem.commands.Command] in a [CardSession]
 */
interface CardSessionRunnable<T : CommandResponse> {

    /**
     * The starting point for custom business logic.
     * Implement this interface and use [TangemSdk.startSession] to run.
     * @param session run commands in this [CardSession].
     * @param callback trigger the callback to complete the task.
     */
    fun run(session: CardSession, callback: (result: CompletionResult<T>) -> Unit)
}

/**
 * Allows interaction with Tangem cards. Should be opened before sending commands.
 *
 * @property environment
 * @property reader  is an interface that is responsible for NFC connection and
 * transfer of data to and from the Tangem Card.
 * @property viewDelegate is an  interface that allows interaction with users and shows relevant UI.
 * @property cardId ID, Unique Tangem card ID number. If not null, the SDK will check that you the card
 * with which you tapped a phone has this [cardId] and SDK will return
 * the [SessionError.WrongCard] otherwise.
 * @property initialMessage A custom description that will be shown at the beginning of the NFC session.
 * If null, a default header and text body will be used.
 */
class CardSession(
        val environment: SessionEnvironment,
        private val reader: CardReader,
        val viewDelegate: SessionViewDelegate,
        private var cardId: String? = null,
        private val initialMessage: Message? = null
) {

    private val tag = this.javaClass.simpleName
    /**
     * True if some operation is still in progress.
     */
    private var isBusy = false

    /**
     * This metod starts a card session, performs preflight [ReadCommand],
     * invokes [CardSessionRunnable.run] and closes the session.
     * @param runnable [CardSessionRunnable] that will be performed in the session.
     * @param callback will be triggered with a [CompletionResult] of a session.
     */
    fun <T : CardSessionRunnable<R>, R : CommandResponse> startWithRunnable(
            runnable: T, callback: (result: CompletionResult<R>) -> Unit) {

        start { session, error ->
            if (error != null) {
                callback(CompletionResult.Failure(error))
                return@start
            }
            if (runnable is ReadCommand) {
                callback(CompletionResult.Success(environment.card as R))
                return@start
            }

            runnable.run(this) { result ->
                when (result) {
                    is CompletionResult.Success -> stop()
                    is CompletionResult.Failure -> stopWithError(result.error)
                }
                callback(result)
            }
        }
    }

    /**
     * Starts a card session and performs preflight [ReadCommand].
     * @param callback: callback with the card session. Can contain [SessionError] if something goes wrong.
     */
    fun start(callback: (session: CardSession, error: SessionError?) -> Unit) {
        try {
            startSession()
        } catch (error: SessionError) {
            callback(this, error)
        }

        preflightRead() { result ->
            when (result) {
                is CompletionResult.Failure -> {
                    callback(this, result.error)
                    stopWithError(result.error)
                }
                is CompletionResult.Success -> {
                    callback(this, null)
                }
            }
        }
    }

    private fun startSession() {
        if (isBusy) throw SessionError.Busy()
        isBusy = true
        viewDelegate.onNfcSessionStarted(cardId, initialMessage)
        reader.openSession()
    }

    private fun preflightRead(callback: (result: CompletionResult<Card>) -> Unit) {
        val readCommand = ReadCommand()
        readCommand.run(this) { result ->
            when (result) {
                is CompletionResult.Failure -> {
                    tryHandleError(result.error) { handleErrorResult ->
                        when (handleErrorResult) {
                            is CompletionResult.Success -> preflightRead(callback)
                            is CompletionResult.Failure -> {
                                stopWithError(result.error)
                                callback(CompletionResult.Failure(result.error))
                            }
                        }
                    }
                }
                is CompletionResult.Success -> {
                    val receivedCardId = result.data.cardId
                    if (cardId != null && receivedCardId != cardId) {
                        stopWithError(SessionError.WrongCard())
                        callback(CompletionResult.Failure(SessionError.WrongCard()))
                        return@run
                    }
                    environment.card = result.data
                    cardId = receivedCardId
                    callback(CompletionResult.Success(result.data))
                }
            }
        }
    }

    /**
     * Stops the current session with the text message.
     * @param message If null, the default message will be shown.
     */
    private fun stop(message: Message? = null) {
        reader.closeSession()
        viewDelegate.onNfcSessionCompleted(message)
        isBusy = false
    }

    /**
     * Stops the current session on error.
     * @param error An error that will be shown.
     */
    private fun stopWithError(error: Exception) {
        reader.closeSession()
        isBusy = false

        val errorMessage = if (error is SessionError) {
            "${error::class.simpleName}: ${error.code}"
        } else {
            error.localizedMessage
        }
        if (error !is SessionError.UserCancelled) {
            Log.e("tag", "Finishing with error: $errorMessage")
            viewDelegate.onError(errorMessage)
        }
    }

    fun send(apdu: CommandApdu, callback: (result: CompletionResult<ResponseApdu>) -> Unit) {
        reader.transceiveApdu(apdu, callback)
    }

    private fun tryHandleError(
            error: SessionError, callback: (result: CompletionResult<Boolean>) -> Unit) {

        when (error) {
            is SessionError.NeedEncryption -> {
                Log.i(tag, "Establishing encryption")
                when (environment.encryptionMode) {
                    EncryptionMode.NONE -> {
                        environment.encryptionKey = null
                        environment.encryptionMode = EncryptionMode.FAST
                    }
                    EncryptionMode.FAST -> {
                        environment.encryptionKey = null
                        environment.encryptionMode = EncryptionMode.STRONG
                    }
                    EncryptionMode.STRONG -> {
                        Log.e(tag, "Encryption doesn't work")
                        callback(CompletionResult.Failure(SessionError.NeedEncryption()))
                    }
                }
                return establishEncryption(callback)
            }
            else -> callback(CompletionResult.Failure(SessionError.UnknownError()))
        }
    }

    private fun establishEncryption(callback: (result: CompletionResult<Boolean>) -> Unit) {
        val encryptionHelper: EncryptionHelper =
                if (environment.encryptionMode == EncryptionMode.STRONG) {
                    StrongEncryptionHelper()
                } else {
                    FastEncryptionHelper()
                }
        val openSesssionCommand = OpenSessionCommand(encryptionHelper.keyA)
        openSesssionCommand.run(this) { result ->
            when (result) {
                is CompletionResult.Success -> {
                    val uid = result.data.uid
                    val protocolKey = environment.pin1.pbkdf2Hash(uid, 50)
                    val secret = encryptionHelper.generateSecret(result.data.sessionKeyB)
                    val sessionKey = (secret + protocolKey).calculateSha256()
                    environment.encryptionKey = sessionKey
                    callback(CompletionResult.Success(true))
                }
                is CompletionResult.Failure -> callback(CompletionResult.Failure(result.error))
            }
        }
    }
}