package com.tangem.domain.card

import com.tangem.domain.card.repository.DerivationsRepository
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.wallets.models.UserWalletId

/**
 * Use case to check if user has missed derivations
 *
 * @author Andrew Khokhlov on 29/08/2024
 */
class HasMissedDerivationsUseCase(
    private val derivationsRepository: DerivationsRepository,
) {

    suspend operator fun invoke(userWalletId: UserWalletId, networkIds: List<Network.ID>): Boolean {
        return derivationsRepository.hasMissedDerivations(userWalletId, networkIds)
    }
}
