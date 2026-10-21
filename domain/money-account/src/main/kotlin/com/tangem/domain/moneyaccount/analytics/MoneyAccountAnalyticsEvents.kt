package com.tangem.domain.moneyaccount.analytics

import com.tangem.core.analytics.models.AnalyticsEvent

sealed class MoneyAccountAnalyticsEvents(
    categoryName: String,
    event: String,
    params: Map<String, String> = emptyMap(),
) : AnalyticsEvent(category = categoryName, event = event, params = params) {

    class AddFundsClicked : MoneyAccountAnalyticsEvents(
        categoryName = SCREEN_CATEGORY,
        event = "Button - VA Add Funds",
    )

    class DepositInfoScreenOpened : MoneyAccountAnalyticsEvents(
        categoryName = DEPOSIT_CATEGORY,
        event = "VA Deposit Info Screen Opened",
    )

    class DepositDetailsScreenOpened : MoneyAccountAnalyticsEvents(
        categoryName = DEPOSIT_CATEGORY,
        event = "VA Deposit Details Screen Opened",
    )

    data class DetailsFieldCopied(val fieldName: String) : MoneyAccountAnalyticsEvents(
        categoryName = DETAILS_CATEGORY,
        event = "VA Details Field Copied",
        params = mapOf("field_name" to fieldName),
    )

    class DetailsShared : MoneyAccountAnalyticsEvents(
        categoryName = DETAILS_CATEGORY,
        event = "VA Details Shared",
    )

    class DetailsLoadFailed : MoneyAccountAnalyticsEvents(
        categoryName = DETAILS_CATEGORY,
        event = "VA Details Load Failed",
    )

    class DetailsRetryTapped : MoneyAccountAnalyticsEvents(
        categoryName = DETAILS_CATEGORY,
        event = "VA Details Retry Tapped",
    )

    class DetailsErrorSupportTapped : MoneyAccountAnalyticsEvents(
        categoryName = DETAILS_CATEGORY,
        event = "VA Account Details Error Support Tapped",
    )

    private companion object {
        const val SCREEN_CATEGORY = "VA Screen"
        const val DEPOSIT_CATEGORY = "VA Deposit"
        const val DETAILS_CATEGORY = "VA Details"
    }
}