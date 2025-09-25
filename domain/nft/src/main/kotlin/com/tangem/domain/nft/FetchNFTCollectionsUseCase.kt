package com.tangem.domain.nft

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.nft.repository.NFTRepository
import com.tangem.domain.tokens.MultiWalletCryptoCurrenciesProducer
import com.tangem.domain.tokens.MultiWalletCryptoCurrenciesSupplier

class FetchNFTCollectionsUseCase(
    private val multiWalletCryptoCurrenciesSupplier: MultiWalletCryptoCurrenciesSupplier,
    private val nftRepository: NFTRepository,
) {

    suspend operator fun invoke(userWalletId: UserWalletId) {
        val currencies = multiWalletCryptoCurrenciesSupplier.getSyncOrNull(
            params = MultiWalletCryptoCurrenciesProducer.Params(userWalletId),
        )
            .orEmpty()

        nftRepository.refreshCollections(userWalletId, currencies.map { it.network }.distinct())
    }
}