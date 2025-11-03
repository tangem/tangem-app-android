package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.R
import com.tangem.core.ui.test.BaseBottomSheetTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString
import androidx.compose.ui.test.hasTestTag as withTestTag
import androidx.compose.ui.test.hasText as withText

class TokenActionsBottomSheetPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<TokenActionsBottomSheetPageObject>(semanticsProvider = semanticsProvider) {

    val analyticsButton: KNode = child {
        hasAnyChild(withText(getResourceString(R.string.common_analytics)))
        hasTestTag(BaseBottomSheetTestTags.ACTION_BUTTON)
        hasAnyChild(withTestTag(BaseBottomSheetTestTags.ACTION_ICON))
        useUnmergedTree = true
    }

    val copyAddressButton: KNode = child {
        hasAnyChild(withText(getResourceString(R.string.common_copy_address)))
        hasTestTag(BaseBottomSheetTestTags.ACTION_BUTTON)
        hasAnyChild(withTestTag(BaseBottomSheetTestTags.ACTION_ICON))
        useUnmergedTree = true
    }

    val receiveButton: KNode = child {
        hasAnyChild(withText(getResourceString(R.string.common_receive)))
        hasTestTag(BaseBottomSheetTestTags.ACTION_BUTTON)
        hasAnyChild(withTestTag(BaseBottomSheetTestTags.ACTION_ICON))
        useUnmergedTree = true
    }

    val sendButton: KNode = child {
        hasAnyChild(withText(getResourceString(R.string.common_send)))
        hasTestTag(BaseBottomSheetTestTags.ACTION_BUTTON)
        hasAnyChild(withTestTag(BaseBottomSheetTestTags.ACTION_ICON))
        useUnmergedTree = true
    }

    val swapButton: KNode = child {
        hasAnyChild(withText(getResourceString(R.string.common_swap)))
        hasTestTag(BaseBottomSheetTestTags.ACTION_BUTTON)
        hasAnyChild(withTestTag(BaseBottomSheetTestTags.ACTION_ICON))
        useUnmergedTree = true
    }

    val buyButton: KNode = child {
        hasAnyChild(withText(getResourceString(R.string.common_buy)))
        hasTestTag(BaseBottomSheetTestTags.ACTION_BUTTON)
        hasAnyChild(withTestTag(BaseBottomSheetTestTags.ACTION_ICON))
        useUnmergedTree = true
    }

    val sellButton: KNode = child {
        hasAnyChild(withText(getResourceString(R.string.common_sell)))
        hasTestTag(BaseBottomSheetTestTags.ACTION_BUTTON)
        hasAnyChild(withTestTag(BaseBottomSheetTestTags.ACTION_ICON))
        useUnmergedTree = true
    }

    val hideTokenButton: KNode = child {
        hasAnyChild(withText(getResourceString(R.string.token_details_hide_token)))
        hasTestTag(BaseBottomSheetTestTags.ACTION_BUTTON)
        hasAnyChild(withTestTag(BaseBottomSheetTestTags.ACTION_ICON))
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onTokenActionsBottomSheet(function: TokenActionsBottomSheetPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)