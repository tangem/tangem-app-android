package com.tangem.domain.polymarket.flow

import com.tangem.domain.core.flow.FlowFetcher
import com.tangem.domain.models.wallet.UserWalletId

interface PredictionPositionsValueFetcher : FlowFetcher<PredictionPositionsValueFetcher.Params> {

    data class Params(val userWalletId: UserWalletId)
}