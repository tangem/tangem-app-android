package com.tangem.tests.send.feeScreen

import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.BITCOIN_RECIPIENT_ADDRESS
import com.tangem.common.constants.TestConstants.ETHEREUM_RECIPIENT_ADDRESS
import com.tangem.common.constants.TestConstants.POLKADOT_RECIPIENT_ADDRESS
import com.tangem.common.constants.TestConstants.QUOTES_API_SCENARIO
import com.tangem.common.constants.TestConstants.TERRA_RECIPIENT_ADDRESS
import com.tangem.common.constants.TestConstants.USER_TOKENS_API_SCENARIO
import com.tangem.common.extensions.SwipeDirection
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.extensions.swipeVertical
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
class SendFeeScreenTest : BaseTestCase() {

    @AllureId("4906")
    @DisplayName("Send (Fee screen): check fee block for fee in token")
    @Test
    fun checkFeeBlockForFeeInTokenTest() {
        val tokenName = "TerraClassicUSD"
        val scenarioName = "Terra"
        val tokenAmount = "1"
        val feeAmount = "<$0.01"

        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(USER_TOKENS_API_SCENARIO)
                resetWireMockScenarioState(QUOTES_API_SCENARIO)
            }
        ).run {
            step("Open 'Send' screen") {
                openSendScreen(tokenName, scenarioName)
            }
            step("Type '$tokenAmount' in input text field") {
                onSendScreen {
                    amountInputTextField.performClick()
                    amountInputTextField.performTextReplacement(tokenAmount)
                }
            }
            step("Click on 'Next' button") {
                onSendAddressScreen { nextButton.clickWithAssertion() }
            }
            step("Type recipient address") {
                onSendAddressScreen { addressTextField.performTextReplacement(TERRA_RECIPIENT_ADDRESS) }
            }
            step("Click on 'Next' button") {
                onSendScreen { nextButton.clickWithAssertion() }
            }
            step("Assert fee block is displayed without fee selector") {
                checkNetworkFeeBlock(currentFeeAmount = feeAmount, withFeeSelector = false)
            }
        }
    }

    @AllureId("4868")
    @DisplayName("Send (Fee screen): check fee block for fixed fee")
    @Test
    fun checkFeeBlockForFixedFeeTest() {
        val tokenName = "Polkadot"
        val tokenAmount = "1"
        val feeAmount = "$0.05"

        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(USER_TOKENS_API_SCENARIO)
                resetWireMockScenarioState(QUOTES_API_SCENARIO)
            }
        ).run {
            step("Open 'Send' screen") {
                openSendScreen(tokenName)
            }
            step("Type '$tokenAmount' in input text field") {
                onSendScreen {
                    amountInputTextField.performClick()
                    amountInputTextField.performTextReplacement(tokenAmount)
                }
            }
            step("Click on 'Next' button") {
                onSendAddressScreen { nextButton.clickWithAssertion() }
            }
            step("Type recipient address") {
                onSendAddressScreen { addressTextField.performTextReplacement(POLKADOT_RECIPIENT_ADDRESS) }
            }
            step("Click on 'Next' button") {
                onSendScreen { nextButton.clickWithAssertion() }
            }
            step("Assert fee block is displayed without fee selector") {
                checkNetworkFeeBlock(currentFeeAmount = feeAmount, withFeeSelector = false)
            }
            step("Click on 'Fee selector' block") {
                onSendConfirmScreen { feeSelectorBlock.performClick() }
            }
            step("Assert 'Fee selector' bottom sheet is not displayed") {
                onSwapSelectNetworkFeeBottomSheet { title.assertIsNotDisplayed() }
            }
        }
    }

    @AllureId("4869")
    @DisplayName("Send (Fee screen): check network fee bottom sheet for EVM networks")
    @Test
    fun checkNetworkFeeBottomSheetForEvmTest() {
        val tokenName = "Ethereum"
        val tokenAmount = "0.1"
        val feeAmount = "~$1.06"
        val fiatFeeAmount = "$1.09"
        val newFeeAmount = "~$1.09"
        val marketSelectorItem = getResourceString(R.string.common_fee_selector_option_market)
        val fastSelectorItem = getResourceString(R.string.common_fee_selector_option_fast)
        val slowSelectorItem = getResourceString(R.string.common_fee_selector_option_slow)
        val feeUpTo = getResourceString(R.string.send_max_fee)
        val feeUpToTooltip = getResourceString(R.string.send_custom_amount_fee_footer)
        val feeUpToValue = "0.00042 ETH"
        val newFeeUpToValue = "0.00043"
        val maxFee = getResourceString(R.string.send_custom_evm_max_fee)
        val maxFeeTooltip = getResourceString(R.string.send_custom_evm_max_fee_footer)
        val maxFeeValue = "20 GWEI"
        val newMaxFeeValue = "21"
        val priorityFee = getResourceString(R.string.send_custom_evm_priority_fee)
        val priorityFeeTooltip = getResourceString(R.string.send_custom_evm_priority_fee_footer)
        val priorityFeeValue = "2 GWEI"
        val newPriorityFeeValue = "3"
        val gasLimit = getResourceString(R.string.send_gas_limit)
        val gasLimitTooltip = getResourceString(R.string.send_gas_limit_footer)
        val gasLimitValue = "21,000 "
        val newGasLimitValue = "22"
        val nonce = getResourceString(R.string.send_nonce)
        val nonceTooltip = getResourceString(R.string.send_nonce_footer)
        val nonceValue = "1"

        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(USER_TOKENS_API_SCENARIO)
                resetWireMockScenarioState(QUOTES_API_SCENARIO)
            }
        ).run {
            step("Open 'Send' screen") {
                openSendScreen(tokenName)
            }
            step("Type '$tokenAmount' in input text field") {
                onSendScreen {
                    amountInputTextField.performClick()
                    amountInputTextField.performTextReplacement(tokenAmount)
                }
            }
            step("Click on 'Next' button") {
                onSendAddressScreen { nextButton.clickWithAssertion() }
            }
            step("Type recipient address") {
                onSendAddressScreen { addressTextField.performTextReplacement(ETHEREUM_RECIPIENT_ADDRESS) }
            }
            step("Click on 'Next' button") {
                onSendScreen { nextButton.clickWithAssertion() }
            }
            step("Assert fee block is displayed with fee selector") {
                checkNetworkFeeBlock(currentFeeAmount = feeAmount, withFeeSelector = true)
            }
            step("Click on fee selector icon") {
                onSendConfirmScreen { feeSelectorIcon.performClick() }
            }
            step("Assert 'Fee selector' bottom sheet title is displayed") {
                onSendSelectNetworkFeeBottomSheet { title.assertIsDisplayed() }
            }
            step("Click on '$marketSelectorItem' selector item") {
                onSendSelectNetworkFeeBottomSheet { regularFeeSelectorItem(marketSelectorItem).performClick() }
            }
            step("Assert '$fastSelectorItem' selector item is displayed") {
                onSendSelectNetworkFeeBottomSheet { regularFeeSelectorItem(fastSelectorItem).assertIsDisplayed() }
            }
            step("Assert '$slowSelectorItem' selector item is displayed") {
                onSendSelectNetworkFeeBottomSheet { regularFeeSelectorItem(slowSelectorItem).assertIsDisplayed() }
            }
            step("Assert '$marketSelectorItem' selector item is displayed") {
                onSendSelectNetworkFeeBottomSheet { regularFeeSelectorItem(marketSelectorItem).assertIsDisplayed() }
            }
            step("Assert 'Custom' selector item is displayed") {
                onSendSelectNetworkFeeBottomSheet { customSelectorItem.assertIsDisplayed() }
            }
            step("Click on 'Custom' selector item") {
                onSendSelectNetworkFeeBottomSheet { customSelectorItem.performClick() }
            }
            step("Assert '$feeUpTo' input item is displayed") {
                onSendSelectNetworkFeeBottomSheet {
                    customInputItem(title = feeUpTo, hasFiatAmount = true).assertIsDisplayed()
                }
            }
            step("Assert '$maxFee' input item is displayed") {
                onSendSelectNetworkFeeBottomSheet { customInputItem(maxFee).assertIsDisplayed() }
            }
            step("Assert '$priorityFee' input item is displayed") {
                onSendSelectNetworkFeeBottomSheet { customInputItem(priorityFee).assertIsDisplayed() }
            }
            step("Assert '$gasLimit' input item is displayed") {
                onSendSelectNetworkFeeBottomSheet { customInputItem(gasLimit).assertIsDisplayed() }
            }
            step("Swipe up") {
                swipeVertical(SwipeDirection.UP)
            }
            step("Assert '$nonce' input item is displayed") {
                onSendSelectNetworkFeeBottomSheet { nonceInputItem.assertIsDisplayed() }
            }
            step("Check '$feeUpTo' tooltip") {
                checkCustomFeeTooltip(title = feeUpTo, tooltip = feeUpToTooltip)
            }
            step("Check '$maxFee' tooltip") {
                checkCustomFeeTooltip(title = maxFee, tooltip = maxFeeTooltip)
            }
            step("Check '$priorityFee' tooltip") {
                checkCustomFeeTooltip(title = priorityFee, tooltip = priorityFeeTooltip)
            }
            step("Check '$gasLimit' tooltip") {
                checkCustomFeeTooltip(title = gasLimit, tooltip = gasLimitTooltip)
            }
            step("Check '$nonce' tooltip") {
                checkCustomFeeTooltip(title = nonce, tooltip = nonceTooltip)
            }
            step("Assert '$feeUpTo' value: '$feeUpToValue'") {
                onSendSelectNetworkFeeBottomSheet { inputTextFieldValue(feeUpTo).assertTextContains(feeUpToValue) }
            }
            step("Assert '$maxFee' value: '$maxFeeValue'") {
                onSendSelectNetworkFeeBottomSheet { inputTextFieldValue(maxFee).assertTextContains(maxFeeValue) }
            }
            step("Assert '$priorityFee' value: '$priorityFeeValue'") {
                onSendSelectNetworkFeeBottomSheet { inputTextFieldValue(priorityFee).assertTextContains(priorityFeeValue) }
            }
            step("Assert '$gasLimit' value: '$gasLimitValue'") {
                onSendSelectNetworkFeeBottomSheet { inputTextFieldValue(gasLimit).assertTextContains(gasLimitValue) }
            }
            step("Check changes in '$maxFee' input text field") {
                checkChangesInInputTextField(title = maxFee, newValue = newMaxFeeValue, addition = " GWEI")
            }
            step("Check changes in '$priorityFee' input text field") {
                checkChangesInInputTextField(title = priorityFee, newValue = newPriorityFeeValue, addition = " GWEI")
            }
            step("Check changes in '$gasLimit' input text field") {
                checkChangesInInputTextField(title = gasLimit, newValue = newGasLimitValue, addition = " ")
            }
            step("Type '$nonceValue' in '$nonce' text field") {
                onSendSelectNetworkFeeBottomSheet {
                    nonceInputTextField.performClick()
                    nonceInputTextField.performTextReplacement(nonceValue)
                }
            }
            step("Assert '$nonce' value: '$nonceValue'") {
                onSendSelectNetworkFeeBottomSheet { nonceInputTextField.assertTextContains(nonceValue) }
            }
            step("Check changes in '$feeUpTo' input text field") {
                checkChangesInInputTextField(title = feeUpTo, newValue = newFeeUpToValue, addition = " ETH")
            }
            step("Assert new fiat fee amount: '$fiatFeeAmount'") {
                onSendSelectNetworkFeeBottomSheet { customInputItemFiatAmount.assertTextContains(fiatFeeAmount) }
            }
            step("Click on 'Done' button") {
                onSendSelectNetworkFeeBottomSheet { doneButton.performClick() }
            }
            step("Click on 'Confirm' button") {
                onSendSelectNetworkFeeBottomSheet { confirmButton.performClick() }
            }
            step("Assert fee block is displayed with new fee amount: '$newFeeAmount'") {
                checkNetworkFeeBlock(currentFeeAmount = newFeeAmount, withFeeSelector = true)
            }
        }
    }

    @AllureId("4870")
    @DisplayName("Send (Fee screen): check network fee bottom sheet for Bitcoin")
    @Test
    fun checkNetworkFeeBottomSheetForBitcoinTest() {
        val tokenName = "Bitcoin"
        val tokenAmount = "0.00000001"
        val feeAmount = "$2.86"
        val fiatFeeAmount = "$0.24"
        val marketSelectorItem = getResourceString(R.string.common_fee_selector_option_market)
        val fastSelectorItem = getResourceString(R.string.common_fee_selector_option_fast)
        val slowSelectorItem = getResourceString(R.string.common_fee_selector_option_slow)
        val feeUpTo = getResourceString(R.string.send_max_fee)
        val feeUpToValue = "0.0000264 BTC"
        val newFeeUpToValue = "0.0000022 BTC"
        val satoshi = getResourceString(R.string.send_satoshi_per_byte_title)
        val satoshiValue = "2"
        val decimalNumber = "2.11"
        val newSatoshiValue = "1"
        val bitcoinUtxoScenarioName = "bitcoin_utxo"
        val bitcoinUtxoScenarioState = "Balance"
        val feeScenarioName = "bitcoin_estimate_smart_fee"
        val feeScenarioState = "Started"

        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(USER_TOKENS_API_SCENARIO)
                resetWireMockScenarioState(bitcoinUtxoScenarioName)
            }
        ).run {
            step("Set WireMock scenario: '$bitcoinUtxoScenarioName' to state: '$bitcoinUtxoScenarioState'") {
                setWireMockScenarioState(bitcoinUtxoScenarioName, bitcoinUtxoScenarioState)
            }
            step("Set WireMock scenario: '$feeScenarioName' to state: '$feeScenarioState'") {
                setWireMockScenarioState(feeScenarioName, feeScenarioState)
            }
            step("Open 'Main' screen") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Open 'Send address' screen") {
                openSendAddressScreen(tokenName, tokenAmount)
            }
            step("Type recipient address") {
                onSendAddressScreen { addressTextField.performTextReplacement(BITCOIN_RECIPIENT_ADDRESS) }
            }
            step("Click on 'Next' button") {
                onSendScreen { nextButton.clickWithAssertion() }
            }
            step("Assert fee block is displayed with fee selector") {
                checkNetworkFeeBlock(currentFeeAmount = feeAmount, withFeeSelector = true)
            }
            step("Click on fee selector icon") {
                onSendConfirmScreen { feeSelectorIcon.performClick() }
            }
            step("Assert 'Choose speed' bottom sheet title is displayed") {
                onSendSelectNetworkFeeBottomSheet { chooseSpeedTitle.assertIsDisplayed() }
            }
            step("Assert '$fastSelectorItem' selector item is displayed") {
                onSendSelectNetworkFeeBottomSheet { regularFeeSelectorItem(fastSelectorItem).assertIsDisplayed() }
            }
            step("Assert '$slowSelectorItem' selector item is displayed") {
                onSendSelectNetworkFeeBottomSheet { regularFeeSelectorItem(slowSelectorItem).assertIsDisplayed() }
            }
            step("Assert '$marketSelectorItem' selector item is displayed") {
                onSendSelectNetworkFeeBottomSheet { regularFeeSelectorItem(marketSelectorItem).assertIsDisplayed() }
            }
            step("Assert 'Custom' selector item is displayed") {
                onSendSelectNetworkFeeBottomSheet { customSelectorItem.assertIsDisplayed() }
            }
            step("Click on 'Custom' selector item") {
                onSendSelectNetworkFeeBottomSheet { customSelectorItem.performClick() }
            }
            step("Assert '$feeUpTo' input item is displayed") {
                onSendSelectNetworkFeeBottomSheet {
                    customInputItem(title = feeUpTo, hasFiatAmount = true).assertIsDisplayed()
                }
            }
            step("Assert '$feeUpTo' value: '$feeUpToValue'") {
                onSendSelectNetworkFeeBottomSheet { inputTextFieldValue(feeUpTo).assertTextContains(feeUpToValue) }
            }
            step("Assert '$satoshi' input item is displayed") {
                onSendSelectNetworkFeeBottomSheet { customInputItem(satoshi).assertIsDisplayed() }
            }
            step("Click on '$satoshi' input text field") {
                onSendSelectNetworkFeeBottomSheet { inputTextFieldValue(satoshi).performClick() }
            }
            step("Type '$decimalNumber' in '$satoshi' input text field") {
                onSendSelectNetworkFeeBottomSheet { inputTextFieldValue(satoshi).performTextReplacement(decimalNumber) }
            }
            step("Assert '$satoshi' value: '$satoshiValue +  '") {
                onSendSelectNetworkFeeBottomSheet { inputTextFieldValue(satoshi).assertTextContains("$satoshiValue ") }
            }
            step("Check changes in '$satoshi' input text field") {
                checkChangesInInputTextField(title = satoshi, newValue = newSatoshiValue, addition = " ")
            }
            step("Assert new fiat fee amount: '$fiatFeeAmount'") {
                onSendSelectNetworkFeeBottomSheet { customInputItemFiatAmount.assertTextContains(fiatFeeAmount) }
            }
            step("Assert '$feeUpTo' value: '$newFeeUpToValue'") {
                onSendSelectNetworkFeeBottomSheet { inputTextFieldValue(feeUpTo).assertTextContains(newFeeUpToValue) }
            }
            step("Click on 'Done' button") {
                onSendSelectNetworkFeeBottomSheet { doneButton.performClick() }
            }
            step("Assert fee block is displayed with new fee amount: '$fiatFeeAmount'") {
                checkNetworkFeeBlock(currentFeeAmount = fiatFeeAmount, withFeeSelector = true)
            }
        }
    }

    @AllureId("4871")
    @DisplayName("Send (Fee screen): check network fee bottom sheet networks with fee in token")
    @Test
    fun checkNetworkFeeBottomSheetForVeThorTest() {
        val tokenName = "VeThor"
        val mockState = "Vechain"
        val tokenAmount = "0.1"
        val feeAmount = "~$0.01"
        val marketSelectorItem = getResourceString(R.string.common_fee_selector_option_market)
        val fastSelectorItem = getResourceString(R.string.common_fee_selector_option_fast)
        val slowSelectorItem = getResourceString(R.string.common_fee_selector_option_slow)

        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(USER_TOKENS_API_SCENARIO)
                resetWireMockScenarioState(QUOTES_API_SCENARIO)
            }
        ).run {
            step("Open 'Send' screen") {
                openSendScreen(tokenName = tokenName, mockState = mockState)
            }
            step("Type '$tokenAmount' in input text field") {
                onSendScreen {
                    amountInputTextField.performClick()
                    amountInputTextField.performTextReplacement(tokenAmount)
                }
            }
            step("Click on 'Next' button") {
                onSendAddressScreen { nextButton.clickWithAssertion() }
            }
            step("Type recipient address") {
                onSendAddressScreen { addressTextField.performTextReplacement(ETHEREUM_RECIPIENT_ADDRESS) }
            }
            step("Click on 'Next' button") {
                onSendScreen { nextButton.clickWithAssertion() }
            }
            step("Assert fee block is displayed with fee selector") {
                checkNetworkFeeBlock(currentFeeAmount = feeAmount, withFeeSelector = true)
            }
            step("Click on fee selector icon") {
                onSendConfirmScreen { feeSelectorIcon.performClick() }
            }
            step("Assert 'Choose speed' bottom sheet title is displayed") {
                onSendSelectNetworkFeeBottomSheet { chooseSpeedTitle.assertIsDisplayed() }
            }
            step("Assert '$fastSelectorItem' selector item is displayed") {
                onSendSelectNetworkFeeBottomSheet { regularFeeSelectorItem(fastSelectorItem).assertIsDisplayed() }
            }
            step("Assert '$slowSelectorItem' selector item is displayed") {
                onSendSelectNetworkFeeBottomSheet { regularFeeSelectorItem(slowSelectorItem).assertIsDisplayed() }
            }
            step("Assert '$marketSelectorItem' selector item is displayed") {
                onSendSelectNetworkFeeBottomSheet { regularFeeSelectorItem(marketSelectorItem).assertIsDisplayed() }
            }
        }
    }
}