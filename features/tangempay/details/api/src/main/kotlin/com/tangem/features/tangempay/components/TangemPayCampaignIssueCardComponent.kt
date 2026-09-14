package com.tangem.features.tangempay.components

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.wallet.UserWalletId
import java.math.BigDecimal
import java.util.Currency

interface TangemPayCampaignIssueCardComponent : ComposableContentComponent {

    data class Params(
        val userWalletId: UserWalletId,
        val campaignKey: String,
        val cardArtUrl: String?,
        val feeAmount: BigDecimal,
        val feeCurrency: Currency,
    )

    interface Factory : ComponentFactory<Params, TangemPayCampaignIssueCardComponent>
}