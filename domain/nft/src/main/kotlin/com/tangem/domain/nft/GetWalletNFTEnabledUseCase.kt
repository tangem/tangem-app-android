package com.tangem.domain.nft

import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.domain.wallets.repository.WalletsRepository
import kotlinx.coroutines.flow.Flow

class GetWalletNFTEnabledUseCase(
    private val walletsRepository: WalletsRepository,
) {

    operator fun invoke(userWalletId: UserWalletId): Flow<Boolean> = walletsRepository
        .nftEnabledStatus(userWalletId)
}