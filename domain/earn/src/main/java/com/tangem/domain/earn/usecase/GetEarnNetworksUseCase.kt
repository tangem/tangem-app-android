package com.tangem.domain.earn.usecase

import arrow.core.Either
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.account.supplier.MultiAccountListSupplier
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.earn.repository.EarnRepository
import com.tangem.domain.models.earn.EarnNetwork
import com.tangem.domain.models.earn.EarnNetworks
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.isLocked
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Observes earn networks with [EarnNetwork.isAdded] enriched from user's active (non-archived)
 * accounts via [multiAccountListSupplier]. Single entry point for all/mine filtering.
 *
 * Uses [MultiAccountListSupplier] so that only networks from active accounts are considered;
 * archived accounts are not included in [AccountList.accounts].
 */
class GetEarnNetworksUseCase(
    private val earnRepository: EarnRepository,
    private val multiAccountListSupplier: MultiAccountListSupplier,
    private val userWalletsListRepository: UserWalletsListRepository,
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

    private fun observeMyNetworkIds(): Flow<Set<String>> {
        return combine(
            multiAccountListSupplier(),
            userWalletsListRepository.userWallets,
        ) { accountLists, wallets ->
            val unlockedWalletsId = wallets
                .orEmpty()
                .filterNot(UserWallet::isLocked)
                .mapTo(HashSet()) { it.walletId }

            if (unlockedWalletsId.isEmpty()) {
                return@combine emptySet()
            }

            accountLists
                .filter { it.userWalletId in unlockedWalletsId }
                .flatMap(AccountList::flattenCurrencies)
                .mapTo(HashSet()) { it.network.backendId }
        }
    }
}