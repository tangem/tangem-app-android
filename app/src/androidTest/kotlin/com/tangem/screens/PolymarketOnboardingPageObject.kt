package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.test.PredictionTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode

class PolymarketOnboardingPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<PolymarketOnboardingPageObject>(semanticsProvider = semanticsProvider) {

    val welcomeScreen: KNode = child {
        hasTestTag(PredictionTestTags.WELCOME_SCREEN)
    }

    val reconnectScreen: KNode = child {
        hasTestTag(PredictionTestTags.RECONNECT_SCREEN)
    }

    val actionButton: KNode = child {
        hasTestTag(PredictionTestTags.ONBOARDING_ACTION_BUTTON)
    }

    val regionRestrictionsSheet: KNode = child {
        hasTestTag(PredictionTestTags.REGION_RESTRICTIONS_SHEET)
    }
}

internal fun BaseTestCase.onPolymarketOnboardingScreen(function: PolymarketOnboardingPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)