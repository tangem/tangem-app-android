package com.tangem.tap.domain

import androidx.activity.ComponentActivity
import com.tangem.*
import com.tangem.commands.*
import com.tangem.common.CompletionResult
import com.tangem.common.extensions.CardType
import com.tangem.common.extensions.calculateSha256
import com.tangem.tangem_sdk_new.extensions.init
import com.tangem.tap.domain.tasks.CreateWalletAndRescanTask
import com.tangem.tap.domain.tasks.ScanNoteResponse
import com.tangem.tap.domain.tasks.ScanNoteTask
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class TangemSdkManager(val activity: ComponentActivity) {
    private val tangemSdk = TangemSdk.init(
            activity, Config(cardFilter = CardFilter(EnumSet.allOf(CardType::class.java)))
    )

    suspend fun scanNote(): CompletionResult<ScanNoteResponse> {
        return runTaskAsyncReturnOnMain(ScanNoteTask())
    }

    suspend fun createWallet(cardId: String?): CompletionResult<ScanNoteResponse> {
        return runTaskAsyncReturnOnMain(CreateWalletAndRescanTask(), cardId)
    }

    suspend fun eraseWallet(cardId: String?): CompletionResult<PurgeWalletResponse> {
        return runTaskAsyncReturnOnMain(PurgeWalletCommand(), cardId)
    }

    suspend fun setPasscode(cardId: String?): CompletionResult<SetPinResponse> {
        return runTaskAsyncReturnOnMain(SetPinCommand(
                pinType = PinType.Pin2,
                newPin1 = tangemSdk.config.defaultPin1.calculateSha256(),
                newPin2 = null
        ), cardId)
    }

    suspend fun setAccessCode(cardId: String?): CompletionResult<SetPinResponse> {
        return runTaskAsyncReturnOnMain(SetPinCommand(
                pinType = PinType.Pin1,
                newPin1 = null,
                newPin2 = tangemSdk.config.defaultPin2.calculateSha256()
        ), cardId)
    }

    suspend fun setLongTap(cardId: String?): CompletionResult<SetPinResponse> {
        return runTaskAsyncReturnOnMain(SetPinCommand(
                pinType = PinType.Pin1,
                newPin1 = tangemSdk.config.defaultPin1.calculateSha256(),
                newPin2 = tangemSdk.config.defaultPin2.calculateSha256()
        ), cardId)
    }

    private suspend fun <T : CommandResponse> runTaskAsync(
            runnable: CardSessionRunnable<T>, cardId: String? = null, initialMessage: Message? = null
    ): CompletionResult<T> =
            withContext(Dispatchers.IO) {
                suspendCoroutine { continuation ->
                    tangemSdk.startSessionWithRunnable(runnable, cardId, initialMessage) { result ->
                        continuation.resume(result)
                    }
                }
            }

    private suspend fun <T : CommandResponse> runTaskAsyncReturnOnMain(
            runnable: CardSessionRunnable<T>, cardId: String? = null, initialMessage: Message? = null
    ): CompletionResult<T> {
        val result = runTaskAsync(runnable, cardId, initialMessage)
        return withContext(Dispatchers.Main) { result }
    }
}