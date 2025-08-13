package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.test.DisclaimerScreenTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode

class DisclaimerPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<DisclaimerPageObject>(
        semanticsProvider = semanticsProvider,
        viewBuilderAction = { hasTestTag(DisclaimerScreenTestTags.SCREEN_CONTAINER) }
    ) {

    val acceptButton: KNode = child {
        hasTestTag(DisclaimerScreenTestTags.ACCEPT_BUTTON)
    }
}

internal fun BaseTestCase.onDisclaimerScreen(function: DisclaimerPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)