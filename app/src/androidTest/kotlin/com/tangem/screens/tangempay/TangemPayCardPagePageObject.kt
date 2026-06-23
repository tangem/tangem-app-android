package com.tangem.screens.tangempay

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.test.TangemPayTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode

class TangemPayCardPagePageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<TangemPayCardPagePageObject>(semanticsProvider = semanticsProvider) {

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
}

internal fun BaseTestCase.onTangemPayCardPageScreen(function: TangemPayCardPagePageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)