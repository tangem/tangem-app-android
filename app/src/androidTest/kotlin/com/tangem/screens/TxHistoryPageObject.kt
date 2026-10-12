package com.tangem.screens

import androidx.annotation.StringRes
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.test.TransactionHistoryItemTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString
import androidx.compose.ui.test.hasText as withText
import com.tangem.core.res.R as CoreResR

class TxHistoryPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<TxHistoryPageObject>(semanticsProvider = semanticsProvider) {

    fun transactionItem(title: String): KNode = child {
        hasTestTag(TransactionHistoryItemTestTags.ITEM)
        hasAnyDescendant(withText(title))
        useUnmergedTree = true
    }

    /** A settled yield row is a pill whose label and end state read as one text node: "Yield mode Enabled". */
    fun yieldModePill(@StringRes state: Int): KNode = transactionItem(
        title = "${getResourceString(CoreResR.string.common_yield_mode_inline)} ${getResourceString(state)}",
    )

    /**
     * A supply / withdraw row is a pill whose label and provider read as one text node — "Supplied to: %image% Aave",
     * with the provider avatar as the inline-image marker of [directionTemplate].
     */
    fun yieldSupplyPill(@StringRes label: Int, @StringRes directionTemplate: Int): KNode = transactionItem(
        title = "${getResourceString(label)} " +
            getResourceString(directionTemplate, getResourceString(CoreResR.string.yield_module_provider)),
    )

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

    fun transactionUnconfirmedStatus(title: String): KNode = transactionItem(title).child {
        hasTestTag(TransactionHistoryItemTestTags.STATUS_UNCONFIRMED)
        useUnmergedTree = true
    }

    fun transactionSubtitle(title: String, subtitle: String): KNode = transactionItem(title).child {
        hasText(subtitle)
        useUnmergedTree = true
    }

    fun transactionAddress(title: String, address: String): KNode = transactionItem(title).child {
        hasText(text = address, substring = true)
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onTxHistoryScreen(function: TxHistoryPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)