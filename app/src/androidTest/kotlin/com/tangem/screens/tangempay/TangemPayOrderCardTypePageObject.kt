package com.tangem.screens.tangempay

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.res.R as CoreResR
import com.tangem.core.ui.test.TangemPayTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString

/** 'Order new card' screen — the card-type step that precedes the issue bottom sheet. */
class TangemPayOrderCardTypePageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<TangemPayOrderCardTypePageObject>(semanticsProvider = semanticsProvider) {

    val title: KNode = child {
        hasText(getResourceString(CoreResR.string.tangempay_order_type_title))
        useUnmergedTree = true
    }

    val virtualTab: KNode = child {
        hasText(getResourceString(CoreResR.string.tangempay_order_type_segment_virtual))
        useUnmergedTree = true
    }

    val selectButton: KNode = child {
        hasTestTag(TangemPayTestTags.ORDER_CARD_TYPE_SELECT_BUTTON)
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onTangemPayOrderCardTypeScreen(function: TangemPayOrderCardTypePageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)