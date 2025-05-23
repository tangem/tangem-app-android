package com.tangem.domain.nft

import arrow.core.Either
import com.tangem.domain.nft.models.NFTAsset
import com.tangem.domain.nft.models.NFTSalePrice
import com.tangem.domain.nft.repository.NFTRepository
import com.tangem.domain.quotes.single.SingleQuoteProducer
import com.tangem.domain.quotes.single.SingleQuoteSupplier
import com.tangem.domain.tokens.model.Quote
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GetNFTPriceUseCase(
    private val nftRepository: NFTRepository,
    private val singleQuoteSupplier: SingleQuoteSupplier,
) {

    suspend operator fun invoke(userWalletId: UserWalletId, nftAsset: NFTAsset): Either<Throwable, Flow<NFTSalePrice>> {
        return Either.catch {
            val nftCurrency = nftRepository.getNFTCurrency(nftAsset.network)
            val rawId = nftCurrency.id.rawCurrencyId ?: error("Invalid nft currency id")

            singleQuoteSupplier(
                params = SingleQuoteProducer.Params(rawCurrencyId = rawId),
            ).map { quote ->
                val nftPrice = nftRepository.getNFTSalePrice(
                    userWalletId = userWalletId,
                    network = nftAsset.network,
                    collectionId = nftAsset.collectionId,
                    assetId = nftAsset.id,
                )

                val quoteValue = quote as? Quote.Value

                if (nftPrice !is NFTSalePrice.Value) {
                    nftPrice
                } else {
                    nftPrice.copy(fiatValue = quoteValue?.fiatRate?.multiply(nftPrice.value))
                }
            }
        }
    }
}