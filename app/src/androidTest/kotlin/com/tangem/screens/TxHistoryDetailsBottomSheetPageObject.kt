package com.tangem.screens

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.res.R
import com.tangem.core.ui.test.PopUpMenuTestTags
import com.tangem.core.ui.test.TxHistoryDetailsTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString
import androidx.compose.ui.test.hasTestTag as withTestTag

/**
 * Details sheet of a transaction-history row (the new tx history). Swap rows open it in place of the
 * express-status sheet; only the header carries test tags, the rest is matched by text.
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

    fun headerTitle(title: String): KNode = child {
        hasTestTag(TxHistoryDetailsTestTags.HEADER_TITLE)
        hasText(title)
        useUnmergedTree = true
    }

    val headerDateTime: KNode = child {
        hasTestTag(TxHistoryDetailsTestTags.HEADER_SUBTITLE)
        useUnmergedTree = true
    }

    /** An asset leg amount, rendered as "<sign> <amount> <SYMBOL>" (sign is "-", "~" or "+"; a Unicode minus is treated as "-"). */
    fun assetAmount(sign: String, symbol: String): KNode = child {
        addSemanticsMatcher(
            SemanticsMatcher("text starts with '$sign ' and ends with ' $symbol'") { node ->
                node.config.getOrNull(SemanticsProperties.Text).orEmpty().any {
                    val text = it.text.replace(UNICODE_MINUS, ASCII_MINUS).replace(WHITESPACE_REGEX, " ")
                    text.startsWith("${sign.replace(UNICODE_MINUS, ASCII_MINUS)} ") && text.endsWith(" $symbol")
                }
            },
        )
        useUnmergedTree = true
    }

    val moreButton: KNode = child {
        hasContentDescription(getResourceString(R.string.common_more))
    }

    fun menuItem(title: String): KNode = child {
        hasAnyAncestor(withTestTag(PopUpMenuTestTags.CONTAINER))
        hasText(title)
        useUnmergedTree = true
    }

    fun message(text: String): KNode = child {
        hasText(text)
        useUnmergedTree = true
    }
}

private const val UNICODE_MINUS = "\u2212"
private const val ASCII_MINUS = "-"
private val WHITESPACE_REGEX = "\\s".toRegex()

internal fun BaseTestCase.onTxHistoryDetailsBottomSheet(
    function: TxHistoryDetailsBottomSheetPageObject.() -> Unit,
) = onComposeScreen(composeTestRule, function)