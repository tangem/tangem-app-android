package com.tangem.domain.earn.usecase

import arrow.core.Either
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.earn.repository.EarnRepository
import com.tangem.domain.models.earn.EarnNetwork
import com.tangem.domain.models.earn.EarnNetworks
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.isLocked
import com.tangem.domain.models.wallet.isMultiCurrency
import com.tangem.domain.networks.multi.MultiNetworkStatusProducer
import com.tangem.domain.networks.multi.MultiNetworkStatusSupplier
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

/**
 * Observes earn networks with [EarnNetwork.isAdded] enriched from user's wallets
 * via [multiNetworkStatusSupplier]. Single entry point for all/mine filtering.
 */
class GetEarnNetworksUseCase(
    private val earnRepository: EarnRepository,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val multiNetworkStatusSupplier: MultiNetworkStatusSupplier,
) {

    operator fun invoke(): Flow<EarnNetworks> {
        return combine(
            earnRepository.observeEarnNetworks(),
            observeMyNetworkIds(),
        ) { earn, myNetworkIds ->
            earn?.map { earnNetworks ->
                earnNetworks.map { network ->
                    network.copy(isAdded = network.networkId in myNetworkIds)
                }
            } ?: Either.Right(emptyList())
        }.distinctUntilChanged()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeMyNetworkIds(): Flow<Set<String>> {
        return userWalletsListRepository.userWallets
            .map { it.orEmpty() }
            .flatMapLatest { wallets ->
                val activeWallets = wallets
                    .filterNot(UserWallet::isLocked)
                    .filter(UserWallet::isMultiCurrency)
                if (activeWallets.isEmpty()) {
                    flowOf(emptySet())
                } else {
                    val flows = activeWallets.map { wallet ->
                        multiNetworkStatusSupplier(
                            MultiNetworkStatusProducer.Params(userWalletId = wallet.walletId),
                        ).map { statuses -> statuses.map { it.network.backendId }.toSet() }
                    }
                    combine(flows) { arrays -> arrays.flatMap { it }.toSet() }
                }
            }
    }
}