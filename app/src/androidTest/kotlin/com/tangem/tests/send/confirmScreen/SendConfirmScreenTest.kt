package com.tangem.tests.send.confirmScreen

import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.ETHEREUM_RECIPIENT_ADDRESS
import com.tangem.common.constants.TestConstants.POLKADOT_RECIPIENT_ADDRESS
import com.tangem.common.constants.TestConstants.QUOTES_API_SCENARIO
import com.tangem.common.constants.TestConstants.USER_TOKENS_API_SCENARIO
import com.tangem.common.extensions.*
import com.tangem.common.utils.resetWireMockScenarioState
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.core.ui.R
import com.tangem.scenarios.*
import com.tangem.screens.*
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.kakaocup.kakao.common.utilities.getResourceString
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Test

@HiltAndroidTest
class SendConfirmScreenTest : BaseTestCase() {

    @AllureId("4003")
    @DisplayName("Send (Confirm screen): change sending amount")
    @Test
    fun changeSendingAmountTest() {
        val tokenName = "Ethereum"
        val inputAmount = "1"
        val newInputAmount = "0.9"
        val ethereumAmount = "1.00"
        val fiatAmount = "$2,535.63"
        val newFiatAmount = "$2,282.07"
        val newEthereumAmount = "0.90"
        val recipientAddress = ETHEREUM_RECIPIENT_ADDRESS

        setupHooks().run {
            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Click on token with name: '$tokenName'") {
                onMainScreen { tokenWithTitleAndAddress(tokenName).clickWithAssertion() }
            }
            step("Click on 'Send' button") {
                onTokenDetailsScreen { sendButton().performClick() }
            }
            step("Type '$inputAmount' in input text field") {
                onSendScreen {
                    amountInputTextField.performClick()
                    amountInputTextField.performTextReplacement(inputAmount)
                }
            }
            step("Assert fiat amount = '$fiatAmount'") {
                onSendScreen { equivalentInputAmount.assertTextContains(fiatAmount) }
            }
            step("Click on 'Next' button") {
                onSendScreen { nextButton.clickWithAssertion() }
            }
            step("Type recipient address") {
                onSendAddressScreen { addressTextField.performTextReplacement(recipientAddress) }
            }
            step("Click on 'Next' button") {
                onSendAddressScreen { nextButton.clickWithAssertion() }
            }
            step("Assert token amount = '$ethereumAmount'") {
                onSendConfirmScreen { primaryAmount.assertTextContains(ethereumAmount) }
            }
            step("Assert fiat amount = '$fiatAmount'") {
                onSendConfirmScreen { secondaryAmount.assertTextContains(fiatAmount) }
            }
            step("Click on token amount") {
                onSendConfirmScreen { primaryAmount.performClick() }
            }
            step("Clear input text field") {
                onSendScreen {
                    amountInputTextField.performClick()
                    amountInputTextField.performTextClearance()
                }
            }
            step("Type '$newInputAmount' in input text field") {
                onSendScreen {
                    amountInputTextField.performTextReplacement(newInputAmount)
                }
            }
            step("Assert fiat amount = '$newFiatAmount'") {
                onSendScreen { equivalentInputAmount.assertTextContains(newFiatAmount) }
            }
            step("Click on 'Continue' button") {
                onSendScreen { continueButton.clickWithAssertion() }
            }
            step("Assert token amount = '$newEthereumAmount'") {
                onSendConfirmScreen { primaryAmount.assertTextContains(newEthereumAmount) }
            }
            step("Assert fiat amount = '$newFiatAmount'") {
                onSendConfirmScreen { secondaryAmount.assertTextContains(newFiatAmount) }
            }
        }
    }

    @AllureId("552")
    @DisplayName("Send (Confirm screen): switch to equivalent")
    @Test
    fun switchToEquivalentTest() {
        val tokenName = "Ethereum"
        val inputAmount = "1"
        val tokenAmount = "1.00"
        val ethereumAmount = "ETH 1.00"
        val fiatAmount = "$2,535.63"
        val recipientAddress = ETHEREUM_RECIPIENT_ADDRESS

        setupHooks().run {
            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Click on token with name: '$tokenName'") {
                onMainScreen { tokenWithTitleAndAddress(tokenName).clickWithAssertion() }
            }
            step("Click on 'Send' button") {
                onTokenDetailsScreen { sendButton().performClick() }
            }
            step("Type '$inputAmount' in input text field") {
                onSendScreen {
                    amountInputTextField.performClick()
                    amountInputTextField.performTextReplacement(inputAmount)
                }
            }
            step("Assert fiat amount = '$fiatAmount'") {
                onSendScreen { equivalentInputAmount.assertTextContains(fiatAmount) }
            }
            step("Click on exchange icon") {
                onSendScreen { exchangeIcon.performClick() }
            }
            step("Assert token amount = '$ethereumAmount'") {
                onSendScreen { equivalentInputAmount.assertTextContains(ethereumAmount) }
            }
            step("Click on exchange icon") {
                onSendScreen { exchangeIcon.performClick() }
            }
            step("Assert fiat amount = '$fiatAmount'") {
                onSendScreen { equivalentInputAmount.assertTextContains(fiatAmount) }
            }
            step("Click on 'Next' button") {
                onSendScreen { nextButton.clickWithAssertion() }
            }
            step("Type recipient address") {
                onSendAddressScreen { addressTextField.performTextReplacement(recipientAddress) }
            }
            step("Click on 'Next' button") {
                onSendAddressScreen { nextButton.clickWithAssertion() }
            }
            step("Assert primary amount = '$tokenAmount'") {
                onSendConfirmScreen { primaryAmount.assertTextContains(tokenAmount) }
            }
            step("Assert secondary amount = '$fiatAmount'") {
                onSendConfirmScreen { secondaryAmount.assertTextContains(fiatAmount) }
            }
        }
    }

    @AllureId("553")
    @DisplayName("Send (Confirm screen): check fee for blockchain with unknown fee")
    @Test
    fun checkFeeForBlockchainWithUnknownFeeTest() {
        val tokenName = "Polygon"
        val inputAmount = "1"
        val recipientAddress = ETHEREUM_RECIPIENT_ADDRESS
        val currentFeeAmount = "<$0.01"

        setupHooks().run {
            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Swipe up to token") {
                swipeVertical(SwipeDirection.UP)
            }
            step("Open 'Send Confirm' screen for token '$tokenName'") {
                openSendConfirmScreen(
                    tokenName = tokenName,
                    inputAmount = inputAmount,
                    recipientAddress = recipientAddress
                )
            }
            step("Check network fee block") {
                checkNetworkFeeBlock(currentFeeAmount = currentFeeAmount, withFeeSelector = true)
            }
        }
    }

    @AllureId("4565")
    @DisplayName("Send (Confirm screen): check fee for token with unknown fee")
    @Test
    fun checkFeeForTokenWithUnknownFeeTest() {
        val tokenName = "POL (ex-MATIC)"
        val inputAmount = "1"
        val recipientAddress = ETHEREUM_RECIPIENT_ADDRESS
        val currentFeeAmount = "<$0.01"
        val scenarioName = "eth_estimate_gas"
        val scenarioState = "UnknownFee"

        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(scenarioName)
            }
        ).run {
            step("Set WireMock scenario: '$scenarioName' to state: '$scenarioState'") {
                setWireMockScenarioState(scenarioName = scenarioName, state = scenarioState)
            }
            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Swipe up to token") {
                swipeVertical(SwipeDirection.UP)
            }
            step("Open 'Send Confirm' screen for token '$tokenName'") {
                openSendConfirmScreen(
                    tokenName = tokenName,
                    inputAmount = inputAmount,
                    recipientAddress = recipientAddress
                )
            }
            step("Check network fee block") {
                checkNetworkFeeBlock(currentFeeAmount = currentFeeAmount, withFeeSelector = true)
            }
        }
    }

    @AllureId("554")
    @DisplayName("Send (Confirm screen): check fee warning")
    @Test
    fun checkFeeWarningTest() {
        val tokenName = "Polkadot"
        val tokenAmount = "0.1"
        val warningTitle = getResourceString(R.string.send_fee_unreachable_error_title)
        val warningMessageResId = R.string.send_fee_unreachable_error_text
        val currentFeeAmount = "$0.05"

        setupHooks(
            additionalAfterSection = {
                enableWiFi()
                enableMobileData()
                resetWireMockScenarioState(USER_TOKENS_API_SCENARIO)
                resetWireMockScenarioState(QUOTES_API_SCENARIO)
            }
        ).run {
            step("Open 'Send Screen' with token: $tokenName") {
                openSendScreen(tokenName)
            }
            step("Type '$tokenAmount' in input text field") {
                onSendScreen {
                    amountInputTextField.performClick()
                    amountInputTextField.performTextReplacement(tokenAmount)
                }
            }
            step("Click on 'Next' button") {
                onSendScreen { nextButton.clickWithAssertion() }
            }
            step("Type address in input text field") {
                onSendAddressScreen { addressTextField.performTextReplacement(POLKADOT_RECIPIENT_ADDRESS) }
            }
            step("Turn off internet") {
                disableWiFi()
                disableMobileData()
            }
            step("Click on 'Next' button") {
                onSendAddressScreen { nextButton.clickWithAssertion() }
            }
            step("Turn on internet") {
                enableWiFi()
                enableMobileData()
            }
            step("Assert 'Network fee info unreachable' warning title is displayed") {
                onSendConfirmScreen { warningTitle(warningTitle).assertIsDisplayed() }
            }
            step("Assert 'Check your internet connection' warning message is displayed") {
                onSendConfirmScreen { warningMessage(warningMessageResId).assertIsDisplayed() }
            }
            step("Assert warning icon is displayed") {
                onSendConfirmScreen { warningIcon(warningTitle).assertIsDisplayed() }
            }
            step("Click on 'Refresh' button") {
                waitForIdle()
                onSendConfirmScreen { refreshButton.clickWithAssertion() }
            }
            step("Check network fee block") {
                checkNetworkFeeBlock(currentFeeAmount = currentFeeAmount, withFeeSelector = false)
            }
        }
    }
}