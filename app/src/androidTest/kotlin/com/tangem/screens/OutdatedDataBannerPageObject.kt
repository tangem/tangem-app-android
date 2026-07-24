package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString
import com.tangem.core.res.R as CoreResR

// Cross-screen "Balances may be outdated" banner (DS3 TangemMessageBanner) — appears on both the
// main and token-details screens. It puts no testTag on its text nodes, so match by title / message.
class OutdatedDataBannerPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<OutdatedDataBannerPageObject>(semanticsProvider = semanticsProvider) {

    val title: KNode = child {
        hasText(getResourceString(CoreResR.string.warning_outdated_data_title))
        useUnmergedTree = true
    }

    val message: KNode = child {
        hasText(getResourceString(CoreResR.string.warning_outdated_data_message))
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onOutdatedDataBanner(function: OutdatedDataBannerPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)