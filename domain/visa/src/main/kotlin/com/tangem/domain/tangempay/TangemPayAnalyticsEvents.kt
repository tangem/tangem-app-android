package com.tangem.domain.tangempay

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.AppsFlyerIncludedEvent
import com.tangem.domain.pay.model.bankofframp.BankRail

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

    class PermanentBannerShowed : TangemPayAnalyticsEvents(
        categoryName = "Visa Onboarding",
        event = "Visa Permanent Banner Showed",
    )

    class PermanentButtonShowed : TangemPayAnalyticsEvents(
        categoryName = "Visa Onboarding",
        event = "Visa Permanent Button Showed",
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

    class CopyCardholderNameClicked : TangemPayAnalyticsEvents(
        categoryName = "Visa Screen",
        event = "Button - Copy Cardholder Name",
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

    class CloseCardClicked : TangemPayAnalyticsEvents(
        categoryName = "Visa Card Management",
        event = "Visa Close Card Clicked",
    )

    class CloseCardConfirmationPopupOpened : TangemPayAnalyticsEvents(
        categoryName = "Visa Card Management",
        event = "Visa Close Card Confirmation Popup Opened",
    )

    class CloseCardConfirmed : TangemPayAnalyticsEvents(
        categoryName = "Visa Card Management",
        event = "Visa Close Card Confirmed",
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

    class CobrandOnboardingNewHotWallet : TangemPayAnalyticsEvents(
        categoryName = "Visa Onboarding",
        event = "Cobrand Design Onboarding New HotWallet",
    )

    class CobrandOnboardingNotInPay : TangemPayAnalyticsEvents(
        categoryName = "Visa Onboarding",
        event = "Cobrand Design Onboarding Not In Pay",
    )

    class CobrandOnboardingExistPay : TangemPayAnalyticsEvents(
        categoryName = "Visa Onboarding",
        event = "Cobrand Design Onboarding Exist Pay",
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

    class IssueAdditionalCardPopupShown : TangemPayAnalyticsEvents(
        categoryName = "Visa Card Management",
        event = "Visa Extra Card Issuance Popup Displayed",
    )

    class IssueAdditionalCardConfirmed : TangemPayAnalyticsEvents(
        categoryName = "Visa Card Management",
        event = "Visa Extra Card Issuance Confirmed",
    )

    class VaTopupButtonShowed : TangemPayAnalyticsEvents(
        categoryName = "Visa VA Topup",
        event = "Visa VA Topup Button Showed",
    )

    class VaTopupButtonClicked : TangemPayAnalyticsEvents(
        categoryName = "Visa VA Topup",
        event = "Visa VA Topup Button Clicked",
    )

    class VaConditionsPopupShowedFirstTime : TangemPayAnalyticsEvents(
        categoryName = "Visa VA Topup",
        event = "Conditions Popup Showed First Time",
    )

    class VaShowDetailsFirstTimeClicked : TangemPayAnalyticsEvents(
        categoryName = "Visa VA Topup",
        event = "Show Details First Time Clicked",
    )

    class VaSuccessScreenActivation : TangemPayAnalyticsEvents(
        categoryName = "Visa VA Topup",
        event = "Success Screen Activation",
    )

    class VaPreparationPopupShowed : TangemPayAnalyticsEvents(
        categoryName = "Visa VA Topup",
        event = "Preparation Popup Showed",
    )

    class VaConditionsPopupShowed : TangemPayAnalyticsEvents(
        categoryName = "Visa VA Topup",
        event = "Conditions Popup Showed",
    )

    class VaShowDetailsClicked : TangemPayAnalyticsEvents(
        categoryName = "Visa VA Topup",
        event = "Show Details Clicked",
    )

    class VaBankingDetailsShowed : TangemPayAnalyticsEvents(
        categoryName = "Visa VA Topup",
        event = "Banking Details Showed",
    )

    class VaDetailsErrorShowed : TangemPayAnalyticsEvents(
        categoryName = "Visa VA Topup",
        event = "Details Error Showed",
    )

    class VaShareDetailsButtonClicked : TangemPayAnalyticsEvents(
        categoryName = "Visa VA Topup",
        event = "Share Details Button Clicked",
    )

    data class VaCopyFieldClicked(val field: String) : TangemPayAnalyticsEvents(
        categoryName = "Visa VA Topup",
        event = "Copy Field Clicked",
        params = mapOf("field" to field),
    )

    object Tiers {

        private const val CATEGORY = "Visa Tiers"

        class TierSelectionScreenShowed : TangemPayAnalyticsEvents(
            categoryName = CATEGORY,
            event = "Tier Selection Screen Showed",
        )

        class TiersSwiped : TangemPayAnalyticsEvents(
            categoryName = CATEGORY,
            event = "Tiers Swiped",
        )

        class ComparePlansClicked : TangemPayAnalyticsEvents(
            categoryName = CATEGORY,
            event = "Compare Plans Clicked",
        )

        class PlansComparisonPopupShowed : TangemPayAnalyticsEvents(
            categoryName = CATEGORY,
            event = "Plans Comparison Popup Showed",
        )

        class PlansComparisonPopupClosed : TangemPayAnalyticsEvents(
            categoryName = CATEGORY,
            event = "Plans Comparison Popup Closed",
        )

        data class PlanSelectedClick(val tierId: String) : TangemPayAnalyticsEvents(
            categoryName = CATEGORY,
            event = "Plan Selected Click",
            params = mapOf("Plan" to tierId),
        )

        class TopUpBannerForPlusShowed : TangemPayAnalyticsEvents(
            categoryName = CATEGORY,
            event = "Topup Banner For Plus Showed",
        )

        class CancelPlusMoveToBasicClicked : TangemPayAnalyticsEvents(
            categoryName = CATEGORY,
            event = "Cancel Plus Move To Basic Clicked",
        )

        class CurrentPlanClicked : TangemPayAnalyticsEvents(
            categoryName = CATEGORY,
            event = "Current Plan Clicked",
        )

        class ChangePlanClicked : TangemPayAnalyticsEvents(
            categoryName = CATEGORY,
            event = "Change Plan Clicked",
        )

        data class PlanChangeConfirmationScreenShowed(val tierId: String) : TangemPayAnalyticsEvents(
            categoryName = CATEGORY,
            event = "Plan Change Confirmation Screen Showed",
            params = mapOf("Plan" to tierId),
        )

        class PlanChangeCancelClicked : TangemPayAnalyticsEvents(
            categoryName = CATEGORY,
            event = "Plan Change Cancel Clicked",
        )

        class PlanChangeUpgradeClicked : TangemPayAnalyticsEvents(
            categoryName = CATEGORY,
            event = "Plan Change Upgrade Clicked",
        )

        class PlusCardsClosureWarningBannerShowed : TangemPayAnalyticsEvents(
            categoryName = CATEGORY,
            event = "Plus Cards Closure Warning Banner Showed",
        )

        class StayOnPlusConditionsClicked : TangemPayAnalyticsEvents(
            categoryName = CATEGORY,
            event = "Stay On Plus Conditions Clicked",
        )

        class StayOnPlusPopupShowed : TangemPayAnalyticsEvents(
            categoryName = CATEGORY,
            event = "Stay On Plus Popup Showed",
        )

        class StayOnPlusPopupClicked : TangemPayAnalyticsEvents(
            categoryName = CATEGORY,
            event = "Stay On Plus Popup Clicked",
        )
    }

    object Plastic {

        private const val CATEGORY = "Visa Plastic"

        class CardTypeSelectionScreenOpened : TangemPayAnalyticsEvents(
            categoryName = CATEGORY,
            event = "Card Type Selection Screen Opened",
        )

        class VirtualTypeClicked : TangemPayAnalyticsEvents(
            categoryName = CATEGORY,
            event = "Virtual Type Clicked",
        )

        class PlasticTypeClicked : TangemPayAnalyticsEvents(
            categoryName = CATEGORY,
            event = "Plastic Type Clicked",
        )

        class CardTypeSwiped : TangemPayAnalyticsEvents(
            categoryName = CATEGORY,
            event = "Card Type Swiped",
        )

        class VirtualSelectClicked : TangemPayAnalyticsEvents(
            categoryName = CATEGORY,
            event = "Virtual Select Clicked",
        )

        class PlasticSelectClicked : TangemPayAnalyticsEvents(
            categoryName = CATEGORY,
            event = "Plastic Select Clicked",
        )

        class DeliveryCostNotEnoughMoneyShowed : TangemPayAnalyticsEvents(
            categoryName = CATEGORY,
            event = "Delivery Cost Not Enough Money Showed",
        )

        class AddressScreenOpened : TangemPayAnalyticsEvents(
            categoryName = CATEGORY,
            event = "Address Screen Opened",
        )

        class OrderCardClicked : TangemPayAnalyticsEvents(
            categoryName = CATEGORY,
            event = "Order Card Clicked",
        )

        class CardOrderedSuccessScreenShowed : TangemPayAnalyticsEvents(
            categoryName = CATEGORY,
            event = "Card Ordered Success Screen Showed",
        )

        class CardInTransitBannerShowed : TangemPayAnalyticsEvents(
            categoryName = CATEGORY,
            event = "Card In Transit Banner Showed",
        )

        class ActivateCardBannerButtonClicked : TangemPayAnalyticsEvents(
            categoryName = CATEGORY,
            event = "Activate Card Banner Button Clicked",
        )

        class CardInTransitDetailsShowed : TangemPayAnalyticsEvents(
            categoryName = CATEGORY,
            event = "Card In Transit Details Showed",
        )

        class ActivateCardManagementButtonClicked : TangemPayAnalyticsEvents(
            categoryName = CATEGORY,
            event = "Activate Card Management Button Clicked",
        )

        class CardActivationScreenOpened : TangemPayAnalyticsEvents(
            categoryName = CATEGORY,
            event = "Card Activation Screen Opened",
        )

        class CardLast4DigitsEntered : TangemPayAnalyticsEvents(
            categoryName = CATEGORY,
            event = "Card Last 4 Digits Entered",
        )

        class ActivationContinueClicked : TangemPayAnalyticsEvents(
            categoryName = CATEGORY,
            event = "Activation Continue Clicked",
        )

        class Last4DigitsValidationErrorShowed : TangemPayAnalyticsEvents(
            categoryName = CATEGORY,
            event = "Last 4 Digits Validation Error Showed",
        )

        class CardActivationSuccess : TangemPayAnalyticsEvents(
            categoryName = CATEGORY,
            event = "Card Activation Success",
        )
    }

    object Cashback {

        private const val CATEGORY = "Visa Cashback"

        class BannerShowed : TangemPayAnalyticsEvents(
            categoryName = CATEGORY,
            event = "Cashback Banner Showed",
        )

        class DeactivationBannerShowed : TangemPayAnalyticsEvents(
            categoryName = CATEGORY,
            event = "Cashback Deactivation Banner Showed",
        )

        class BannerClicked : TangemPayAnalyticsEvents(
            categoryName = CATEGORY,
            event = "Cashback Banner Clicked",
        )

        class DeactivationBannerGotItClicked : TangemPayAnalyticsEvents(
            categoryName = CATEGORY,
            event = "Cashback Deactivation Banner Got It Clicked",
        )

        class DetailsScreenOpened : TangemPayAnalyticsEvents(
            categoryName = CATEGORY,
            event = "Cashback Details Screen Opened",
        )

        class ConditionsTileClicked : TangemPayAnalyticsEvents(
            categoryName = CATEGORY,
            event = "Cashback Conditions Tile Clicked",
        )

        class AccrualsTileClicked : TangemPayAnalyticsEvents(
            categoryName = CATEGORY,
            event = "Cashback Accruals Tile Clicked",
        )

        class UpcomingAccrualBannerShowed : TangemPayAnalyticsEvents(
            categoryName = CATEGORY,
            event = "Cashback Upcoming Accrual Banner Showed",
        )

        class NegativeBannerShowed : TangemPayAnalyticsEvents(
            categoryName = CATEGORY,
            event = "Cashback Negative Banner Showed",
        )

        class LoadingErrorShowed : TangemPayAnalyticsEvents(
            categoryName = CATEGORY,
            event = "Cashback Loading Error Showed",
        )

        data class TermsDocClicked(val title: String) : TangemPayAnalyticsEvents(
            categoryName = CATEGORY,
            event = "Cashback Terms Doc Clicked",
            params = mapOf("title" to title),
        )

        class ButtonInSettingsShowed : TangemPayAnalyticsEvents(
            categoryName = CATEGORY,
            event = "Cashback Button In Settings Showed",
        )

        class ButtonInSettingsClicked : TangemPayAnalyticsEvents(
            categoryName = CATEGORY,
            event = "Cashback Button In Settings Clicked",
        )

        class BannerErrorStateShowed : TangemPayAnalyticsEvents(
            categoryName = CATEGORY,
            event = "Cashback Banner Error State Showed",
        )
    }

    object Eligibility {

        private const val CATEGORY = "VA Onboarding"

        class CountrySelectScreenOpened : TangemPayAnalyticsEvents(
            categoryName = CATEGORY,
            event = "VA Country Select Screen Opened",
        )

        data class CountrySelected(
            val country: String,
            val isSuggested: Boolean,
        ) : TangemPayAnalyticsEvents(
            categoryName = CATEGORY,
            event = "VA Country Selected",
            params = mapOf(
                "country" to country,
                "if_suggested" to isSuggested.toYesNo(),
            ),
        )

        class StateScreenOpened : TangemPayAnalyticsEvents(
            categoryName = CATEGORY,
            event = "VA State Screen Opened",
        )

        data class StateSelected(
            val state: String,
            val isSuggested: Boolean,
        ) : TangemPayAnalyticsEvents(
            categoryName = CATEGORY,
            event = "VA State Selected",
            params = mapOf(
                "state" to state,
                "if_suggested" to isSuggested.toYesNo(),
            ),
        )

        data class EligibilityCheckResult(
            val isEligible: Boolean,
            val country: String,
            val state: String?,
        ) : TangemPayAnalyticsEvents(
            categoryName = CATEGORY,
            event = "VA Eligibility Check Result",
            params = buildMap {
                put("result", if (isEligible) "eligible" else "not_eligible")
                put("country", country)
                if (state != null) put("state", state)
            },
        )

        class RegionNotAvailableScreenOpened : TangemPayAnalyticsEvents(
            categoryName = CATEGORY,
            event = "VA Region Not Available Screen Opened",
        )

        class RegionNotAvailableSelectAnotherTapped : TangemPayAnalyticsEvents(
            categoryName = CATEGORY,
            event = "VA Region Not Available Select Another Tapped",
        )

        class RegionNotAvailableJoinWaitlistTapped : TangemPayAnalyticsEvents(
            categoryName = CATEGORY,
            event = "VA Region Not Available Join Waitlist Tapped",
        )

        class WaitlistScreenOpened : TangemPayAnalyticsEvents(
            categoryName = CATEGORY,
            event = "VA Waitlist Screen Opened",
        )

        class WaitlistSaved : TangemPayAnalyticsEvents(
            categoryName = CATEGORY,
            event = "VA Waitlist Saved",
        )
    }

    object Multichain {

        private const val CATEGORY = "Visa Multichain"

        class ChooseNetworkPopupShowed : TangemPayAnalyticsEvents(
            categoryName = CATEGORY,
            event = "Choose Network Popup Showed",
        )

        data class FastWayNetworkClicked(val blockchain: String, val chainId: Long) : TangemPayAnalyticsEvents(
            categoryName = CATEGORY,
            event = "Fast Way Network Clicked",
            params = networkParams(blockchain = blockchain, chainId = chainId),
        )

        data class OtherWayNetworkClicked(val blockchain: String, val chainId: Long) : TangemPayAnalyticsEvents(
            categoryName = CATEGORY,
            event = "Other Way Network Clicked",
            params = networkParams(blockchain = blockchain, chainId = chainId),
        )

        data class FastWayNetworkAddressPopupShowed(
            val blockchain: String,
            val chainId: Long,
        ) : TangemPayAnalyticsEvents(
            categoryName = CATEGORY,
            event = "Fast Way Network Address Popup Showed",
            params = networkParams(blockchain = blockchain, chainId = chainId),
        )

        data class AddressFetchErrorShowed(val blockchain: String, val chainId: Long) : TangemPayAnalyticsEvents(
            categoryName = CATEGORY,
            event = "Address Fetch Error Showed",
            params = networkParams(blockchain = blockchain, chainId = chainId),
        )

        class OtherWaySwapPopupShowed : TangemPayAnalyticsEvents(
            categoryName = CATEGORY,
            event = "Other Way 1:1 Swap Popup Showed",
        )

        private fun networkParams(blockchain: String, chainId: Long): Map<String, String> = mapOf(
            AnalyticsParam.Key.BLOCKCHAIN to blockchain,
            "Chain Id" to chainId.toString(),
        )
    }

    object Offramp {

        private const val CATEGORY = "Visa VA Offramp"
        private const val FIRST_PARTY = "1st party"
        private const val THIRD_PARTY = "3rd party"
        private const val RAIL_ACH = "ACH"
        private const val RAIL_WIRE = "Wire"

        class BankTransferSheetOpened : TangemPayAnalyticsEvents(
            categoryName = CATEGORY,
            event = "Bank Transfer Sheet Opened",
        )

        class ToPersonalAccountClicked : TangemPayAnalyticsEvents(
            categoryName = CATEGORY,
            event = "To Personal Account Clicked",
        )

        class ToBusinessAccountClicked : TangemPayAnalyticsEvents(
            categoryName = CATEGORY,
            event = "To Business Account Clicked",
        )

        class SavedContactClicked : TangemPayAnalyticsEvents(
            categoryName = CATEGORY,
            event = "Saved Contact Clicked",
        )

        class SavedContactsSeeAllClicked : TangemPayAnalyticsEvents(
            categoryName = CATEGORY,
            event = "Saved Contacts See All Clicked",
        )

        class SavedContactsScreenOpened : TangemPayAnalyticsEvents(
            categoryName = CATEGORY,
            event = "Saved Contacts Screen Opened",
        )

        class SearchBarClicked : TangemPayAnalyticsEvents(
            categoryName = CATEGORY,
            event = "Search Bar Clicked",
        )

        class SavedContactOnContactsScreenClicked : TangemPayAnalyticsEvents(
            categoryName = CATEGORY,
            event = "Saved Contact On Contacts Screen Clicked",
        )

        class AmountEntered : TangemPayAnalyticsEvents(
            categoryName = CATEGORY,
            event = "Amount Entered",
        )

        class CoinSelectorClicked : TangemPayAnalyticsEvents(
            categoryName = CATEGORY,
            event = "Coin Selector Clicked",
        )

        class CoinSelectionSheetOpened : TangemPayAnalyticsEvents(
            categoryName = CATEGORY,
            event = "Coin Selection Sheet Opened",
        )

        class CoinInSelectionSheetClicked : TangemPayAnalyticsEvents(
            categoryName = CATEGORY,
            event = "Coin In Selection Sheet Clicked",
        )

        class AmountContinueClicked : TangemPayAnalyticsEvents(
            categoryName = CATEGORY,
            event = "Amount Continue Clicked",
        )

        class PersonalAccountTypeScreenOpened : TangemPayAnalyticsEvents(
            categoryName = CATEGORY,
            event = "Personal Account Type Screen Opened",
        )

        data class PersonalAccountTypeContinueClicked(val isThirdParty: Boolean) : TangemPayAnalyticsEvents(
            categoryName = CATEGORY,
            event = "Personal Account Type Continue Clicked",
            params = transferTypeParams(isThirdParty = isThirdParty),
        )

        class RecipientDetailsScreenOpened : TangemPayAnalyticsEvents(
            categoryName = CATEGORY,
            event = "Recipient Details Screen Opened",
        )

        data class RecipientDetailsFieldFilled(val field: String) : TangemPayAnalyticsEvents(
            categoryName = CATEGORY,
            event = "Recipient Details Field Filled",
            params = mapOf(AnalyticsParam.Key.FIELD to field),
        )

        class RecipientDetailsContinueClicked : TangemPayAnalyticsEvents(
            categoryName = CATEGORY,
            event = "Recipient Details Continue Clicked",
        )

        class RecipientBankDetailsScreenOpened : TangemPayAnalyticsEvents(
            categoryName = CATEGORY,
            event = "Recipient Bank Details Screen Opened",
        )

        data class RecipientBankDetailsFieldFilled(val field: String) : TangemPayAnalyticsEvents(
            categoryName = CATEGORY,
            event = "Recipient Bank Details Field Filled",
            params = mapOf(AnalyticsParam.Key.FIELD to field),
        )

        class RecipientBankDetailsContinueClicked : TangemPayAnalyticsEvents(
            categoryName = CATEGORY,
            event = "Recipient Bank Details Continue Clicked",
        )

        class ConfirmationScreenOpened : TangemPayAnalyticsEvents(
            categoryName = CATEGORY,
            event = "Confirmation Screen Opened",
        )

        class RailSelectorClicked : TangemPayAnalyticsEvents(
            categoryName = CATEGORY,
            event = "Rail Selector Clicked",
        )

        class RecipientDetailsOnConfirmationClicked : TangemPayAnalyticsEvents(
            categoryName = CATEGORY,
            event = "Recipient Details On Confirmation Clicked",
        )

        class RecipientBankDetailsOnConfirmationClicked : TangemPayAnalyticsEvents(
            categoryName = CATEGORY,
            event = "Recipient Bank Details On Confirmation Clicked",
        )

        class RailSelectionSheetOpened : TangemPayAnalyticsEvents(
            categoryName = CATEGORY,
            event = "Rail Selection Sheet Opened",
        )

        class RailInSelectionSheetClicked : TangemPayAnalyticsEvents(
            categoryName = CATEGORY,
            event = "Rail In Selection Sheet Clicked",
        )

        class InsufficientFundsStateShowed : TangemPayAnalyticsEvents(
            categoryName = CATEGORY,
            event = "Insufficient Funds State Showed",
        )

        class SendOnConfirmationClicked : TangemPayAnalyticsEvents(
            categoryName = CATEGORY,
            event = "Send On Confirmation Clicked",
        )

        class SendErrorScreenOpened : TangemPayAnalyticsEvents(
            categoryName = CATEGORY,
            event = "Send Error Screen Opened",
        )

        data class SendSuccessScreenShowed(val rail: BankRail?) : TangemPayAnalyticsEvents(
            categoryName = CATEGORY,
            event = "Send Success Screen Showed",
            params = railParams(rail = rail),
        )

        private fun transferTypeParams(isThirdParty: Boolean): Map<String, String> = mapOf(
            AnalyticsParam.Key.TYPE to if (isThirdParty) THIRD_PARTY else FIRST_PARTY,
        )

        private fun railParams(rail: BankRail?): Map<String, String> {
            val railName = when (rail) {
                BankRail.ACH -> RAIL_ACH
                BankRail.WIRE -> RAIL_WIRE
                null -> return emptyMap()
            }

            return mapOf(AnalyticsParam.Key.TYPE to railName)
        }
    }
}

private fun Boolean.toYesNo(): String = if (this) "Yes" else "No"