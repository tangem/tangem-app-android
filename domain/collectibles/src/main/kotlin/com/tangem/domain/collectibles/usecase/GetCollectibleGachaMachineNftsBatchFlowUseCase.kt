package com.tangem.domain.collectibles.usecase

import com.tangem.domain.collectibles.CollectibleGachaMachineNftsBatchFlow
import com.tangem.domain.collectibles.CollectibleGachaMachineNftsBatchingContext
import com.tangem.domain.collectibles.repository.CollectiblesRepository
import com.tangem.domain.models.wallet.UserWalletId
import javax.inject.Inject

class GetCollectibleGachaMachineNftsBatchFlowUseCase @Inject constructor(
    private val repository: CollectiblesRepository,
) {

    operator fun invoke(
        userWalletId: UserWalletId,
        context: CollectibleGachaMachineNftsBatchingContext,
        batchSize: Int = DEFAULT_BATCH_SIZE,
    ): CollectibleGachaMachineNftsBatchFlow = repository.getMachineNftsBatchFlow(
        context = context,
        batchSize = batchSize,
        userWalletId = userWalletId,
    )

    private companion object {
        const val DEFAULT_BATCH_SIZE = 50
    }
}