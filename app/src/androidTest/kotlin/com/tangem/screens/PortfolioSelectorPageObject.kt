package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.test.BaseBottomSheetTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode

/**
 * The shared portfolio (account) picker bottom sheet — the one behind 'Add tokens' in Add & Manage and
 * behind the referral 'Account for rewards' row. Scoped to the sheet so account names can be matched by
 * text without colliding with the screen underneath.
 */
class PortfolioSelectorPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<PortfolioSelectorPageObject>(
        semanticsProvider = semanticsProvider,
        viewBuilderAction = { hasTestTag(BaseBottomSheetTestTags.CONTAINER) },
    ) {

    fun portfolioOption(portfolioName: String): KNode = child {
        hasText(portfolioName)
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onPortfolioSelectorSheet(function: PortfolioSelectorPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)