package com.tangem.screens.tangempay

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.res.R as CoreResR
import com.tangem.core.ui.test.TangemPayTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString
import androidx.compose.ui.test.hasTestTag as withTestTag

// The guide reuses the card block, so its CARD_DETAILS_* tags collide with the card page — scope to the guide container.
class TangemPayAddToWalletGuidePageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<TangemPayAddToWalletGuidePageObject>(semanticsProvider = semanticsProvider) {

    val container: KNode = child {
        hasTestTag(TangemPayTestTags.ADD_TO_WALLET_SCREEN)
        useUnmergedTree = true
    }

    val closeButton: KNode = child {
        hasTestTag(TangemPayTestTags.ADD_TO_WALLET_CLOSE_BUTTON)
        useUnmergedTree = true
    }

    val showDetailsButton: KNode = child {
        hasAnyAncestor(withTestTag(TangemPayTestTags.ADD_TO_WALLET_SCREEN))
        hasTestTag(TangemPayTestTags.CARD_DETAILS_SHOW_BUTTON)
        useUnmergedTree = true
    }

    val numberValue: KNode = child {
        hasAnyAncestor(withTestTag(TangemPayTestTags.ADD_TO_WALLET_SCREEN))
        hasTestTag(TangemPayTestTags.CARD_DETAILS_NUMBER_VALUE)
        useUnmergedTree = true
    }

    val expirationValue: KNode = child {
        hasAnyAncestor(withTestTag(TangemPayTestTags.ADD_TO_WALLET_SCREEN))
        hasTestTag(TangemPayTestTags.CARD_DETAILS_EXPIRATION_VALUE)
        useUnmergedTree = true
    }

    val cvcValue: KNode = child {
        hasAnyAncestor(withTestTag(TangemPayTestTags.ADD_TO_WALLET_SCREEN))
        hasTestTag(TangemPayTestTags.CARD_DETAILS_CVC_VALUE)
        useUnmergedTree = true
    }

    val cardNumberShort: KNode = child {
        hasAnyAncestor(withTestTag(TangemPayTestTags.ADD_TO_WALLET_SCREEN))
        hasTestTag(TangemPayTestTags.CARD_NUMBER_SHORT)
        useUnmergedTree = true
    }

    val title: KNode = child {
        hasAnyAncestor(withTestTag(TangemPayTestTags.ADD_TO_WALLET_SCREEN))
        hasText(getResourceString(CoreResR.string.tangempay_card_details_open_wallet_title))
        useUnmergedTree = true
    }

    // Step 1's text equals the 'Open Google Wallet' button label, so scope steps to the step row.
    fun stepWithText(text: String): KNode = child {
        hasAnyAncestor(withTestTag(TangemPayTestTags.ADD_TO_WALLET_STEP))
        hasText(text)
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onTangemPayAddToWalletGuideScreen(
    function: TangemPayAddToWalletGuidePageObject.() -> Unit,
) = onComposeScreen(composeTestRule, function)