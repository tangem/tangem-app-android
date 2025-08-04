package com.tangem.domain.nft

import com.tangem.domain.nft.models.NFTNetworks
import com.tangem.domain.nft.repository.NFTRepository
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GetNFTNetworksUseCase(
    private val currenciesRepository: CurrenciesRepository,
    private val nftRepository: NFTRepository,
) {
    operator fun invoke(userWalletId: UserWalletId): Flow<NFTNetworks> = currenciesRepository
        .getWalletCurrenciesUpdates(userWalletId)
        .map { cryptoCurrencies ->
            val availableNetworks = cryptoCurrencies
                .map { cryptoCurrency -> cryptoCurrency.network }
                .filter { nftRepository.isNFTSupported(userWalletId, it) }
                .sortedBy { it.name }

            val unavailableNetworks = nftRepository
                .getNFTSupportedNetworks(userWalletId)
                .filter { supportedNetwork -> availableNetworks.none { it.id == supportedNetwork.id } }
                .sortedBy { it.name }

            NFTNetworks(
                availableNetworks = availableNetworks,
                unavailableNetworks = unavailableNetworks,
            )
        }
}