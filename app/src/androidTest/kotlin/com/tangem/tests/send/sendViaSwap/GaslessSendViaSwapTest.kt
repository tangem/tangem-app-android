package com.tangem.tests.send.sendViaSwap

import com.tangem.common.BaseTestCase
import com.tangem.common.R
import com.tangem.common.constants.TestConstants.BITCOIN_RECIPIENT_ADDRESS
import com.tangem.common.constants.TestConstants.PROVIDERS_API_SCENARIO
import com.tangem.common.constants.TestConstants.QUOTES_API_SCENARIO
import com.tangem.common.constants.TestConstants.SVS_SEED_PHRASE_12
import com.tangem.common.constants.TestConstants.USER_TOKENS_API_SCENARIO
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_LONG
import com.tangem.common.utils.resetWireMockScenarioState
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.scenarios.*
import com.tangem.screens.*
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.kakaocup.kakao.common.utilities.getResourceString
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Test

/**
 * Gasless send-via-swap: paying the network fee with the stablecoin while converting it through an
 * express swap. Covers the fee-token selection on the swap summary, the stablecoin balance validation
 * against the gasless fee, and the full signed swap-and-send on a hot wallet.
 */
@HiltAndroidTest
class GaslessSendViaSwapTest : BaseTestCase() {

    private val tokenName = "USDC"
    private val currencySymbol = "USDC"
    private val nativeTokenName = "Polygon"
    private val swapTokenName = "Bitcoin"
    private val mainNetwork = "MAIN"
    private val providerName = "Changelly"
    private val tokenAmount = "1"
    private val hotWalletTokensState = "PolygonUSDCHotWallet"
    private val quotesState = "PolygonUSDC"
    private val assetsScenarioName = "express_api_assets"
    private val assetsExchangeEnabledState = "BitcoinExchangeEnabled"
    private val providersState = "HotWalletSvS"

    @AllureId("5120")
    @DisplayName("Gasless Send via Swap: the network fee is selectable and payable with the stablecoin")
    @Test
    fun checkFeeTokenSelectionForSwapTest() {
        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(USER_TOKENS_API_SCENARIO)
                resetWireMockScenarioState(QUOTES_API_SCENARIO)
                resetWireMockScenarioState(assetsScenarioName)
                resetWireMockScenarioState(PROVIDERS_API_SCENARIO)
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
            step("Set WireMock scenario '$PROVIDERS_API_SCENARIO' to '$providersState'") {
                setWireMockScenarioState(scenarioName = PROVIDERS_API_SCENARIO, state = providersState)
            }

            step("Open the send-via-swap flow for '$tokenName' on an existing hot wallet") {
                openSendViaSwapScreenWithHotWallet(
                    seedPhrase = SVS_SEED_PHRASE_12,
                    tokenName = tokenName,
                    swapTokenName = swapTokenName,
                    networkName = swapTokenName,
                    networkType = mainNetwork,
                )
            }
            step("Enter amount '$tokenAmount' and open the 'Send confirm' screen") {
                enterSwapAmountAndOpenSendConfirm(amount = tokenAmount, recipientAddress = BITCOIN_RECIPIENT_ADDRESS)
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
            step("Click on '$nativeTokenName' fee token to open 'Choose token'") {
                onSendFeeSelectorBottomSheet { feeTokenItem(nativeTokenName).performClick() }
            }
            step("Assert 'Choose token' bottom sheet is displayed") {
                onSendFeeSelectorBottomSheet { chooseTokenTitle.assertIsDisplayed() }
            }
            step("Assert '$tokenName' is available for the fee payment") {
                onSendFeeSelectorBottomSheet { feeTokenItem(tokenName).assertIsDisplayed() }
            }
            step("Select '$tokenName' as the fee-paying token") {
                onSendFeeSelectorBottomSheet { feeTokenItem(tokenName).performClick() }
            }
            step("Click on 'Apply' button") {
                onSendFeeSelectorBottomSheet { applyButton.performClick() }
            }
            step("Assert the network fee is calculated in '$currencySymbol' on the summary") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onSendConfirmScreen { feeBlockCurrency(currencySymbol).assertIsDisplayed() }
                }
            }
        }
    }

    @AllureId("5121")
    @DisplayName("Gasless Send via Swap: insufficient stablecoin balance to cover the fee blocks the swap")
    @Test
    fun checkBalanceValidationForFeeTest() {
        val usdcBalanceScenario = "polygon_usdc_balance"
        val lowBalanceState = "LowBalance"

        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(USER_TOKENS_API_SCENARIO)
                resetWireMockScenarioState(QUOTES_API_SCENARIO)
                resetWireMockScenarioState(assetsScenarioName)
                resetWireMockScenarioState(PROVIDERS_API_SCENARIO)
                resetWireMockScenarioState(usdcBalanceScenario)
            }
        ).run {
            step("Set WireMock scenario '$usdcBalanceScenario' to '$lowBalanceState'") {
                setWireMockScenarioState(scenarioName = usdcBalanceScenario, state = lowBalanceState)
            }
            step("Set WireMock scenario '$USER_TOKENS_API_SCENARIO' to '$hotWalletTokensState'") {
                setWireMockScenarioState(scenarioName = USER_TOKENS_API_SCENARIO, state = hotWalletTokensState)
            }
            step("Set WireMock scenario '$QUOTES_API_SCENARIO' to '$quotesState'") {
                setWireMockScenarioState(scenarioName = QUOTES_API_SCENARIO, state = quotesState)
            }
            step("Set WireMock scenario '$assetsScenarioName' to '$assetsExchangeEnabledState'") {
                setWireMockScenarioState(scenarioName = assetsScenarioName, state = assetsExchangeEnabledState)
            }
            step("Set WireMock scenario '$PROVIDERS_API_SCENARIO' to '$providersState'") {
                setWireMockScenarioState(scenarioName = PROVIDERS_API_SCENARIO, state = providersState)
            }

            step("Open the send-via-swap flow for '$tokenName' on an existing hot wallet") {
                openSendViaSwapScreenWithHotWallet(
                    seedPhrase = SVS_SEED_PHRASE_12,
                    tokenName = tokenName,
                    swapTokenName = swapTokenName,
                    networkName = swapTokenName,
                    networkType = mainNetwork,
                )
            }
            step("Enter amount '$tokenAmount' and open the 'Send confirm' screen") {
                enterSwapAmountAndOpenSendConfirm(amount = tokenAmount, recipientAddress = BITCOIN_RECIPIENT_ADDRESS)
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

    @AllureId("5122")
    @DisplayName("Gasless Send via Swap: sign and send a swap paying the fee with the stablecoin")
    @Test
    fun checkSendViaSwapFinalScreenAndSendTest() {
        val exchangeStatusScenario = "exchange_status_provider"
        val changellyStatusState = "Changelly"
        val expressStatusItemTitle = getResourceString(R.string.express_exchange_by, providerName)

        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(USER_TOKENS_API_SCENARIO)
                resetWireMockScenarioState(QUOTES_API_SCENARIO)
                resetWireMockScenarioState(assetsScenarioName)
                resetWireMockScenarioState(PROVIDERS_API_SCENARIO)
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
            step("Set WireMock scenario '$PROVIDERS_API_SCENARIO' to '$providersState'") {
                setWireMockScenarioState(scenarioName = PROVIDERS_API_SCENARIO, state = providersState)
            }
            step("Set WireMock scenario '$exchangeStatusScenario' to '$changellyStatusState'") {
                setWireMockScenarioState(scenarioName = exchangeStatusScenario, state = changellyStatusState)
            }

            step("Open the send-via-swap flow for '$tokenName' on an existing hot wallet") {
                openSendViaSwapScreenWithHotWallet(
                    seedPhrase = SVS_SEED_PHRASE_12,
                    tokenName = tokenName,
                    swapTokenName = swapTokenName,
                    networkName = swapTokenName,
                    networkType = mainNetwork,
                )
            }
            step("Enter amount '$tokenAmount' and open the 'Send confirm' screen") {
                enterSwapAmountAndOpenSendConfirm(amount = tokenAmount, recipientAddress = BITCOIN_RECIPIENT_ADDRESS)
            }
            step("Pay the network fee with '$tokenName' via the fee selector") {
                selectStablecoinAsFeeToken(coinName = nativeTokenName, tokenName = tokenName)
            }
            step("Click on 'Apply' button") {
                onSendFeeSelectorBottomSheet { applyButton.performClick() }
            }
            step("Assert the sent '$tokenName' amount is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onSendConfirmScreen { primaryAmount.assertIsDisplayed() }
                }
            }
            step("Assert the recipient address is displayed") {
                onSendConfirmScreen { recipientAddress(BITCOIN_RECIPIENT_ADDRESS).assertIsDisplayed() }
            }
            step("Assert the amount to receive after the swap is displayed") {
                onSendConfirmScreen { secondaryAmount.assertIsDisplayed() }
            }
            step("Assert the network fee is paid in '$currencySymbol'") {
                onSendConfirmScreen { feeBlockCurrency(currencySymbol).assertIsDisplayed() }
            }
            step("Sign, send and open the 'Transaction sent' screen") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    openSendSuccessScreenViaLongClickOnSendButton()
                }
            }
            step("Check 'Send via swap' success screen") {
                checkSendViaSwapSuccessScreen()
            }
            step("Click on 'Close' button") {
                onSendSuccessScreen { closeButton.performClick() }
            }
            step("Assert 'Express status' item with title '$expressStatusItemTitle' is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onTokenDetailsScreen { expressStatusItem(expressStatusItemTitle).assertIsDisplayed() }
                }
            }
        }
    }
}