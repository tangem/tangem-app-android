package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.test.SecurityModeScreenTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode

class SecurityModePageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<SecurityModePageObject>(semanticsProvider = semanticsProvider) {

    val screenContainer: KNode = child {
        hasTestTag(SecurityModeScreenTestTags.SCREEN_CONTAINER)
    }
}

internal fun BaseTestCase.onSecurityModeScreen(function: SecurityModePageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)