package com.tangem.screens.moneyaccount

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.swipeDown
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.test.MoneyAccountTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import androidx.compose.ui.test.hasTestTag as withTestTag
import androidx.compose.ui.test.hasText as withText

class MoneyAccountPageObject(private val semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<MoneyAccountPageObject>(semanticsProvider = semanticsProvider) {

    val mainRow: KNode = child {
        hasTestTag(MoneyAccountTestTags.MAIN_ROW)
    }

    val balance: KNode = child {
        hasTestTag(MoneyAccountTestTags.DETAILS_BALANCE)
        useUnmergedTree = true
    }

    val balanceMatcher: SemanticsMatcher = withTestTag(MoneyAccountTestTags.DETAILS_BALANCE)

    fun swipeDownToRefresh() {
        balance {
            performTouchInput {
                swipeDown(startY = 0f, endY = visibleSize.height.toFloat() * PULL_DISTANCE_FACTOR, durationMillis = 800)
            }
        }
    }

    fun mainRowWithTextMatcher(text: String): SemanticsMatcher =
        withTestTag(MoneyAccountTestTags.MAIN_ROW) and withText(text = text, substring = true)

    // Raw Compose on purpose: polled in a wait loop, where a Kakao check of an absent row waits out a 10 s retry.
    fun isMainRowWithTextShown(text: String): Boolean = semanticsProvider.onAllNodes(mainRowWithTextMatcher(text))
        .fetchSemanticsNodes(atLeastOneRootRequired = false)
        .isNotEmpty()

    val addFundsButton: KNode = child {
        hasClickAction()
        hasAnyAncestor(withTestTag(MoneyAccountTestTags.ADD_FUNDS_BUTTON))
    }

    val sendButton: KNode = child {
        hasClickAction()
        hasAnyAncestor(withTestTag(MoneyAccountTestTags.SEND_BUTTON))
    }

    // A real tap lands on the Markets sheet once the row sits under it at the bottom of Main.
    fun clickMainRow() {
        mainRow { performSemanticsAction(SemanticsActions.OnClick) }
    }

    fun mainRowWithText(text: String): KNode = child {
        hasTestTag(MoneyAccountTestTags.MAIN_ROW)
        hasText(text = text, substring = true)
    }
}

private const val PULL_DISTANCE_FACTOR = 6f

internal fun BaseTestCase.onMoneyAccountScreen(function: MoneyAccountPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)