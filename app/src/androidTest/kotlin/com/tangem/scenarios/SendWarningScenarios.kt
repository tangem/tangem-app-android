package com.tangem.scenarios

import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_LONG
import com.tangem.common.extensions.assertVisibility
import com.tangem.screens.onSendConfirmScreen
import io.qameta.allure.kotlin.Allure.step

fun BaseTestCase.checkSendWarning(
    title: String,
    message: String,
    isDisplayed: Boolean = true,
    sendButtonIsDisabled: Boolean = isDisplayed,
) {
    val assertDisplay = if (isDisplayed) "displayed" else "not displayed"

    // When we expect the warning to be absent, it may still be finishing its disappear animation right after the
    // Confirm screen opens (e.g. after re-entering a valid address), so poll the Compose tree until it is actually
    // gone instead of asserting once. When we expect it to be present, assert straight away.
    fun assertWarning(block: () -> Unit) {
        if (isDisplayed) block() else awaitSuccess(block = block)
    }

    step("Assert 'Send confirm screen' is displayed") {
        onSendConfirmScreen {
            appBarTitle.assertIsDisplayed()
        }
    }
    // Only when the screen is expected to be usable. The fee decides whether a warning applies and whether
    // 'Send' is enabled, so the checks race it — but an amount that is invalid on purpose (dust change, for
    // instance) never gets a fee at all, and waiting for one there hangs until the timeout.
    if (!isDisplayed) step("Wait for the network fee to finish loading") {
        var previousFee: String? = null
        awaitSuccess(timeoutMillis = WAIT_UNTIL_TIMEOUT_LONG) {
            val currentFee = readNetworkFeeAmount()
            val isStable = currentFee.isNotEmpty() && currentFee == previousFee
            previousFee = currentFee
            if (!isStable) throw AssertionError("Network fee is still settling (current='$currentFee')")
        }
    }
    step("Assert warning title is $assertDisplay") {
        assertWarning {
            onSendConfirmScreen {
                warningTitle(title).assertVisibility(isDisplayed)
            }
        }
    }
    step("Assert warning icon is $assertDisplay") {
        assertWarning {
            onSendConfirmScreen {
                sendWarningIcon(message).assertVisibility(isDisplayed)
            }
        }
    }
    step("Assert warning message is $assertDisplay") {
        assertWarning {
            onSendConfirmScreen {
                sendWarningMessage(message).assertVisibility(isDisplayed)
            }
        }
    }
    if (sendButtonIsDisabled)
        step("Assert 'Send' button is disabled") {
            onSendConfirmScreen {
                sendButton.assertIsNotEnabled()
            }
        }
    else
        step("Assert 'Send' button is enabled") {
            awaitSuccess {
                onSendConfirmScreen {
                    sendButton.assertIsEnabled()
                }
            }
        }
}