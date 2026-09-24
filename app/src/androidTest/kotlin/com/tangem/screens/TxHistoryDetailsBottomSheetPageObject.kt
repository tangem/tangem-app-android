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

    fun title(text: String): KNode = child {
        hasText(text)
        useUnmergedTree = true
    }

    /** The provider row reads "<name> • <CEX|DEX>", so match the name as a substring. */
    fun providerName(name: String): KNode = child {
        hasText(text = name, substring = true)
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onTxHistoryDetailsBottomSheet(
    function: TxHistoryDetailsBottomSheetPageObject.() -> Unit,
) = onComposeScreen(composeTestRule, function)