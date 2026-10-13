package com.tangem.screens.tangempay

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.swipeLeft
import com.tangem.common.BaseTestCase
import com.tangem.core.res.R as CoreResR
import com.tangem.core.ui.test.TangemPayTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString
import androidx.compose.ui.test.hasAnyDescendant as withAnyDescendant
import androidx.compose.ui.test.hasTestTag as withTestTag
import androidx.compose.ui.test.hasText as withText

class TangemPayWithdrawHubPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<TangemPayWithdrawHubPageObject>(semanticsProvider = semanticsProvider) {

    val screen: KNode = child {
        hasTestTag(TangemPayTestTags.WITHDRAW_HUB_SCREEN)
        useUnmergedTree = true
    }

    val bankSectionTitle: KNode = child {
        hasText(getResourceString(CoreResR.string.tangempay_withdraw_hub_bank_section_title))
        hasAnyAncestor(withTestTag(TangemPayTestTags.WITHDRAW_HUB_BANK_SECTION))
        useUnmergedTree = true
    }

    val bankUnavailableSubtitle: KNode = child {
        hasText(getResourceString(CoreResR.string.tangempay_withdraw_hub_bank_section_subtitle_unavailable))
        hasAnyAncestor(withTestTag(TangemPayTestTags.WITHDRAW_HUB_BANK_SECTION))
        useUnmergedTree = true
    }

    val swapRecentsEmpty: KNode = recentsEmpty(sectionTag = TangemPayTestTags.WITHDRAW_HUB_SWAP_SECTION)

    val bankRecentsEmpty: KNode = recentsEmpty(sectionTag = TangemPayTestTags.WITHDRAW_HUB_BANK_SECTION)

    val recentsLoadError: KNode = child {
        hasTestTag(TangemPayTestTags.WITHDRAW_HUB_RECENTS_ERROR)
        hasAnyDescendant(withText(getResourceString(CoreResR.string.tangempay_withdraw_hub_accounts_load_error)))
        useUnmergedTree = true
    }

    val withinPortfolioCard: KNode = child {
        hasTestTag(TangemPayTestTags.WITHDRAW_HUB_WITHIN_PORTFOLIO)
        useUnmergedTree = true
    }

    val toPersonalAccountButton: KNode = bankAction(CoreResR.string.tangempay_withdraw_hub_to_personal_account)

    val toBusinessAccountButton: KNode = bankAction(CoreResR.string.tangempay_withdraw_hub_to_business_account)

    val seeAllChip: KNode = child {
        hasTestTag(TangemPayTestTags.WITHDRAW_HUB_SEE_ALL)
        hasAnyDescendant(withText(getResourceString(CoreResR.string.common_see_all)))
        useUnmergedTree = true
    }

    private val bankList: KNode = child {
        hasTestTag(TangemPayTestTags.WITHDRAW_HUB_BANK_LIST)
        useUnmergedTree = true
    }

    fun recentRecipient(initial: String, name: String, accountNumber: String): KNode = child {
        addSemanticsMatcher(recentRecipientMatcher(name))
        hasAnyDescendant(withText(initial))
        hasAnyDescendant(withText(accountNumber))
        useUnmergedTree = true
    }

    fun recentRecipientWithName(name: String): KNode = child {
        addSemanticsMatcher(recentRecipientMatcher(name))
        useUnmergedTree = true
    }

    fun recentRecipientAt(index: Int): KNode = child {
        hasTestTag(TangemPayTestTags.WITHDRAW_HUB_RECENT_RECIPIENT)
        hasPosition(index)
        useUnmergedTree = true
    }

    fun recentToken(name: String): KNode = child {
        hasTestTag(TangemPayTestTags.WITHDRAW_HUB_RECENT_TOKEN)
        hasAnyAncestor(withTestTag(TangemPayTestTags.WITHDRAW_HUB_SWAP_SECTION))
        hasAnyDescendant(withText(name))
        useUnmergedTree = true
    }

    @OptIn(ExperimentalTestApi::class)
    fun scrollToRecentRecipient(name: String) = bankList {
        performScrollToNode(recentRecipientMatcher(name))
    }

    fun scrollBankListToEnd() = bankList {
        performTouchInput { swipeLeft() }
    }

    @OptIn(ExperimentalTestApi::class)
    fun scrollToSeeAllChip() = bankList {
        performScrollToNode(withTestTag(TangemPayTestTags.WITHDRAW_HUB_SEE_ALL))
    }

    private fun recentsEmpty(sectionTag: String): KNode = child {
        hasTestTag(TangemPayTestTags.WITHDRAW_HUB_RECENTS_EMPTY)
        hasAnyAncestor(withTestTag(sectionTag))
        hasAnyDescendant(withText(getResourceString(CoreResR.string.tangempay_withdraw_hub_recents_empty)))
        useUnmergedTree = true
    }

    private fun bankAction(titleRes: Int): KNode = child {
        hasTestTag(TangemPayTestTags.WITHDRAW_HUB_BANK_ACTION)
        hasAnyDescendant(withText(getResourceString(titleRes)))
        useUnmergedTree = true
    }
}

private fun recentRecipientMatcher(name: String): SemanticsMatcher =
    withTestTag(TangemPayTestTags.WITHDRAW_HUB_RECENT_RECIPIENT) and withAnyDescendant(withText(name))

internal fun BaseTestCase.onTangemPayWithdrawHubScreen(function: TangemPayWithdrawHubPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)