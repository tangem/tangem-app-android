package com.tangem.domain.collectibles.flow

import com.tangem.domain.collectibles.models.CollectibleActiveTx
import com.tangem.domain.core.flow.FlowCachingSupplier
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.flow.Flow

/** The last read of the user's active purchases; emits nothing until [CollectiblesActiveTxsFetcher] succeeds once. */
open class CollectiblesActiveTxsSupplier(
    override val factory: CollectiblesActiveTxsProducer.Factory,
    override val keyCreator: (CollectiblesActiveTxsProducer.Params) -> String,
) : FlowCachingSupplier<
    CollectiblesActiveTxsProducer,
    CollectiblesActiveTxsProducer.Params,
    List<CollectibleActiveTx>,
    >() {

    operator fun invoke(userWalletId: UserWalletId): Flow<List<CollectibleActiveTx>> {
        return invoke(CollectiblesActiveTxsProducer.Params(userWalletId = userWalletId))
    }
}