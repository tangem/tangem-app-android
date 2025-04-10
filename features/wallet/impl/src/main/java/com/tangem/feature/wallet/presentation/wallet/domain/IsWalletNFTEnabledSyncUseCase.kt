package com.tangem.feature.wallet.presentation.wallet.domain

import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.domain.wallets.repository.WalletsRepository
import com.tangem.features.nft.NFTFeatureToggles
import kotlinx.coroutines.flow.firstOrNull

class IsWalletNFTEnabledSyncUseCase(
    private val walletsRepository: WalletsRepository,
    private val nftFeatureToggles: NFTFeatureToggles,
) {

    suspend operator fun invoke(userWalletId: UserWalletId): Boolean = if (nftFeatureToggles.isNFTEnabled) {
        walletsRepository
            .nftEnabledStatuses()
            .firstOrNull()
            ?.let { it[userWalletId] }
            ?: false
    } else {
        false
    }
}