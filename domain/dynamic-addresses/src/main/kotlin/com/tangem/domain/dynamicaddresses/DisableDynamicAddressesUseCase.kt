package com.tangem.domain.dynamicaddresses

import arrow.core.Either
import com.tangem.domain.dynamicaddresses.repository.DynamicAddressesRepository
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId

class DisableDynamicAddressesUseCase(
    private val dynamicAddressesRepository: DynamicAddressesRepository,
) {

    /**
     * Returns true when consolidation is required before disabling (non-base balances exist),
     * or false when DA was disabled immediately.
     */
    suspend operator fun invoke(userWalletId: UserWalletId, network: Network): Either<Throwable, Boolean> =
        Either.catch {
            val hasNonBaseBalances = dynamicAddressesRepository.hasNonBaseBalances(userWalletId, network)

            if (!hasNonBaseBalances) {
                dynamicAddressesRepository.disable(userWalletId, network)
                return@catch false
            }

            true
        }
}