package com.tangem.domain.dynamicaddresses

import arrow.core.Either
import com.tangem.domain.dynamicaddresses.repository.DynamicAddressesRepository
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId

/**
 * Returns `true` if a consolidation transaction must be broadcast before
 * [DynamicAddressesRepository.disable] is called (non-base balances exist).
 */
class IsDynamicAddressesConsolidationRequiredUseCase(
    private val dynamicAddressesRepository: DynamicAddressesRepository,
) {

    suspend operator fun invoke(userWalletId: UserWalletId, network: Network): Either<Throwable, Boolean> =
        Either.catch {
            dynamicAddressesRepository.hasNonBaseBalances(userWalletId, network)
        }
}