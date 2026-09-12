package com.tangem.domain.collectibles.account.flow

import com.tangem.domain.core.flow.FlowCachingSupplier
import com.tangem.domain.models.account.CollectiblesAccountStatusValue
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.flow.Flow

/**
 * Supplier of the collectibles account status, one shared flow per wallet.
 *
 * @property factory    factory of [CollectiblesAccountStatusProducer]
 * @property keyCreator key creator
 */
open class CollectiblesAccountStatusSupplier(
    override val factory: CollectiblesAccountStatusProducer.Factory,
    override val keyCreator: (CollectiblesAccountStatusProducer.Params) -> String,
) : FlowCachingSupplier<
    CollectiblesAccountStatusProducer,
    CollectiblesAccountStatusProducer.Params,
    CollectiblesAccountStatusValue,
    >() {

    operator fun invoke(userWalletId: UserWalletId): Flow<CollectiblesAccountStatusValue> {
        return invoke(CollectiblesAccountStatusProducer.Params(userWalletId = userWalletId))
    }
}