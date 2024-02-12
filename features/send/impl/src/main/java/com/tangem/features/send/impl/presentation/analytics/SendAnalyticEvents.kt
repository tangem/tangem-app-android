package com.tangem.features.send.impl.presentation.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam.Key.SOURCE
import com.tangem.core.analytics.models.AnalyticsParam.Key.TYPE
import com.tangem.core.analytics.models.AnalyticsParam.Key.VALIDATION

/**
 * Send screen analytics
 */
internal sealed class SendAnalyticEvents(
    event: String,
    params: Map<String, String> = mapOf(),
) : AnalyticsEvent(category = "Token / Send", event = event, params = params) {

    /** Send screen opened */
    object SendOpened : SendAnalyticEvents(event = "Send Screen Opened")

    /** Next button clicked */
    data class NextButtonClicked(val source: SendScreenSource) : SendAnalyticEvents(
        event = "Button - Next",
        params = mapOf(SOURCE to source.name),
    )

    /** Back button clicked */
    data class BackButtonClicked(val source: SendScreenSource) : SendAnalyticEvents(
        event = "Button - Back",
        params = mapOf(SOURCE to source.name),
    )

    // region Address
    /** Recipient address screen opened */
    object AddressScreenOpened : SendAnalyticEvents(event = "Address Screen Opened")

    /** Address to send entered */
    data class AddressEntered(val source: EnterAddressSource, val isValid: Boolean) : SendAnalyticEvents(
        event = "Address Entered",
        params = mapOf(
            SOURCE to source.name,
            VALIDATION to if (isValid) "Success" else "Fail",
        ),
    )

    /** Paste from clipboard button clicked */
    data class PasteButtonClicked(val type: PasteType) : SendAnalyticEvents(
        event = "Button - Paste",
        params = mapOf(
            TYPE to type.name,
        ),
    )

    /** Qr Code button clicked */
    object QrCodeButtonClicked : SendAnalyticEvents(event = "Button - QR Code")
    // endregion

    // region Amount
    /** Amount screen opened */
    object AmountScreenOpened : SendAnalyticEvents(event = "Amount Screen Opened")

    /** Selected currency */
    data class SelectedCurrency(val type: SelectedCurrencyType) : SendAnalyticEvents(
        event = "Selected Currency",
        params = mapOf(TYPE to type.value),
    )

    /** Currency selector button clicked */
    object SwapCurrencyButtonClicked : SendAnalyticEvents(event = "Button - Swap Currency")
    // endregion

    // region Fee
    /** Fee screen opened */
    object FeeScreenOpened : SendAnalyticEvents(event = "Fee Screen Opened")

    /** Selected fee (send after next screen opened) */
    data class SelectedFee(val fee: String) : SendAnalyticEvents(
        event = "Fee Selected",
        params = mapOf("Commission" to fee),
    )

    /** Custom fee selected */
    object CustomFeeButtonClicked : SendAnalyticEvents(event = "Custom Fee Clicked")

    /** Custom fee edited */
    object GasPriceInserter : SendAnalyticEvents(event = "Gas Price Inserted")

    /** Subtract from amount selector switched (send after next screen opened) */
    object SubtractFromAmount : SendAnalyticEvents(event = "Subtract from Amount")
    // endregion

    // region Confirmation
    /** Confirmation screen opened */
    object ConfirmationScreenOpened : SendAnalyticEvents(event = "Confirm Screen Opened")

    /** Send transaction button clicked */
    object SendButtonClicked : SendAnalyticEvents(event = "Button - Send")

    /** Screen reopened from confirmation screen */
    data class ScreenReopened(val source: SendScreenSource) : SendAnalyticEvents(
        event = "Screen Reopened",
        params = mapOf(SOURCE to source.name),
    )
    // endregion

    // region Transaction Result
    /** Transaction send screen opened */
    object TransactionScreenOpened : SendAnalyticEvents(event = "Transaction Sent Screen Opened")

    /** Share button clicked */
    object ShareButtonClicked : SendAnalyticEvents(event = "Button - Share")

    /** Expore button clicked */
    object ExploreButtonClicked : SendAnalyticEvents(event = "Button - Explore")
    // endregion
}

internal enum class SendScreenSource {
    Address,
    Amount,
    Fee,
}

internal enum class EnterAddressSource {
    QRCode,
    PasteButton,
    RecentAddress,
}

internal enum class PasteType {
    Address,
    Memo,
}

internal enum class SelectedCurrencyType(val value: String) {
    Token("Token"),
    AppCurrency("App Currency"),
}
