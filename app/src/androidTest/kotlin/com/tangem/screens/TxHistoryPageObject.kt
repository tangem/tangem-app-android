package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.test.TransactionHistoryItemTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import androidx.compose.ui.test.hasText as withText

class TxHistoryPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<TxHistoryPageObject>(semanticsProvider = semanticsProvider) {

    fun transactionItem(title: String): KNode = child {
        hasTestTag(TransactionHistoryItemTestTags.ITEM)
        hasAnyDescendant(withText(title))
        useUnmergedTree = true
    }

    fun transactionAmount(title: String): KNode = transactionItem(title).child {
        hasTestTag(TransactionHistoryItemTestTags.AMOUNT)
        useUnmergedTree = true
    }

    fun transactionCurrency(title: String): KNode = transactionItem(title).child {
        hasTestTag(TransactionHistoryItemTestTags.CURRENCY)
        useUnmergedTree = true
    }

    fun transactionConfirmedStatus(title: String): KNode = transactionItem(title).child {
        hasTestTag(TransactionHistoryItemTestTags.STATUS_CONFIRMED)
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onTxHistoryScreen(function: TxHistoryPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)