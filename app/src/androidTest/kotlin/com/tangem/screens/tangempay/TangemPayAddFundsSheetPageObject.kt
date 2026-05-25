package com.tangem.screens.tangempay

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.res.R as CoreResR
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString

class TangemPayAddFundsSheetPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<TangemPayAddFundsSheetPageObject>(semanticsProvider = semanticsProvider) {

    val swapOption: KNode = child {
        hasText(getResourceString(CoreResR.string.common_exchange))
        useUnmergedTree = true
    }

    val receiveOption: KNode = child {
        hasText(getResourceString(CoreResR.string.common_receive))
        useUnmergedTree = true
    }

    val title: KNode = child {
        hasText(getResourceString(CoreResR.string.tangempay_card_details_add_funds))
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onTangemPayAddFundsSheet(function: TangemPayAddFundsSheetPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)