package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.common.R
import com.tangem.core.ui.test.BaseButtonTestTags
import com.tangem.core.ui.test.TransactionSuccessScreenTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString
import androidx.compose.ui.test.hasText as withText

class SendSuccessPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<SendSuccessPageObject>(semanticsProvider = semanticsProvider) {

    val container: KNode = child {
        hasTestTag(TransactionSuccessScreenTestTags.CONTAINER)
    }

    val title: KNode = child {
        hasTestTag(TransactionSuccessScreenTestTags.TITLE)
        useUnmergedTree = true
    }

    val transactionDate: KNode = child {
        hasTestTag(TransactionSuccessScreenTestTags.TRANSACTION_DATE)
        useUnmergedTree = true
    }

    val sendFromBlock: KNode = child {
        hasTestTag(TransactionSuccessScreenTestTags.AMOUNT_BLOCK)
        hasAnyDescendant(withText(getResourceString(R.string.send_from_title)))
        useUnmergedTree = true
    }

    val sendToAmountBlock: KNode = child {
        hasTestTag(TransactionSuccessScreenTestTags.AMOUNT_BLOCK)
        hasAnyDescendant(
            withText(getResourceString(R.string.send_with_swap_recipient_amount_success_title))
        )
        useUnmergedTree = true
    }

    val providerBlock: KNode = child {
        hasTestTag(TransactionSuccessScreenTestTags.PROVIDER_BLOCK)
        useUnmergedTree = true
    }

    val recipientAddressBlock: KNode = child {
        hasTestTag(TransactionSuccessScreenTestTags.RECIPIENT_BLOCK)
        useUnmergedTree = true
    }

    val feeBlock: KNode = child {
        hasTestTag(TransactionSuccessScreenTestTags.FEE_BLOCK)
        useUnmergedTree = true
    }

    val exploreButton: KNode = child {
        hasTestTag(BaseButtonTestTags.BUTTON)
        hasText(getResourceString(R.string.common_explore))
    }

    val closeButton: KNode = child {
        hasTestTag(BaseButtonTestTags.BUTTON)
        hasText(getResourceString(R.string.common_close))
    }

    val shareButton: KNode = child {
        hasTestTag(BaseButtonTestTags.BUTTON)
        hasText(getResourceString(R.string.common_share))
    }
}

internal fun BaseTestCase.onSendSuccessScreen(function: SendSuccessPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)