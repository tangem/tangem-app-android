package com.tangem.domain.card

import com.tangem.domain.card.repository.DerivationsRepository
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.wallets.models.UserWalletId

/**
 * Use case to check if user has missed derivations
 *
[REDACTED_AUTHOR]
 */
class HasMissedDerivationsUseCase(
    private val derivationsRepository: DerivationsRepository,
) {

    /** Check if user [userWalletId] has missed derivations using map of [Network.ID] with extraDerivationPath */
    suspend operator fun invoke(
        userWalletId: UserWalletId,
        networksWithDerivationPath: Map<Network.ID, String?>,
    ): Boolean {
        return derivationsRepository.hasMissedDerivations(userWalletId, networksWithDerivationPath)
    }
}