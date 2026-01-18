package com.tangem.domain.tangempay

import com.tangem.core.analytics.models.AnalyticsEvent

sealed class TangemPayAnalyticsEvents(
    categoryName: String,
    event: String,
    params: Map<String, String> = emptyMap(),
) : AnalyticsEvent(category = categoryName, event = event, params = params) {

    class ActivationScreenOpened : TangemPayAnalyticsEvents(
        categoryName = "Visa Onboarding",
        event = "Visa Activation Screen Opened",
    )

    class ViewTermsClicked : TangemPayAnalyticsEvents(
        categoryName = "Visa Onboarding",
        event = "Button - Visa View Terms",
    )

    class GetCardClicked : TangemPayAnalyticsEvents(
        categoryName = "Visa Onboarding",
        event = "Button - Visa Get Card",
    )

    class KycFlowOpened : TangemPayAnalyticsEvents(
        categoryName = "Visa Onboarding",
        event = "Visa KYC Flow Opened",
    )

    class IssuingBannerDisplayed : TangemPayAnalyticsEvents(
        categoryName = "Visa Onboarding",
        event = "Visa Issuing Banner Displayed",
    )

    class MainScreenOpened : TangemPayAnalyticsEvents(
        categoryName = "Visa Screen",
        event = "Visa Main Screen Opened",
    )

    class ReceiveFundsClicked : TangemPayAnalyticsEvents(
        categoryName = "Visa Screen",
        event = "Button - Visa Receive",
    )

    class AddFundsClicked : TangemPayAnalyticsEvents(
        categoryName = "Visa Screen",
        event = "Button - Visa Add Funds",
    )

    class SwapClicked : TangemPayAnalyticsEvents(
        categoryName = "Visa Screen",
        event = "Button - Visa Swap",
    )

    class ChooseWalletPopup : TangemPayAnalyticsEvents(
        categoryName = "Visa Onboarding",
        event = "Choose Wallet Popup",
    )

    class CardSettingsClicked : TangemPayAnalyticsEvents(
        categoryName = "Visa Screen",
        event = "Button - Card Settings",
    )

    class TermsAndLimitsClicked : TangemPayAnalyticsEvents(
        categoryName = "Visa Screen",
        event = "Button - Terms And Limits",
    )

    class FreezeCardClicked : TangemPayAnalyticsEvents(
        categoryName = "Visa Screen",
        event = "Button - Freeze Card",
    )

    class FreezeCardConfirmShown : TangemPayAnalyticsEvents(
        categoryName = "Visa Screen",
        event = "Popup - Freeze Confirmation",
    )

    class FreezeCardConfirmClicked : TangemPayAnalyticsEvents(
        categoryName = "Visa Screen",
        event = "Button - Freeze Confirmation On Popup",
    )

    class UnfreezeCardClicked : TangemPayAnalyticsEvents(
        categoryName = "Visa Screen",
        event = "Button - Unfreeze Card",
    )

    class UnfreezeCardConfirmShown : TangemPayAnalyticsEvents(
        categoryName = "Visa Screen",
        event = "Popup - Unfreeze Confirmation",
    )

    class UnfreezeCardConfirmClicked : TangemPayAnalyticsEvents(
        categoryName = "Visa Screen",
        event = "Button - Unfreeze Confirmation On Popup",
    )

    class PinCodeClicked : TangemPayAnalyticsEvents(
        categoryName = "Visa Screen",
        event = "Button - PIN Code",
    )

    class ChangePinScreenShown : TangemPayAnalyticsEvents(
        categoryName = "Visa Screen",
        event = "Screen - Change PIN",
    )

    class ChangePinSubmitClicked : TangemPayAnalyticsEvents(
        categoryName = "Visa Screen",
        event = "Button - Set PIN On Change PIN Screen",
    )

    class ChangePinSuccessShown : TangemPayAnalyticsEvents(
        categoryName = "Visa Screen",
        event = "Message - PIN Setup Success",
    )

    class CurrentPinShown : TangemPayAnalyticsEvents(
        categoryName = "Visa Screen",
        event = "Popup - Current PIN",
    )

    class ChangePinOnCurrentPinClicked : TangemPayAnalyticsEvents(
        categoryName = "Visa Screen",
        event = "Button - Change PIN On Current PIN",
    )

    class ViewCardDetailsClicked : TangemPayAnalyticsEvents(
        categoryName = "Visa Screen",
        event = "Button - View Card Details",
    )

    class CopyCardNumberClicked : TangemPayAnalyticsEvents(
        categoryName = "Visa Screen",
        event = "Button - Copy Card Number",
    )

    class CopyCardExpiryClicked : TangemPayAnalyticsEvents(
        categoryName = "Visa Screen",
        event = "Button - Copy Card Expiry",
    )

    class CopyCardCVVClicked : TangemPayAnalyticsEvents(
        categoryName = "Visa Screen",
        event = "Button - Copy CVV",
    )

    class AddToWalletClicked : TangemPayAnalyticsEvents(
        categoryName = "Visa Screen",
        event = "Button - Add Card To Wallet",
    )

    class WithdrawClicked : TangemPayAnalyticsEvents(
        categoryName = "Visa Screen",
        event = "Button - Visa Withdraw",
    )

    class GoToSupportOnBetaBannerClicked : TangemPayAnalyticsEvents(
        categoryName = "Visa Screen",
        event = "Button - Go To Support On Beta Banner",
    )

    class TransactionInListClicked(type: String, status: String) : TangemPayAnalyticsEvents(
        categoryName = "Visa Screen",
        event = "Clicked On Transaction In List",
        params = mapOf(
            "type" to type,
            "status" to status,
        ),
    )

    class SupportOnTransactionPopupClicked : TangemPayAnalyticsEvents(
        categoryName = "Visa Screen",
        event = "Button - Support On Transaction Popup",
    )
}