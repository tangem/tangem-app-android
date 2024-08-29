package com.tangem.domain.managetokens

import arrow.core.Either
import arrow.core.flatten
import com.tangem.domain.card.repository.DerivationsRepository
import com.tangem.domain.managetokens.model.ManagedCryptoCurrency
import com.tangem.domain.managetokens.repository.ManageTokensRepository
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.wallets.models.UserWalletId

class SaveManagedTokensUseCase(
    private val manageTokensRepository: ManageTokensRepository,
    private val derivationsRepository: DerivationsRepository,
) {

    suspend operator fun invoke(
        userWalletId: UserWalletId,
        currenciesToAdd: Map<ManagedCryptoCurrency.Token, Set<Network>>,
        currenciesToRemove: Map<ManagedCryptoCurrency.Token, Set<Network>>,
    ): Either<Throwable, Unit> {
        return Either.catch {
            derivationsRepository.derivePublicKeysByNetworks(
                userWalletId = userWalletId,
                networks = currenciesToAdd.values.flatten(),
            )
            manageTokensRepository.saveManagedCurrencies(
                userWalletId = userWalletId,
                currenciesToAdd = currenciesToAdd,
                currenciesToRemove = currenciesToRemove,
            )
        }
    }
}