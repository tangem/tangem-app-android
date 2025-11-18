package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.R
import com.tangem.core.ui.test.BaseButtonTestTags
import com.tangem.core.ui.test.NotificationTestTags
import com.tangem.core.ui.test.SendConfirmScreenTestTags
import com.tangem.core.ui.test.TopAppBarTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString
import androidx.compose.ui.test.hasText as withText
import com.tangem.common.ui.R as CommonUiR

class SendConfirmPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<SendConfirmPageObject>(semanticsProvider = semanticsProvider) {

    val title: KNode = child {
        hasTestTag(TopAppBarTestTags.TITLE)
        useUnmergedTree = true
    }

    val sendButton: KNode = child {
        hasTestTag(BaseButtonTestTags.TEXT)
        hasText(getResourceString(R.string.common_send))
        useUnmergedTree = true
    }


    val minimumSendAmountErrorTitle: KNode = child {
        hasTestTag(NotificationTestTags.TITLE)
        hasText(getResourceString(CommonUiR.string.send_notification_invalid_amount_title))
        useUnmergedTree = true
    }

    val sendingText: KNode = child {
        hasTestTag(SendConfirmScreenTestTags.SENDING_TEXT)
        useUnmergedTree = true
    }

    fun minimumSendAmountErrorIcon(amount: String): KNode = child {
        hasAnySibling(
            withText(
                getResourceString(
                    CommonUiR.string.send_notification_invalid_minimum_amount_text,
                    amount,
                    amount,
                )
            )
        )
        hasTestTag(NotificationTestTags.ICON)
        useUnmergedTree = true
    }

    fun minimumSendAmountErrorMessage(
        amount: String,
    ): KNode = child {
        hasTestTag(NotificationTestTags.MESSAGE)
        hasText(
            getResourceString(
                CommonUiR.string.send_notification_invalid_minimum_amount_text,
                amount,
                amount,
            )
        )
        useUnmergedTree = true
    }

}

internal fun BaseTestCase.onSendConfirmScreen(function: SendConfirmPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)