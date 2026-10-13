package com.tangem.screens.tangempay

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString
import com.tangem.core.res.R as CoreResR

class TangemPayResidencyPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<TangemPayResidencyPageObject>(semanticsProvider = semanticsProvider) {

    val countryTitle: KNode = child {
        hasText(getResourceString(CoreResR.string.tangempay_residency_title_country))
        useUnmergedTree = true
    }

    val regionTitle: KNode = child {
        hasText(getResourceString(CoreResR.string.tangempay_residency_title_state))
        useUnmergedTree = true
    }

    // The selected item leads the list in a section of its own and stays in the full list, so the name can repeat.
    fun item(name: String): KNode = child {
        hasText(name)
        hasPosition(0)
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onTangemPayResidencyScreen(function: TangemPayResidencyPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)