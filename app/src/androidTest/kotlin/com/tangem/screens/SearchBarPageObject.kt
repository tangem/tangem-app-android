package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.test.BaseSearchBarTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode

class SearchBarPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<SearchBarPageObject>(semanticsProvider = semanticsProvider) {

    val searchField: KNode = child {
        hasTestTag(BaseSearchBarTestTags.SEARCH_BAR)
    }
}

internal fun BaseTestCase.onSearchBar(function: SearchBarPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)