package com.tangem.screens.tangempay

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.test.BaseActionButtonsBlockTestTags
import com.tangem.core.ui.test.TangemPayTestTags
import com.tangem.core.res.R as CoreResR
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString
import androidx.compose.ui.test.hasText as withText

class TangemPayMainPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<TangemPayMainPageObject>(semanticsProvider = semanticsProvider) {

    val mainScreenTile: KNode = child {
        hasTestTag(TangemPayTestTags.MAIN_SCREEN_TILE)
        useUnmergedTree = true
    }

    val balance: KNode = child {
        hasTestTag(TangemPayTestTags.PAYMENT_ACCOUNT_BALANCE)
        useUnmergedTree = true
    }

    val cardButton: KNode = child {
        hasTestTag(TangemPayTestTags.PAYMENT_ACCOUNT_CARD_BUTTON)
        useUnmergedTree = true
    }

    val topUpButton: KNode = child {
        hasTestTag(BaseActionButtonsBlockTestTags.ACTION_BUTTON)
        hasAnyDescendant(withText(getResourceString(CoreResR.string.tangempay_card_details_add_funds)))
        useUnmergedTree = true
    }

    val withdrawButton: KNode = child {
        hasTestTag(BaseActionButtonsBlockTestTags.ACTION_BUTTON)
        hasAnyDescendant(withText(getResourceString(CoreResR.string.tangempay_card_details_withdraw)))
        useUnmergedTree = true
    }

    fun transactionRowWithText(text: String): KNode = child {
        hasText(text)
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onTangemPayMainScreen(function: TangemPayMainPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)