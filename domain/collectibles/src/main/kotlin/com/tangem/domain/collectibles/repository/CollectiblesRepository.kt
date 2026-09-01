package com.tangem.domain.collectibles.repository

import com.tangem.domain.collectibles.model.CollectibleGachaMachinesBatchFlow
import com.tangem.domain.collectibles.model.CollectibleGachaMachinesBatchingContext
import com.tangem.domain.collectibles.models.*

interface CollectiblesRepository {

    suspend fun getCategories(): CollectibleGachaMachineCategories

    suspend fun getMachine(externalId: String): CollectibleGachaMachine

    fun getMachinesBatchFlow(
        context: CollectibleGachaMachinesBatchingContext,
        batchSize: Int,
    ): CollectibleGachaMachinesBatchFlow

    suspend fun generateTx(params: GenerateTxParams): CollectibleTransaction

    suspend fun submitTx(signedTxData: String): SubmittedCollectibleTransaction
}