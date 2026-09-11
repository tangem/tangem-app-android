package com.tangem.data.collectibles.account.flow

import arrow.core.Either
import com.tangem.data.collectibles.account.store.CollectiblesAccountStatusStore
import com.tangem.domain.collectibles.account.derivation.CollectiblesOwnerDeriver
import com.tangem.domain.collectibles.account.flow.CollectiblesAccountStatusFetcher
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.common.wallets.getSyncOrNull
import com.tangem.domain.core.utils.catchOn
import com.tangem.domain.models.account.CollectiblesAccountStatusValue
import com.tangem.domain.models.account.isAccountCreated
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.wallet.isLocked
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.logging.TangemLogger
import javax.inject.Inject

internal class DefaultCollectiblesAccountStatusFetcher @Inject constructor(
    private val collectiblesAccountStatusStore: CollectiblesAccountStatusStore,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val collectiblesOwnerDeriver: CollectiblesOwnerDeriver,
    private val dispatchers: CoroutineDispatcherProvider,
) : CollectiblesAccountStatusFetcher {

    override suspend fun invoke(params: CollectiblesAccountStatusFetcher.Params): Either<Throwable, Unit> {
        return Either.catchOn(dispatchers.default) {
            when (resolve(userWalletId = params.userWalletId)) {
                Resolution.NotDerived -> collectiblesAccountStatusStore.store(
                    userWalletId = params.userWalletId,
                    value = CollectiblesAccountStatusValue.MissedDerivation,
                )
                Resolution.Unreadable -> markUnrefreshed(userWalletId = params.userWalletId)
            }
        }.onLeft { error ->
            logger.e("Failed to refresh the collectibles vault of ${params.userWalletId}", error)
            markUnrefreshed(userWalletId = params.userWalletId)
        }
    }

    private suspend fun resolve(userWalletId: UserWalletId): Resolution {
        if (!collectiblesAccountStatusStore.getSyncOrNull(userWalletId).isAccountCreated) return Resolution.Unreadable

        val userWallet = userWalletsListRepository.getSyncOrNull(userWalletId) ?: return Resolution.Unreadable
        if (userWallet.isLocked) return Resolution.Unreadable

        if (collectiblesOwnerDeriver.storedOwner(userWalletId) == null) return Resolution.NotDerived

        // TODO: the vault's USDC waits on `getCollectiblesVaultUsdcBalance()` in the blockchain SDK — nothing in
        //  the app can read it, so the amount stays unknown and the row shows a dash rather than a zero
        return Resolution.Unreadable
    }

    private suspend fun markUnrefreshed(userWalletId: UserWalletId) {
        runSuspendCatching { collectiblesAccountStatusStore.markUnrefreshed(userWalletId = userWalletId) }
            .onFailure { logger.e("Failed to mark the collectibles vault of $userWalletId as un-refreshed", it) }
    }

    private sealed interface Resolution {

        /** The vault exists but its owner key is not on this device, so there is no address to read. */
        data object NotDerived : Resolution

        /** Nothing could be found out — whatever is cached stays, marked stale. */
        data object Unreadable : Resolution
    }

    private companion object {
        val logger = TangemLogger.withTag("CollectiblesAccountStatusFetcher")
    }
}