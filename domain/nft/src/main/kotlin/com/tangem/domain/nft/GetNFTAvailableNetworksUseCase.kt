package com.tangem.domain.nft

import com.tangem.domain.nft.repository.NFTRepository
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GetNFTAvailableNetworksUseCase(
    private val currenciesRepository: CurrenciesRepository,
    private val nftRepository: NFTRepository,
) {
    operator fun invoke(userWalletId: UserWalletId): Flow<List<Network>> = currenciesRepository
        .getWalletCurrenciesUpdates(userWalletId)
        .map { cryptoCurrencies ->
            cryptoCurrencies
                .map { cryptoCurrency -> cryptoCurrency.network }
                .distinct()
                .filter { nftRepository.isNFTSupported(it) }
        }
}