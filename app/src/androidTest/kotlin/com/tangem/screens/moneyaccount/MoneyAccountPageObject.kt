package com.tangem.screens.moneyaccount

import androidx.annotation.StringRes
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.swipeDown
import com.tangem.common.BaseTestCase
import com.tangem.core.res.R as CoreResR
import com.tangem.core.ui.test.ModalStatusTestTags
import com.tangem.core.ui.test.MoneyAccountTestTags
import com.tangem.core.ui.test.NotificationTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString
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

    val balanceUnavailableBanner: KNode = child {
        hasText(getResourceString(CoreResR.string.money_account_balance_unavailable_title))
        useUnmergedTree = true
    }

    // A real tap lands on the Markets sheet once the row sits under it at the bottom of Main.
    fun clickMainRow() {
        mainRow { performSemanticsAction(SemanticsActions.OnClick) }
    }

    fun mainRowWithText(text: String): KNode = child {
        hasTestTag(MoneyAccountTestTags.MAIN_ROW)
        hasText(text = text, substring = true)
    }

    val mainRowCacheIcon: KNode = child {
        hasTestTag(MoneyAccountTestTags.MAIN_ROW_CACHE_ICON)
        useUnmergedTree = true
    }

    fun isMainRowCacheIconShown(): Boolean = semanticsProvider
        .onAllNodes(withTestTag(MoneyAccountTestTags.MAIN_ROW_CACHE_ICON), useUnmergedTree = true)
        .fetchSemanticsNodes(atLeastOneRootRequired = false)
        .isNotEmpty()

    val sessionExpiredBannerTitle: KNode = bannerTitle(CoreResR.string.tangempay_sync_needed_title)

    val renewSessionButton: KNode = child {
        hasText(getResourceString(CoreResR.string.tangempay_sync_needed_button))
        useUnmergedTree = true
    }

    val balanceUnavailableBannerTitle: KNode = bannerTitle(CoreResR.string.money_account_balance_unavailable_title)

    private fun bannerTitle(@StringRes textRes: Int): KNode = child {
        hasTestTag(NotificationTestTags.TITLE)
        hasText(getResourceString(textRes))
        useUnmergedTree = true
    }

    fun modalStatusTitle(title: String): KNode = child {
        hasTestTag(ModalStatusTestTags.TITLE)
        hasText(title)
    }
}

private const val PULL_DISTANCE_FACTOR = 6f

internal fun BaseTestCase.onMoneyAccountScreen(function: MoneyAccountPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)