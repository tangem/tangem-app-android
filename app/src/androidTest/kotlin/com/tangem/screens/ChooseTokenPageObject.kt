package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.test.BaseSearchBarTestTags
import com.tangem.core.ui.test.BuyTokenScreenTestTags
import com.tangem.core.ui.test.TokenElementsTestTags
import com.tangem.core.ui.test.TopAppBarTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import androidx.compose.ui.test.hasTestTag as withTestTag
import androidx.compose.ui.test.hasText as withText

/**
 * "You receive" token chooser opened from the main-screen "Add funds" button.
 */
class ChooseTokenPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<ChooseTokenPageObject>(semanticsProvider = semanticsProvider) {

    val topAppBarTitle: KNode = child {
        hasTestTag(TopAppBarTestTags.TITLE)
        useUnmergedTree = true
    }

    val searchBar: KNode = child {
        hasTestTag(BaseSearchBarTestTags.SEARCH_BAR)
    }

    fun tokenWithTitle(tokenTitle: String): KNode = child {
        hasTestTag(BuyTokenScreenTestTags.LAZY_LIST_ITEM)
        hasAnyDescendant(withTestTag(TokenElementsTestTags.TOKEN_TITLE))
        hasAnyDescendant(withText(tokenTitle))
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onChooseTokenScreen(function: ChooseTokenPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)