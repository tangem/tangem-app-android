package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.hasAnyDescendant as withAnyDescendant
import androidx.compose.ui.test.hasTestTag as withTestTag
import androidx.compose.ui.test.hasText as withText
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.R
import com.tangem.core.ui.test.BaseButtonTestTags
import com.tangem.core.ui.test.TransactionSuccessScreenTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString

// Legacy swap feature's success screen — lacks CONTAINER testTag that SendSuccessPageObject relies on.
class SwapSuccessPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<SwapSuccessPageObject>(semanticsProvider = semanticsProvider) {

    val title: KNode = child {
        hasTestTag(TransactionSuccessScreenTestTags.TITLE)
        useUnmergedTree = true
    }

    // App Transfers reuses the swap success screen; in transfer mode its title is "Transfer in progress".
    val transferInProgressTitle: KNode = child {
        hasTestTag(TransactionSuccessScreenTestTags.TITLE)
        hasText(getResourceString(R.string.transfer_in_progress_title))
        useUnmergedTree = true
    }

    val closeButton: KNode = child {
        hasTestTag(BaseButtonTestTags.BUTTON)
        hasAnyDescendant(withText(getResourceString(R.string.common_close)))
        useUnmergedTree = true
    }

    val exploreButton: KNode = child {
        hasTestTag(BaseButtonTestTags.BUTTON)
        hasAnyDescendant(withText(getResourceString(R.string.common_explore)))
        useUnmergedTree = true
    }

    val transactionDate: KNode = child {
        hasTestTag(TransactionSuccessScreenTestTags.TRANSACTION_DATE)
        useUnmergedTree = true
    }

    fun sendBlockText(text: String): KNode =
        amountBlockText(getResourceString(R.string.swapping_from_title_v2), text)

    fun receiveBlockText(text: String): KNode =
        amountBlockText(getResourceString(R.string.swapping_to_title), text)

    private fun amountBlockText(blockTitle: String, text: String): KNode = child {
        hasText(text, substring = true)
        hasAnyAncestor(
            withTestTag(TransactionSuccessScreenTestTags.AMOUNT_BLOCK) and
                withAnyDescendant(withText(blockTitle, substring = true)),
        )
        useUnmergedTree = true
    }

    fun providerBlockText(text: String): KNode = child {
        hasText(text, substring = true)
        hasAnyAncestor(withTestTag(TransactionSuccessScreenTestTags.PROVIDER_BLOCK))
        useUnmergedTree = true
    }

    val providerIcon: KNode = child {
        hasTestTag(TransactionSuccessScreenTestTags.PROVIDER_ICON)
        useUnmergedTree = true
    }

    fun feeBlockText(text: String): KNode = child {
        hasText(text, substring = true)
        hasAnyAncestor(withTestTag(TransactionSuccessScreenTestTags.FEE_BLOCK))
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onSwapSuccessScreen(function: SwapSuccessPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)