package com.tangem.scenarios

import com.tangem.common.BaseTestCase
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
    // Settling the fee first keeps the checks below from racing it — it decides whether a warning applies
    // and whether 'Send' is enabled, and it is re-fetched on every recipient change.
    //
    // Best effort on purpose. Not every flow reaching this point gets a fee at all: an amount that is
    // invalid by design never produces one, and some recipients leave it unsettled for longer than any
    // sane wait. Both are legitimate, and the assertions below retry on their own, so a fee that never
    // settles must not fail the test here — it did, for the dust-change and Stellar reserve cases.
    if (!isDisplayed) step("Wait for the network fee to finish loading") {
        runCatching {
            var previousFee: String? = null
            awaitSuccess(timeoutMillis = FEE_SETTLE_TIMEOUT_MS) {
                val currentFee = readNetworkFeeAmount()
                val isStable = currentFee.isNotEmpty() && currentFee == previousFee
                previousFee = currentFee
                if (!isStable) throw AssertionError("Network fee is still settling (current='$currentFee')")
            }
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

/** Shorter than the assertion timeouts below it: this wait is an optimisation, not a gate. */
private const val FEE_SETTLE_TIMEOUT_MS = 10_000L