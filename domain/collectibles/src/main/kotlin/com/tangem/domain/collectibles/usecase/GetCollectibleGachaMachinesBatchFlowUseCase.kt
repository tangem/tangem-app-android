package com.tangem.domain.collectibles.usecase

import com.tangem.domain.collectibles.CollectibleGachaMachinesBatchFlow
import com.tangem.domain.collectibles.CollectibleGachaMachinesBatchingContext
import com.tangem.domain.collectibles.repository.CollectiblesRepository
import javax.inject.Inject

class GetCollectibleGachaMachinesBatchFlowUseCase @Inject constructor(
    private val repository: CollectiblesRepository,
) {

    operator fun invoke(
        context: CollectibleGachaMachinesBatchingContext,
        batchSize: Int = DEFAULT_BATCH_SIZE,
    ): CollectibleGachaMachinesBatchFlow = repository.getMachinesBatchFlow(context = context, batchSize = batchSize)

    private companion object {
        const val DEFAULT_BATCH_SIZE = 20
    }
}