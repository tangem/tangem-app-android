package com.tangem.domain.tokensync.usecase

import arrow.core.Either
import com.tangem.domain.account.status.usecase.ManageCryptoCurrenciesUseCase
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tokensync.repository.TokenSyncRepository
import com.tangem.utils.coroutines.AppCoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap

class SyncTokensUseCase(
    private val tokenSyncRepository: TokenSyncRepository,
    private val manageCryptoCurrenciesUseCase: ManageCryptoCurrenciesUseCase,
    private val appCoroutineScope: AppCoroutineScope,
) {

    private val activeSyncJobs = ConcurrentHashMap<UserWalletId, Job>()

    operator fun invoke(userWalletId: UserWalletId) {
        activeSyncJobs[userWalletId]?.cancel()
        activeSyncJobs[userWalletId] = appCoroutineScope.launch {
            try {
                tokenSyncRepository.runSync(userWalletId)
                applyDiscoveredTokens(userWalletId)
            } catch (e: Exception) {
                Timber.e(e, "Token sync failed for wallet: $userWalletId")
            } finally {
                activeSyncJobs.remove(userWalletId)
            }
        }
    }

    suspend fun cancel(userWalletId: UserWalletId): Either<Throwable, Unit> = Either.catch {
        activeSyncJobs.remove(userWalletId)?.cancel()
        tokenSyncRepository.clearPendingFlag(userWalletId)
        tokenSyncRepository.clearDiscoveredTokens(userWalletId)
    }

    fun applyPendingSyncs() {
        appCoroutineScope.launch {
            try {
                val pendingIds = tokenSyncRepository.getPendingSyncWalletIds()
                for (walletId in pendingIds) {
                    val isApplied = applyDiscoveredTokens(walletId)
                    if (isApplied) {
                        tokenSyncRepository.clearPendingFlag(walletId)
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to apply pending syncs")
            }
        }
    }

    private suspend fun applyDiscoveredTokens(userWalletId: UserWalletId): Boolean {
        val currencies = tokenSyncRepository.getDiscoveredCurrencies(userWalletId)

        if (currencies.isEmpty()) return true

        val accountId = AccountId.forMainCryptoPortfolio(userWalletId)
        return manageCryptoCurrenciesUseCase(
            accountId = accountId,
            add = currencies,
        ).fold(
            ifRight = {
                tokenSyncRepository.clearDiscoveredTokens(userWalletId)
                true
            },
            ifLeft = { error ->
                Timber.e("Failed to apply discovered tokens for wallet: $userWalletId, error: $error")
                false
            },
        )
    }
}