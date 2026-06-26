package com.tangem.scenarios

import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.QUOTES_API_SCENARIO
import com.tangem.common.constants.TestConstants.USER_TOKENS_API_SCENARIO
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_LONG
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.screens.*
import io.qameta.allure.kotlin.Allure.step

/**
 * From the recipient step: fill the address and advance to the 'Send confirm' screen.
 * Uses `composeTestRule.waitUntil` because `flakySafely` is unavailable in extensions on [BaseTestCase].
 */
fun BaseTestCase.enterRecipientAndOpenSendConfirm(recipientAddress: String) {
    step("Type recipient address") {
        onSendAddressScreen { addressTextField.performTextReplacement(recipientAddress) }
    }
    step("Click on 'Next' button until 'Send confirm' screen opens") {
        composeTestRule.waitUntil(timeoutMillis = WAIT_UNTIL_TIMEOUT_LONG) {
            runCatching { openSendConfirmScreenViaNextButton() }.isSuccess
        }
    }
}

/** Enter the send amount, then fill the recipient and open the 'Send confirm' screen. */
fun BaseTestCase.enterAmountAndOpenSendConfirm(amount: String, recipientAddress: String) {
    step("Type '$amount' in input text field") {
        onSendScreen {
            amountInputTextField.performClick()
            amountInputTextField.performTextReplacement(amount)
        }
    }
    step("Click on 'Next' button") {
        onSendScreen { nextButton.clickWithAssertion() }
    }
    enterRecipientAndOpenSendConfirm(recipientAddress)
}

/**
 * On the 'Send confirm' screen, open the network-fee selector and switch the fee token from the
 * native coin to the given (stablecoin) token — the core gasless action repeated across the suite.
 */
fun BaseTestCase.selectStablecoinAsFeeToken(coinName: String, tokenName: String) {
    step("Click on 'Network fee' block") {
        onSendConfirmScreen {
            feeSelectorBlock.assertIsDisplayed()
            feeSelectorBlock.performClick()
        }
    }
    step("Click on '$coinName' fee token to open 'Choose token'") {
        composeTestRule.waitUntil(timeoutMillis = WAIT_UNTIL_TIMEOUT_LONG) {
            runCatching { onSendFeeSelectorBottomSheet { feeTokenItem(coinName).performClick() } }.isSuccess
        }
    }
    step("Select '$tokenName' as the fee-paying token") {
        onSendFeeSelectorBottomSheet { feeTokenItem(tokenName).performClick() }
    }
    step("Wait until the '$tokenName' fee is loaded and 'Apply' is enabled") {
        composeTestRule.waitUntil(timeoutMillis = WAIT_UNTIL_TIMEOUT_LONG) {
            runCatching {
                onSendFeeSelectorBottomSheet {
                    networkFeeTitle.assertIsDisplayed()
                    feeTokenItem(tokenName).assertIsDisplayed()
                    applyButton.assertIsEnabled()
                }
            }.isSuccess
        }
    }
}

/**
 * Open an existing hot wallet (gasless signing needs a hot wallet, not the mock card), set the
 * portfolio and quotes mocks, and reach the send amount input for the given token.
 */
fun BaseTestCase.openGaslessSendScreenWithHotWallet(
    seedPhrase: String,
    tokenName: String,
    userTokensState: String,
    quotesState: String,
) {
    step("Set WireMock scenario '$USER_TOKENS_API_SCENARIO' to '$userTokensState'") {
        setWireMockScenarioState(scenarioName = USER_TOKENS_API_SCENARIO, state = userTokensState)
    }
    step("Set WireMock scenario '$QUOTES_API_SCENARIO' to '$quotesState'") {
        setWireMockScenarioState(scenarioName = QUOTES_API_SCENARIO, state = quotesState)
    }
    step("Open 'Main' screen with existing hot wallet") {
        openMainScreenWithExistingHotWallet(seedPhrase)
    }
    step("Click on token with name: '$tokenName'") {
        onMainScreen { tokenWithTitleAndAddress(tokenName).clickWithAssertion() }
    }
    step("Click on 'Transfer' button") {
        onTokenDetailsScreen { transferButton.clickWithAssertion() }
    }
    step("Click on 'Send' button in bottom sheet") {
        onTransferBottomSheet { sendButton.clickWithAssertion() }
    }
}

/**
 * Open an existing hot wallet, select the token to send and choose the swap target token/network —
 * the shared entry into the gasless send-via-swap flow. Scenario states stay in the test body.
 */
fun BaseTestCase.openSendViaSwapScreenWithHotWallet(
    seedPhrase: String,
    tokenName: String,
    swapTokenName: String,
    networkName: String,
    networkType: String? = null,
) {
    step("Open 'Main' screen with existing hot wallet") {
        openMainScreenWithExistingHotWallet(seedPhrase)
    }
    step("Click on token with name: '$tokenName'") {
        onMainScreen { tokenWithTitleAndAddress(tokenName).clickWithAssertion() }
    }
    step("Select '$swapTokenName' as the token to receive via swap") {
        selectTokenToSendViaSwap(
            swapTokenName = swapTokenName,
            networkName = networkName,
            networkType = networkType,
        )
    }
}

/**
 * Send-via-swap amount entry: type the amount, advance past the quote-gated 'Next' button (waiting
 * until it becomes enabled once the swap quote loads), then fill the recipient and open the
 * 'Send confirm' screen. Uses `composeTestRule.waitUntil` because `flakySafely` is unavailable in
 * extensions on [BaseTestCase].
 */
fun BaseTestCase.enterSwapAmountAndOpenSendConfirm(amount: String, recipientAddress: String) {
    step("Type amount '$amount' in input field") {
        onSendScreen { amountInputTextField.performTextReplacement(amount) }
    }
    step("Click on 'Next' button") {
        composeTestRule.waitUntil(timeoutMillis = WAIT_UNTIL_TIMEOUT_LONG) {
            runCatching {
                onSendScreen {
                    nextButton.assertIsEnabled()
                    nextButton.performClick()
                }
            }.isSuccess
        }
    }
    enterRecipientAndOpenSendConfirm(recipientAddress)
}