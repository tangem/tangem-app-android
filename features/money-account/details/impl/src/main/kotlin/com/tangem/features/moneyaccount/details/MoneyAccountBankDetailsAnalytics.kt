package com.tangem.features.moneyaccount.details

import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.domain.moneyaccount.MoneyAccountFeatureToggles
import com.tangem.domain.moneyaccount.analytics.MoneyAccountAnalyticsEvents
import com.tangem.domain.moneyaccount.analytics.MoneyAccountAnalyticsEvents.DepositCurrency
import com.tangem.domain.moneyaccount.analytics.MoneyAccountAnalyticsEvents.DepositRail
import com.tangem.domain.tangempay.model.BankCredentials
import com.tangem.features.tangempay.components.TangemPayBankTransferOnrampDetailsComponent
import com.tangem.features.tangempay.components.TangemPayBankTransferOnrampDetailsComponent.RequisiteField
import javax.inject.Inject

internal class MoneyAccountBankDetailsAnalytics @Inject constructor(
    private val analytics: AnalyticsEventHandler,
    private val moneyAccountFeatureToggles: MoneyAccountFeatureToggles,
) : TangemPayBankTransferOnrampDetailsComponent.Analytics {

    override fun onDetailsShown(credentials: BankCredentials) {
        analytics.send(
            MoneyAccountAnalyticsEvents.AccountDetailsScreenOpened(
                currency = credentials.currencyIfMxnEnabled(),
                rail = DepositRail.of(credentials).takeIf { moneyAccountFeatureToggles.isMxnEnabled },
            ),
        )
    }

    override fun onFieldCopied(field: RequisiteField, credentials: BankCredentials) {
        analytics.send(
            MoneyAccountAnalyticsEvents.DetailsFieldCopied(
                field = field.toFieldName(),
                currency = credentials.currencyIfMxnEnabled(),
            ),
        )
    }

    override fun onShareClicked(credentials: BankCredentials) {
        analytics.send(MoneyAccountAnalyticsEvents.DetailsShared(currency = credentials.currencyIfMxnEnabled()))
    }

    private fun BankCredentials.currencyIfMxnEnabled(): DepositCurrency? =
        DepositCurrency.of(this).takeIf { moneyAccountFeatureToggles.isMxnEnabled }

    private fun RequisiteField.toFieldName(): String = when (this) {
        RequisiteField.ACCOUNT_NUMBER -> "account_number"
        RequisiteField.CLABE -> "clabe"
        RequisiteField.ROUTING_NUMBER -> "routing_number"
        RequisiteField.BENEFICIARY_NAME -> "beneficiary"
        RequisiteField.BENEFICIARY_ADDRESS -> "beneficiary_address"
        RequisiteField.BANK_NAME -> "bank_name"
        RequisiteField.BANK_ADDRESS -> "bank_address"
    }
}