package com.tangem.domain.nft

import com.tangem.domain.account.supplier.SingleAccountListSupplier
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.nft.models.NFTCollections
import com.tangem.domain.nft.models.WalletNFTCollections
import com.tangem.domain.nft.repository.NFTRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

class GetNFTCollectionsUseCase(
    private val nftRepository: NFTRepository,
    private val singleAccountListSupplier: SingleAccountListSupplier,
) {

    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(userWalletId: UserWalletId): Flow<WalletNFTCollections> {
        return singleAccountListSupplier(userWalletId)
            .mapLatest { statusList ->
                statusList.accounts.filterIsInstance<Account.CryptoPortfolio>().mapNotNull(::flowOfNFTCollections)
            }
            .flatMapLatest { flows ->
                combine(flows) { WalletNFTCollections(it.toMap()) }
            }
    }

    private fun flowOfNFTCollections(
        account: Account.CryptoPortfolio,
    ): Flow<Pair<Account.CryptoPortfolio, List<NFTCollections>>>? {
        val currencies = account.cryptoCurrencies

        if (currencies.isEmpty()) return null

        return getNftCollections(userWalletId = account.userWalletId, cryptoCurrencies = currencies.toList())
            .map { nfts -> account to nfts }
    }

    private fun getNftCollections(
        userWalletId: UserWalletId,
        cryptoCurrencies: List<CryptoCurrency>,
    ): Flow<List<NFTCollections>> {
        val networks = cryptoCurrencies
            .map(CryptoCurrency::network)
            .distinct()

        if (networks.isEmpty()) return flowOf(emptyList())

        return nftRepository.observeCollections(userWalletId, networks)
    }
}