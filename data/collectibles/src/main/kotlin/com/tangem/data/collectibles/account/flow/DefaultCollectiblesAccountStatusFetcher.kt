package com.tangem.data.collectibles.account.flow

import arrow.core.Either
import arrow.core.getOrElse
import com.tangem.data.collectibles.account.store.CollectiblesAccountStore
import com.tangem.data.common.account.WalletAccountsFetcher
import com.tangem.datasource.api.tangemTech.models.account.WalletAccountDTO
import com.tangem.datasource.api.tangemTech.models.account.isCollectibles
import com.tangem.domain.collectibles.account.derivation.CollectiblesOwnerDeriver
import com.tangem.domain.collectibles.account.flow.CollectiblesAccountStatusFetcher
import com.tangem.domain.collectibles.account.repository.CollectiblesAccountRepository
import com.tangem.domain.collectibles.delegate.CollectiblesSessionKey
import com.tangem.domain.collectibles.vault.repository.GachaVaultRepository
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.common.wallets.getSyncOrNull
import com.tangem.domain.core.utils.catchOn
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.account.CollectiblesAccountStatusValue
import com.tangem.domain.models.account.CollectiblesVaultBalance
import com.tangem.domain.models.account.isAccountCreated
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.wallet.isLocked
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.logging.TangemLogger
import javax.inject.Inject

@Suppress("LongParameterList")
internal class DefaultCollectiblesAccountStatusFetcher @Inject constructor(
    private val collectiblesAccountStore: CollectiblesAccountStore,
    private val collectiblesAccountRepository: CollectiblesAccountRepository,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val collectiblesOwnerDeriver: CollectiblesOwnerDeriver,
    private val gachaVaultRepository: GachaVaultRepository,
    private val sessionKey: CollectiblesSessionKey,
    private val walletAccountsFetcher: WalletAccountsFetcher,
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
        val stored = collectiblesAccountStore.getSyncOrNull(userWalletId).takeIf { it.isAccountCreated }

        if (stored == null && !isListedOnBackend(userWalletId)) return

        val userWallet = userWalletsListRepository.getSyncOrNull(userWalletId)
            ?: return markUnrefreshed(userWalletId = userWalletId)

        if (userWallet.isLocked) return markUnrefreshed(userWalletId = userWalletId)

        if (collectiblesOwnerDeriver.storedOwner(userWalletId) == null) return

        val accountState = resolveAccountState(userWallet = userWallet, currentState = stored) ?: return

        if (stored == null) {
            // Onboarding may have recorded the account during the vault read; its record is newer than this read
            if (collectiblesAccountStore.getSyncOrNull(userWalletId).isAccountCreated) return

            collectiblesAccountStore.store(userWalletId = userWalletId, value = accountState)

            if (accountState is CollectiblesAccountStatusValue.NotOnboarded) return
        }

        val balance = resolveBalance(userWallet, accountState.balance)

        // A relink that finished during the reads above has already recorded its result, which outranks this one
        collectiblesAccountStore.update(userWalletId) { current ->
            val refreshed = if (collectiblesAccountRepository.isRelinkPending(userWalletId)) current else accountState
            balance?.let { refreshed.copySealed(source = StatusSource.ACTUAL, vaultBalance = it) } ?: refreshed
        }

        if (balance == null) markUnrefreshed(userWalletId = userWalletId)
    }

    /**
     * Whether the vault's `session_key` is this device's key. A failed read changes nothing, and neither does another
     * key while this device's relink may still land. A missing vault keeps a record, while an account only the
     * backend lists is then no account.
     */
    private suspend fun resolveAccountState(
        userWallet: UserWallet,
        currentState: CollectiblesAccountStatusValue?,
    ): CollectiblesAccountStatusValue? {
        val accountId = AccountId.forCollectiblesAccount(userWallet.walletId)
        val localSessionKey = sessionKey.getPublicKeyOrNull(accountId)?.base58
        val vaultSessionKey = gachaVaultRepository.getVaultSessionKey(userWallet).getOrElse { error ->
            logger.e("Failed to read the collectibles vault session key of ${userWallet.walletId}: $error")
            return currentState
        }

        return when {
            vaultSessionKey == null -> currentState ?: CollectiblesAccountStatusValue.NotOnboarded
            vaultSessionKey == localSessionKey -> {
                collectiblesAccountRepository.clearRelinkPending(userWallet.walletId)
                CollectiblesAccountStatusValue.Onboarded(
                    source = StatusSource.ACTUAL,
                    vaultBalance = currentState?.balance,
                )
            }
            collectiblesAccountRepository.isRelinkPending(userWallet.walletId) -> currentState
            else -> CollectiblesAccountStatusValue.UpdateRequired(
                source = StatusSource.ACTUAL,
                vaultBalance = currentState?.balance,
            )
        }
    }

    private suspend fun isListedOnBackend(userWalletId: UserWalletId): Boolean {
        return walletAccountsFetcher.getSaved(userWalletId)?.accounts.orEmpty().any(WalletAccountDTO::isCollectibles)
    }

    /** The vault's own amount, which for an empty vault is an honest zero. `null` is a read that did not answer. */
    private suspend fun resolveBalance(
        userWallet: UserWallet,
        cached: CollectiblesVaultBalance?,
    ): CollectiblesVaultBalance? = gachaVaultRepository.getVaultBalance(userWallet, cached = cached)
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