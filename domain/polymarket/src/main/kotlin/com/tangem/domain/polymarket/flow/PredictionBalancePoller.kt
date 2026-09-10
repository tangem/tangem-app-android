package com.tangem.domain.polymarket.flow

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.model.PredictionBalancePollingConfig
import com.tangem.utils.coroutines.AppCoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PredictionBalancePoller(
    private val refresher: PredictionBalanceRefresher,
    private val config: PredictionBalancePollingConfig,
    private val appScope: AppCoroutineScope,
) {

    private val loops = mutableMapOf<UserWalletId, Loop>()

    fun subscribe(userWalletId: UserWalletId): Flow<Unit> = flow {
        acquire(userWalletId = userWalletId)
        try {
            awaitCancellation()
        } finally {
            release(userWalletId = userWalletId)
        }
    }

    @Synchronized
    private fun acquire(userWalletId: UserWalletId) {
        val loop = loops[userWalletId]
        if (loop != null && loop.job.isActive) {
            loop.subscribers += 1
        } else {
            loops[userWalletId] = Loop(job = appScope.launch { run(userWalletId = userWalletId) }, subscribers = 1)
        }
    }

    @Synchronized
    private fun release(userWalletId: UserWalletId) {
        val loop = loops[userWalletId] ?: return
        loop.subscribers -= 1
        if (loop.subscribers == 0) {
            loop.job.cancel()
            loops.remove(userWalletId)
        }
    }

    private suspend fun run(userWalletId: UserWalletId) {
        while (currentCoroutineContext().isActive) {
            refresher.refresh(userWalletId = userWalletId)
            delay(config.baseInterval)
        }
    }

    private class Loop(val job: Job, var subscribers: Int)
}