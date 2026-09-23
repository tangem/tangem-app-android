package com.tangem.domain.collectibles.repository

import com.tangem.domain.collectibles.CollectibleGachaMachineNftsBatchFlow
import com.tangem.domain.collectibles.CollectibleGachaMachineNftsBatchingContext
import com.tangem.domain.collectibles.CollectibleGachaMachinesBatchFlow
import com.tangem.domain.collectibles.CollectibleGachaMachinesBatchingContext
import com.tangem.domain.collectibles.models.*
import com.tangem.domain.models.wallet.UserWalletId

interface CollectiblesRepository {

    suspend fun getCategories(): CollectibleGachaMachineCategories

    suspend fun getMachine(externalId: String, skipCache: Boolean = false): CollectibleGachaMachine

    fun getMachinesBatchFlow(
        context: CollectibleGachaMachinesBatchingContext,
        batchSize: Int,
    ): CollectibleGachaMachinesBatchFlow

    fun getMachineNftsBatchFlow(
        context: CollectibleGachaMachineNftsBatchingContext,
        batchSize: Int,
        userWalletId: UserWalletId,
    ): CollectibleGachaMachineNftsBatchFlow

    suspend fun generateTx(userWalletId: UserWalletId, params: GenerateTxParams): CollectibleTransaction

    suspend fun submitTx(userWalletId: UserWalletId, signedTxData: String): SubmittedCollectibleTransaction
}