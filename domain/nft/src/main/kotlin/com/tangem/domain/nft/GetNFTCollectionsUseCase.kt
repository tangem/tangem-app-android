package com.tangem.domain.nft

import com.tangem.domain.nft.models.NFTCollections
import com.tangem.domain.nft.repository.NFTRepository
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest

class GetNFTCollectionsUseCase(
    private val currenciesRepository: CurrenciesRepository,
    private val nftRepository: NFTRepository,
) {

    @OptIn(ExperimentalCoroutinesApi::class)
    fun launch(userWalletId: UserWalletId): Flow<List<NFTCollections>> = currenciesRepository
        .getWalletCurrenciesUpdates(userWalletId)
        .flatMapLatest {
            val networks = it
                .map { cryptoCurrency -> cryptoCurrency.network }
                .distinct()
            nftRepository.observeCollections(userWalletId, networks)
        }
}