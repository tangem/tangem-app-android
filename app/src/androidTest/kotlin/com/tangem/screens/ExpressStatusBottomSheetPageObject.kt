package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.res.R
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString

class ExpressStatusBottomSheetPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<ExpressStatusBottomSheetPageObject>(semanticsProvider = semanticsProvider) {

    val title: KNode = child {
        hasText(getResourceString(R.string.express_exchange_status_title))
        useUnmergedTree = true
    }

    fun providerName(name: String): KNode = child {
        hasText(name)
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onExpressStatusBottomSheet(function: ExpressStatusBottomSheetPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)