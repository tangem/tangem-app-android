package com.tangem.tests

import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.CARDANO_ADDRESS
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.extensions.pullToRefresh
import com.tangem.common.utils.resetWireMockScenarioState
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.scenarios.openMainScreen
import com.tangem.scenarios.synchronizeAddresses
import com.tangem.screens.*
import dagger.hilt.android.testing.HiltAndroidTest
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Test

@HiltAndroidTest
class BlockchainTest : BaseTestCase() {

    @AllureId("3644")
    @DisplayName("ADA: Checking the min amount/change = 1ADA")
    @Test
    fun adaCheckMinAmountTest() {
        val tokenName = "Cardano"
        val errorSendAmount = "0.1"
        val validSendAmount = "10"
        val minAmount = "ADA 1.00"
        val address = CARDANO_ADDRESS
        val scenarioName = "user_tokens_api"
        val scenarioState = "Cardano"

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
            step("Click on token with name: '$tokenName'") {
                onMainScreen { tokenWithTitleAndAddress(tokenName).clickWithAssertion() }
            }
            step("Click on 'Send' button") {
                onTokenDetailsScreen { sendButton.performClick() }
            }
            step("Type '$errorSendAmount' in input text field") {
                onSendScreen {
                    amountInputTextField.performClick()
                    amountInputTextField.performTextReplacement(errorSendAmount)
                }
            }
            step("Click on 'Next' button") {
                onSendScreen { nextButton.clickWithAssertion() }
            }
            step("Type address in input text field") {
                onSendAddressScreen { addressTextField.performTextReplacement(address) }
            }
            step("Click on 'Next' button") {
                onSendAddressScreen { nextButton.clickWithAssertion() }
            }
            step("Assert 'Invalid amount' error title is displayed") {
                onSendConfirmScreen { minimumSendAmountErrorTitle.assertIsDisplayed() }
            }
            step("Assert 'Invalid amount' error icon is displayed") {
                onSendConfirmScreen { minimumSendAmountErrorIcon(minAmount).assertIsDisplayed() }
            }
            step("Assert 'Invalid amount' error message is displayed") {
                onSendConfirmScreen { minimumSendAmountErrorMessage(minAmount).assertIsDisplayed() }
            }
            step("Press system 'Back' button") {
                device.uiDevice.pressBack()
            }
            step("Assert address text field is displayed") {
                onSendAddressScreen { addressTextField.assertIsDisplayed() }
            }
            step("Press system 'Back' button") {
                device.uiDevice.pressBack()
            }
            step("Type '$validSendAmount' in input text field") {
                onSendScreen {
                    amountInputTextField.performClick()
                    amountInputTextField.performTextReplacement(errorSendAmount)
                }
            }
            step("Click on 'Next' button") {
                onSendScreen { nextButton.clickWithAssertion() }
            }
            step("Assert address text field is displayed") {
                onSendAddressScreen { addressTextField.assertIsDisplayed() }
            }
            step("Click on 'Next' button") {
                onSendAddressScreen { nextButton.clickWithAssertion() }
            }
            step("Assert 'Invalid amount' error title is not displayed") {
                onSendConfirmScreen { minimumSendAmountErrorTitle.assertIsNotDisplayed() }
            }
            step("Assert 'Invalid amount' error icon is not displayed") {
                onSendConfirmScreen { minimumSendAmountErrorIcon(minAmount).assertIsNotDisplayed() }
            }
            step("Assert 'Invalid amount' error message is not displayed") {
                onSendConfirmScreen { minimumSendAmountErrorMessage(minAmount).assertIsNotDisplayed() }
            }
        }
    }

    @AllureId("990")
    @DisplayName("XRP: Check warning")
    @Test
    fun xrpCheckWarningTest() {
        val tokenName = "XRP Ledger"
        val amount = "1.00"
        val currencySymbol = "XRP"
        val userTokensScenarioName = "user_tokens_api"
        val userTokensScenarioState = "XRP"
        val rippleAccountInfoScenarioName = "ripple_account_info"
        val rippleAccountInfoStartedState = "Started"
        val rippleAccountInfoErrorState = "AccountNotFound"
        val rippleAccountLinesScenarioName = "ripple_account_lines"
        val rippleAccountLinesStartedState = "Started"
        val rippleAccountLinesErrorState = "AccountNotFound"

        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(userTokensScenarioName)
                resetWireMockScenarioState(rippleAccountInfoScenarioName)
                resetWireMockScenarioState(rippleAccountLinesScenarioName)
            }
        ).run {
            step("Set WireMock scenario: '$userTokensScenarioName' to state: '$userTokensScenarioState'") {
                setWireMockScenarioState(scenarioName = userTokensScenarioName, state = userTokensScenarioState)
            }
            step("Set WireMock scenario: '$rippleAccountInfoScenarioName' to state: '$rippleAccountInfoErrorState'") {
                setWireMockScenarioState(scenarioName = rippleAccountInfoScenarioName, state = rippleAccountInfoErrorState)
            }
            step("Set WireMock scenario: '$rippleAccountLinesScenarioName' to state: '$rippleAccountLinesErrorState'") {
                setWireMockScenarioState(scenarioName = rippleAccountLinesScenarioName, state = rippleAccountLinesErrorState)
            }
            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Click on token with name: '$tokenName'") {
                onMainScreen { tokenWithTitleAndAddress(tokenName).performClick() }
            }
            step("Assert 'Top up wallet' notification title is displayed") {
                onTokenDetailsScreen { topUpYourWalletNotificationTitle.assertIsDisplayed() }
            }
            step("Assert 'Top up wallet' notification message is displayed") {
                onTokenDetailsScreen {
                    topUpYourWalletNotificationMessage(
                        networkName = tokenName,
                        amount = amount,
                        currencySymbol = currencySymbol,
                    ).assertIsDisplayed()
                }
            }
            step("Assert 'Top up wallet' notification icon is displayed") {
                onTokenDetailsScreen { topUpYourWalletNotificationIcon.assertIsDisplayed() }
            }
            step("Set WireMock scenario: '$userTokensScenarioName' to state: '$userTokensScenarioState'") {
                setWireMockScenarioState(scenarioName = userTokensScenarioName, state = userTokensScenarioState)
            }
            step("Set WireMock scenario: '$rippleAccountInfoScenarioName' to state: '$rippleAccountInfoStartedState'") {
                setWireMockScenarioState(scenarioName = rippleAccountInfoScenarioName, state = rippleAccountInfoStartedState)
            }
            step("Set WireMock scenario: '$rippleAccountLinesScenarioName' to state: '$rippleAccountLinesStartedState'") {
                setWireMockScenarioState(scenarioName = rippleAccountLinesScenarioName, state = rippleAccountLinesStartedState)
            }
            step("Pull to refresh") {
                pullToRefresh()
            }
            step("Assert 'Top up wallet' notification title is not displayed") {
                onTokenDetailsScreen { topUpYourWalletNotificationTitle.assertIsNotDisplayed() }
            }
            step("Assert 'Top up wallet' notification message not is displayed") {
                onTokenDetailsScreen {
                    topUpYourWalletNotificationMessage(
                        networkName = tokenName,
                        amount = amount,
                        currencySymbol = currencySymbol,
                    ).assertIsNotDisplayed()
                }
            }
            step("Assert 'Top up wallet' notification icon not is displayed") {
                onTokenDetailsScreen { topUpYourWalletNotificationIcon.assertIsNotDisplayed() }
            }
        }
    }
}