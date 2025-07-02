package com.tangem.domain.managetokens

import arrow.core.Either
import arrow.core.raise.catch
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import com.tangem.domain.managetokens.model.exceptoin.SupportedBlockchainException
import com.tangem.domain.managetokens.repository.CustomTokensRepository
import com.tangem.domain.models.network.Network
import com.tangem.domain.wallets.models.UserWalletId

class GetSupportedNetworksUseCase(
    private val repository: CustomTokensRepository,
) {

    suspend operator fun invoke(userWalletId: UserWalletId): Either<SupportedBlockchainException, List<Network>> {
        return either {
            val networks = catch({ repository.getSupportedNetworks(userWalletId) }) {
                raise(SupportedBlockchainException.DataError(it))
            }

            ensureNotNull(networks.takeIf { it.isNotEmpty() }) {
                SupportedBlockchainException.EmptyList
            }.sortedBy(Network::name)
        }
    }
}