package com.tangem.domain.nft

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import com.tangem.domain.models.network.Network
import com.tangem.domain.nft.repository.NFTRepository
import com.tangem.domain.quotes.single.SingleQuoteStatusFetcher
import com.tangem.utils.logging.TangemLogger

class FetchNFTPriceUseCase(
    private val nftRepository: NFTRepository,
    private val singleQuoteStatusFetcher: SingleQuoteStatusFetcher,
) {

    suspend operator fun invoke(network: Network): Either<Throwable, Unit> = either {
        val nftCurrency = arrow.core.raise.catch(
            block = { nftRepository.getNFTCurrency(network) },
            catch = ::raise,
        )

        val rawId = ensureNotNull(nftCurrency.id.rawCurrencyId) {
            IllegalStateException("Invalid nft currency id: ${nftCurrency.id}")
        }

        singleQuoteStatusFetcher(params = SingleQuoteStatusFetcher.Params(rawCurrencyId = rawId)).bind()
    }
        .onLeft { TangemLogger.e(messageString = "Error fetching nft price", throwable = it) }
}