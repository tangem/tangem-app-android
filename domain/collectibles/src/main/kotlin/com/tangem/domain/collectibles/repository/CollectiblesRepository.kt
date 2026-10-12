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

    suspend fun generatePurchaseTx(userWalletId: UserWalletId, params: GeneratePurchaseTxParams): CollectiblePurchaseTx

    /** Reports the hash of the spin sent on chain; the backend then starts watching the payment. */
    suspend fun reportPurchaseTxSent(userWalletId: UserWalletId, txId: String, txHash: String): CollectibleTxStatus

    /** @param includeNftWon asks for the card won, which the backend leaves out otherwise */
    suspend fun getTxState(userWalletId: UserWalletId, txId: String, includeNftWon: Boolean = false): CollectibleTxState

    /** The card may come later than the opening: the answer can be `pack-opened` without it. */
    suspend fun openPack(userWalletId: UserWalletId, txId: String): CollectibleTxState
}