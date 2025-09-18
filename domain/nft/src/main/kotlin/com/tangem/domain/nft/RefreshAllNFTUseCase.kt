package com.tangem.domain.nft

import arrow.core.Either
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.nft.repository.NFTRepository
import com.tangem.domain.tokens.MultiWalletCryptoCurrenciesProducer
import com.tangem.domain.tokens.MultiWalletCryptoCurrenciesSupplier

class RefreshAllNFTUseCase(
    private val multiWalletCryptoCurrenciesSupplier: MultiWalletCryptoCurrenciesSupplier,
    private val nftRepository: NFTRepository,
) {

    suspend operator fun invoke(userWalletId: UserWalletId): Either<Throwable, Unit> = Either.catch {
        val currencies = multiWalletCryptoCurrenciesSupplier.getSyncOrNull(
            params = MultiWalletCryptoCurrenciesProducer.Params(userWalletId),
        )
            .orEmpty()

        nftRepository.refreshAll(userWalletId, currencies.map { it.network }.distinct())
    }
}