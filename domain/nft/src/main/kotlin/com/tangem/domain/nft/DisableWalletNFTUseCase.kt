package com.tangem.domain.nft

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.nft.repository.NFTRepository
import com.tangem.domain.tokens.MultiWalletCryptoCurrenciesProducer
import com.tangem.domain.tokens.MultiWalletCryptoCurrenciesSupplier
import com.tangem.domain.wallets.repository.WalletsRepository

class DisableWalletNFTUseCase(
    private val walletsRepository: WalletsRepository,
    private val nftRepository: NFTRepository,
    private val multiWalletCryptoCurrenciesSupplier: MultiWalletCryptoCurrenciesSupplier,
) {

    suspend operator fun invoke(userWalletId: UserWalletId) {
        walletsRepository.disableNFT(userWalletId)

        val currencies = multiWalletCryptoCurrenciesSupplier.getSyncOrNull(
            params = MultiWalletCryptoCurrenciesProducer.Params(userWalletId),
        )
            .orEmpty()

        val networks = currencies.map { it.network }
        nftRepository.clearCache(userWalletId, networks)
    }
}