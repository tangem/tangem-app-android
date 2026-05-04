package com.tangem.tests.send.sendViaSwap

import com.tangem.common.BaseTestCase
import com.tangem.common.R
import com.tangem.common.extensions.extractText
import com.tangem.common.constants.TestConstants.ETHEREUM_RECIPIENT_ADDRESS
import com.tangem.common.constants.TestConstants.SOLANA_RECIPIENT_ADDRESS
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_LONG
import com.tangem.common.utils.resetWireMockScenarioState
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.scenarios.openSendConfirmScreenViaNextButton
import com.tangem.scenarios.openSendScreen
import com.tangem.screens.*
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.kakaocup.kakao.common.utilities.getResourceString
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Test

@HiltAndroidTest
class SendViaSwapTest : BaseTestCase() {

    @AllureId("3970")
    @DisplayName("Send via Swap: check unsupported token error")
    @Test
    fun unsupportedTokenErrorTest() {
        val tokenName = "Bitcoin"
        val stellar = "Stellar"
        val bitcoinBalanceScenarioName = "bitcoin_utxo"
        val bitcoinBalanceScenarioState = "Balance"
        val assetsScenarioName = "express_api_assets"
        val assetsScenarioState = "BitcoinExchangeEnabled"
        val warningTitle = getResourceString(R.string.express_swap_not_supported_title, stellar)
        val warningMessage = getResourceString(R.string.express_swap_not_supported_text)

        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(bitcoinBalanceScenarioName)
                resetWireMockScenarioState(assetsScenarioName)
            }
        ).run {

            step("Set WireMock scenario: '$bitcoinBalanceScenarioName' to state: '$bitcoinBalanceScenarioState'") {
                setWireMockScenarioState(scenarioName = bitcoinBalanceScenarioName, state = bitcoinBalanceScenarioState)
            }
            step("Set WireMock scenario: '$assetsScenarioName' to state: '$assetsScenarioState'") {
                setWireMockScenarioState(scenarioName = assetsScenarioName, state = assetsScenarioState)
            }

            step("Open 'Send' screen") {
                openSendScreen(tokenName)
            }
            step("Click on 'Swap to another token button'") {
                onSendScreen { swapToAnotherTokenButton.performClick() }
            }
            step("Click on token: '$stellar'") {
                onSendViaSwapScreen { tokenItem(stellar).performClick() }
            }
            step("Assert warning bottom sheet icon is displayed") {
                onWarningBottomSheet { icon.assertIsDisplayed() }
            }
            step("Assert warning bottom sheet title is displayed") {
                onWarningBottomSheet { title(warningTitle).assertIsDisplayed() }
            }
            step("Assert warning bottom sheet message is displayed") {
                onWarningBottomSheet { message(warningMessage).assertIsDisplayed() }
            }
            step("Click on 'Ok, Got it!' button") {
                onWarningBottomSheet { gotItButton.performClick() }
            }
        }
    }

    @AllureId("3968")
    @DisplayName("Send via Swap: flow with token search and data changes")
    @Test
    fun sendViaSwapFlowWithTokenSearchAndDataChangesTest() {
        val tokenName = "Bitcoin"
        val swapTokenName = "Ethereum"
        val main = "MAIN"
        val firstInputAmount = "0.001"
        val secondInputAmount = "0.002"
        val bestRateProviderName = "SimpleSwap"
        val regularProviderName = "ChangeHero"
        val cexType = "CEX"
        val fastSelectorItem = getResourceString(R.string.common_fee_selector_option_fast)
        var capturedFastFee: String? = null
        val bitcoinBalanceScenarioName = "bitcoin_utxo"
        val bitcoinBalanceScenarioState = "Balance"
        val assetsScenarioName = "express_api_assets"
        val assetsScenarioState = "BitcoinExchangeEnabled"

        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(bitcoinBalanceScenarioName)
                resetWireMockScenarioState(assetsScenarioName)
            }
        ).run {

            step("Set WireMock scenario: '$bitcoinBalanceScenarioName' to state: '$bitcoinBalanceScenarioState'") {
                setWireMockScenarioState(scenarioName = bitcoinBalanceScenarioName, state = bitcoinBalanceScenarioState)
            }
            step("Set WireMock scenario: '$assetsScenarioName' to state: '$assetsScenarioState'") {
                setWireMockScenarioState(scenarioName = assetsScenarioName, state = assetsScenarioState)
            }

            step("Open 'Send' screen") {
                openSendScreen(tokenName)
            }
            step("Type '$firstInputAmount' in text field") {
                onSendScreen { amountInputTextField.performTextInput(firstInputAmount) }
            }
            step("Click on 'Swap to another token' button") {
                onSendScreen { swapToAnotherTokenButton.performClick() }
            }
            step("Type '$swapTokenName' in search text field") {
                onSendViaSwapScreen {
                    searchField.performClick()
                    searchField.performTextInput(swapTokenName)
                }
            }
            step("Click on token: '$swapTokenName'") {
                onSendViaSwapScreen { tokenItem(swapTokenName).performClick() }
            }
            step("Click on '$swapTokenName $main' network") {
                onChooseNetworkBottomSheet { networkItem(swapTokenName, main).performClick() }
            }
            step("Click on 'Next' button") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onSendScreen {
                        nextButton.assertIsEnabled()
                        nextButton.performClick()
                    }
                }
            }
            step("Type recipient address") {
                onSendAddressScreen { addressTextField.performTextReplacement(ETHEREUM_RECIPIENT_ADDRESS) }
            }
            step("Open 'Send confirm' screen via 'Next' button") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    openSendConfirmScreenViaNextButton()
                }
            }
            step("Assert 'Best rate' badge is displayed") {
                onSendConfirmScreen { bestRateBadge.assertIsDisplayed() }
            }
            step("Click on 'Provider'") {
                onSendConfirmScreen { provider.performClick() }
            }
            step("Assert '$bestRateProviderName' is displayed") {
                onSelectSwapProviderBottomSheet {
                    providerItem(
                        name = bestRateProviderName,
                        type = cexType,
                        isBestRate = true
                    ).assertIsDisplayed()
                }
            }
            step("Click on '$regularProviderName'") {
                onSelectSwapProviderBottomSheet {
                    providerItem(
                        name = regularProviderName,
                        type = cexType,
                        isBestRate = false
                    ).performClick()
                }
            }
            step("Assert provider name is '$regularProviderName'") {
                onSendConfirmScreen { providerName.assertTextContains(regularProviderName) }
            }
            step("Assert 'Best rate' badge is not displayed") {
                onSendConfirmScreen { bestRateBadge.assertIsNotDisplayed() }
            }
            step("Assert provider name is '$regularProviderName'") {
                onSendConfirmScreen { providerName.assertTextContains(regularProviderName) }
            }
            step("Click on fee selector icon") {
                onSendConfirmScreen { feeSelectorIcon.performClick() }
            }
            step("Click on '$fastSelectorItem' selector item is displayed") {
                onSendSelectNetworkFeeBottomSheet { regularFeeSelectorItem(fastSelectorItem).performClick() }
            }
            step("Capture fast fee value") {
                onSendConfirmScreen { capturedFastFee = feeAmount.extractText() }
            }
            step("Click on send amount") {
                onSendConfirmScreen { primaryAmount.performClick() }
            }
            step("Click on swap token '$tokenName'") {
                onSendScreen { swapTokenName(tokenName).performClick() }
            }
            step("Type '$secondInputAmount' in text field") {
                onSendScreen { amountInputTextField.performTextReplacement(secondInputAmount) }
            }
            step("Click on 'Continue' button") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onSendScreen {
                        continueButton.assertIsDisplayed()
                        continueButton.assertIsEnabled()
                        continueButton.performClick()
                    }
                    onSendConfirmScreen { sendButton.assertIsDisplayed() }
                }
            }
            step("Assert 'Best rate' badge is displayed") {
                onSendConfirmScreen { bestRateBadge.assertIsDisplayed() }
            }
            step("Assert provider name is '$bestRateProviderName'") {
                onSendConfirmScreen { providerName.assertTextContains(bestRateProviderName) }
            }
            step("Assert fee is equal to previously captured fast fee") {
                val expectedFee = requireNotNull(capturedFastFee) { "Fast fee was not captured" }
                onSendConfirmScreen { feeAmount.assertTextContains(expectedFee) }
            }
        }
    }

    @AllureId("3969")
    @DisplayName("Send via Swap: flow cancel and return")
    @Test
    fun sendViaSwapCancelAndReturnFlowTest() {
        val tokenName = "Bitcoin"
        val firstSwapTokenName = "Ethereum"
        val secondSwapTokenName = "Solana"
        val main = "MAIN"
        val inputAmount = "0.001"
        val bitcoinBalanceScenarioName = "bitcoin_utxo"
        val bitcoinBalanceScenarioState = "Balance"
        val assetsScenarioName = "express_api_assets"
        val assetsScenarioState = "BitcoinExchangeEnabled"
        val dialogTitle = getResourceString(R.string.send_with_swap_change_token_alert_title)
        val dialogMessage = getResourceString(R.string.send_with_swap_change_token_alert_message)
        val addressHint = getResourceString(R.string.send_enter_address_field_ens)

        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(bitcoinBalanceScenarioName)
                resetWireMockScenarioState(assetsScenarioName)
            }
        ).run {

            step("Set WireMock scenario: '$bitcoinBalanceScenarioName' to state: '$bitcoinBalanceScenarioState'") {
                setWireMockScenarioState(scenarioName = bitcoinBalanceScenarioName, state = bitcoinBalanceScenarioState)
            }
            step("Set WireMock scenario: '$assetsScenarioName' to state: '$assetsScenarioState'") {
                setWireMockScenarioState(scenarioName = assetsScenarioName, state = assetsScenarioState)
            }

            step("Open 'Send' screen") {
                openSendScreen(tokenName)
            }
            step("Type '$inputAmount' in text field") {
                onSendScreen { amountInputTextField.performTextInput(inputAmount) }
            }
            step("Click on 'Swap to another token' button") {
                onSendScreen { swapToAnotherTokenButton.performClick() }
            }
            step("Click on token: '$firstSwapTokenName'") {
                onSendViaSwapScreen { tokenItem(firstSwapTokenName).performClick() }
            }
            step("Click on '$firstSwapTokenName $main' network") {
                onChooseNetworkBottomSheet { networkItem(firstSwapTokenName, main).performClick() }
            }
            step("Click on 'Close convert' button") {
                onSendScreen { closeConvertIcon.performClick() }
            }
            step("Assert 'Swap to another token' button is displayed") {
                onSendScreen { swapToAnotherTokenButton.assertIsDisplayed() }
            }
            step("Assert amount: '$inputAmount'") {
                onSendScreen { amountInputTextField.assertTextContains(inputAmount) }
            }
            step("Click on 'Swap to another token' button") {
                onSendScreen { swapToAnotherTokenButton.performClick() }
            }
            step("Click on token: '$secondSwapTokenName'") {
                onSendViaSwapScreen { tokenItem(secondSwapTokenName).performClick() }
            }
            step("Click on '$secondSwapTokenName $main' network") {
                onChooseNetworkBottomSheet { networkItem(secondSwapTokenName, main).performClick() }
            }
            step("Click on token '$tokenName'") {
                onSendScreen { swapTokenName(tokenName).performClick() }
            }
            step("Type '$inputAmount' in text field") {
                onSendScreen { amountInputTextField.performTextInput(inputAmount) }
            }
            step("Click on 'Next' button") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onSendScreen {
                        nextButton.assertIsEnabled()
                        nextButton.performClick()
                    }
                }
            }
            step("Type recipient address") {
                onSendAddressScreen { addressTextField.performTextReplacement(SOLANA_RECIPIENT_ADDRESS) }
            }
            step("Open 'Send confirm' screen via 'Next' button") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    openSendConfirmScreenViaNextButton()
                }
            }
            step("Click on 'Amount to receive'") {
                onSendConfirmScreen { secondaryAmount.performClick() }
            }
            step("Click on token '$secondSwapTokenName' to open 'Send via Swap' screen") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onSendScreen { swapTokenName(secondSwapTokenName).performClick() }
                    onSendViaSwapScreen { tokenItem(firstSwapTokenName).assertIsDisplayed() }
                }
            }
            step("Click on token: '$firstSwapTokenName'") {
                onSendViaSwapScreen { tokenItem(firstSwapTokenName).performClick() }
            }
            step("Click on '$firstSwapTokenName $main' network") {
                onChooseNetworkBottomSheet { networkItem(firstSwapTokenName, main).performClick() }
            }
            step("Assert '$dialogTitle' dialog title is displayed") {
                onDialog { title.assertTextEquals(dialogTitle) }
            }
            step("Assert '$dialogMessage' dialog message is displayed") {
                onDialog { text.assertTextEquals(dialogMessage) }
            }
            step("Click on 'Cancel' button") {
                onDialog { cancelButton.performClick() }
            }
            step("Click on 'Close' button") {
                onSendViaSwapScreen { closeButton.performClick() }
            }
            step("Click on 'Continue' button") {
                onSendScreen { continueButton.performClick() }
            }
            step("Assert recipient address is displayed") {
                onSendConfirmScreen { recipientAddress(SOLANA_RECIPIENT_ADDRESS).assertIsDisplayed() }
            }
            step("Click on 'Amount to receive'") {
                onSendConfirmScreen { secondaryAmount.performClick() }
            }
            step("Click on token '$secondSwapTokenName' to open 'Send via Swap' screen") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onSendScreen { swapTokenName(secondSwapTokenName).performClick() }
                    onSendViaSwapScreen { tokenItem(firstSwapTokenName).assertIsDisplayed() }
                }
            }
            step("Click on token: $firstSwapTokenName") {
                onSendViaSwapScreen { tokenItem(firstSwapTokenName).performClick() }
            }
            step("Click on '$firstSwapTokenName $main' network") {
                onChooseNetworkBottomSheet { networkItem(firstSwapTokenName, main).performClick() }
            }
            step("Click on 'Change' button") {
                onDialog { changeButton.performClick() }
            }
            step("Assert '$firstSwapTokenName' is displayed") {
                onSendScreen { swapTokenName(firstSwapTokenName).assertIsDisplayed() }
            }
            step("Click on 'Next' button to open 'Send address' screen") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onSendScreen {
                        nextButton.assertIsEnabled()
                        nextButton.performClick()
                    }
                    onSendAddressScreen { container.assertIsDisplayed() }
                }
            }
            step("Assert address text field is empty and hint is displayed") {
                onSendAddressScreen { addressTextFieldHint.assertTextContains(addressHint) }
            }
        }
    }
}