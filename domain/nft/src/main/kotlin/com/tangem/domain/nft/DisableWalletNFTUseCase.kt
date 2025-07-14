package com.tangem.domain.nft

import com.tangem.domain.nft.repository.NFTRepository
import com.tangem.domain.tokens.MultiWalletCryptoCurrenciesProducer
import com.tangem.domain.tokens.MultiWalletCryptoCurrenciesSupplier
import com.tangem.domain.tokens.TokensFeatureToggles
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.domain.wallets.repository.WalletsRepository

class DisableWalletNFTUseCase(
    private val walletsRepository: WalletsRepository,
    private val nftRepository: NFTRepository,
    private val currenciesRepository: CurrenciesRepository,
    private val multiWalletCryptoCurrenciesSupplier: MultiWalletCryptoCurrenciesSupplier,
    private val tokensFeatureToggles: TokensFeatureToggles,
) {

    suspend operator fun invoke(userWalletId: UserWalletId) {
        walletsRepository.disableNFT(userWalletId)

        val currencies = if (tokensFeatureToggles.isWalletBalanceFetcherEnabled) {
            multiWalletCryptoCurrenciesSupplier.getSyncOrNull(
                params = MultiWalletCryptoCurrenciesProducer.Params(userWalletId),
            )
                .orEmpty()
        } else {
            currenciesRepository.getMultiCurrencyWalletCachedCurrenciesSync(userWalletId)
        }

        val networks = currencies.map { it.network }
        nftRepository.clearCache(userWalletId, networks)
    }
}