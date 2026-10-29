package com.tangem.domain.moneyaccount.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import java.math.BigDecimal

sealed class MoneyAccountAnalyticsEvents(
    categoryName: String,
    event: String,
    params: Map<String, String> = emptyMap(),
) : AnalyticsEvent(category = categoryName, event = event, params = params) {

    class AddFundsClicked : MoneyAccountAnalyticsEvents(
        categoryName = SCREEN_CATEGORY,
        event = "Button - VA Add Funds",
    )

    class SendClicked : MoneyAccountAnalyticsEvents(
        categoryName = SCREEN_CATEGORY,
        event = "Button - VA Send",
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

    data class WithdrawOptionSelected(val method: WithdrawMethod) : MoneyAccountAnalyticsEvents(
        categoryName = TRANSFER_CATEGORY,
        event = "VA Withdraw Option Selected",
        params = mapOf("method" to method.value),
    )

    class OfframpRecipientListOpened : MoneyAccountAnalyticsEvents(
        categoryName = TRANSFER_CATEGORY,
        event = "VA Offramp Recipient List Opened",
    )

    class OfframpRecipientSelected : MoneyAccountAnalyticsEvents(
        categoryName = TRANSFER_CATEGORY,
        event = "VA Offramp Recipient Selected",
    )

    class OfframpAmountScreenOpened : MoneyAccountAnalyticsEvents(
        categoryName = TRANSFER_CATEGORY,
        event = "VA Offramp Amount Screen Opened",
    )

    class OfframpConfirmationScreenOpened : MoneyAccountAnalyticsEvents(
        categoryName = TRANSFER_CATEGORY,
        event = "VA Offramp Confirmation Screen Opened",
    )

    class OfframpRecipientTransferConfirmed : MoneyAccountAnalyticsEvents(
        categoryName = TRANSFER_CATEGORY,
        event = "VA Offramp Recipient Transfer Confirmed",
    )

    data class SendConfirmed(val amountUsdc: BigDecimal, val method: SendMethod) : MoneyAccountAnalyticsEvents(
        categoryName = TRANSFER_CATEGORY,
        event = "VA Send Confirmed",
        params = mapOf(
            "amount_usdc" to amountUsdc.toPlainString(),
            "method" to method.value,
        ),
    )

    class SendFailed : MoneyAccountAnalyticsEvents(
        categoryName = TRANSFER_CATEGORY,
        event = "VA Send failed",
    )

    class SendSucceeded : MoneyAccountAnalyticsEvents(
        categoryName = TRANSFER_CATEGORY,
        event = "VA Send succesfull",
    )

    enum class WithdrawMethod(val value: String) {
        ON_CHAIN("on_chain"),
        BANK_TRANSFER("bank_transfer"),
    }

    enum class SendMethod(val value: String) {
        BANK_TRANSFER_ACH("bank_transfer_ach"),
        BANK_TRANSFER_WIRE("bank_transfer_wire"),
    }

    private companion object {
        const val SCREEN_CATEGORY = "VA Screen"
        const val DEPOSIT_CATEGORY = "VA Deposit"
        const val DETAILS_CATEGORY = "VA Details"
        const val TRANSFER_CATEGORY = "VA Transfer"
    }
}