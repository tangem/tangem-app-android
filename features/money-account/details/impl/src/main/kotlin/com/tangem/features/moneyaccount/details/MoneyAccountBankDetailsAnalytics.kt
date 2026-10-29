package com.tangem.features.moneyaccount.details

import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.domain.moneyaccount.analytics.MoneyAccountAnalyticsEvents
import com.tangem.features.tangempay.components.TangemPayBankTransferOnrampDetailsComponent
import com.tangem.features.tangempay.components.TangemPayBankTransferOnrampDetailsComponent.RequisiteField
import javax.inject.Inject

internal class MoneyAccountBankDetailsAnalytics @Inject constructor(
    private val analytics: AnalyticsEventHandler,
) : TangemPayBankTransferOnrampDetailsComponent.Analytics {

    override fun onDetailsShown() {
        analytics.send(MoneyAccountAnalyticsEvents.AccountDetailsScreenOpened())
    }

    override fun onFieldCopied(field: RequisiteField) {
        analytics.send(MoneyAccountAnalyticsEvents.DetailsFieldCopied(field = field.toFieldName()))
    }

    override fun onShareClicked() {
        analytics.send(MoneyAccountAnalyticsEvents.DetailsShared())
    }

    private fun RequisiteField.toFieldName(): String = when (this) {
        RequisiteField.ACCOUNT_NUMBER -> "account_number"
        RequisiteField.ROUTING_NUMBER -> "routing_number"
        RequisiteField.BENEFICIARY_NAME -> "beneficiary"
        RequisiteField.BENEFICIARY_ADDRESS -> "beneficiary_address"
        RequisiteField.BANK_NAME -> "bank_name"
        RequisiteField.BANK_ADDRESS -> "bank_address"
    }
}