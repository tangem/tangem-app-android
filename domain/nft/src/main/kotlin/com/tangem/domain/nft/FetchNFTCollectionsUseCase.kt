package com.tangem.domain.nft

import com.tangem.domain.nft.repository.NFTRepository
import com.tangem.domain.tokens.MultiWalletCryptoCurrenciesProducer
import com.tangem.domain.tokens.MultiWalletCryptoCurrenciesSupplier
import com.tangem.domain.tokens.TokensFeatureToggles
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.wallets.models.UserWalletId

class FetchNFTCollectionsUseCase(
    private val currenciesRepository: CurrenciesRepository,
    private val multiWalletCryptoCurrenciesSupplier: MultiWalletCryptoCurrenciesSupplier,
    private val tokensFeatureToggles: TokensFeatureToggles,
    private val nftRepository: NFTRepository,
) {

    suspend operator fun invoke(userWalletId: UserWalletId) {
        val currencies = if (tokensFeatureToggles.isWalletBalanceFetcherEnabled) {
            multiWalletCryptoCurrenciesSupplier.getSyncOrNull(
                params = MultiWalletCryptoCurrenciesProducer.Params(userWalletId),
            )
                .orEmpty()
        } else {
            currenciesRepository.getMultiCurrencyWalletCurrenciesSync(userWalletId)
        }

        nftRepository.refreshCollections(userWalletId, currencies.map { it.network }.distinct())
    }
}