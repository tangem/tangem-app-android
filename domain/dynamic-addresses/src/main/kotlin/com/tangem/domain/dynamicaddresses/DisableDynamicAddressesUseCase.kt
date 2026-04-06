package com.tangem.domain.dynamicaddresses

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.raise.either
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.dynamicaddresses.model.ConsolidationInfo
import com.tangem.domain.dynamicaddresses.repository.ConsolidationRepository
import com.tangem.domain.dynamicaddresses.repository.DynamicAddressesRepository

class DisableDynamicAddressesUseCase(
    private val dynamicAddressesRepository: DynamicAddressesRepository,
    private val consolidationRepository: ConsolidationRepository,
) {

    /**
     * Returns [ConsolidationInfo] when consolidation is required before disabling,
     * or null when DA can be disabled immediately (no non-base balances).
     */
    suspend operator fun invoke(userWalletId: UserWalletId, network: Network): Either<Throwable, ConsolidationInfo?> =
        either {
            val hasNonBaseBalances = dynamicAddressesRepository.hasNonBaseBalances(userWalletId, network)

            if (!hasNonBaseBalances) {
                dynamicAddressesRepository.disable(userWalletId, network)
                return@either null
            }

            consolidationRepository.getConsolidationInfo(userWalletId, network)
                .getOrElse { raise(it) }
        }
}