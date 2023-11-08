package com.tangem.domain.tokens

import com.tangem.domain.tokens.model.Network
import com.tangem.domain.tokens.repository.NetworksRepository
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.coroutineScope

/**
 * Use case responsible for fetching current pending transactions
 *
 * @param networksRepository The repository for retrieving network-related data.
 */
class FetchPendingTransactionsUseCase(
    private val networksRepository: NetworksRepository,
) {

    suspend operator fun invoke(userWalletId: UserWalletId, networks: Set<Network>) {
        coroutineScope {
            networksRepository.fetchNetworkPendingTransactions(userWalletId, networks)
        }
    }
}