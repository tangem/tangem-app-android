package com.tangem.features.tangempay.campaign

import com.tangem.domain.models.serialization.SerializedBigDecimal
import com.tangem.domain.models.serialization.SerializedCurrency
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.serialization.Serializable

@Serializable
internal sealed class TangemPayCampaignChild {

    @Serializable
    data class IssueCard(
        val userWalletId: UserWalletId,
        val cardArtUrl: String?,
        val feeAmount: SerializedBigDecimal,
        val feeCurrency: SerializedCurrency,
    ) : TangemPayCampaignChild()
}