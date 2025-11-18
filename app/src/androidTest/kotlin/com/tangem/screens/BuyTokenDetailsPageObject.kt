package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.R
import com.tangem.core.ui.test.*
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString
import androidx.compose.ui.test.hasTestTag as withTestTag
import com.tangem.features.onramp.impl.R as OnrampImplR

class BuyTokenDetailsPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<BuyTokenDetailsPageObject>(semanticsProvider = semanticsProvider) {

    val topBarTitle: KNode = child {
        hasTestTag(TopAppBarTestTags.TITLE)
        useUnmergedTree = true
    }

    val topBarMoreButton: KNode = child {
        hasTestTag(TopAppBarTestTags.MORE_BUTTON)
        useUnmergedTree = true
    }

    val topBarCloseButton: KNode = child {
        hasTestTag(TopAppBarTestTags.CLOSE_BUTTON)
        useUnmergedTree = true
    }

    val errorNotificationTitle: KNode = child {
        hasTestTag(NotificationTestTags.TITLE)
        hasText(getResourceString(R.string.common_error))
        useUnmergedTree = true
    }

    val errorNotificationText: KNode = child {
        hasTestTag(NotificationTestTags.MESSAGE)
        hasText(getResourceString(R.string.common_unknown_error))
        useUnmergedTree = true
    }

    val refreshButton: KNode = child {
        hasTestTag(BaseButtonTestTags.BUTTON)
        hasText(getResourceString(R.string.warning_button_refresh))
    }

    val fiatCurrencyIcon: KNode = child {
        hasTestTag(BuyTokenDetailsScreenTestTags.FIAT_CURRENCY_ICON)
        useUnmergedTree = true
    }

    val expandFiatListButton: KNode = child {
        hasTestTag(BuyTokenDetailsScreenTestTags.EXPAND_FIAT_LIST_BUTTON)
        useUnmergedTree = true
    }

    val fiatAmountTextField: KNode = child {
        hasParent(withTestTag(BuyTokenDetailsScreenTestTags.FIAT_AMOUNT_TEXT_FIELD))
        useUnmergedTree = true
    }

    val recommendedTitle: KNode = child {
        hasText(getResourceString(OnrampImplR.string.onramp_recommended_title))
    }

    val bestRateTitle: KNode = child {
        hasTestTag(OnrampOffersBlockTestTags.BEST_RATE_TITLE)
        useUnmergedTree = true
    }

    val offerTokenAmount: KNode = child {
        hasTestTag(OnrampOffersBlockTestTags.OFFER_TOKEN_AMOUNT)
        useUnmergedTree = true
    }

    val timingIcon: KNode = child {
        hasTestTag(OnrampOffersBlockTestTags.TIMING_ICON)
        useUnmergedTree = true
    }

    val payWith: KNode = child {
        hasTestTag(OnrampOffersBlockTestTags.PAY_WITH)
        useUnmergedTree = true
    }

    val paymentMethodIcon: KNode = child {
        hasTestTag(OnrampOffersBlockTestTags.PAYMENT_METHOD_ICON)
        useUnmergedTree = true
    }

    val instantProcessingSpeedText: KNode = child {
        hasText(getResourceString(R.string.onramp_instant_status))
        useUnmergedTree = true
    }

    val providerName: KNode = child {
        hasTestTag(OnrampOffersBlockTestTags.PROVIDER_NAME)
        useUnmergedTree = true
    }

    val buyButton: KNode = child {
        hasTestTag(BaseButtonTestTags.TEXT)
        hasText(getResourceString(R.string.common_buy))
        useUnmergedTree = true
    }

    val continueButton: KNode = child {
        hasTestTag(BaseButtonTestTags.TEXT)
        hasText(getResourceString(R.string.common_continue))
        useUnmergedTree = true
    }

    val allOffersButton: KNode = child {
        hasTestTag(BaseButtonTestTags.TEXT)
        hasText(getResourceString(R.string.onramp_all_offers_button_title))
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onBuyTokenDetailsScreen(function: BuyTokenDetailsPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)