package com.tangem.screens.tangempay

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.test.TangemPayTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import androidx.compose.ui.test.hasText as withText

class TangemPaySelectPlanPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<TangemPaySelectPlanPageObject>(semanticsProvider = semanticsProvider) {

    val screen: KNode = child {
        hasTestTag(TangemPayTestTags.SELECT_PLAN_SCREEN)
        useUnmergedTree = true
    }

    /** Plan name while selecting, transition headline while confirming — the screen reuses one headline slot. */
    val title: KNode = child {
        hasTestTag(TangemPayTestTags.SELECT_PLAN_TITLE)
        useUnmergedTree = true
    }

    val compareButton: KNode = child {
        hasTestTag(TangemPayTestTags.SELECT_PLAN_COMPARE_BUTTON)
        useUnmergedTree = true
    }

    val selectButton: KNode = child {
        hasTestTag(TangemPayTestTags.SELECT_PLAN_SELECT_BUTTON)
        useUnmergedTree = true
    }

    val cancelButton: KNode = child {
        hasTestTag(TangemPayTestTags.SELECT_PLAN_CANCEL_BUTTON)
        useUnmergedTree = true
    }

    val confirmButton: KNode = child {
        hasTestTag(TangemPayTestTags.SELECT_PLAN_CONFIRM_BUTTON)
        useUnmergedTree = true
    }

    fun point(text: String): KNode = child {
        hasTestTag(TangemPayTestTags.SELECT_PLAN_POINT)
        hasAnyDescendant(withText(text, substring = true))
        useUnmergedTree = true
    }

    private val pager: KNode = child {
        hasTestTag(TangemPayTestTags.SELECT_PLAN_PAGER)
        useUnmergedTree = true
    }

    /** Settling the pager is what selects a plan, so the details below the cards follow this scroll. */
    @OptIn(ExperimentalTestApi::class)
    fun scrollToPlanAt(index: Int) = pager {
        performScrollToIndex(index)
    }
}

internal fun BaseTestCase.onTangemPaySelectPlanScreen(function: TangemPaySelectPlanPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)