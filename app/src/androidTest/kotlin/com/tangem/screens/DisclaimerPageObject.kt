package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.test.DisclaimerScreenTestTags
import com.tangem.core.ui.test.TopAppBarTestTags
import com.tangem.features.disclaimer.impl.R
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString

class DisclaimerPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<DisclaimerPageObject>(
        semanticsProvider = semanticsProvider,
        viewBuilderAction = { hasTestTag(DisclaimerScreenTestTags.SCREEN_CONTAINER) }
    ) {

    val title: KNode = child {
        hasTestTag(TopAppBarTestTags.TITLE)
        hasText(getResourceString(R.string.disclaimer_title))
    }

    val webView: KNode = child {
        hasTestTag(DisclaimerScreenTestTags.WEB_VIEW)
    }

    val acceptButton: KNode = child {
        hasTestTag(DisclaimerScreenTestTags.ACCEPT_BUTTON)
    }
}

internal fun BaseTestCase.onDisclaimerScreen(function: DisclaimerPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)