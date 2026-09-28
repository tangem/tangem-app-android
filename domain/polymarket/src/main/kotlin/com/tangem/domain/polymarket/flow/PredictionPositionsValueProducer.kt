package com.tangem.domain.polymarket.flow

import com.tangem.domain.core.flow.FlowProducer
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.model.PredictionPositionsValue

interface PredictionPositionsValueProducer : FlowProducer<PredictionPositionsValue> {

    data class Params(val userWalletId: UserWalletId)

    interface Factory : FlowProducer.Factory<Params, PredictionPositionsValueProducer>
}