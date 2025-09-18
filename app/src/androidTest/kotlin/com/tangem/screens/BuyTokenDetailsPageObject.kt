package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.R
import com.tangem.core.ui.test.BaseButtonTestTags
import com.tangem.core.ui.test.BuyTokenDetailsScreenTestTags
import com.tangem.core.ui.test.NotificationTestTags
import com.tangem.core.ui.test.TopAppBarTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString
import androidx.compose.ui.test.hasTestTag as withTestTag

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

    val tokenAmountField: KNode = child {
        hasParent(withTestTag(BuyTokenDetailsScreenTestTags.TOKEN_AMOUNT))
        useUnmergedTree = true
    }

    val providerLoadingTitle: KNode = child {
        hasTestTag(BuyTokenDetailsScreenTestTags.PROVIDER_LOADING_TITLE)
    }

    val providerLoadingText: KNode = child {
        hasTestTag(BuyTokenDetailsScreenTestTags.PROVIDER_LOADING_TEXT)
    }

    val providerTitle: KNode = child {
        hasTestTag(BuyTokenDetailsScreenTestTags.PROVIDER_TITLE)
        useUnmergedTree = true
    }

    val providerText: KNode = child {
        hasTestTag(BuyTokenDetailsScreenTestTags.PROVIDER_TEXT)
        useUnmergedTree = true
    }

    val buyButton: KNode = child {
        hasTestTag(BaseButtonTestTags.BUTTON)
        hasText(getResourceString(R.string.common_buy))
    }

    val toSBlock: KNode = child {
        hasTestTag(BuyTokenDetailsScreenTestTags.TOS_BLOCK)
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onBuyTokenDetailsScreen(function: BuyTokenDetailsPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)