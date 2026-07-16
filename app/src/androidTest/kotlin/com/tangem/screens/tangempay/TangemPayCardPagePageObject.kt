package com.tangem.screens.tangempay

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.res.R as CoreResR
import com.tangem.core.ui.test.TangemPayTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString

class TangemPayCardPagePageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<TangemPayCardPagePageObject>(semanticsProvider = semanticsProvider) {

    val moreButton: KNode = child {
        hasTestTag(TangemPayTestTags.CARD_PAGE_MORE_BUTTON)
        useUnmergedTree = true
    }

    val replaceCardMenuItem: KNode = child {
        hasText(getResourceString(CoreResR.string.tangempay_card_details_reissue_card))
        useUnmergedTree = true
    }

    val cardNumberShort: KNode = child {
        hasTestTag(TangemPayTestTags.CARD_NUMBER_SHORT)
        useUnmergedTree = true
    }

    val reissueInProgressBlock: KNode = child {
        hasText(
            text = getResourceString(CoreResR.string.tangempay_reissue_card_in_progress),
            substring = true,
        )
        useUnmergedTree = true
    }

    val changePinRow: KNode = child {
        hasTestTag(TangemPayTestTags.CHANGE_PIN_ROW)
        useUnmergedTree = true
    }

    val freezeCardRow: KNode = child {
        hasTestTag(TangemPayTestTags.FREEZE_CARD_ROW)
        useUnmergedTree = true
    }

    val cardFrozenBadge: KNode = child {
        hasTestTag(TangemPayTestTags.CARD_FROZEN_BADGE)
        useUnmergedTree = true
    }

    val showDetailsButton: KNode = child {
        hasTestTag(TangemPayTestTags.SHOW_DETAILS_ROW)
        useUnmergedTree = true
    }

    val hideDetailsButton: KNode = child {
        hasTestTag(TangemPayTestTags.CARD_DETAILS_HIDE_BUTTON)
        useUnmergedTree = true
    }

    val numberValue: KNode = child {
        hasTestTag(TangemPayTestTags.CARD_DETAILS_NUMBER_VALUE)
        useUnmergedTree = true
    }

    val expirationValue: KNode = child {
        hasTestTag(TangemPayTestTags.CARD_DETAILS_EXPIRATION_VALUE)
        useUnmergedTree = true
    }

    val cvcValue: KNode = child {
        hasTestTag(TangemPayTestTags.CARD_DETAILS_CVC_VALUE)
        useUnmergedTree = true
    }

    val copyNumberButton: KNode = child {
        hasTestTag(TangemPayTestTags.CARD_DETAILS_COPY_NUMBER)
        useUnmergedTree = true
    }

    val copyExpirationButton: KNode = child {
        hasTestTag(TangemPayTestTags.CARD_DETAILS_COPY_EXPIRATION)
        useUnmergedTree = true
    }

    val copyCvcButton: KNode = child {
        hasTestTag(TangemPayTestTags.CARD_DETAILS_COPY_CVC)
        useUnmergedTree = true
    }

    val dailyLimitChangeButton: KNode = child {
        hasTestTag(TangemPayTestTags.DAILY_LIMIT_CHANGE_BUTTON)
        useUnmergedTree = true
    }

    val dailyLimitValue: KNode = child {
        hasTestTag(TangemPayTestTags.DAILY_LIMIT_CURRENT_VALUE)
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onTangemPayCardPageScreen(function: TangemPayCardPagePageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)