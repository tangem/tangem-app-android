package com.tangem.domain.hotwallet

import com.tangem.domain.hotwallet.repository.HotWalletRepository
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged

class GetAccessCodeSkippedUseCase(
    private val hotWalletRepository: HotWalletRepository,
) {
    operator fun invoke(userWalletId: UserWalletId): Flow<Boolean> = hotWalletRepository
        .accessCodeSkipped(userWalletId)
        .distinctUntilChanged()
}