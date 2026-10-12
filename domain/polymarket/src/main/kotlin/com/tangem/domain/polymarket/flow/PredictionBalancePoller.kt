package com.tangem.domain.polymarket.flow

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.flow.PredictionAccountStatusFetcher.Mode
import com.tangem.domain.polymarket.model.PredictionBalancePollingConfig
import com.tangem.utils.coroutines.AppCoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PredictionBalancePoller(
    private val fetcher: PredictionAccountStatusFetcher,
    private val fundingWindow: PredictionFundingWindow,
    private val config: PredictionBalancePollingConfig,
    private val appScope: AppCoroutineScope,
) {

    private val loops = mutableMapOf<UserWalletId, Loop>()

    fun subscribe(userWalletId: UserWalletId): Flow<Nothing> = flow {
        val loop = acquire(userWalletId = userWalletId)
        try {
            awaitCancellation()
        } finally {
            release(userWalletId = userWalletId, loop = loop)
        }
    }

    @Synchronized
    private fun acquire(userWalletId: UserWalletId): Loop {
        val loop = loops[userWalletId]
        return if (loop != null && loop.job.isActive) {
            loop.subscribers += 1
            loop
        } else {
            Loop(job = appScope.launch { run(userWalletId = userWalletId) }, subscribers = 1).also {
                loops[userWalletId] = it
            }
        }
    }

    @Synchronized
    private fun release(userWalletId: UserWalletId, loop: Loop) {
        loop.subscribers -= 1
        if (loop.subscribers == 0 && loops[userWalletId] === loop) {
            loop.job.cancel()
            loops.remove(userWalletId)
        }
    }

    private suspend fun run(userWalletId: UserWalletId) = coroutineScope {
        launch { fundingWindow.watch(userWalletId = userWalletId) }

        fundingWindow.isOpen(userWalletId = userWalletId).collectLatest { isOpen ->
            val interval = if (isOpen) config.fundingInterval else config.baseInterval
            val mode = if (isOpen) Mode.AVAILABLE_ONLY else Mode.FULL

            while (currentCoroutineContext().isActive) {
                fetcher(PredictionAccountStatusFetcher.Params(userWalletId = userWalletId, mode = mode))
                delay(interval)
            }
        }
    }

    private class Loop(val job: Job, var subscribers: Int)
}