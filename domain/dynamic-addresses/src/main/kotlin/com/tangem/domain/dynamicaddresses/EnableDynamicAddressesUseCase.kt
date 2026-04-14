package com.tangem.domain.dynamicaddresses

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.domain.dynamicaddresses.repository.DynamicAddressesRepository
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId

class EnableDynamicAddressesUseCase(
    private val dynamicAddressesRepository: DynamicAddressesRepository,
) {

    suspend operator fun invoke(
        userWalletId: UserWalletId,
        network: Network,
        xpub: String,
    ): Either<EnableDynamicAddressesError, Unit> {
        return try {
            if (dynamicAddressesRepository.hasConflictingCustomTokens(userWalletId, network)) {
                return EnableDynamicAddressesError.ConflictingCustomTokens.left()
            }
            dynamicAddressesRepository.enable(userWalletId, network, xpub)
            Unit.right()
        } catch (e: Throwable) {
            EnableDynamicAddressesError.ServiceError(e).left()
        }
    }
}