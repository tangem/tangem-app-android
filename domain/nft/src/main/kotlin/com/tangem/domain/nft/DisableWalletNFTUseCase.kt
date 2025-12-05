package com.tangem.domain.nft

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.nft.utils.NFTCleaner
import com.tangem.domain.tokens.MultiWalletCryptoCurrenciesProducer
import com.tangem.domain.tokens.MultiWalletCryptoCurrenciesSupplier
import com.tangem.domain.wallets.repository.WalletsRepository

class DisableWalletNFTUseCase(
    private val walletsRepository: WalletsRepository,
    private val multiWalletCryptoCurrenciesSupplier: MultiWalletCryptoCurrenciesSupplier,
    private val nftCleaner: NFTCleaner,
) {

    suspend operator fun invoke(userWalletId: UserWalletId) {
        walletsRepository.disableNFT(userWalletId)

        val currencies = multiWalletCryptoCurrenciesSupplier.getSyncOrNull(
            params = MultiWalletCryptoCurrenciesProducer.Params(userWalletId),
        )
            .orEmpty()

        val networks = currencies.mapTo(destination = hashSetOf(), transform = CryptoCurrency::network)
        nftCleaner(userWalletId, networks)
    }
}