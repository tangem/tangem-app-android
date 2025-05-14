package com.tangem.domain.tokens

import arrow.core.Either
import com.tangem.domain.networks.repository.NetworksRepository
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.wallets.models.UserWalletId

/**
 * Use case responsible for fetching current pending transactions
 *
 * @param networksRepository The repository for retrieving network-related data.
 */
class FetchPendingTransactionsUseCase(
    private val networksRepository: NetworksRepository,
) {

    suspend operator fun invoke(userWalletId: UserWalletId, network: Network) = Either.catch {
        networksRepository.fetchPendingTransactions(userWalletId = userWalletId, network = network)
    }
}