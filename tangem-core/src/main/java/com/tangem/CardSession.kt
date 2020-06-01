package com.tangem

import com.tangem.commands.CommandResponse
import com.tangem.commands.OpenSessionCommand
import com.tangem.commands.ReadCommand
import com.tangem.common.CompletionResult
import com.tangem.common.apdu.CommandApdu
import com.tangem.common.apdu.ResponseApdu
import com.tangem.common.extensions.calculateSha256
import com.tangem.common.extensions.getType
import com.tangem.crypto.EncryptionHelper
import com.tangem.crypto.FastEncryptionHelper
import com.tangem.crypto.StrongEncryptionHelper
import com.tangem.crypto.pbkdf2Hash
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

/**
 * Basic interface for running tasks and [com.tangem.commands.Command] in a [CardSession]
 */
interface CardSessionRunnable<T : CommandResponse> {

    val performPreflightRead: Boolean

    /**
     * The starting point for custom business logic.
     * Implement this interface and use [TangemSdk.startSessionWithRunnable] to run.
     * @param session run commands in this [CardSession].
     * @param callback trigger the callback to complete the task.
     */
    fun run(session: CardSession, callback: (result: CompletionResult<T>) -> Unit)
}

enum class CardSessionState {
    Inactive,
    Active
}

enum class TagType {
    Nfc,
    Slix
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
 * the [TangemSdkError.WrongCardNumber] otherwise.
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

    var connectedTag: TagType? = null

    /**
     * True if some operation is still in progress.
     */
    private var state = CardSessionState.Inactive

    private val scope = CoroutineScope(Dispatchers.IO)

    private val tag = this.javaClass.simpleName

    /**
     * This metod starts a card session, performs preflight [ReadCommand],
     * invokes [CardSessionRunnable.run] and closes the session.
     * @param runnable [CardSessionRunnable] that will be performed in the session.
     * @param callback will be triggered with a [CompletionResult] of a session.
     */
    fun <T : CardSessionRunnable<R>, R : CommandResponse> startWithRunnable(
            runnable: T, callback: (result: CompletionResult<R>) -> Unit) {

        start(runnable.performPreflightRead) { session, error ->
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
                    is CompletionResult.Failure -> {
                        if (result.error is TangemSdkError.ExtendedLengthNotSupported) {
                            if (session.environment.terminalKeys != null) {
                                session.environment.terminalKeys = null
                                startWithRunnable(runnable, callback)
                                return@run
                            }
                        }
                        stopWithError(result.error)
                    }
                }
                callback(result)
            }
        }
    }

    /**
     * Starts a card session and performs preflight [ReadCommand].
     * @param callback: callback with the card session. Can contain [TangemSdkError] if something goes wrong.
     */
    fun start(performPreflightRead: Boolean = true,
              callback: (session: CardSession, error: TangemSdkError?) -> Unit) {

        if (state != CardSessionState.Inactive) {
            callback(this, TangemSdkError.Busy())
            return
        }
        state = CardSessionState.Active
        viewDelegate.onSessionStarted(cardId)

        scope.launch {
            reader.tag
                    .asFlow()
                    .collect { tagType ->
                        if (tagType == null && connectedTag != null) {
                            handleTagLost()
                        } else if (tagType != null) {
                            connectedTag = tagType
                            viewDelegate.onTagConnected()

                            if (tagType == TagType.Nfc && performPreflightRead) {
                                preflightCheck(callback)
                            } else {
                                callback(this@CardSession, null)
                            }
                        }
                    }
        }
        reader.scope = scope
        reader.startSession()
    }

    private fun handleTagLost() {
        connectedTag = null
        environment.encryptionKey = null
        viewDelegate.onTagLost()
    }

    private fun preflightCheck(callback: (session: CardSession, error: TangemSdkError?) -> Unit) {
        val readCommand = ReadCommand()
        readCommand.run(this) { result ->
            when (result) {
                is CompletionResult.Failure -> {
                    tryHandleError(result.error) { handleErrorResult ->
                        when (handleErrorResult) {
                            is CompletionResult.Success -> preflightCheck(callback)
                            is CompletionResult.Failure -> {
                                stopWithError(result.error)
                                callback(this, result.error)
                            }
                        }
                    }
                }
                is CompletionResult.Success -> {
                    val receivedCardId = result.data.cardId
                    if (cardId != null && receivedCardId != cardId) {
                        viewDelegate.onWrongCard()
                        preflightCheck(callback)
                        return@run
                    }
                    val allowedCardTypes = environment.cardFilter.allowedCardTypes
                    if (!allowedCardTypes.contains(result.data.getType())) {
                        stopWithError(TangemSdkError.WrongCardType())
                        callback(this, TangemSdkError.WrongCardType())
                        return@run
                    }
                    environment.card = result.data
                    cardId = receivedCardId
                    callback(this, null)
                }
            }
        }
    }

    fun readSlixTag(callback: (result: CompletionResult<ResponseApdu>) -> Unit) {
        reader.readSlixTag(callback)
    }

    /**
     * Stops the current session with the text message.
     * @param message If null, the default message will be shown.
     */
    private fun stop(message: Message? = null) {
        stopSession()
        viewDelegate.onSessionStopped(message)
    }

    /**
     * Stops the current session on error.
     * @param error An error that will be shown.
     */
    private fun stopWithError(error: TangemSdkError) {
        stopSession()
        if (error !is TangemSdkError.UserCancelled) {
            Log.e(tag, "Finishing with error: ${error::class.simpleName}: ${error.code}")
            viewDelegate.onError(error)
        } else {
            Log.i(tag, "User cancelled NFC session")
        }
    }

    private fun stopSession() {
        reader.stopSession()
        state = CardSessionState.Inactive
        scope.cancel()
    }

    fun send(apdu: CommandApdu, callback: (result: CompletionResult<ResponseApdu>) -> Unit) {
        val subscription = reader.tag.openSubscription()

        scope.launch {
            subscription.consumeAsFlow()
                    .filterNotNull()
                    .collect {
                        reader.transceiveApdu(apdu) { result ->
                            subscription.cancel()
                            callback(result)
                        }
                    }
        }
    }

    private fun tryHandleError(
            error: TangemSdkError, callback: (result: CompletionResult<Boolean>) -> Unit) {

        when (error) {
            is TangemSdkError.NeedEncryption -> {
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
                        callback(CompletionResult.Failure(TangemSdkError.NeedEncryption()))
                    }
                }
                return establishEncryption(callback)
            }
            else -> callback(CompletionResult.Failure(TangemSdkError.UnknownError()))
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