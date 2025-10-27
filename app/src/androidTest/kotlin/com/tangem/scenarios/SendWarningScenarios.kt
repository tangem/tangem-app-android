package com.tangem.scenarios

import com.tangem.common.BaseTestCase
import com.tangem.screens.onSendConfirmScreen
import io.github.kakaocup.compose.node.element.KNode
import io.qameta.allure.kotlin.Allure.step

fun BaseTestCase.checkSendWarning(
    titleResId: Int,
    messageResId: Int,
    amount: String,
    isDisplayed: Boolean = true,
) {
    val assertDisplay = if (isDisplayed) "displayed" else "not displayed"

    step("Assert 'Send confirm screen' is displayed") {
        onSendConfirmScreen {
            title.assertIsDisplayed()
        }
    }
    step("Assert warning title is $assertDisplay") {
        onSendConfirmScreen {
            warningTitle(titleResId).assertVisibility(isDisplayed)
        }
    }
    step("Assert warning icon is $assertDisplay") {
        onSendConfirmScreen {
            sendWarningIcon(messageResId, amount).assertVisibility(isDisplayed)
        }

    }
    step("Assert warning message is $assertDisplay") {
        onSendConfirmScreen {
            sendWarningMessage(messageResId, amount).assertVisibility(isDisplayed)
        }
    }
    if (isDisplayed)
        step("Assert 'Send' button is disabled") {
            onSendConfirmScreen {
                sendButton.assertIsNotEnabled()
            }
        }
    else
        step("Assert 'Send' button is enabled") {
            onSendConfirmScreen {
                sendButton.assertIsEnabled()
            }
        }
}

private fun KNode.assertVisibility(shouldBeDisplayed: Boolean) {
    if (shouldBeDisplayed) {
        assertIsDisplayed()
    } else {
        assertIsNotDisplayed()
    }
}