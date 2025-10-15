package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.R
import com.tangem.core.ui.test.BaseButtonTestTags
import com.tangem.core.ui.test.TokenReceiveWarningBottomSheetTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString

class TokenReceiveWarningBottomSheetPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<TokenReceiveWarningBottomSheetPageObject>(semanticsProvider = semanticsProvider) {

    val bottomSheet: KNode = child {
        hasTestTag(TokenReceiveWarningBottomSheetTestTags.BOTTOM_SHEET)
    }

    val gotItButton: KNode = child {
        hasTestTag(BaseButtonTestTags.TEXT)
        hasText(getResourceString(R.string.common_got_it))
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onTokenReceiveWarningBottomSheet(function: TokenReceiveWarningBottomSheetPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)