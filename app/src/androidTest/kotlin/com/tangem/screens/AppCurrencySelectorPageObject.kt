package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.test.AppCurrencySelectorScreenTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import androidx.compose.ui.test.hasText as withText

class AppCurrencySelectorPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<AppCurrencySelectorPageObject>(semanticsProvider = semanticsProvider) {

    val searchActionButton: KNode = child {
        hasTestTag(AppCurrencySelectorScreenTestTags.TOP_BAR_ACTION_BUTTON)
    }

    val searchField: KNode = child {
        hasTestTag(AppCurrencySelectorScreenTestTags.SEARCH_FIELD)
    }

    fun currencyItem(code: String): KNode = child {
        hasTestTag(AppCurrencySelectorScreenTestTags.CURRENCY_ITEM)
        hasAnyDescendant(withText(code, substring = true))
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onAppCurrencySelectorScreen(function: AppCurrencySelectorPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)