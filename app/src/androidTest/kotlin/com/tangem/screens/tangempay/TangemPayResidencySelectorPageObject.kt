package com.tangem.screens.tangempay

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.test.TangemPayTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import androidx.compose.ui.test.hasText as withText

class TangemPayResidencySelectorPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<TangemPayResidencySelectorPageObject>(semanticsProvider = semanticsProvider) {

    fun row(name: String): KNode = child {
        hasTestTag(TangemPayTestTags.RESIDENCY_ROW)
        hasAnyDescendant(withText(name))
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onTangemPayResidencySelectorScreen(
    function: TangemPayResidencySelectorPageObject.() -> Unit,
) = onComposeScreen(composeTestRule, function)