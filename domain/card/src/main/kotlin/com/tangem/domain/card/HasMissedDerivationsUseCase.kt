package com.tangem.domain.card

import com.tangem.domain.card.repository.DerivationsRepository
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId

/**
 * Use case to check if user has missed derivations
 *
[REDACTED_AUTHOR]
 */

typealias BackendId = String

class HasMissedDerivationsUseCase(
    private val derivationsRepository: DerivationsRepository,
) {

    /** Check if user [userWalletId] has missed derivations using map of [Network.ID] with extraDerivationPath */
    suspend operator fun invoke(
        userWalletId: UserWalletId,
        networksWithDerivationPath: Map<BackendId, String?>,
    ): Boolean {
        return derivationsRepository.hasMissedDerivations(userWalletId, networksWithDerivationPath)
    }
}