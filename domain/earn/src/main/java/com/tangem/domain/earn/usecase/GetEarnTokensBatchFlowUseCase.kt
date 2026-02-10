package com.tangem.domain.earn.usecase

import com.tangem.domain.earn.model.EarnTokensBatchFlow
import com.tangem.domain.earn.model.EarnTokensBatchingContext
import com.tangem.domain.earn.repository.EarnRepository

/**
 * Returns BatchFlow of Earn-tokens.
 */
class GetEarnTokensBatchFlowUseCase(
    private val repository: EarnRepository,
) {

    operator fun invoke(context: EarnTokensBatchingContext, batchSize: Int = DEFAULT_BATCH_SIZE): EarnTokensBatchFlow {
        return repository.getEarnTokensBatchFlow(
            context = context,
            batchSize = batchSize,
        )
    }

    private companion object {
        private const val DEFAULT_BATCH_SIZE = 20
    }
}