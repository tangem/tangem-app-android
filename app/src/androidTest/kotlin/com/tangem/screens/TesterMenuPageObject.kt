package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.feature.tester.impl.R as TesterImplR
import com.tangem.core.ui.test.BaseButtonTestTags
import com.tangem.core.ui.test.TopAppBarTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString

class TesterMenuPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<TesterMenuPageObject>(semanticsProvider = semanticsProvider) {

    val backButton: KNode = child {
        hasTestTag(TopAppBarTestTags.CLOSE_BUTTON)
        useUnmergedTree = true
    }

    val addressesInfoButton: KNode = child {
        hasTestTag(BaseButtonTestTags.TEXT)
        hasText(getResourceString(TesterImplR.string.addresses_info))
        useUnmergedTree = true
    }

    val jsonTab: KNode = child {
        hasText("JSON")
        useUnmergedTree = true
    }

    val copyButton: KNode = child {
        hasContentDescription("Copy")
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onTesterMenuScreen(function: TesterMenuPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)