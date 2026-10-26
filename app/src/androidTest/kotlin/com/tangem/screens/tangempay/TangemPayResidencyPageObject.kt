package com.tangem.screens.tangempay

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.test.TangemPayTestTags
import com.tangem.core.ui.test.TopNavigationTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString
import com.tangem.core.res.R as CoreResR
import androidx.compose.ui.test.hasSetTextAction as withSetTextAction
import androidx.compose.ui.test.hasTestTag as withTestTag
import androidx.compose.ui.test.hasText as withText

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

    val suggestedCountryHeader: KNode = child {
        hasText(getResourceString(CoreResR.string.tangempay_residency_suggested_country))
        useUnmergedTree = true
    }

    val allCountriesHeader: KNode = child {
        hasText(getResourceString(CoreResR.string.tangempay_residency_all_country))
        useUnmergedTree = true
    }

    val suggestedStateHeader: KNode = child {
        hasText(getResourceString(CoreResR.string.tangempay_residency_suggested_state))
        useUnmergedTree = true
    }

    val allStatesHeader: KNode = child {
        hasText(getResourceString(CoreResR.string.tangempay_residency_all_state))
        useUnmergedTree = true
    }

    val searchField: KNode = child {
        addSemanticsMatcher(withSetTextAction())
        hasAnyAncestor(withTestTag(TangemPayTestTags.RESIDENCY_SEARCH))
        useUnmergedTree = true
    }

    val emptyState: KNode = child {
        hasTestTag(TangemPayTestTags.RESIDENCY_EMPTY)
        hasAnyDescendant(withText(getResourceString(CoreResR.string.tangempay_residency_search_empty)))
        useUnmergedTree = true
    }

    val closeButton: KNode = child {
        hasTestTag(TopNavigationTestTags.CLOSE_BUTTON)
        useUnmergedTree = true
    }

    // The selected item leads the list in a section of its own and stays in the full list, so the name can repeat.
    fun item(name: String): KNode = child {
        hasText(name)
        hasPosition(0)
        useUnmergedTree = true
    }

    fun row(name: String, index: Int = 0): KNode = child {
        hasTestTag(TangemPayTestTags.RESIDENCY_ROW)
        hasAnyDescendant(withText(name))
        hasPosition(index)
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onTangemPayResidencyScreen(function: TangemPayResidencyPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)