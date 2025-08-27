package com.tangem.domain.nft

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.nft.repository.NFTRepository

class GetNFTCurrencyUseCase(private val nftRepository: NFTRepository) {

    operator fun invoke(network: Network): CryptoCurrency = nftRepository.getNFTCurrency(network)
}