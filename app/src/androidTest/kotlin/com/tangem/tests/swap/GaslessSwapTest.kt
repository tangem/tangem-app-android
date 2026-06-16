package com.tangem.tests.swap

import com.tangem.common.BaseTestCase
import com.tangem.common.R as CommonR
import com.tangem.common.constants.TestConstants.QUOTES_API_SCENARIO
import com.tangem.common.constants.TestConstants.SVS_SEED_PHRASE_12
import com.tangem.common.constants.TestConstants.USER_TOKENS_API_SCENARIO
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_LONG
import com.tangem.common.extensions.extractText
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

/**
 * Gasless swap on a CEX route, paying the swap network fee with a stablecoin. Covers the best-rate
 * block / unchanged rate (5117), the fee-token selector bottom sheet (5111), the network-fee
 * selection for a USDC -> POL swap (5110), the signed swap reaching the provider (5116, hot wallet),
 * the insufficient-stablecoin-balance-for-fee error (5118), the stablecoin fee shown with its
 * fiat equivalent (5112), switching the fee token between the coin and the stablecoin (5114), and the
 * fee-selection options when paying with a token (5115), and the max amount reserving the fee (5113).
 * Validation cases run on the default (cold) wallet without signing.
 */
@HiltAndroidTest
class GaslessSwapTest : BaseTestCase() {

    private val tokenName = "USDC"
    private val currencySymbol = "USDC"
    private val swapTokenName = "Ethereum"
    private val nativeTokenName = "Polygon"
    private val inputAmount = "50"
    private val userTokensState = "PolygonUSDCEthereum"
    private val quotesState = "PolygonUSDC"
    private val assetsScenarioName = "express_api_assets"
    private val assetsExchangeEnabledState = "BitcoinExchangeEnabled"

    @AllureId("5117")
    @DisplayName("Gasless Swap: CEX best rate stays the same when the fee is paid with a stablecoin")
    @Test
    fun checkBestRateUnchangedWithStablecoinFeeTest() {
        var capturedReceiveAmount: String? = null

        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(USER_TOKENS_API_SCENARIO)
                resetWireMockScenarioState(QUOTES_API_SCENARIO)
                resetWireMockScenarioState(assetsScenarioName)
            }
        ).run {
            step("Set WireMock scenario '$USER_TOKENS_API_SCENARIO' to '$userTokensState'") {
                setWireMockScenarioState(scenarioName = USER_TOKENS_API_SCENARIO, state = userTokensState)
            }
            step("Set WireMock scenario '$QUOTES_API_SCENARIO' to '$quotesState'") {
                setWireMockScenarioState(scenarioName = QUOTES_API_SCENARIO, state = quotesState)
            }
            step("Set WireMock scenario '$assetsScenarioName' to '$assetsExchangeEnabledState'") {
                setWireMockScenarioState(scenarioName = assetsScenarioName, state = assetsExchangeEnabledState)
            }

            step("Open the swap amount screen for '$tokenName' -> '$swapTokenName'") {
                openSwapAmountScreen(fromTokenName = tokenName, receiveTokenName = swapTokenName, amount = inputAmount)
            }
            step("Assert 'Best rate' label is displayed") {
                onSwapTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { bestRateText.assertIsDisplayed() }
                }
            }
            step("Capture the received amount (exchange rate) before changing the fee token") {
                onSwapTokenScreen { capturedReceiveAmount = receiveAmount.extractText() }
            }
            step("Pay the network fee with the stablecoin '$tokenName'") {
                switchFeeTokenAndApply(currentFeeToken = nativeTokenName, newFeeToken = tokenName)
            }
            step("Assert the received amount (rate) is unchanged after selecting the stablecoin fee") {
                val expected = requireNotNull(capturedReceiveAmount) { "Receive amount was not captured" }
                onSwapTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { receiveAmount.assertTextEquals(expected) }
                }
            }
        }
    }

    @AllureId("5111")
    @DisplayName("Gasless Swap: fee-token selector — coin has a speed choice, stablecoin only Market, token is selectable")
    @Test
    fun checkFeeSelectorBottomSheetTest() {
        val marketSpeed = getResourceString(R.string.common_fee_selector_option_market)
        val fastSpeed = getResourceString(R.string.common_fee_selector_option_fast)
        val slowSpeed = getResourceString(R.string.common_fee_selector_option_slow)

        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(USER_TOKENS_API_SCENARIO)
                resetWireMockScenarioState(QUOTES_API_SCENARIO)
                resetWireMockScenarioState(assetsScenarioName)
            }
        ).run {
            step("Set WireMock scenario '$USER_TOKENS_API_SCENARIO' to '$userTokensState'") {
                setWireMockScenarioState(scenarioName = USER_TOKENS_API_SCENARIO, state = userTokensState)
            }
            step("Set WireMock scenario '$QUOTES_API_SCENARIO' to '$quotesState'") {
                setWireMockScenarioState(scenarioName = QUOTES_API_SCENARIO, state = quotesState)
            }
            step("Set WireMock scenario '$assetsScenarioName' to '$assetsExchangeEnabledState'") {
                setWireMockScenarioState(scenarioName = assetsScenarioName, state = assetsExchangeEnabledState)
            }

            step("Open the swap amount screen for '$tokenName' -> '$swapTokenName'") {
                openSwapAmountScreen(fromTokenName = tokenName, receiveTokenName = swapTokenName, amount = inputAmount)
            }
            step("Open the 'Network fee' bottom sheet") {
                openSwapNetworkFeeSelector()
            }
            step("Assert the '$nativeTokenName' fee coin is displayed") {
                onSendFeeSelectorBottomSheet { feeTokenItem(nativeTokenName).assertIsDisplayed() }
            }
            step("Open 'Choose speed' for the '$nativeTokenName' fee") {
                onSendFeeSelectorBottomSheet { feeSpeedItemTitle(marketSpeed).performClick() }
            }
            step("Assert 'Choose speed' offers multiple speeds ('$marketSpeed', '$fastSpeed') for the coin") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onSendSelectNetworkFeeBottomSheet {
                        chooseSpeedTitle.assertIsDisplayed()
                        regularFeeSelectorItem(marketSpeed).assertIsDisplayed()
                        regularFeeSelectorItem(fastSpeed).assertIsDisplayed()
                    }
                }
            }
            step("Select '$marketSpeed' speed") {
                onSendSelectNetworkFeeBottomSheet { regularFeeSelectorItem(marketSpeed).performClick() }
            }
            step("Click on '$nativeTokenName' fee token to open 'Choose token'") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onSendFeeSelectorBottomSheet { feeTokenItem(nativeTokenName).performClick() }
                }
            }
            step("Assert 'Choose token' is displayed and '$tokenName' is available for the fee") {
                onSendFeeSelectorBottomSheet {
                    chooseTokenTitle.assertIsDisplayed()
                    feeTokenItem(tokenName).assertIsDisplayed()
                }
            }
            step("Select '$tokenName' as the fee-paying token") {
                onSendFeeSelectorBottomSheet { feeTokenItem(tokenName).performClick() }
            }
            step("Assert only '$marketSpeed' speed is available for the stablecoin fee") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onSendFeeSelectorBottomSheet {
                        feeSpeedItemTitle(marketSpeed).assertIsDisplayed()
                        feeSpeedItemTitle(fastSpeed).assertIsNotDisplayed()
                        feeSpeedItemTitle(slowSpeed).assertIsNotDisplayed()
                    }
                }
            }
            step("Click on '$marketSpeed' fee row") {
                onSendFeeSelectorBottomSheet { feeSpeedItemTitle(marketSpeed).performClick() }
            }
            step("Assert 'Choose speed' bottom sheet did not open for the stablecoin fee") {
                onSendSelectNetworkFeeBottomSheet { chooseSpeedTitle.assertIsNotDisplayed() }
            }
            step("Click on 'Apply' button") {
                onSendFeeSelectorBottomSheet { applyButton.performClick() }
            }
            step("Assert the network fee is shown in '$currencySymbol' on the swap screen") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onSwapTokenScreen { feeBlockCurrency(currencySymbol).assertIsDisplayed() }
                }
            }
        }
    }

    @AllureId("5110")
    @DisplayName("Gasless Swap: network fee with fee-token selection is shown for a USDC -> POL swap")
    @Test
    fun checkNetworkFeeSelectionForSwapTest() {
        val receiveTokenName = "Polygon"

        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(USER_TOKENS_API_SCENARIO)
                resetWireMockScenarioState(QUOTES_API_SCENARIO)
                resetWireMockScenarioState(assetsScenarioName)
            }
        ).run {
            step("Set WireMock scenario '$USER_TOKENS_API_SCENARIO' to '$userTokensState'") {
                setWireMockScenarioState(scenarioName = USER_TOKENS_API_SCENARIO, state = userTokensState)
            }
            step("Set WireMock scenario '$QUOTES_API_SCENARIO' to '$quotesState'") {
                setWireMockScenarioState(scenarioName = QUOTES_API_SCENARIO, state = quotesState)
            }
            step("Set WireMock scenario '$assetsScenarioName' to '$assetsExchangeEnabledState'") {
                setWireMockScenarioState(scenarioName = assetsScenarioName, state = assetsExchangeEnabledState)
            }

            step("Open the swap amount screen for '$tokenName' -> '$receiveTokenName'") {
                openSwapAmountScreen(fromTokenName = tokenName, receiveTokenName = receiveTokenName, amount = inputAmount)
            }
            step("Assert 'Network fee' block with token selection is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onSwapTokenScreen {
                        networkFeeBlock.assertIsDisplayed()
                        selectFeeIcon.assertIsDisplayed()
                    }
                }
            }
            step("Open the 'Network fee' bottom sheet") {
                openSwapNetworkFeeSelector()
            }
            step("Click on '$nativeTokenName' fee token to open 'Choose token'") {
                onSendFeeSelectorBottomSheet { feeTokenItem(nativeTokenName).performClick() }
            }
            step("Assert 'Choose token' is displayed and '$tokenName' is available for the fee") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onSendFeeSelectorBottomSheet {
                        chooseTokenTitle.assertIsDisplayed()
                        feeTokenItem(tokenName).assertIsDisplayed()
                    }
                }
            }
        }
    }

    @AllureId("5116")
    @DisplayName("Gasless Swap: sign a swap paying the fee with the stablecoin and reach the provider")
    @Test
    fun checkSignSwapWithStablecoinFeeTest() {
        val hotWalletTokensState = "PolygonUSDCHotWallet"
        val receiveTokenName = "Polygon"
        val providerName = "Changelly"
        val exchangeStatusScenario = "exchange_status_provider"
        val changellyStatusState = "Changelly"
        val expressStatusItemTitle = getResourceString(CommonR.string.express_exchange_by, providerName)

        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(USER_TOKENS_API_SCENARIO)
                resetWireMockScenarioState(QUOTES_API_SCENARIO)
                resetWireMockScenarioState(assetsScenarioName)
                resetWireMockScenarioState(exchangeStatusScenario)
            }
        ).run {
            step("Set WireMock scenario '$USER_TOKENS_API_SCENARIO' to '$hotWalletTokensState'") {
                setWireMockScenarioState(scenarioName = USER_TOKENS_API_SCENARIO, state = hotWalletTokensState)
            }
            step("Set WireMock scenario '$QUOTES_API_SCENARIO' to '$quotesState'") {
                setWireMockScenarioState(scenarioName = QUOTES_API_SCENARIO, state = quotesState)
            }
            step("Set WireMock scenario '$assetsScenarioName' to '$assetsExchangeEnabledState'") {
                setWireMockScenarioState(scenarioName = assetsScenarioName, state = assetsExchangeEnabledState)
            }
            step("Set WireMock scenario '$exchangeStatusScenario' to '$changellyStatusState'") {
                setWireMockScenarioState(scenarioName = exchangeStatusScenario, state = changellyStatusState)
            }

            step("Open the swap amount screen for '$tokenName' -> '$receiveTokenName' on an existing hot wallet") {
                openSwapAmountScreen(
                    fromTokenName = tokenName,
                    receiveTokenName = receiveTokenName,
                    amount = inputAmount,
                    seedPhrase = SVS_SEED_PHRASE_12,
                )
            }
            step("Pay the network fee with the stablecoin '$tokenName'") {
                switchFeeTokenAndApply(currentFeeToken = nativeTokenName, newFeeToken = tokenName)
            }
            step("Assert the network fee is paid in '$currencySymbol'") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onSwapTokenScreen {
                        feeBlockCurrency(currencySymbol).assertIsDisplayed()
                    }
                }
            }
            step("Confirm the swap by holding the 'Swap' button and sign") {
                confirmSwapByHolding()
            }
            step("Assert the 'Swap in progress' screen is displayed (transaction sent to provider)") {
                onSwapSuccessScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { title.assertIsDisplayed() }
                }
            }
            step("Click on 'Close' button") {
                onSwapSuccessScreen { closeButton.performClick() }
            }
            step("Assert 'Express status' item with title '$expressStatusItemTitle' is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onTokenDetailsScreen { expressStatusItem(expressStatusItemTitle).assertIsDisplayed() }
                }
            }
        }
    }

    @AllureId("5118")
    @DisplayName("Gasless Swap: insufficient stablecoin balance to cover the fee shows an error and blocks the swap")
    @Test
    fun checkInsufficientBalanceForFeeTest() {
        val usdcBalanceScenario = "polygon_usdc_balance"
        val lowBalanceState = "LowBalance"
        val lowAmount = "0.0005"

        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(USER_TOKENS_API_SCENARIO)
                resetWireMockScenarioState(QUOTES_API_SCENARIO)
                resetWireMockScenarioState(assetsScenarioName)
                resetWireMockScenarioState(usdcBalanceScenario)
            }
        ).run {
            step("Set WireMock scenario '$usdcBalanceScenario' to '$lowBalanceState'") {
                setWireMockScenarioState(scenarioName = usdcBalanceScenario, state = lowBalanceState)
            }
            step("Set WireMock scenario '$USER_TOKENS_API_SCENARIO' to '$userTokensState'") {
                setWireMockScenarioState(scenarioName = USER_TOKENS_API_SCENARIO, state = userTokensState)
            }
            step("Set WireMock scenario '$QUOTES_API_SCENARIO' to '$quotesState'") {
                setWireMockScenarioState(scenarioName = QUOTES_API_SCENARIO, state = quotesState)
            }
            step("Set WireMock scenario '$assetsScenarioName' to '$assetsExchangeEnabledState'") {
                setWireMockScenarioState(scenarioName = assetsScenarioName, state = assetsExchangeEnabledState)
            }
            step("Open the swap amount screen for '$tokenName' -> '$swapTokenName'") {
                openSwapAmountScreen(fromTokenName = tokenName, receiveTokenName = swapTokenName, amount = lowAmount)
            }
            step("Open the 'Network fee' bottom sheet") {
                openSwapNetworkFeeSelector()
            }
            step("Click on '$nativeTokenName' fee token to open 'Choose token'") {
                onSendFeeSelectorBottomSheet { feeTokenItem(nativeTokenName).performClick() }
            }
            step("Select '$tokenName' as the fee-paying token") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onSendFeeSelectorBottomSheet { feeTokenItem(tokenName).performClick() }
                }
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

    @AllureId("5112")
    @DisplayName("Gasless Swap: the stablecoin network fee is shown with its fiat (dollar) equivalent")
    @Test
    fun checkStablecoinFeeShownWithFiatTest() {
        val fiatSign = "$"

        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(USER_TOKENS_API_SCENARIO)
                resetWireMockScenarioState(QUOTES_API_SCENARIO)
                resetWireMockScenarioState(assetsScenarioName)
            }
        ).run {
            step("Set WireMock scenario '$USER_TOKENS_API_SCENARIO' to '$userTokensState'") {
                setWireMockScenarioState(scenarioName = USER_TOKENS_API_SCENARIO, state = userTokensState)
            }
            step("Set WireMock scenario '$QUOTES_API_SCENARIO' to '$quotesState'") {
                setWireMockScenarioState(scenarioName = QUOTES_API_SCENARIO, state = quotesState)
            }
            step("Set WireMock scenario '$assetsScenarioName' to '$assetsExchangeEnabledState'") {
                setWireMockScenarioState(scenarioName = assetsScenarioName, state = assetsExchangeEnabledState)
            }
            step("Open the swap amount screen for '$tokenName' -> '$swapTokenName'") {
                openSwapAmountScreen(fromTokenName = tokenName, receiveTokenName = swapTokenName, amount = inputAmount)
            }
            step("Pay the network fee with the stablecoin '$tokenName'") {
                switchFeeTokenAndApply(currentFeeToken = nativeTokenName, newFeeToken = tokenName)
            }
            step("Assert the network fee is shown in '$currencySymbol' with its fiat ('$fiatSign') equivalent") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onSwapTokenScreen {
                        feeBlockCurrency(currencySymbol).assertIsDisplayed()
                        feeAmount.assertTextContains(fiatSign, substring = true)
                    }
                }
            }
        }
    }

    @AllureId("5114")
    @DisplayName("Gasless Swap: switching the fee token from the coin to the stablecoin and back updates the summary")
    @Test
    fun checkSwitchFeeTokenBetweenCoinAndStablecoinTest() {
        val nativeSymbol = "POL"

        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(USER_TOKENS_API_SCENARIO)
                resetWireMockScenarioState(QUOTES_API_SCENARIO)
                resetWireMockScenarioState(assetsScenarioName)
            }
        ).run {
            step("Set WireMock scenario '$USER_TOKENS_API_SCENARIO' to '$userTokensState'") {
                setWireMockScenarioState(scenarioName = USER_TOKENS_API_SCENARIO, state = userTokensState)
            }
            step("Set WireMock scenario '$QUOTES_API_SCENARIO' to '$quotesState'") {
                setWireMockScenarioState(scenarioName = QUOTES_API_SCENARIO, state = quotesState)
            }
            step("Set WireMock scenario '$assetsScenarioName' to '$assetsExchangeEnabledState'") {
                setWireMockScenarioState(scenarioName = assetsScenarioName, state = assetsExchangeEnabledState)
            }
            step("Open the swap amount screen for '$tokenName' -> '$swapTokenName'") {
                openSwapAmountScreen(fromTokenName = tokenName, receiveTokenName = swapTokenName, amount = inputAmount)
            }
            step("Switch the fee token from the coin '$nativeTokenName' to the stablecoin '$tokenName'") {
                switchFeeTokenAndApply(currentFeeToken = nativeTokenName, newFeeToken = tokenName)
            }
            step("Assert the network fee is now paid in '$currencySymbol' on the summary") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onSwapTokenScreen { feeBlockCurrency(currencySymbol).assertIsDisplayed() }
                }
            }
            step("Switch the fee token from the stablecoin '$tokenName' back to the coin '$nativeTokenName'") {
                switchFeeTokenAndApply(currentFeeToken = tokenName, newFeeToken = nativeTokenName)
            }
            step("Assert the network fee is back to the coin ('$nativeSymbol') on the summary") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onSwapTokenScreen { feeBlockCurrency(nativeSymbol).assertIsDisplayed() }
                }
            }
        }
    }

    @AllureId("5115")
    @DisplayName("Gasless Swap: fee selector offers token selection and no speed choice when paying with a token")
    @Test
    fun checkFeeSelectionOptionsTest() {
        val marketSpeed = getResourceString(R.string.common_fee_selector_option_market)
        val fastSpeed = getResourceString(R.string.common_fee_selector_option_fast)
        val slowSpeed = getResourceString(R.string.common_fee_selector_option_slow)

        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(USER_TOKENS_API_SCENARIO)
                resetWireMockScenarioState(QUOTES_API_SCENARIO)
                resetWireMockScenarioState(assetsScenarioName)
            }
        ).run {
            step("Set WireMock scenario '$USER_TOKENS_API_SCENARIO' to '$userTokensState'") {
                setWireMockScenarioState(scenarioName = USER_TOKENS_API_SCENARIO, state = userTokensState)
            }
            step("Set WireMock scenario '$QUOTES_API_SCENARIO' to '$quotesState'") {
                setWireMockScenarioState(scenarioName = QUOTES_API_SCENARIO, state = quotesState)
            }
            step("Set WireMock scenario '$assetsScenarioName' to '$assetsExchangeEnabledState'") {
                setWireMockScenarioState(scenarioName = assetsScenarioName, state = assetsExchangeEnabledState)
            }

            step("Open the swap amount screen for '$tokenName' -> '$swapTokenName'") {
                openSwapAmountScreen(fromTokenName = tokenName, receiveTokenName = swapTokenName, amount = inputAmount)
            }
            step("Open the 'Network fee' bottom sheet") {
                openSwapNetworkFeeSelector()
            }
            step("Click on '$nativeTokenName' fee token to open 'Choose token'") {
                onSendFeeSelectorBottomSheet { feeTokenItem(nativeTokenName).performClick() }
            }
            step("Assert 'Choose token' is displayed and '$tokenName' is available for the fee") {
                onSendFeeSelectorBottomSheet {
                    chooseTokenTitle.assertIsDisplayed()
                    feeTokenItem(tokenName).assertIsDisplayed()
                }
            }
            step("Select '$tokenName' as the fee-paying token") {
                onSendFeeSelectorBottomSheet { feeTokenItem(tokenName).performClick() }
            }
            step("Assert only '$marketSpeed' speed is available for the token fee") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onSendFeeSelectorBottomSheet {
                        feeSpeedItemTitle(marketSpeed).assertIsDisplayed()
                        feeSpeedItemTitle(fastSpeed).assertIsNotDisplayed()
                        feeSpeedItemTitle(slowSpeed).assertIsNotDisplayed()
                    }
                }
            }
            step("Click on '$marketSpeed' fee row") {
                onSendFeeSelectorBottomSheet { feeSpeedItemTitle(marketSpeed).performClick() }
            }
            step("Assert 'Choose speed' bottom sheet did not open when paying with a token") {
                onSendSelectNetworkFeeBottomSheet { chooseSpeedTitle.assertIsNotDisplayed() }
            }
            step("Click on 'Apply' button") {
                onSendFeeSelectorBottomSheet { applyButton.performClick() }
            }
            step("Assert the network fee is shown in '$currencySymbol' on the summary") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onSwapTokenScreen { feeBlockCurrency(currencySymbol).assertIsDisplayed() }
                }
            }
        }
    }

    @AllureId("5113")
    @DisplayName("Gasless Swap: the max amount reserves the stablecoin fee and stays valid without an insufficient error")
    @Test
    fun checkMaxAmountReservesFeeTest() {
        val maxInputAmount = "100"

        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(USER_TOKENS_API_SCENARIO)
                resetWireMockScenarioState(QUOTES_API_SCENARIO)
                resetWireMockScenarioState(assetsScenarioName)
            }
        ).run {
            step("Set WireMock scenario '$USER_TOKENS_API_SCENARIO' to '$userTokensState'") {
                setWireMockScenarioState(scenarioName = USER_TOKENS_API_SCENARIO, state = userTokensState)
            }
            step("Set WireMock scenario '$QUOTES_API_SCENARIO' to '$quotesState'") {
                setWireMockScenarioState(scenarioName = QUOTES_API_SCENARIO, state = quotesState)
            }
            step("Set WireMock scenario '$assetsScenarioName' to '$assetsExchangeEnabledState'") {
                setWireMockScenarioState(scenarioName = assetsScenarioName, state = assetsExchangeEnabledState)
            }

            step("Open the swap amount screen for '$tokenName' -> '$swapTokenName' with the max amount '$maxInputAmount'") {
                openSwapAmountScreen(fromTokenName = tokenName, receiveTokenName = swapTokenName, amount = maxInputAmount)
            }
            step("Pay the network fee with the stablecoin '$tokenName'") {
                switchFeeTokenAndApply(currentFeeToken = nativeTokenName, newFeeToken = tokenName)
            }
            step("Assert no insufficient-balance error and 'Swap' is enabled (the fee is reserved from the max amount)") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onSwapTokenScreen {
                        insufficientFundsErrorTitle.assertIsNotDisplayed()
                        swapButton.assertIsEnabled()
                    }
                }
            }
        }
    }
}