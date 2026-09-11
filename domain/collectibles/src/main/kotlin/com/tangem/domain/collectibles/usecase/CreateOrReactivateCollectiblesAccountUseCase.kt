package com.tangem.domain.collectibles.usecase

import arrow.core.Either
import arrow.core.raise.either
import com.tangem.domain.collectibles.delegate.CollectiblesHotDelegate
import com.tangem.domain.collectibles.delegate.HotDelegatePublicKey
import com.tangem.domain.collectibles.account.derivation.CollectiblesOwnerDeriver
import com.tangem.domain.collectibles.account.flow.CollectiblesAccountStatusFetcher
import com.tangem.domain.collectibles.vault.repository.GachaVaultRepository
import com.tangem.domain.collectibles.CollectiblesAccountError
import com.tangem.domain.collectibles.account.repository.CollectiblesAccountRepository
import com.tangem.domain.collectibles.vault.GachaVaultLimits
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.account.CollectiblesAccountStatusValue
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.utils.coroutines.AppCoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Creates a wallet's collectibles account: the local delegate, the on-chain vault, the local record of it.
 *
 * Never more than one tap, and none at all for a mobile wallet or for an account that already exists. What the
 * device already has decides which operation that tap signs.
 */
class CreateOrReactivateCollectiblesAccountUseCase @Inject constructor(
    private val hotDelegate: CollectiblesHotDelegate,
    private val ownerDeriver: CollectiblesOwnerDeriver,
    private val gachaVaultRepository: GachaVaultRepository,
    private val accountRepository: CollectiblesAccountRepository,
    private val accountStatusFetcher: CollectiblesAccountStatusFetcher,
    private val appScope: AppCoroutineScope,
) {

    suspend operator fun invoke(userWallet: UserWallet): Either<CollectiblesAccountError, Unit> = either {
        val accountStatus = accountRepository.getStatusSync(userWallet.walletId)

        if (accountStatus is CollectiblesAccountStatusValue.Onboarded) return@either

        val limits = GachaVaultLimits.Default
        val delegate = resolveDelegate(userWallet).bind()

        if (ownerDeriver.storedOwner(userWallet.walletId) == null) {
            gachaVaultRepository.deriveOwnerAndSetUpVault(userWallet, delegate, limits).bind()
        } else {
            gachaVaultRepository.initOrUpdateVault(userWallet, delegate, limits).bind()
        }

        accountRepository.saveAccount(
            userWalletId = userWallet.walletId,
            status = CollectiblesAccountStatusValue.Onboarded(source = StatusSource.ACTUAL),
        )

        appScope.launch {
            accountStatusFetcher(CollectiblesAccountStatusFetcher.Params(userWallet.walletId))
        }
    }

    /** Costs no card tap either way: the delegate is a local ed25519 keypair, created on first use. */
    private suspend fun resolveDelegate(
        userWallet: UserWallet,
    ): Either<CollectiblesAccountError, HotDelegatePublicKey> = either {
        val accountId = AccountId.forCollectiblesAccount(userWalletId = userWallet.walletId)

        hotDelegate.getPublicKey(accountId)
            ?: hotDelegate.provision(accountId).bind()
    }
}