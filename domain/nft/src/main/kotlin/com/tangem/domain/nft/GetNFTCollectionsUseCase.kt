package com.tangem.domain.nft

import com.tangem.domain.account.featuretoggle.AccountsFeatureToggles
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountStatus
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
    private val singleAccountStatusListSupplier: SingleAccountStatusListSupplier,
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

    fun invokeForAccounts(userWalletId: UserWalletId): Flow<WalletNFTCollections> {
        fun AccountStatus.flowOfNFTCollections(): Flow<Pair<Account, List<NFTCollections>>> =
            nftCollections(userWalletId, this.flattenCurrencies().map { it.currency })
                .map { nfts -> this.account to nfts }

        return singleAccountStatusListSupplier(userWalletId)
            .mapLatest { statusList -> statusList.accountStatuses.map { it.flowOfNFTCollections() } }
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