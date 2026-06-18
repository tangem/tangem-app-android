package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.R
import com.tangem.core.ui.test.BaseBottomSheetTestTags
import com.tangem.core.ui.test.BuyTokenScreenTestTags
import com.tangem.core.ui.test.MarketsTestTags
import com.tangem.core.ui.test.TokenActionsTestTags
import com.tangem.core.ui.test.TokenElementsTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString
import androidx.compose.ui.test.hasTestTag as withTestTag
import androidx.compose.ui.test.hasText as withText

class AddFundsBottomSheetPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<AddFundsBottomSheetPageObject>(
        semanticsProvider = semanticsProvider,
        viewBuilderAction = { hasTestTag(BaseBottomSheetTestTags.CONTAINER) },
    ) {

    val buyButton: KNode = child {
        hasAnyChild(withText(getResourceString(R.string.common_buy)))
        useUnmergedTree = true
    }

    val swapButton: KNode = child {
        hasAnyChild(withText(getResourceString(R.string.common_swap)))
        useUnmergedTree = true
    }

    val receiveButton: KNode = child {
        hasAnyChild(withText(getResourceString(R.string.common_receive)))
        useUnmergedTree = true
    }

    val closeButton: KNode = child {
        hasTestTag(BaseBottomSheetTestTags.CLOSE_BUTTON)
        useUnmergedTree = true
    }

    val buyTokenButton: KNode = child {
        hasTestTag(TokenActionsTestTags.BUY_ACTION)
        useUnmergedTree = true
    }

    fun userTokenWithTitle(tokenTitle: String): KNode = child {
        hasTestTag(BuyTokenScreenTestTags.LAZY_LIST_ITEM)
        hasAnyDescendant(withTestTag(TokenElementsTestTags.TOKEN_TITLE))
        hasAnyDescendant(withText(tokenTitle))
        useUnmergedTree = true
    }

    fun trendingTokenWithTitle(tokenTitle: String): KNode = child {
        hasTestTag(MarketsTestTags.TOKENS_LIST_ITEM)
        hasAnyDescendant(withText(tokenTitle))
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onAddFundsBottomSheet(function: AddFundsBottomSheetPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)