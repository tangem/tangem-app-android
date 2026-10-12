package com.tangem.screens.tangempay

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.test.TangemPayTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import androidx.compose.ui.test.hasTestTag as withTestTag
import androidx.compose.ui.test.hasText as withText

class TangemPayComparePlansSheetPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<TangemPayComparePlansSheetPageObject>(semanticsProvider = semanticsProvider) {

    private val content: KNode = child {
        hasTestTag(TangemPayTestTags.COMPARE_PLANS_CONTENT)
        useUnmergedTree = true
    }

    fun sectionTitle(title: String): KNode = child {
        hasTestTag(TangemPayTestTags.COMPARE_PLANS_SECTION_TITLE)
        hasText(title)
        useUnmergedTree = true
    }

    fun planRow(planName: String): KNode = child {
        hasTestTag(TangemPayTestTags.COMPARE_PLANS_ROW)
        hasAnyDescendant(withText(planName))
        useUnmergedTree = true
    }

    @OptIn(ExperimentalTestApi::class)
    fun scrollToSection(title: String) = content {
        performScrollToNode(withTestTag(TangemPayTestTags.COMPARE_PLANS_SECTION_TITLE) and withText(title))
    }
}

internal fun BaseTestCase.onTangemPayComparePlansSheet(
    function: TangemPayComparePlansSheetPageObject.() -> Unit,
) = onComposeScreen(composeTestRule, function)