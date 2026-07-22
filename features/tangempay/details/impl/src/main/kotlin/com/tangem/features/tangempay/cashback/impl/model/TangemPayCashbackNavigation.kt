package com.tangem.features.tangempay.cashback.impl.model

import kotlinx.serialization.Serializable

@Serializable
internal sealed class TangemPayCashbackNavigation {

    @Serializable
    data object Details : TangemPayCashbackNavigation()

    @Serializable
    data object Accruals : TangemPayCashbackNavigation()
}