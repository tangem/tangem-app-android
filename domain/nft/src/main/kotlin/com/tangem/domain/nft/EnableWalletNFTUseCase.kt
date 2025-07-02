package com.tangem.domain.nft

import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.domain.wallets.repository.WalletsRepository

class EnableWalletNFTUseCase(
    private val walletsRepository: WalletsRepository,
) {

    suspend operator fun invoke(userWalletId: UserWalletId) {
        walletsRepository.enableNFT(userWalletId)
    }
}