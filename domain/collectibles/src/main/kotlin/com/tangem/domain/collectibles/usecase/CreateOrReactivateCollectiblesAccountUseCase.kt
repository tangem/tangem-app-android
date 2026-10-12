package com.tangem.domain.collectibles.usecase

import arrow.core.Either
import arrow.core.raise.either
import com.tangem.domain.collectibles.delegate.CollectiblesHotDelegate
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
import com.tangem.utils.logging.TangemLogger
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

        if (accountStatus is CollectiblesAccountStatusValue.Onboarded) {
            TangemLogger.d("create account: the account is ready to use, nothing to do")
            return@either
        }

        val limits = GachaVaultLimits.Default
        val accountId = AccountId.forCollectiblesAccount(userWalletId = userWallet.walletId)
        val delegate = hotDelegate.provision(accountId)
            .onLeft { TangemLogger.d("create account: hot delegate provisioning failed: $it") }
            .onRight { TangemLogger.d("create account: hot delegate ready") }
            .bind()

        val storedOwner = ownerDeriver.storedOwner(userWallet.walletId)

        if (storedOwner == null) {
            TangemLogger.d("create account: -> No locally stored Solana address, deriveOwnerAndSetUpVault")
            gachaVaultRepository.deriveOwnerAndSetUpVault(userWallet, delegate, limits)
                .onLeft { TangemLogger.d("create account: deriveOwnerAndSetUpVault failed: $it") }
                .bind()
        } else {
            TangemLogger.d("create account: -> Solana address of the key stored locally, initOrUpdateVault")
            gachaVaultRepository.initOrUpdateVault(userWallet, delegate, limits)
                .onLeft { TangemLogger.d("create account: initOrUpdateVault failed: $it") }
                .bind()
        }

        accountRepository.saveAccount(
            userWalletId = userWallet.walletId,
            status = CollectiblesAccountStatusValue.Onboarded(source = StatusSource.ACTUAL),
        )
        TangemLogger.d("create account: the collectibles account is created, the gacha account is ready to use")

        appScope.launch {
            accountStatusFetcher(CollectiblesAccountStatusFetcher.Params(userWallet.walletId))
        }
    }
}