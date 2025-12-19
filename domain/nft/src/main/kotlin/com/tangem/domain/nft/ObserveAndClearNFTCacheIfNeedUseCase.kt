package com.tangem.domain.nft

import com.tangem.domain.account.featuretoggle.AccountsFeatureToggles
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.account.supplier.SingleAccountListSupplier
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.nft.utils.NFTCleaner
import com.tangem.domain.tokens.repository.CurrenciesRepository
import kotlinx.coroutines.flow.*

class ObserveAndClearNFTCacheIfNeedUseCase(
    private val nftCleaner: NFTCleaner,
    private val currenciesRepository: CurrenciesRepository,
    private val accountsFeatureToggles: AccountsFeatureToggles,
    private val singleAccountListSupplier: SingleAccountListSupplier,
) {

    operator fun invoke(userWalletId: UserWalletId): Flow<Set<Network>> = getCryptoCurrencies(userWalletId)
        .map { it.map(CryptoCurrency::network) }
        .mapDiff { old, new ->
            // calculate networks sets difference to determine which networks were removed
            old.toSet() - new.toSet()
        }
        .distinctUntilChanged()
        .onEach { removedNetworks ->
            if (removedNetworks.isNotEmpty()) {
                nftCleaner(userWalletId, removedNetworks)
            }
        }

    private fun getCryptoCurrencies(userWalletId: UserWalletId): Flow<Collection<CryptoCurrency>> {
        return if (accountsFeatureToggles.isFeatureEnabled) {
            singleAccountListSupplier(userWalletId).map(AccountList::flattenCurrencies)
        } else {
            currenciesRepository.getWalletCurrenciesUpdates(userWalletId)
        }
    }

    private fun <T, R> Flow<T>.mapDiff(diff: (old: T, new: T) -> R): Flow<R> = flow {
        var previous: T? = null
        collect { current ->
            val prev = previous
            if (prev != null) {
                emit(diff(prev, current))
            }
            previous = current
        }
    }
}