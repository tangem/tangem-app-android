package com.tangem.tap.domain

import android.content.Context
import com.tangem.Message
import com.tangem.TangemSdk
import com.tangem.common.CardFilter
import com.tangem.common.CompletionResult
import com.tangem.common.SuccessResponse
import com.tangem.common.card.Card
import com.tangem.common.card.FirmwareVersion
import com.tangem.common.core.CardSessionRunnable
import com.tangem.common.core.Config
import com.tangem.common.core.TangemSdkError
import com.tangem.operations.CommandResponse
import com.tangem.operations.pins.CheckUserCodesCommand
import com.tangem.operations.pins.CheckUserCodesResponse
import com.tangem.operations.pins.SetUserCodeCommand
import com.tangem.operations.wallet.PurgeWalletCommand
import com.tangem.tap.common.analytics.AnalyticsEvent
import com.tangem.tap.common.analytics.AnalyticsHandler
import com.tangem.tap.common.analytics.FirebaseAnalyticsHandler
import com.tangem.tap.domain.tasks.CreateWalletAndRescanTask
import com.tangem.tap.domain.tasks.ScanNoteResponse
import com.tangem.tap.domain.tasks.ScanNoteTask
import com.tangem.tap.domain.twins.isTangemTwin
import com.tangem.wallet.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class TangemSdkManager(private val tangemSdk: TangemSdk, private val context: Context) {

    suspend fun scanNote(
        analyticsHandler: AnalyticsHandler, messageRes: Int? = null,
    ): CompletionResult<ScanNoteResponse> {
        analyticsHandler.triggerEvent(AnalyticsEvent.READY_TO_SCAN, null)

        return runTaskAsyncReturnOnMain(
            ScanNoteTask(),
            initialMessage = Message(context.getString(messageRes ?: R.string.initial_message_scan_header))
        ).also { sendScanFailuresToAnalytics(analyticsHandler, it) }
    }

    private fun sendScanFailuresToAnalytics(
        analyticsHandler: AnalyticsHandler,
        result: CompletionResult<ScanNoteResponse>
    ) {
        if (result is CompletionResult.Failure && result.error is TangemSdkError) {
            (result.error as? TangemSdkError)?.let { error ->
                analyticsHandler.logCardSdkError(error, FirebaseAnalyticsHandler.ActionToLog.Scan)
            }
        }
    }

    suspend fun createWallet(cardId: String?): CompletionResult<Card> {
        return runTaskAsyncReturnOnMain(CreateWalletAndRescanTask(), cardId,
            initialMessage = Message(context.getString(R.string.initial_message_create_wallet_body)))
    }

    suspend fun eraseWallet(card: Card): CompletionResult<SuccessResponse> {
        return runTaskAsyncReturnOnMain(
            PurgeWalletCommand(card.wallets.first().publicKey),
            card.cardId,
            initialMessage = Message(context.getString(R.string.initial_message_purge_wallet_body)))
    }

    suspend fun setPasscode(cardId: String?): CompletionResult<SuccessResponse> {
        return runTaskAsyncReturnOnMain(
            SetUserCodeCommand.changePasscode(null),
            cardId,
            initialMessage = Message(context.getString(R.string.initial_message_change_passcode_body)))
    }

    suspend fun setAccessCode(cardId: String?): CompletionResult<SuccessResponse> {
        return runTaskAsyncReturnOnMain(
            SetUserCodeCommand.changeAccessCode(null),
            cardId,
            initialMessage = Message(context.getString(R.string.initial_message_change_access_code_body)))
    }

    suspend fun setLongTap(cardId: String?): CompletionResult<SuccessResponse> {
        return runTaskAsyncReturnOnMain(
            SetUserCodeCommand.resetUserCodes(),
            cardId,
            initialMessage = Message(context.getString(R.string.initial_message_tap_header)))
    }

    suspend fun checkUserCodes(cardId: String?): CompletionResult<CheckUserCodesResponse> {
        return runTaskAsyncReturnOnMain(
            CheckUserCodesCommand(),
            cardId,
            initialMessage = Message(context.getString(R.string.initial_message_tap_header)))
    }

    suspend fun <T : CommandResponse> runTaskAsync(
        runnable: CardSessionRunnable<T>, cardId: String? = null, initialMessage: Message? = null,
    ): CompletionResult<T> =
        withContext(Dispatchers.Main) {
            suspendCoroutine { continuation ->
                tangemSdk.startSessionWithRunnable(runnable, cardId, initialMessage) { result ->
                    continuation.resume(result)
                }
            }
        }

    private suspend fun <T : CommandResponse> runTaskAsyncReturnOnMain(
        runnable: CardSessionRunnable<T>, cardId: String? = null, initialMessage: Message? = null,
    ): CompletionResult<T> {
        val result = runTaskAsync(runnable, cardId, initialMessage)
        return withContext(Dispatchers.Main) { result }
    }

    fun changeDisplayedCardIdNumbersCount(card: Card) {
        tangemSdk.config.cardIdDisplayedNumbersCount = if (card.isTangemTwin()) 4 else null
    }

    companion object {
        val config = Config(
            filter = CardFilter(
                allowedCardTypes = FirmwareVersion.FirmwareType.values().toList()
            )
        )
    }
}