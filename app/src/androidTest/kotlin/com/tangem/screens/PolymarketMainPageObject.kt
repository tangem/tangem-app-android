package com.tangem.screens

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasTestTag as withTestTag
import androidx.compose.ui.test.hasText as withText
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.test.ModalStatusTestTags
import com.tangem.core.ui.test.PredictionTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString
import com.tangem.core.res.R as CoreResR

class PolymarketMainPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<PolymarketMainPageObject>(semanticsProvider = semanticsProvider) {

    val screen: KNode = child {
        hasTestTag(PredictionTestTags.MAIN_SCREEN)
    }

    val regionRestrictedBanner: KNode = child {
        hasTestTag(PredictionTestTags.REGION_RESTRICTED_BANNER)
    }

    val regionRestrictedBannerTitle: KNode = child {
        useUnmergedTree = true
        hasText(getResourceString(CoreResR.string.prediction_region_restrictions_title))
        hasAnyAncestor(withTestTag(PredictionTestTags.REGION_RESTRICTED_BANNER))
    }

    val regionRestrictedBannerSubtitle: KNode = child {
        useUnmergedTree = true
        hasText(getResourceString(CoreResR.string.prediction_region_restrictions_subtitle))
        hasAnyAncestor(withTestTag(PredictionTestTags.REGION_RESTRICTED_BANNER))
    }

    val availableBalanceLabel: KNode = child {
        hasText(getResourceString(CoreResR.string.prediction_main_available_balance))
    }

    val addFundsButton: KNode = child {
        hasClickAction()
        hasAnySibling(withText(getResourceString(CoreResR.string.common_add_funds)))
    }

    val withdrawButton: KNode = child {
        hasClickAction()
        hasAnySibling(withText(getResourceString(CoreResR.string.prediction_main_withdraw)))
    }

    val availableBalance: KNode = child {
        hasText(AVAILABLE_BALANCE_PREFIX, substring = true)
    }

    val viewAllActivePositionsButton: KNode = child {
        hasText(getResourceString(CoreResR.string.prediction_positions_view_all_active))
    }

    val feedList: KNode = child {
        hasTestTag(PredictionTestTags.FEED_LIST)
    }

    @OptIn(ExperimentalTestApi::class)
    fun scrollToFirstOutcomeButton() {
        feedList { performScrollToNode(hasTestTag(PredictionTestTags.OUTCOME_BUTTON)) }
    }

    val firstOutcomeButton: KNode = child {
        hasTestTag(PredictionTestTags.OUTCOME_BUTTON)
        hasPosition(0)
    }

    val regionRestrictionsModalClose: KNode = child {
        hasClickAction()
        hasText(getResourceString(CoreResR.string.common_close))
        addSemanticsMatcher(!withTestTag(PredictionTestTags.POSITION_CLOSE_BUTTON))
    }

    val addFundsSwapOption: KNode = child {
        hasText(getResourceString(CoreResR.string.prediction_add_funds_swap_title))
    }

    val addFundsReceiveOption: KNode = child {
        hasText(getResourceString(CoreResR.string.prediction_add_funds_receive_title))
    }

    val firstPositionCard: KNode = child {
        hasTestTag(PredictionTestTags.POSITION_CARD)
        hasPosition(0)
    }

    val redeemablePosition: KNode = child {
        hasText(REDEEMABLE_POSITION_TITLE)
    }

    val claimRewardButton: KNode = child {
        hasClickAction()
        hasText(getResourceString(CoreResR.string.prediction_position_result_claim_action))
    }

    val moreButton: KNode = child {
        hasContentDescription(getResourceString(CoreResR.string.common_more))
    }

    val historyMenuItem: KNode = child {
        hasText(getResourceString(CoreResR.string.prediction_history_title))
    }

    fun clickFirstOutcomeButtonBySemantics() {
        firstOutcomeButton { performSemanticsAction(SemanticsActions.OnClick) }
    }

    val regionRestrictionsModalTitle: KNode = child {
        hasTestTag(ModalStatusTestTags.TITLE)
        hasText(getResourceString(CoreResR.string.prediction_region_restrictions_title))
    }
}

internal fun BaseTestCase.onPolymarketMainScreen(function: PolymarketMainPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)

private const val AVAILABLE_BALANCE_PREFIX = "$25"
private const val REDEEMABLE_POSITION_TITLE = "Will the redeemable mock prediction resolve?"