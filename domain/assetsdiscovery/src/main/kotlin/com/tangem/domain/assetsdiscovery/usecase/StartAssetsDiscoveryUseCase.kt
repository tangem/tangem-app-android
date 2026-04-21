package com.tangem.domain.assetsdiscovery.usecase

import arrow.core.Either
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.event.AssetsDiscoveryAnalyticsEvent
import com.tangem.domain.account.status.usecase.ManageCryptoCurrenciesUseCase
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.assetsdiscovery.repository.AssetsDiscoveryRepository
import com.tangem.utils.coroutines.AppCoroutineScope
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

class StartAssetsDiscoveryUseCase(
    private val assetsDiscoveryRepository: AssetsDiscoveryRepository,
    private val manageCryptoCurrenciesUseCase: ManageCryptoCurrenciesUseCase,
    private val appCoroutineScope: AppCoroutineScope,
    private val analyticsEventHandler: AnalyticsEventHandler,
) {

    private val activeSyncJobs = ConcurrentHashMap<UserWalletId, Job>()

    operator fun invoke(userWalletId: UserWalletId) {
        activeSyncJobs[userWalletId]?.cancel()
        activeSyncJobs[userWalletId] = appCoroutineScope.launch {
            try {
                analyticsEventHandler.send(AssetsDiscoveryAnalyticsEvent.SyncStarted())
                assetsDiscoveryRepository.runDiscovery(userWalletId)
                applyDiscoveredTokens(userWalletId)
                assetsDiscoveryRepository.completeDiscovery(userWalletId)
                analyticsEventHandler.send(AssetsDiscoveryAnalyticsEvent.SyncCompleted())
            } catch (e: Exception) {
                TangemLogger.e("Token sync failed for wallet: $userWalletId", e)
            } finally {
                activeSyncJobs.remove(userWalletId)
            }
        }
    }

    suspend fun cancel(userWalletId: UserWalletId): Either<Throwable, Unit> = Either.catch {
        activeSyncJobs.remove(userWalletId)?.cancel()
        assetsDiscoveryRepository.clearPendingFlag(userWalletId)
        assetsDiscoveryRepository.clearDiscoveredTokens(userWalletId)
    }

    fun applyPendingAssetsDiscovery() {
        appCoroutineScope.launch {
            try {
                val pendingIds = assetsDiscoveryRepository.getPendingDiscoveryWalletIds()
                for (walletId in pendingIds) {
                    val isApplied = applyDiscoveredTokens(walletId)
                    if (isApplied) {
                        assetsDiscoveryRepository.clearPendingFlag(walletId)
                    }
                }
            } catch (e: Exception) {
                TangemLogger.e("Failed to apply pending syncs", e)
            }
        }
    }

    private suspend fun applyDiscoveredTokens(userWalletId: UserWalletId): Boolean {
        val currencies = assetsDiscoveryRepository.getDiscoveredCurrencies(userWalletId)

        if (currencies.isEmpty()) return true

        val accountId = AccountId.forMainCryptoPortfolio(userWalletId)
        return manageCryptoCurrenciesUseCase.invokeAndAwait(
            accountId = accountId,
            add = currencies,
        ).fold(
            ifRight = {
                assetsDiscoveryRepository.clearDiscoveredTokens(userWalletId)
                true
            },
            ifLeft = { error ->
                TangemLogger.e("Failed to apply discovered tokens for wallet: $userWalletId, error: $error")
                false
            },
        )
    }
}