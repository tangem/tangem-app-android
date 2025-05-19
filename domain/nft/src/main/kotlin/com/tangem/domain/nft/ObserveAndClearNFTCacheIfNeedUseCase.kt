package com.tangem.domain.nft

import com.tangem.domain.nft.repository.NFTRepository
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.flow.*

class ObserveAndClearNFTCacheIfNeedUseCase(
    private val nftRepository: NFTRepository,
    private val currenciesRepository: CurrenciesRepository,
) {
    operator fun invoke(userWalletId: UserWalletId): Flow<Set<Network>> = currenciesRepository
        .getWalletCurrenciesUpdates(userWalletId)
        .map { it.map(CryptoCurrency::network) }
        .mapDiff { old, new ->
            // calculate networks sets difference to determine which networks were removed
            old.toSet() - new.toSet()
        }
        .distinctUntilChanged()
        .onEach { removedNetworks ->
            if (removedNetworks.isNotEmpty()) {
                nftRepository.clearCache(userWalletId, removedNetworks.toList())
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