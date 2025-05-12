package com.tangem.domain.nft

import arrow.core.Either
import com.tangem.domain.nft.repository.NFTRepository
import com.tangem.domain.quotes.single.SingleQuoteFetcher
import com.tangem.domain.tokens.model.Network

class FetchNFTPriceUseCase(
    private val nftRepository: NFTRepository,
    private val singleQuoteFetcher: SingleQuoteFetcher,
) {

    suspend operator fun invoke(network: Network, appCurrencyId: String?): Either<Throwable, Unit> {
        return Either.catch {
            val nftCurrency = nftRepository.getNFTCurrency(network)
            val rawId = nftCurrency.id.rawCurrencyId ?: error("Invalid nft currency id")

            singleQuoteFetcher(
                params = SingleQuoteFetcher.Params(
                    rawCurrencyId = rawId,
                    appCurrencyId = appCurrencyId,
                ),
            )
        }
    }
}