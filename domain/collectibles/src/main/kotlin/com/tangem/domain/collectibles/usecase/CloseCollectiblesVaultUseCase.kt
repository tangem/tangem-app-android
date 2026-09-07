package com.tangem.domain.collectibles.usecase

import arrow.core.Either
import com.tangem.domain.collectibles.account.repository.CollectiblesAccountRepository
import com.tangem.domain.collectibles.vault.repository.GachaVaultRepository
import com.tangem.domain.collectibles.CollectiblesAccountError
import com.tangem.domain.models.wallet.UserWallet
import javax.inject.Inject

class CloseCollectiblesVaultUseCase @Inject constructor(
    private val gachaVaultRepository: GachaVaultRepository,
    private val accountRepository: CollectiblesAccountRepository,
) {

    suspend operator fun invoke(userWallet: UserWallet): Either<CollectiblesAccountError, Unit> {
        return gachaVaultRepository.closeVault(userWallet)
            .onRight { accountRepository.clearAccount(userWallet.walletId) }
    }
}