package com.tangem.screens.tangempay

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.test.SendScreenTestTags
import com.tangem.core.ui.test.TangemPayTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode

class TangemPayDailyLimitPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<TangemPayDailyLimitPageObject>(semanticsProvider = semanticsProvider) {

    // AmountTextField tags its editable field with SendScreenTestTags.INPUT_TEXT_FIELD; it's the only one here.
    val amountField: KNode = child {
        hasTestTag(SendScreenTestTags.INPUT_TEXT_FIELD)
        useUnmergedTree = true
    }

    val hint: KNode = child {
        hasTestTag(TangemPayTestTags.DAILY_LIMIT_HINT)
        useUnmergedTree = true
    }

    val setLimitsButton: KNode = child {
        hasTestTag(TangemPayTestTags.DAILY_LIMIT_SET_BUTTON)
        useUnmergedTree = true
    }

    val successTitle: KNode = child {
        hasTestTag(TangemPayTestTags.DAILY_LIMIT_SUCCESS_TITLE)
        useUnmergedTree = true
    }

    val doneButton: KNode = child {
        hasTestTag(TangemPayTestTags.DAILY_LIMIT_DONE_BUTTON)
        useUnmergedTree = true
    }

    fun presetChip(rawValue: String): KNode = child {
        hasTestTag(TangemPayTestTags.dailyLimitPresetChip(rawValue))
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onTangemPayDailyLimitScreen(function: TangemPayDailyLimitPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)