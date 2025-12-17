package com.tangem.domain.nft

import com.tangem.domain.account.featuretoggle.AccountsFeatureToggles
import com.tangem.domain.account.supplier.SingleAccountListSupplier
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.nft.models.NFTCollections
import com.tangem.domain.nft.models.WalletNFTCollections
import com.tangem.domain.nft.repository.NFTRepository
import com.tangem.domain.tokens.repository.CurrenciesRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

class GetNFTCollectionsUseCase(
    private val currenciesRepository: CurrenciesRepository,
    private val nftRepository: NFTRepository,
    private val singleAccountListSupplier: SingleAccountListSupplier,
    private val accountsFeatureToggles: AccountsFeatureToggles,
) {

    @Deprecated("Use invokeForAccounts instead")
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(userWalletId: UserWalletId): Flow<List<NFTCollections>> =
        if (accountsFeatureToggles.isFeatureEnabled) {
            invokeForAccounts(userWalletId).map { it.flattenCollections }
        } else {
            currenciesRepository
                .getWalletCurrenciesUpdates(userWalletId)
                .flatMapLatest {
                    nftCollections(userWalletId, it)
                }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun invokeForAccounts(userWalletId: UserWalletId): Flow<WalletNFTCollections> {
        fun Account.flowOfNFTCollections(): Flow<Pair<Account, List<NFTCollections>>> {
            val currencies = (this as? Account.CryptoPortfolio)?.cryptoCurrencies.orEmpty()

            return nftCollections(userWalletId = userWalletId, cryptoCurrencies = currencies.toList())
                .map { nfts -> this to nfts }
        }

        return singleAccountListSupplier(userWalletId)
            .mapLatest { statusList -> statusList.accounts.map { it.flowOfNFTCollections() } }
            .flatMapLatest { flows -> combine(flows) { WalletNFTCollections(it.toMap()) } }
    }

    private fun nftCollections(
        userWalletId: UserWalletId,
        cryptoCurrencies: List<CryptoCurrency>,
    ): Flow<List<NFTCollections>> {
        val networks = cryptoCurrencies
            .map { cryptoCurrency -> cryptoCurrency.network }
            .distinct()
        return nftRepository.observeCollections(userWalletId, networks)
    }
}