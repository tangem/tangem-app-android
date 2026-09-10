package com.tangem.domain.collectibles.repository

import com.tangem.domain.collectibles.CollectibleGachaMachinesBatchFlow
import com.tangem.domain.collectibles.CollectibleGachaMachinesBatchingContext
import com.tangem.domain.collectibles.models.*
import com.tangem.domain.models.wallet.UserWalletId

interface CollectiblesRepository {

    suspend fun getCategories(): CollectibleGachaMachineCategories

    suspend fun getMachine(externalId: String): CollectibleGachaMachine

    fun getMachinesBatchFlow(
        context: CollectibleGachaMachinesBatchingContext,
        batchSize: Int,
    ): CollectibleGachaMachinesBatchFlow

    suspend fun generateTx(userWalletId: UserWalletId, params: GenerateTxParams): CollectibleTransaction

    suspend fun submitTx(userWalletId: UserWalletId, signedTxData: String): SubmittedCollectibleTransaction
}