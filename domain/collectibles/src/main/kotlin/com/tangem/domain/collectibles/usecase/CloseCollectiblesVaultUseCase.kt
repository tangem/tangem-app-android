package com.tangem.domain.collectibles.usecase

import arrow.core.Either
import com.tangem.domain.collectibles.account.repository.CollectiblesAccountRepository
import com.tangem.domain.collectibles.delegate.CollectiblesHotDelegate
import com.tangem.domain.collectibles.vault.repository.GachaVaultRepository
import com.tangem.domain.collectibles.CollectiblesAccountError
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.wallet.UserWallet
import javax.inject.Inject

class CloseCollectiblesVaultUseCase @Inject constructor(
    private val gachaVaultRepository: GachaVaultRepository,
    private val accountRepository: CollectiblesAccountRepository,
    private val hotDelegate: CollectiblesHotDelegate,
) {

    suspend operator fun invoke(userWallet: UserWallet): Either<CollectiblesAccountError, Unit> {
        return gachaVaultRepository.closeVault(userWallet)
            .onRight {
                hotDelegate.forget(AccountId.forCollectiblesAccount(userWalletId = userWallet.walletId))

                accountRepository.clearAccount(userWallet.walletId)
            }
    }
}