package com.tangem.tests.send.gasless

import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.ETHEREUM_RECIPIENT_ADDRESS
import com.tangem.common.constants.TestConstants.QUOTES_API_SCENARIO
import com.tangem.common.constants.TestConstants.USER_TOKENS_API_SCENARIO
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_LONG
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.utils.resetWireMockScenarioState
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.core.ui.R
import com.tangem.scenarios.enterAmountAndOpenSendConfirm
import com.tangem.scenarios.enterRecipientAndOpenSendConfirm
import com.tangem.scenarios.openSendScreen
import com.tangem.scenarios.selectStablecoinAsFeeToken
import com.tangem.screens.onSendConfirmScreen
import com.tangem.screens.onSendFeeSelectorBottomSheet
import com.tangem.screens.onSendScreen
import com.tangem.screens.onSendSelectNetworkFeeBottomSheet
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.kakaocup.kakao.common.utilities.getResourceString
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Test

/**
 * Gasless network-fee behaviour on the send summary (fee selector): availability, calculation,
 * speed options, switching the fee token, and balance-driven notifications. All run on the default
 * (cold) wallet without signing a transaction.
 */
@HiltAndroidTest
class GaslessFeeTest : BaseTestCase() {

    private val scenarioState = "PolygonUSDC"
    private val tokenName = "USDC"
    private val nativeTokenName = "Polygon"
    private val tokenAmount = "1"

    @AllureId("5061")
    @DisplayName("Gasless: Network fee on summary is selectable and the stablecoin is available for the fee")
    @Test
    fun checkNetworkFeeTokenSelectionAvailableTest() {
        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(USER_TOKENS_API_SCENARIO)
                resetWireMockScenarioState(QUOTES_API_SCENARIO)
            }
        ).run {
            step("Open 'Send' screen for '$tokenName'") {
                openSendScreen(tokenName = tokenName, mockState = scenarioState)
            }
            step("Enter amount '$tokenAmount' and open the 'Send confirm' screen") {
                enterAmountAndOpenSendConfirm(amount = tokenAmount, recipientAddress = ETHEREUM_RECIPIENT_ADDRESS)
            }
            step("Assert 'Network fee' block with token selection is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onSendConfirmScreen {
                        feeSelectorTitle.assertIsDisplayed()
                        selectFeeIcon.assertIsDisplayed()
                    }
                }
            }
            step("Click on 'Network fee' block") {
                onSendConfirmScreen { feeSelectorBlock.performClick() }
            }
            step("Assert 'Network fee' bottom sheet is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onSendFeeSelectorBottomSheet { networkFeeTitle.assertIsDisplayed() }
                }
            }
            step("Click on '$nativeTokenName' fee token to open 'Choose token'") {
                onSendFeeSelectorBottomSheet { feeTokenItem(nativeTokenName).performClick() }
            }
            step("Assert 'Choose token' bottom sheet is displayed") {
                onSendFeeSelectorBottomSheet { chooseTokenTitle.assertIsDisplayed() }
            }
            step("Assert '$tokenName' is available for the fee payment") {
                onSendFeeSelectorBottomSheet { feeTokenItem(tokenName).assertIsDisplayed() }
            }
        }
    }

    @AllureId("5062")
    @DisplayName("Gasless: network fee for a stablecoin is calculated and shown in the stablecoin")
    @Test
    fun checkFeeCalculatedInStablecoinTest() {
        val marketSpeed = getResourceString(R.string.common_fee_selector_option_market)

        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(USER_TOKENS_API_SCENARIO)
                resetWireMockScenarioState(QUOTES_API_SCENARIO)
            }
        ).run {
            step("Open 'Send' screen for '$tokenName'") {
                openSendScreen(tokenName = tokenName, mockState = scenarioState)
            }
            step("Enter amount '$tokenAmount' and open the 'Send confirm' screen") {
                enterAmountAndOpenSendConfirm(amount = tokenAmount, recipientAddress = ETHEREUM_RECIPIENT_ADDRESS)
            }
            step("Pay the network fee with '$tokenName' via the fee selector") {
                selectStablecoinAsFeeToken(coinName = nativeTokenName, tokenName = tokenName)
            }
            step("Assert the fee is shown under the '$marketSpeed' speed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onSendFeeSelectorBottomSheet { feeSpeedItemTitle(marketSpeed).assertIsDisplayed() }
                }
            }
            step("Click on 'Apply' button") {
                onSendFeeSelectorBottomSheet { applyButton.performClick() }
            }
            step("Assert the network fee is calculated in '$tokenName' (not in the coin) on the summary") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onSendConfirmScreen {
                        feeBlockCurrency(tokenName).assertIsDisplayed()
                        feeAmount.assertIsDisplayed()
                    }
                }
            }
        }
    }

    @AllureId("5064")
    @DisplayName("Gasless: only Market speed is available when paying the fee with a stablecoin")
    @Test
    fun checkOnlyMarketSpeedAvailableForStablecoinFeeTest() {
        val marketSpeed = getResourceString(R.string.common_fee_selector_option_market)
        val fastSpeed = getResourceString(R.string.common_fee_selector_option_fast)
        val slowSpeed = getResourceString(R.string.common_fee_selector_option_slow)

        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(USER_TOKENS_API_SCENARIO)
                resetWireMockScenarioState(QUOTES_API_SCENARIO)
            }
        ).run {
            step("Open 'Send' screen for '$tokenName'") {
                openSendScreen(tokenName = tokenName, mockState = scenarioState)
            }
            step("Enter amount '$tokenAmount' and open the 'Send confirm' screen") {
                enterAmountAndOpenSendConfirm(amount = tokenAmount, recipientAddress = ETHEREUM_RECIPIENT_ADDRESS)
            }
            step("Click on 'Network fee' block") {
                onSendConfirmScreen {
                    feeSelectorBlock.assertIsDisplayed()
                    feeSelectorBlock.performClick()
                }
            }
            step("Assert 'Network fee' bottom sheet is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onSendFeeSelectorBottomSheet { networkFeeTitle.assertIsDisplayed() }
                }
            }
            step("Click on '$nativeTokenName' fee token to open 'Choose token'") {
                onSendFeeSelectorBottomSheet { feeTokenItem(nativeTokenName).performClick() }
            }
            step("Assert 'Choose token' bottom sheet is displayed") {
                onSendFeeSelectorBottomSheet { chooseTokenTitle.assertIsDisplayed() }
            }
            step("Select '$tokenName' as the fee-paying token") {
                onSendFeeSelectorBottomSheet { feeTokenItem(tokenName).performClick() }
            }
            step("Assert 'Network fee' bottom sheet is displayed after token selection") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onSendFeeSelectorBottomSheet { networkFeeTitle.assertIsDisplayed() }
                }
            }
            step("Assert '$marketSpeed' speed is displayed") {
                onSendFeeSelectorBottomSheet { feeSpeedItemTitle(marketSpeed).assertIsDisplayed() }
            }
            step("Assert '$fastSpeed' speed is not displayed") {
                onSendFeeSelectorBottomSheet { feeSpeedItemTitle(fastSpeed).assertIsNotDisplayed() }
            }
            step("Assert '$slowSpeed' speed is not displayed") {
                onSendFeeSelectorBottomSheet { feeSpeedItemTitle(slowSpeed).assertIsNotDisplayed() }
            }
            step("Click on '$marketSpeed' fee row") {
                onSendFeeSelectorBottomSheet { feeSpeedItemTitle(marketSpeed).performClick() }
            }
            step("Assert 'Choose speed' bottom sheet did not open for stablecoin fee") {
                onSendSelectNetworkFeeBottomSheet { chooseSpeedTitle.assertIsNotDisplayed() }
            }
        }
    }

    @AllureId("5068")
    @DisplayName("Gasless: switching the fee token back to the coin restores the standard fee flow")
    @Test
    fun checkSwitchFeeTokenBackToCoinTest() {
        val nativeSymbol = "POL"
        val feeCoverageTitle = getResourceString(R.string.send_network_fee_warning_title)

        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(USER_TOKENS_API_SCENARIO)
                resetWireMockScenarioState(QUOTES_API_SCENARIO)
            }
        ).run {
            step("Open 'Send' screen for '$tokenName'") {
                openSendScreen(tokenName = tokenName, mockState = scenarioState)
            }
            step("Enter amount '$tokenAmount' and open the 'Send confirm' screen") {
                enterAmountAndOpenSendConfirm(amount = tokenAmount, recipientAddress = ETHEREUM_RECIPIENT_ADDRESS)
            }
            step("Pay the network fee with '$tokenName' via the fee selector") {
                selectStablecoinAsFeeToken(coinName = nativeTokenName, tokenName = tokenName)
            }
            step("Open the fee token selector again via the '$tokenName' fee token") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onSendFeeSelectorBottomSheet { feeTokenItem(tokenName).performClick() }
                }
            }
            step("Switch the fee token back to '$nativeTokenName'") {
                onSendFeeSelectorBottomSheet { feeTokenItem(nativeTokenName).performClick() }
            }
            step("Click on 'Apply' button") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onSendFeeSelectorBottomSheet { applyButton.performClick() }
                }
            }
            step("Assert the network fee is now paid in '$nativeSymbol' on the summary") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onSendConfirmScreen { feeBlockCurrency(nativeSymbol).assertIsDisplayed() }
                }
            }
            step("Assert 'Network fee coverage' notification is not displayed (standard fee flow)") {
                onSendConfirmScreen { warningTitle(feeCoverageTitle).assertIsNotDisplayed() }
            }
            step("Assert 'Send' button is enabled") {
                onSendConfirmScreen { sendButton.assertIsEnabled() }
            }
        }
    }

    @AllureId("5063")
    @DisplayName("Gasless: insufficient stablecoin balance to cover the fee shows error and blocks send")
    @Test
    fun checkInsufficientBalanceForFeeTest() {
        val usdcBalanceScenario = "polygon_usdc_balance"
        val lowBalanceState = "LowBalance"

        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(USER_TOKENS_API_SCENARIO)
                resetWireMockScenarioState(QUOTES_API_SCENARIO)
                resetWireMockScenarioState(usdcBalanceScenario)
            }
        ).run {
            step("Set WireMock scenario '$usdcBalanceScenario' to '$lowBalanceState'") {
                setWireMockScenarioState(scenarioName = usdcBalanceScenario, state = lowBalanceState)
            }
            step("Open 'Send' screen for '$tokenName'") {
                openSendScreen(tokenName = tokenName, mockState = scenarioState)
            }
            step("Click on 'Max' button") {
                onSendScreen { maxButton.performClick() }
            }
            step("Click on 'Next' button") {
                onSendScreen { nextButton.clickWithAssertion() }
            }
            step("Enter the recipient and open the 'Send confirm' screen") {
                enterRecipientAndOpenSendConfirm(ETHEREUM_RECIPIENT_ADDRESS)
            }
            step("Pay the network fee with '$tokenName' via the fee selector") {
                selectStablecoinAsFeeToken(coinName = nativeTokenName, tokenName = tokenName)
            }
            step("Assert 'Not enough funds' error is displayed in the fee selector") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onSendFeeSelectorBottomSheet { notEnoughFundsError.assertIsDisplayed() }
                }
            }
            step("Assert 'Apply' button is disabled (cannot pay the fee with insufficient balance)") {
                onSendFeeSelectorBottomSheet { applyButton.assertIsNotEnabled() }
            }
        }
    }

    @AllureId("5097")
    @DisplayName("Gasless: no insufficient-coin-for-fee notification is shown when gasless covers the fee")
    @Test
    fun checkNoInsufficientCoinNotificationWhenGaslessTest() {
        val coinBalanceScenario = "polygon_coin_balance"
        val zeroBalanceState = "ZeroBalance"
        val feeBlockedTitlePart = getResourceString(R.string.warning_send_blocked_funds_for_fee_title, "X")
            .substringAfter("X ")
            .trim()

        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(USER_TOKENS_API_SCENARIO)
                resetWireMockScenarioState(QUOTES_API_SCENARIO)
                resetWireMockScenarioState(coinBalanceScenario)
            }
        ).run {
            step("Set WireMock scenario '$coinBalanceScenario' to '$zeroBalanceState'") {
                setWireMockScenarioState(scenarioName = coinBalanceScenario, state = zeroBalanceState)
            }
            step("Open 'Send' screen for '$tokenName'") {
                openSendScreen(tokenName = tokenName, mockState = scenarioState)
            }
            step("Enter amount '$tokenAmount' and open the 'Send confirm' screen") {
                enterAmountAndOpenSendConfirm(amount = tokenAmount, recipientAddress = ETHEREUM_RECIPIENT_ADDRESS)
            }
            step("Assert the fee defaults to '$tokenName' (gasless covers the missing coin)") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onSendConfirmScreen { feeBlockCurrency(tokenName).assertIsDisplayed() }
                }
            }
            step("Assert the insufficient-coin-for-fee notification is not shown") {
                onSendConfirmScreen { warningTitleContaining(feeBlockedTitlePart).assertIsNotDisplayed() }
            }
            step("Assert 'Send' button is enabled") {
                onSendConfirmScreen { sendButton.assertIsEnabled() }
            }
        }
    }
}