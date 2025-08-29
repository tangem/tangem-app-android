package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.test.SelectNetworkFeeBottomSheetTestTags
import com.tangem.core.ui.test.TopAppBarTestTags
import com.tangem.wallet.R
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString
import androidx.compose.ui.test.hasText as withText

class SelectNetworkFeePageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<SelectNetworkFeePageObject>(semanticsProvider = semanticsProvider) {

    val title: KNode = child {
        hasTestTag(TopAppBarTestTags.TITLE)
        hasText(getResourceString(R.string.common_fee_selector_title))
        useUnmergedTree = true
    }

    val marketSelectorItem: KNode = child {
        hasTestTag(SelectNetworkFeeBottomSheetTestTags.SELECTOR_ITEM)
        hasAnyChild(withText(getResourceString(R.string.common_fee_selector_option_market)))
        useUnmergedTree = true
    }

    val fastSelectorItem: KNode = child {
        hasTestTag(SelectNetworkFeeBottomSheetTestTags.SELECTOR_ITEM)
        hasAnyChild(withText(getResourceString(R.string.common_fee_selector_option_fast)))
        useUnmergedTree = true
    }

    val readMoreTextBlock: KNode = child {
        hasTestTag(SelectNetworkFeeBottomSheetTestTags.READ_MORE_TEXT)
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onSelectNetworkFeeBottomSheet(function: SelectNetworkFeePageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)