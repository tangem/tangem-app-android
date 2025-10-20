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

class SendConfirmPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<SendConfirmPageObject>(semanticsProvider = semanticsProvider) {

    val title: KNode = child {
        hasTestTag(TopAppBarTestTags.TITLE)
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

    fun leaveDepositButton(amount: String): KNode = child {
        hasTestTag(BaseButtonTestTags.BUTTON)
        hasAnyDescendant(withText(getResourceString(R.string.send_notification_leave_button, amount)))
        useUnmergedTree = true
    }

    fun warningTitle(titleResId: Int): KNode = child {
        hasTestTag(NotificationTestTags.TITLE)
        hasText(getResourceString(titleResId))
        useUnmergedTree = true
    }

    val sendingText: KNode = child {
        hasTestTag(SendConfirmScreenTestTags.SENDING_TEXT)
        useUnmergedTree = true
    }

    fun sendWarningIcon(messageResId: Int, amount: String): KNode = child {
        hasTestTag(NotificationTestTags.ICON)
        hasAnySibling(
            withText(
                getResourceString(
                    messageResId,
                    amount,
                    amount,
                )
            )

        )
        useUnmergedTree = true
    }

    fun sendWarningMessage(
        messageResId: Int,
        amount: String,
    ): KNode = child {
        hasTestTag(NotificationTestTags.MESSAGE)
        hasText(
            getResourceString(
                messageResId,
                amount,
                amount,
            )
        )
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onSendConfirmScreen(function: SendConfirmPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)