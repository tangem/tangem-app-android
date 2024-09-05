package com.tangem.features.send.impl.presentation.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.AnalyticsParam.Key.BLOCKCHAIN
import com.tangem.core.analytics.models.AnalyticsParam.Key.FEE_TYPE
import com.tangem.core.analytics.models.AnalyticsParam.Key.SOURCE
import com.tangem.core.analytics.models.AnalyticsParam.Key.TOKEN_PARAM
import com.tangem.core.analytics.models.AnalyticsParam.Key.TYPE
import com.tangem.core.analytics.models.AnalyticsParam.Key.VALIDATION
import com.tangem.core.analytics.models.AnalyticsParam.OnOffState

/**
 * Send screen analytics
 */
internal sealed class SendAnalyticEvents(
    event: String,
    params: Map<String, String> = mapOf(),
) : AnalyticsEvent(category = "Token / Send", event = event, params = params) {

    /** Close button clicked */
    data class CloseButtonClicked(
        val source: SendScreenSource,
        val isFromSummary: Boolean,
        val isValid: Boolean,
    ) : SendAnalyticEvents(
        event = "Button - Close",
        params = mapOf(
            SOURCE to source.name,
            "FromSummary" to if (isFromSummary) "Yes" else "No",
            "isValid" to if (isValid) "Yes" else "No",
        ),
    )

    // region Address
    /** Recipient address screen opened */
    data object AddressScreenOpened : SendAnalyticEvents(event = "Address Screen Opened")

    /** Address to send entered */
    data class AddressEntered(val source: EnterAddressSource, val isValid: Boolean) : SendAnalyticEvents(
        event = "Address Entered",
        params = mapOf(
            SOURCE to source.name,
            VALIDATION to if (isValid) "Success" else "Fail",
        ),
    )

    /** Qr Code button clicked */
    data object QrCodeButtonClicked : SendAnalyticEvents(event = "Button - QR Code")
    // endregion

    // region Amount
    /** Amount screen opened */
    data object AmountScreenOpened : SendAnalyticEvents(event = "Amount Screen Opened")

    /** Selected currency */
    data class SelectedCurrency(val type: SelectedCurrencyType) : SendAnalyticEvents(
        event = "Selected Currency",
        params = mapOf(TYPE to type.value),
    )

    /** Max amount button clicked */
    data object MaxAmountButtonClicked : SendAnalyticEvents(event = "Max Amount Taped")
    // endregion

    // region Fee
    /** Fee screen opened */
    data object FeeScreenOpened : SendAnalyticEvents(event = "Fee Screen Opened")

    /** Selected fee (send after next screen opened) */
    data class SelectedFee(val feeType: AnalyticsParam.FeeType) : SendAnalyticEvents(
        event = "Fee Selected",
        params = mapOf("Fee Type" to feeType.value),
    )

    /** Custom fee selected */
    data object CustomFeeButtonClicked : SendAnalyticEvents(event = "Custom Fee Clicked")

    /** Custom fee edited */
    data object GasPriceInserter : SendAnalyticEvents(event = "Gas Price Inserted")

    /** Subtract from amount selector switched (send after next screen opened) */
    data class SubtractFromAmount(val status: Boolean) : SendAnalyticEvents(
        event = "Subtract from Amount",
        params = mapOf("Status" to if (status) OnOffState.On.value else OnOffState.Off.value),
    )
    // endregion

    // region Confirmation
    /** Confirmation screen opened */
    data object ConfirmationScreenOpened : SendAnalyticEvents(event = "Confirm Screen Opened")

    /** Screen reopened from confirmation screen */
    data class ScreenReopened(val source: SendScreenSource) : SendAnalyticEvents(
        event = "Screen Reopened",
        params = mapOf(SOURCE to source.name),
    )
    // endregion

    // region Transaction Result
    /** Transaction send screen opened */
    data class TransactionScreenOpened(
        val token: String,
        val feeType: AnalyticsParam.FeeType,
    ) : SendAnalyticEvents(
        event = "Transaction Sent Screen Opened",
        params = mapOf(
            TOKEN_PARAM to token,
            FEE_TYPE to feeType.value,
        ),
    )

    /** Share button clicked */
    data object ShareButtonClicked : SendAnalyticEvents(event = "Button - Share")

    /** Expore button clicked */
    data object ExploreButtonClicked : SendAnalyticEvents(event = "Button - Explore")

    /** If not enough fee notification is present */
    data class NoticeNotEnoughFee(val token: String, val blockchain: String) : SendAnalyticEvents(
        event = "Notice - Not Enough Fee",
        params = mapOf(TOKEN_PARAM to token, BLOCKCHAIN to blockchain),
    )

    /** If transaction delays notification is present */
    data class NoticeTransactionDelays(val token: String) : SendAnalyticEvents(
        event = "Notice - Transaction Delays Are Possible",
        params = mapOf(TOKEN_PARAM to token),
    )

    data object NoticeFeeCoverage : SendAnalyticEvents(
        event = "Notice - Network Fee Coverage",
    )

    /** If error occurs during send transactions */
    data class TransactionError(val token: String) : SendAnalyticEvents(
        event = "Error - Transaction Rejected",
        params = mapOf(TOKEN_PARAM to token),
    )
    // endregion
}

internal enum class SendScreenSource {
    Address,
    Amount,
    Fee,
    Confirm,
}

internal enum class EnterAddressSource {
    QRCode,
    PasteButton,
    RecentAddress,
}

internal enum class SelectedCurrencyType(val value: String) {
    Token("Token"),
    AppCurrency("App Currency"),
}