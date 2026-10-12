package com.tangem.domain.polymarket.flow

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.PredictionWithdrawalStore
import com.tangem.domain.polymarket.interactor.SubmitPredictionWithdrawalInteractor
import com.tangem.domain.polymarket.model.PredictionWithdrawError
import com.tangem.domain.polymarket.model.PredictionWithdrawalReceipt
import com.tangem.domain.polymarket.model.PredictionWithdrawalRecord
import com.tangem.domain.polymarket.model.PredictionWithdrawalTrackingConfig
import com.tangem.domain.polymarket.usecase.GetPredictionWithdrawalStateUseCase
import com.tangem.utils.coroutines.AppCoroutineScope
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PredictionWithdrawalTracker(
    private val store: PredictionWithdrawalStore,
    private val submitInteractor: SubmitPredictionWithdrawalInteractor,
    private val getState: GetPredictionWithdrawalStateUseCase,
    private val config: PredictionWithdrawalTrackingConfig,
    private val appScope: AppCoroutineScope,
) {

    private val loops = mutableMapOf<UserWalletId, Job>()

    @Synchronized
    fun track(userWalletId: UserWalletId) {
        if (loops[userWalletId]?.isActive == true) return
        loops[userWalletId] = appScope.launch { run(userWalletId) }
    }

    private suspend fun run(userWalletId: UserWalletId) {
        val record = readRecord(userWalletId) ?: return
        val transactionId = record.transactionId ?: resend(userWalletId) ?: return
        poll(userWalletId = userWalletId, transactionId = transactionId)
    }

    private suspend fun readRecord(userWalletId: UserWalletId): PredictionWithdrawalRecord? =
        runSuspendCatching { store.get(userWalletId) }.getOrElse { cause ->
            TangemLogger.e("The stored withdrawal could not be read", cause)
            null
        }

    private suspend fun resend(userWalletId: UserWalletId): String? = submitInteractor.resend(
        userWalletId = userWalletId,
        walletId = userWalletId.stringValue,
    ).fold(
        ifLeft = { failure ->
            TangemLogger.e("The recorded withdrawal could not be resent: $failure")
            null
        },
        ifRight = { receipt -> receipt.transactionId.takeUnless { release(userWalletId, receipt) } },
    )

    private suspend fun poll(userWalletId: UserWalletId, transactionId: String) {
        repeat(config.maxAttempts) {
            delay(config.interval)
            val isDone = getState(walletId = userWalletId.stringValue, transactionId = transactionId).fold(
                ifLeft = { error -> error.stopsTracking(transactionId) },
                ifRight = { receipt -> release(userWalletId, receipt) },
            )
            if (isDone) return
        }
        TangemLogger.e("Withdrawal $transactionId is still in progress after ${config.maxAttempts} polls")
    }

    /** @return true when [receipt] is terminal and the record has been released, so polling must stop */
    private suspend fun release(userWalletId: UserWalletId, receipt: PredictionWithdrawalReceipt): Boolean {
        if (!receipt.state.isTerminal) return false
        TangemLogger.i("Withdrawal ${receipt.transactionId} reached ${receipt.state}")
        return runSuspendCatching { store.clear(userWalletId) }
            .onFailure { TangemLogger.e("The finished withdrawal record could not be cleared", it) }
            .isSuccess
    }

    private fun PredictionWithdrawError.stopsTracking(transactionId: String): Boolean {
        val shouldStop = when (this) {
            is PredictionWithdrawError.NonceTaken,
            is PredictionWithdrawError.RelayerRejected,
            is PredictionWithdrawError.InvalidRequest,
            PredictionWithdrawError.Unauthorized,
            PredictionWithdrawError.NotFound,
            -> true
            PredictionWithdrawError.RelayerUnavailable,
            PredictionWithdrawError.Network,
            is PredictionWithdrawError.Unexpected,
            -> false
        }
        if (shouldStop) {
            TangemLogger.e("Polling withdrawal $transactionId failed: $this, giving up")
        } else {
            TangemLogger.d("Polling withdrawal $transactionId failed: $this, retrying")
        }
        return shouldStop
    }
}