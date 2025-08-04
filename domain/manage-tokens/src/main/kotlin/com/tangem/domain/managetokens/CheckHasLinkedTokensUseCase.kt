package com.tangem.domain.managetokens

import arrow.core.Either
import com.tangem.domain.managetokens.model.ManagedCryptoCurrency
import com.tangem.domain.managetokens.repository.ManageTokensRepository
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId

class CheckHasLinkedTokensUseCase(
    private val repository: ManageTokensRepository,
) {

    suspend operator fun invoke(
        userWalletId: UserWalletId,
        network: Network,
        tempAddedTokens: Map<ManagedCryptoCurrency.Token, Set<Network>>,
        tempRemovedTokens: Map<ManagedCryptoCurrency.Token, Set<Network>>,
    ): Either<Throwable, Boolean> {
        return Either.catch {
            repository.hasLinkedTokens(
                userWalletId = userWalletId,
                network = network,
                tempAddedTokens = tempAddedTokens,
                tempRemovedTokens = tempRemovedTokens,
            )
        }
    }
}