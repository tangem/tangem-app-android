package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.R
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString

class ConciergeDemandSurveyPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<ConciergeDemandSurveyPageObject>(semanticsProvider = semanticsProvider) {

    val navigationTitle: KNode = child {
        hasText(getResourceString(R.string.survey_demand_navigation_title))
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onConciergeDemandSurveyScreen(function: ConciergeDemandSurveyPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)