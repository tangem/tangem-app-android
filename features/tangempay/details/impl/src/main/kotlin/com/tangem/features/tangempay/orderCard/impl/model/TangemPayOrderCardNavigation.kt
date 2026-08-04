package com.tangem.features.tangempay.orderCard.impl.model

import com.tangem.domain.models.serialization.SerializedBigDecimal
import com.tangem.domain.models.serialization.SerializedCurrency
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.serialization.Serializable

@Serializable
internal sealed class TangemPayOrderCardNavigation {

    @Serializable
    data class IssueVirtual(
        val walletId: UserWalletId,
        val feeAmount: SerializedBigDecimal,
        val feeCurrency: SerializedCurrency,
        val fiatBalance: SerializedBigDecimal,
    ) : TangemPayOrderCardNavigation()
}