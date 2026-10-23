package com.tangem.features.tangempay.addfunds.bankTransferOnramp.details

import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents
import com.tangem.features.tangempay.components.TangemPayBankTransferOnrampDetailsComponent
import com.tangem.features.tangempay.components.TangemPayBankTransferOnrampDetailsComponent.RequisiteField
import javax.inject.Inject

internal class TangemPayBankTransferOnrampDetailsAnalytics @Inject constructor(
    private val analytics: AnalyticsEventHandler,
) : TangemPayBankTransferOnrampDetailsComponent.Analytics {

    override fun onDetailsShown() {
        analytics.send(TangemPayAnalyticsEvents.VaBankingDetailsShowed())
    }

    override fun onFieldCopied(field: RequisiteField) {
        analytics.send(TangemPayAnalyticsEvents.VaCopyFieldClicked(field.label))
    }

    override fun onShareClicked() {
        analytics.send(TangemPayAnalyticsEvents.VaShareDetailsButtonClicked())
    }
}