package com.tangem.domain.nft

import com.tangem.domain.nft.repository.NFTRepository
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.domain.wallets.repository.WalletsRepository

class DisableWalletNFTUseCase(
    private val walletsRepository: WalletsRepository,
    private val nftRepository: NFTRepository,
    private val currenciesRepository: CurrenciesRepository,
) {

    suspend operator fun invoke(userWalletId: UserWalletId) {
        walletsRepository.disableNFT(userWalletId)

        val currencies = currenciesRepository.getMultiCurrencyWalletCachedCurrenciesSync(userWalletId)
        val networks = currencies.map { it.network }
        nftRepository.clearCache(userWalletId, networks)
    }
}