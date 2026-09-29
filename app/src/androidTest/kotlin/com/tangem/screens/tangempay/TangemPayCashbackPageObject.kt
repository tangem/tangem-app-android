package com.tangem.screens.tangempay

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.test.TangemPayTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import androidx.compose.ui.test.hasText as withText

class TangemPayCashbackPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<TangemPayCashbackPageObject>(semanticsProvider = semanticsProvider) {

    val headerTitle: KNode = child {
        hasTestTag(TangemPayTestTags.CASHBACK_HEADER_TITLE)
        useUnmergedTree = true
    }

    val headerSubtitle: KNode = child {
        hasTestTag(TangemPayTestTags.CASHBACK_HEADER_SUBTITLE)
        useUnmergedTree = true
    }

    val banner: KNode = child {
        hasTestTag(TangemPayTestTags.CASHBACK_BANNER)
        useUnmergedTree = true
    }

    fun rateTileWithText(text: String): KNode = child {
        hasTestTag(TangemPayTestTags.CASHBACK_RATE_TILE)
        hasAnyDescendant(withText(text))
        useUnmergedTree = true
    }

    fun accrualsTileWithText(text: String): KNode = child {
        hasTestTag(TangemPayTestTags.CASHBACK_ACCRUALS_TILE)
        hasAnyDescendant(withText(text))
        useUnmergedTree = true
    }

    val histogramTitle: KNode = child {
        hasTestTag(TangemPayTestTags.CASHBACK_HISTOGRAM_TITLE)
        useUnmergedTree = true
    }

    /** Month labels share one tag, so a column is addressed by its position, oldest month first. */
    fun histogramMonthAt(index: Int): KNode = child {
        hasTestTag(TangemPayTestTags.CASHBACK_HISTOGRAM_MONTH)
        hasPosition(index)
        useUnmergedTree = true
    }

    fun histogramAmountAt(index: Int): KNode = child {
        hasTestTag(TangemPayTestTags.cashbackHistogramAmount(index))
        useUnmergedTree = true
    }

    val additionalCashbackTitle: KNode = child {
        hasTestTag(TangemPayTestTags.CASHBACK_ADDITIONAL_TITLE)
        useUnmergedTree = true
    }

    val errorText: KNode = child {
        hasTestTag(TangemPayTestTags.CASHBACK_ERROR_TEXT)
        useUnmergedTree = true
    }

    val reloadButton: KNode = child {
        hasTestTag(TangemPayTestTags.CASHBACK_RELOAD_BUTTON)
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onTangemPayCashbackScreen(function: TangemPayCashbackPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)