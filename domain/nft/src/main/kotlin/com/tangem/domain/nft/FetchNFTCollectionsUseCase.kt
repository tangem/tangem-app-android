package com.tangem.domain.nft

import com.tangem.domain.nft.repository.NFTRepository
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.wallets.models.UserWalletId

class FetchNFTCollectionsUseCase(
    private val currenciesRepository: CurrenciesRepository,
    private val nftRepository: NFTRepository,
) {

    suspend operator fun invoke(userWalletId: UserWalletId) {
        val currencies = currenciesRepository.getMultiCurrencyWalletCurrenciesSync(userWalletId)
        nftRepository.refreshCollections(userWalletId, currencies.map { it.network }.distinct())
    }
}