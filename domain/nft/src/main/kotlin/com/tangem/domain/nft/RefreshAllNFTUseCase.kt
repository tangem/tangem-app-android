package com.tangem.domain.nft

import com.tangem.domain.nft.repository.NFTRepository
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.wallets.models.UserWalletId

class RefreshAllNFTUseCase(
    private val currenciesRepository: CurrenciesRepository,
    private val nftRepository: NFTRepository,
) {

    suspend operator fun invoke(userWalletId: UserWalletId) {
        val currencies = currenciesRepository.getMultiCurrencyWalletCurrenciesSync(userWalletId)
        nftRepository.refreshAll(userWalletId, currencies.map { it.network }.distinct())
    }
}