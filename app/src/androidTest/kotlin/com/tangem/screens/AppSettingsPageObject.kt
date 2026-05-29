package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.test.AppSettingsScreenTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode

class AppSettingsPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<AppSettingsPageObject>(semanticsProvider = semanticsProvider) {

    val currencyButton: KNode = child {
        hasTestTag(AppSettingsScreenTestTags.CURRENCY_BUTTON)
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onAppSettingsScreen(function: AppSettingsPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)