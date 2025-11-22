package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.test.*
import com.tangem.wallet.R
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString
import androidx.compose.ui.test.hasText as withText
import androidx.compose.ui.test.hasTestTag as withTestTag
import com.tangem.core.ui.R as CoreUiR

class SendConfirmPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<SendConfirmPageObject>(semanticsProvider = semanticsProvider) {

    val appBarTitle: KNode = child {
        hasTestTag(TopAppBarTestTags.TITLE)
        useUnmergedTree = true
    }

    val closeButton: KNode = child {
        hasTestTag(TopAppBarTestTags.CLOSE_BUTTON)
        useUnmergedTree = true
    }

    val sendButton: KNode = child {
        hasTestTag(BaseButtonTestTags.BUTTON)
        hasAnyDescendant(withText(getResourceString(R.string.common_send)))
        useUnmergedTree = true
    }

    val primaryAmount: KNode = child {
        hasTestTag(BaseAmountBlockTestTags.PRIMARY_AMOUNT)
        useUnmergedTree = true
    }

    val secondaryAmount: KNode = child {
        hasTestTag(BaseAmountBlockTestTags.SECONDARY_AMOUNT)
        useUnmergedTree = true
    }

    fun leaveDepositButton(amount: String): KNode = child {
        hasTestTag(BaseButtonTestTags.BUTTON)
        hasAnyDescendant(withText(getResourceString(R.string.send_notification_leave_button, amount)))
        useUnmergedTree = true
    }

    fun reduceAmountButton(amount: String): KNode = child {
        hasTestTag(BaseButtonTestTags.BUTTON)
        hasAnyDescendant(withText(getResourceString(R.string.send_notification_reduce_by, amount)))
        useUnmergedTree = true
    }

    fun warningTitle(title: String): KNode = child {
        hasTestTag(NotificationTestTags.TITLE)
        hasText(title)
        useUnmergedTree = true
    }

    val sendingText: KNode = child {
        hasTestTag(SendConfirmScreenTestTags.SENDING_TEXT)
        useUnmergedTree = true
    }

    fun sendWarningIcon(message: String): KNode = child {
        hasTestTag(NotificationTestTags.ICON)
        hasAnySibling(withText(message))
        useUnmergedTree = true
    }

    fun sendWarningMessage(
        message: String,
    ): KNode = child {
        hasTestTag(NotificationTestTags.MESSAGE)
        hasText(message)
        useUnmergedTree = true
    }

    fun warningMessage(messageResId: Int): KNode = child {
        hasTestTag(NotificationTestTags.MESSAGE)
        hasText(getResourceString(messageResId))
        useUnmergedTree = true
    }

    fun warningIcon(message: String): KNode = child {
        hasTestTag(NotificationTestTags.ICON)
        hasAnySibling(withText(message))
        useUnmergedTree = true
    }

    fun recipientAddress(recipientAddress: String): KNode = child {
        hasTestTag(SendConfirmScreenTestTags.RECIPIENT_ADDRESS)
        hasText(recipientAddress)
        useUnmergedTree = true
    }

    val blockchainAddress: KNode = child {
        hasTestTag(SendConfirmScreenTestTags.BLOCKCHAIN_ADDRESS)
        useUnmergedTree = true
    }

    val feeSelectorBlock: KNode = child {
        hasTestTag(FeeSelectorBlockTestTags.SELECTOR_BLOCK)
    }

    val feeSelectorIcon: KNode = child {
        hasTestTag(FeeSelectorBlockTestTags.ICON)
        useUnmergedTree = true
    }

    val feeSelectorTitle: KNode = child {
        hasTestTag(FeeSelectorBlockTestTags.TITLE)
        useUnmergedTree = true
    }

    val feeSelectorTooltipIcon: KNode = child {
        hasTestTag(FeeSelectorBlockTestTags.TOOLTIP_ICON)
        useUnmergedTree = true
    }

    val selectFeeIcon: KNode = child {
        hasTestTag(FeeSelectorBlockTestTags.SELECT_FEE_ICON)
        useUnmergedTree = true
    }

    val feeAmount: KNode = child {
        hasParent(withTestTag(FeeSelectorBlockTestTags.FEE_AMOUNT))
        useUnmergedTree = true
    }

    val refreshButton: KNode = child {
        hasTestTag(BaseButtonTestTags.BUTTON)
        hasText(getResourceString(CoreUiR.string.warning_button_refresh))
    }
}

internal fun BaseTestCase.onSendConfirmScreen(function: SendConfirmPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)