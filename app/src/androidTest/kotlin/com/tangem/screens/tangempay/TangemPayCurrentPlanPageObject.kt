package com.tangem.screens.tangempay

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.test.TangemPayTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import androidx.compose.ui.test.hasTestTag as withTestTag
import androidx.compose.ui.test.hasText as withText

class TangemPayCurrentPlanPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<TangemPayCurrentPlanPageObject>(semanticsProvider = semanticsProvider) {

    val screen: KNode = child {
        hasTestTag(TangemPayTestTags.CURRENT_PLAN_SCREEN)
        useUnmergedTree = true
    }

    /** The active plan is the top-bar subtitle, and the DS top bar exposes no tag of its own for it. */
    fun planName(name: String): KNode = child {
        hasAnyAncestor(withTestTag(TangemPayTestTags.CURRENT_PLAN_SCREEN))
        hasText(name)
        useUnmergedTree = true
    }

    val notification: KNode = child {
        hasTestTag(TangemPayTestTags.CURRENT_PLAN_NOTIFICATION)
        useUnmergedTree = true
    }

    fun notificationText(text: String): KNode = child {
        hasAnyAncestor(withTestTag(TangemPayTestTags.CURRENT_PLAN_NOTIFICATION))
        hasText(text, substring = true)
        useUnmergedTree = true
    }

    val notificationButton: KNode = child {
        hasTestTag(TangemPayTestTags.CURRENT_PLAN_NOTIFICATION_BUTTON)
        useUnmergedTree = true
    }

    fun notificationButtonWithText(text: String): KNode = child {
        hasTestTag(TangemPayTestTags.CURRENT_PLAN_NOTIFICATION_BUTTON)
        hasAnyDescendant(withText(text))
        useUnmergedTree = true
    }

    fun sectionHeader(header: String): KNode = child {
        hasTestTag(TangemPayTestTags.CURRENT_PLAN_SECTION_HEADER)
        hasText(header)
        useUnmergedTree = true
    }

    fun planRow(label: String, value: String): KNode = child {
        hasTestTag(TangemPayTestTags.CURRENT_PLAN_ROW)
        hasAnyDescendant(withText(label))
        hasAnyDescendant(withText(value, substring = true))
        useUnmergedTree = true
    }

    val changePlanButton: KNode = child {
        hasTestTag(TangemPayTestTags.CURRENT_PLAN_CHANGE_BUTTON)
        useUnmergedTree = true
    }

    val fullTermsButton: KNode = child {
        hasTestTag(TangemPayTestTags.CURRENT_PLAN_FULL_TERMS_BUTTON)
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onTangemPayCurrentPlanScreen(function: TangemPayCurrentPlanPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)