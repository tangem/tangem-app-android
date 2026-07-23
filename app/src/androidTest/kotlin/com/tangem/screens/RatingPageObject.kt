package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.test.RatingTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode

class RatingPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<RatingPageObject>(semanticsProvider = semanticsProvider) {

    val block: KNode = child {
        hasTestTag(RatingTestTags.BLOCK)
        useUnmergedTree = true
    }

    fun star(index: Int): KNode = child {
        hasTestTag("${RatingTestTags.STAR}_$index")
        useUnmergedTree = true
    }

    val feedbackInput: KNode = child {
        hasTestTag(RatingTestTags.FEEDBACK_INPUT)
        useUnmergedTree = true
    }

    val sendFeedbackButton: KNode = child {
        hasTestTag(RatingTestTags.FEEDBACK_SUBMIT_BUTTON)
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onRatingScreen(function: RatingPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)