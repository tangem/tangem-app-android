package com.tangem.feature.wallet.presentation.wallet.domain

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.repository.WalletsRepository
import kotlinx.coroutines.flow.firstOrNull

class IsWalletNFTEnabledSyncUseCase(
    private val walletsRepository: WalletsRepository,
) {

    suspend operator fun invoke(userWalletId: UserWalletId): Boolean {
        val isNFTEnabled = walletsRepository.nftEnabledStatuses()
            .firstOrNull()
            ?.get(userWalletId)

        return isNFTEnabled == true
    }
}