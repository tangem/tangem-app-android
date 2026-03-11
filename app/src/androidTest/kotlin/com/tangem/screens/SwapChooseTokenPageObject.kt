package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.R
import com.tangem.core.ui.test.TokenElementsTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString
import androidx.compose.ui.test.hasText as withText

class SwapChooseTokenPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<SwapChooseTokenPageObject>(semanticsProvider = semanticsProvider) {

    val title: KNode = child {
        hasText(getResourceString(R.string.common_choose_token))
    }

    val myTokensTitle: KNode = child {
        hasText(getResourceString(R.string.exchange_tokens_available_tokens_header))
    }

    fun tokenWithTitle(tokenTitle: String): KNode = child {
            hasTestTag(TokenElementsTestTags.TOKEN_TITLE)
            hasAnyDescendant(withText(tokenTitle))
            useUnmergedTree = true
        }
}

internal fun BaseTestCase.onSwapChooseTokenScreen(function: SwapChooseTokenPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)