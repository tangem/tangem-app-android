package com.tangem.domain.polymarket.flow

import com.tangem.domain.core.flow.FlowCachingSupplier
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.model.PredictionPositionsValue
import kotlinx.coroutines.flow.Flow

/**
 * @property factory    factory of [PredictionPositionsValueProducer]
 * @property keyCreator key creator
 */
open class PredictionPositionsValueSupplier(
    override val factory: PredictionPositionsValueProducer.Factory,
    override val keyCreator: (PredictionPositionsValueProducer.Params) -> String,
) : FlowCachingSupplier<
    PredictionPositionsValueProducer,
    PredictionPositionsValueProducer.Params,
    PredictionPositionsValue,
    >() {

    operator fun invoke(userWalletId: UserWalletId): Flow<PredictionPositionsValue> {
        return invoke(PredictionPositionsValueProducer.Params(userWalletId = userWalletId))
    }
}