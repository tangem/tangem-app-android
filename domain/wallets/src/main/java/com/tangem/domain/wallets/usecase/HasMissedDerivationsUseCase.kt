package com.tangem.domain.wallets.usecase

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.derivations.DerivationsRepository

typealias BackendId = String

/**
 * Use case to check if user has missed derivations
 *
[REDACTED_AUTHOR]
 */
class HasMissedDerivationsUseCase(
    private val derivationsRepository: DerivationsRepository,
) {

    /** Check if user [userWalletId] has missed derivations using map of [com.tangem.domain.models.network.Network.ID] with extraDerivationPath */
    suspend operator fun invoke(
        userWalletId: UserWalletId,
        networksWithDerivationPath: Map<BackendId, String?>,
    ): Boolean {
        return derivationsRepository.hasMissedDerivations(userWalletId, networksWithDerivationPath)
    }
}