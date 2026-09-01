package com.tangem.domain.polymarket.flow

import com.tangem.domain.core.flow.FlowCachingSupplier
import com.tangem.domain.models.account.PredictionAccountStatusValue
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.flow.Flow

/**
 * Supplier of the prediction account status, one shared flow per wallet.
 *
 * @property factory    factory of [PredictionAccountStatusProducer]
 * @property keyCreator key creator
 */
open class PredictionAccountStatusSupplier(
    override val factory: PredictionAccountStatusProducer.Factory,
    override val keyCreator: (PredictionAccountStatusProducer.Params) -> String,
) : FlowCachingSupplier<
    PredictionAccountStatusProducer,
    PredictionAccountStatusProducer.Params,
    PredictionAccountStatusValue,
    >() {

    operator fun invoke(userWalletId: UserWalletId): Flow<PredictionAccountStatusValue> {
        return invoke(PredictionAccountStatusProducer.Params(userWalletId = userWalletId))
    }
}