package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode

/**
 * Details sheet of a transaction-history row (the new tx history). Swap rows open it in place of the
 * express-status sheet; the sheet carries no test tags, so its contents are matched by text.
 */
class TxHistoryDetailsBottomSheetPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<TxHistoryDetailsBottomSheetPageObject>(semanticsProvider = semanticsProvider) {

    /** Label of an info row, e.g. the 'Network fee' the gasless transaction paid. */
    fun rowLabel(label: String): KNode = child {
        hasText(label)
        useUnmergedTree = true
    }

    /** Value of an info row: amounts read "<amount> <SYMBOL>" and the provider "<name> • <CEX|DEX>". */
    fun rowValueContaining(value: String): KNode = child {
        hasText(text = value, substring = true)
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onTxHistoryDetailsBottomSheet(
    function: TxHistoryDetailsBottomSheetPageObject.() -> Unit,
) = onComposeScreen(composeTestRule, function)