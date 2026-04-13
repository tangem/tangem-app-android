package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.test.MarketsTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode

class MarketsSecurityScoreDetailsPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<MarketsSecurityScoreDetailsPageObject>(semanticsProvider = semanticsProvider) {

    val detailsTitle: KNode = child {
        hasTestTag(MarketsTestTags.SECURITY_SCORE_DETAILS_TITLE)
        useUnmergedTree = true
    }

    val firstProviderLink: KNode = child {
        hasTestTag(MarketsTestTags.SECURITY_SCORE_DETAILS_PROVIDER_LINK)
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onMarketsSecurityScoreDetailsScreen(
    function: MarketsSecurityScoreDetailsPageObject.() -> Unit,
) = onComposeScreen(composeTestRule, function)