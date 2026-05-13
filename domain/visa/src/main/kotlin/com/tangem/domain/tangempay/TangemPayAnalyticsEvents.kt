package com.tangem.domain.tangempay

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AppsFlyerIncludedEvent

sealed class TangemPayAnalyticsEvents(
    categoryName: String,
    event: String,
    params: Map<String, String> = emptyMap(),
) : AnalyticsEvent(category = categoryName, event = event, params = params) {

    class ActivationScreenOpened : TangemPayAnalyticsEvents(
        categoryName = "Visa Onboarding",
        event = "Visa Activation Screen Opened",
    ), AppsFlyerIncludedEvent

    class ViewTermsClicked : TangemPayAnalyticsEvents(
        categoryName = "Visa Onboarding",
        event = "Button - Visa View Terms",
    )

    class GetCardClicked : TangemPayAnalyticsEvents(
        categoryName = "Visa Onboarding",
        event = "Button - Visa Get Card",
    ), AppsFlyerIncludedEvent

    class KycFlowOpened : TangemPayAnalyticsEvents(
        categoryName = "Visa Onboarding",
        event = "Visa KYC Flow Opened",
    ), AppsFlyerIncludedEvent

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
    ), AppsFlyerIncludedEvent

    class AddFundsClicked : TangemPayAnalyticsEvents(
        categoryName = "Visa Screen",
        event = "Button - Visa Add Funds",
    ), AppsFlyerIncludedEvent

    class SwapClicked : TangemPayAnalyticsEvents(
        categoryName = "Visa Screen",
        event = "Button - Visa Swap",
    ), AppsFlyerIncludedEvent

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

    class KycPassedAndOrderCreated : TangemPayAnalyticsEvents(
        categoryName = "Visa Onboarding",
        event = "Visa KYC Passed And Order Created",
    ), AppsFlyerIncludedEvent

    class KycRejected : TangemPayAnalyticsEvents(
        categoryName = "Visa Onboarding",
        event = "Visa KYC Rejected",
    )

    class KycCancelled : TangemPayAnalyticsEvents(
        categoryName = "Visa Onboarding",
        event = "Visa KYC Canceled",
    )

    class ReplaceCardClicked : TangemPayAnalyticsEvents(
        categoryName = "Visa Card Management",
        event = "Visa Replace Card Clicked",
    )

    class ReplaceCardConfirmationPopupOpened : TangemPayAnalyticsEvents(
        categoryName = "Visa Card Management",
        event = "Visa Replace Card Confirmation Popup Opened",
    )

    class ReplaceCardConfirmed : TangemPayAnalyticsEvents(
        categoryName = "Visa Card Management",
        event = "Visa Replace Card Confirmed",
    )

    class LimitChangeClicked : TangemPayAnalyticsEvents(
        categoryName = "Visa Card Management",
        event = "Visa Daily Limit Change Clicked",
    )

    class LimitManagementOpened : TangemPayAnalyticsEvents(
        categoryName = "Visa Card Management",
        event = "Visa Limit Management Screen Opened",
    )

    data class LimitChangeConfirmed(val amount: String) : TangemPayAnalyticsEvents(
        categoryName = "Visa Card Management",
        event = "Visa Set Limits Confirmed",
        params = mapOf("amount" to amount),
    )

    class MainVisaPermanentBannerClicked : TangemPayAnalyticsEvents(
        categoryName = "Visa Onboarding",
        event = "Visa Permanent Banner Clicked",
    )

    class DetailsVisaPermanentButtonClicked : TangemPayAnalyticsEvents(
        categoryName = "Visa Onboarding",
        event = "Visa Permanent Button Clicked",
    )

    class CardIconClicked : TangemPayAnalyticsEvents(
        categoryName = "Visa Card Management",
        event = "Visa Card Icon Clicked",
    )

    class CardManagementScreenOpened : TangemPayAnalyticsEvents(
        categoryName = "Visa Card Management",
        event = "Visa Card Management Screen Opened",
    )

    class AddExtraCardClicked : TangemPayAnalyticsEvents(
        categoryName = "Visa Card Management",
        event = "Visa Add Extra Card Clicked",
    )

    class FakeDoorPopupDisplayed : TangemPayAnalyticsEvents(
        categoryName = "Visa Card Management",
        event = "Visa Fakedoor Popup Displayed",
    )

    class FakeDoorGotitClicked : TangemPayAnalyticsEvents(
        categoryName = "Visa Card Management",
        event = "Visa Fakedoor Gotit Clicked",
    )
}