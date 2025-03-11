package com.tangem.domain.transaction.usecase

import com.tangem.blockchain.common.AmountType
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.transaction.FeeRepository

/**
 * Use case to check if fee is approximate
 *
 * @param feeRepository [FeeRepository]
 */
class IsFeeApproximateUseCase(
    private val feeRepository: FeeRepository,
) {

    operator fun invoke(networkId: Network.ID, amountType: AmountType): Boolean {
        return feeRepository.isFeeApproximate(networkId, amountType)
    }
}