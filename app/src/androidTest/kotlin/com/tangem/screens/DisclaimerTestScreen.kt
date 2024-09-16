package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.core.ui.test.TestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.KNode

class DisclaimerTestScreen(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<DisclaimerTestScreen>(
        semanticsProvider = semanticsProvider,
        viewBuilderAction = { hasTestTag(TestTags.DISCLAIMER_SCREEN_CONTAINER) }
    ) {

    val acceptButton: KNode = child {
        hasTestTag(TestTags.DISCLAIMER_SCREEN_ACCEPT_BUTTON)
    }
}