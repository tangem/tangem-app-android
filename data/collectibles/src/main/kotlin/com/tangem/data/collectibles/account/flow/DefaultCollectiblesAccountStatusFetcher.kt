package com.tangem.data.collectibles.account.flow

import arrow.core.Either
import arrow.core.getOrElse
import com.tangem.data.collectibles.account.store.CollectiblesAccountStore
import com.tangem.domain.collectibles.account.derivation.CollectiblesOwnerDeriver
import com.tangem.domain.collectibles.account.flow.CollectiblesAccountStatusFetcher
import com.tangem.domain.collectibles.vault.repository.GachaVaultRepository
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.common.wallets.getSyncOrNull
import com.tangem.domain.core.utils.catchOn
import com.tangem.domain.models.account.CollectiblesAccountStatusValue
import com.tangem.domain.models.account.CollectiblesVaultBalance
import com.tangem.domain.models.account.isAccountCreated
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.wallet.isLocked
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.logging.TangemLogger
import javax.inject.Inject

internal class DefaultCollectiblesAccountStatusFetcher @Inject constructor(
    private val collectiblesAccountStore: CollectiblesAccountStore,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val collectiblesOwnerDeriver: CollectiblesOwnerDeriver,
    private val gachaVaultRepository: GachaVaultRepository,
    private val dispatchers: CoroutineDispatcherProvider,
) : CollectiblesAccountStatusFetcher {

    override suspend fun invoke(params: CollectiblesAccountStatusFetcher.Params): Either<Throwable, Unit> {
        val userWalletId = params.userWalletId

        return Either.catchOn(dispatchers.default) { refresh(userWalletId = userWalletId) }
            .onLeft { error ->
                logger.e("Failed to refresh the collectibles vault of $userWalletId", error)
                markUnrefreshed(userWalletId = userWalletId)
            }
    }

    private suspend fun refresh(userWalletId: UserWalletId) {
        val stored = collectiblesAccountStore.getSyncOrNull(userWalletId)

        if (!stored.isAccountCreated) return markUnrefreshed(userWalletId = userWalletId)

        val userWallet = userWalletsListRepository.getSyncOrNull(userWalletId)
            ?: return markUnrefreshed(userWalletId = userWalletId)

        if (userWallet.isLocked) return markUnrefreshed(userWalletId = userWalletId)

        // TODO check whether this branch is still needed once accounts are synced from the backend
        if (collectiblesOwnerDeriver.storedOwner(userWalletId) == null) {
            return collectiblesAccountStore.store(
                userWalletId = userWalletId,
                value = CollectiblesAccountStatusValue.MissedDerivation,
            )
        }

        val balance = resolveBalance(userWallet, stored?.balance?.usdcToken)
            ?: return markUnrefreshed(userWalletId = userWalletId)

        collectiblesAccountStore.storeBalance(userWalletId = userWalletId, balance = balance)
    }

    /** The vault's own amount, which for an empty vault is an honest zero. `null` is a read that did not answer. */
    private suspend fun resolveBalance(
        userWallet: UserWallet,
        cachedToken: CryptoCurrency.Token?,
    ): CollectiblesVaultBalance? = gachaVaultRepository.getVaultBalance(userWallet, cachedToken = cachedToken)
        .getOrElse { error ->
            logger.e("Failed to read the collectibles vault balance of ${userWallet.walletId}: $error")
            null
        }

    private suspend fun markUnrefreshed(userWalletId: UserWalletId) {
        runSuspendCatching { collectiblesAccountStore.markUnrefreshed(userWalletId = userWalletId) }
            .onFailure { logger.e("Failed to mark the collectibles vault of $userWalletId as un-refreshed", it) }
    }

    private companion object {
        val logger = TangemLogger.withTag("CollectiblesAccountStatusFetcher")
    }
}