package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.hasClickAction
import com.tangem.common.BaseTestCase
import com.tangem.wallet.R
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString

class AddToPortfolioPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<AddToPortfolioPageObject>(semanticsProvider = semanticsProvider) {

    fun walletName(walletName: String): KNode = child {
        hasText(walletName)
        useUnmergedTree = true
    }

    val addButton: KNode = child {
        hasText(getResourceString(R.string.common_add))
        hasClickAction()
    }
}

internal fun BaseTestCase.onAddToPortfolioScreen(function: AddToPortfolioPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)