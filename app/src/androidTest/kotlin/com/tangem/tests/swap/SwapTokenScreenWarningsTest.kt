package com.tangem.tests.swap

import com.tangem.common.BaseTestCase
import com.tangem.common.R
import com.tangem.common.annotations.ApiEnv
import com.tangem.common.annotations.ApiEnvConfig
import com.tangem.common.constants.TestConstants.QUOTES_API_SCENARIO
import com.tangem.common.constants.TestConstants.USER_TOKENS_API_SCENARIO
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_LONG
import com.tangem.common.extensions.*
import com.tangem.common.utils.resetWireMockScenarioState
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.datasource.api.common.config.ApiConfig
import com.tangem.datasource.api.common.config.ApiEnvironment
import com.tangem.scenarios.SwapEntryPoint
import com.tangem.scenarios.chackUnableToCoverFeeNotification
import com.tangem.scenarios.checkSwapWarning
import com.tangem.scenarios.openMainScreen
import com.tangem.scenarios.openSwapScreen
import com.tangem.scenarios.synchronizeAddresses
import com.tangem.screens.*
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.kakaocup.kakao.common.utilities.getResourceString
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Test

@HiltAndroidTest
class SwapTokenScreenWarningsTest : BaseTestCase() {

    @ApiEnv(
        ApiEnvConfig(ApiConfig.ID.Express, ApiEnvironment.PROD)
    )
    @AllureId("580")
    @DisplayName("Swap: check 'Insufficient funds' warning")
    @Test
    fun checkSwapInsufficientFundsWarningTest() {
        val tokenTitle = "Polygon"
        val inputAmount = "1000"

        setupHooks().run {

            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Click on token with name: '$tokenTitle'") {
                onMainScreen { tokenWithTitleAndAddress(tokenTitle).clickWithAssertion() }
            }
            step("Assert title: '$tokenTitle' is displayed") {
                onTokenDetailsScreen { title.assertIsDisplayed() }
            }
            step("Open 'Swap' screen") {
                openSwapScreen(from = SwapEntryPoint.TokenDetails)
            }
            step("Assert 'You swap' block is displayed") {
                onSwapTokenScreen { youSwapBlock.assertIsDisplayed() }
            }
            step("Input swap amount = '$inputAmount'") {
                waitForIdle()
                onSwapTokenScreen {
                    textInput.clickWithAssertion()
                    textInput.performTextReplacement(inputAmount)
                }
            }
            step("Assert 'Insufficient funds' error is displayed") {
                waitForIdle()
                onSwapTokenScreen { insufficientFundsErrorTitle.assertIsDisplayed() }
            }
        }
    }

    @AllureId("8502")
    @DisplayName("Swap: check 'Unable to cover network fee' warning")
    @Test
    fun checkUnableToCoverBlockchainFeeWarningTest() {
        val tokenTitle = "USDC"
        val inputAmount = "1000"
        val tokensScenarioState = "SolanaUSDC"
        val balanceScenarioName = "solana_balance"
        val balanceScenarioState = "Empty"
        val networkName = "Solana"
        val currencySymbol = "SOL"

        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(USER_TOKENS_API_SCENARIO)
                resetWireMockScenarioState(QUOTES_API_SCENARIO)
                resetWireMockScenarioState(balanceScenarioName)
            }
        ).run {

            step("Set WireMock scenario: '$USER_TOKENS_API_SCENARIO' to state: '$tokensScenarioState'") {
                setWireMockScenarioState(scenarioName = USER_TOKENS_API_SCENARIO, state = tokensScenarioState)
            }
            step("Set WireMock scenario: '$QUOTES_API_SCENARIO' to state: $tokensScenarioState") {
                setWireMockScenarioState(scenarioName = QUOTES_API_SCENARIO, state = tokensScenarioState)
            }
            step("Set WireMock scenario: '$balanceScenarioName' to state: $balanceScenarioState") {
                setWireMockScenarioState(scenarioName = balanceScenarioName, state = balanceScenarioState)
            }

            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Click on token with name: '$tokenTitle'") {
                onMainScreen { tokenWithTitleAndAddress(tokenTitle).clickWithAssertion() }
            }
            step("Assert title: '$tokenTitle' is displayed") {
                onTokenDetailsScreen { title.assertIsDisplayed() }
            }
            step("Open 'Swap' screen") {
                openSwapScreen(from = SwapEntryPoint.TokenDetails)
            }
            step("Assert 'You swap' block is displayed") {
                onSwapTokenScreen { youSwapBlock.assertIsDisplayed() }
            }
            step("Input swap amount = '$inputAmount'") {
                waitForIdle()
                onSwapTokenScreen {
                    textInput.clickWithAssertion()
                    textInput.performTextReplacement(inputAmount)
                }
            }
            step("Check 'Unable to cover '$networkName' fee notification") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    chackUnableToCoverFeeNotification(networkName = networkName, currencySymbol = currencySymbol)
                }
            }
            step("Assert 'Swap' button is disabled") {
                onSwapTokenScreen { swapButton.assertIsNotEnabled() }
            }
        }
    }

    @AllureId("8503")
    @DisplayName("Swap: check 'High price impact' warning on CEX")
    @Test
    fun checkHighPriceImpactWarningCEXTest() {
        val tokenTitle = "USDC"
        val inputAmount = "100"
        val currencySymbol = "SOL"
        val slippagePercent = "5%"
        val tokensScenarioState = "SolanaUSDC"
        val exchangeQuoteScenarioName = "exchange_quote_solana"
        val exchangeQuoteScenarioState = "HighPriceImpact"
        val dialogTitle = getResourceString(R.string.swapping_alert_title)
        val dialogText = getResourceString(
            R.string.swapping_alert_cex_description_with_slippage,
            currencySymbol,
            slippagePercent
        )

        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(USER_TOKENS_API_SCENARIO)
                resetWireMockScenarioState(QUOTES_API_SCENARIO)
                resetWireMockScenarioState(exchangeQuoteScenarioName)
            }
        ).run {

            step("Set WireMock scenario: '$USER_TOKENS_API_SCENARIO' to state: '$tokensScenarioState'") {
                setWireMockScenarioState(scenarioName = USER_TOKENS_API_SCENARIO, state = tokensScenarioState)
            }
            step("Set WireMock scenario: '$QUOTES_API_SCENARIO' to state: $tokensScenarioState") {
                setWireMockScenarioState(scenarioName = QUOTES_API_SCENARIO, state = tokensScenarioState)
            }
            step("Set WireMock scenario: '$exchangeQuoteScenarioName' to state: $exchangeQuoteScenarioState") {
                setWireMockScenarioState(
                    scenarioName = exchangeQuoteScenarioName,
                    state = exchangeQuoteScenarioState
                )
            }

            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Click on token with name: '$tokenTitle'") {
                onMainScreen { tokenWithTitleAndAddress(tokenTitle).clickWithAssertion() }
            }
            step("Assert title: '$tokenTitle' is displayed") {
                onTokenDetailsScreen { title.assertIsDisplayed() }
            }
            step("Open 'Swap' screen") {
                openSwapScreen(from = SwapEntryPoint.TokenDetails)
            }
            step("Assert 'You swap' block is displayed") {
                onSwapTokenScreen { youSwapBlock.assertIsDisplayed() }
            }
            step("Input swap amount = '$inputAmount'") {
                waitForIdle()
                onSwapTokenScreen {
                    textInput.clickWithAssertion()
                    textInput.performTextReplacement(inputAmount)
                }
            }
            step("Assert fiat amount with warning is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onSwapTokenScreen {
                        waitForIdle()
                        receiveFiatAmount.assertTextContains("%", substring = true)
                    }
                }
            }
            step("Assert receive amount information icon is displayed") {
                onSwapTokenScreen { receiveFiatAmountInformationIcon.assertIsDisplayed() }
            }
            step("Click on receive amount information icon") {
                onSwapTokenScreen { receiveFiatAmountInformationIcon.performClick() }
            }
            step("Assert information dialog is displayed") {
                onDialog { dialogContainer.assertIsDisplayed() }
            }
            step("Assert information dialog title is displayed") {
                onDialog { title.assertTextEquals(dialogTitle) }
            }
            step("Assert information dialog text for CEX is displayed") {
                onDialog { text.assertTextEquals(dialogText) }
            }
            step("Assert dialog 'OK' button is displayed") {
                onDialog { okButton.assertIsDisplayed() }
            }
        }
    }

    @AllureId("8504")
    @DisplayName("Swap: check 'High price impact' warning on DEX")
    @Test
    fun checkHighPriceImpactWarningDEXTest() {
        val tokenTitle = "Polygon"
        val inputAmount = "1000"
        val slippagePercent = "3.5%"
        val dialogTitle = getResourceString(R.string.swapping_alert_title)
        val highPriceImpactDescription = getResourceString(R.string.swapping_high_price_impact_description)
        val swappingAlertDEXDescription = getResourceString(R.string.swapping_alert_dex_description)
        val swappingAlertDEXDescriptionWithSlippage = getResourceString(
            R.string.swapping_alert_dex_description_with_slippage,
            slippagePercent
        )
        val pairsToScenarioName = "polygon_pos_to_pairs"
        val pairsFromScenarioName = "polygon_pos_from_pairs"
        val scenarioState = "DexProvider"

        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(pairsToScenarioName)
                resetWireMockScenarioState(pairsFromScenarioName)
            }
        ).run {
            step("Set WireMock scenario: '$pairsToScenarioName' to state: $scenarioState") {
                setWireMockScenarioState(scenarioName = pairsToScenarioName, state = scenarioState)
            }
            step("Set WireMock scenario: '$pairsFromScenarioName' to state: $scenarioState") {
                setWireMockScenarioState(scenarioName = pairsFromScenarioName, state = scenarioState)
            }

            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Click on token with name: '$tokenTitle'") {
                onMainScreen { tokenWithTitleAndAddress(tokenTitle).clickWithAssertion() }
            }
            step("Assert title: '$tokenTitle' is displayed") {
                onTokenDetailsScreen { title.assertIsDisplayed() }
            }
            step("Open 'Swap' screen") {
                openSwapScreen(from = SwapEntryPoint.TokenDetails)
            }
            step("Assert 'You swap' block is displayed") {
                onSwapTokenScreen { youSwapBlock.assertIsDisplayed() }
            }
            step("Input swap amount = '$inputAmount'") {
                waitForIdle()
                onSwapTokenScreen {
                    textInput.clickWithAssertion()
                    textInput.performTextReplacement(inputAmount)
                }
            }
            step("Assert fiat amount with warning is displayed") {
                onSwapTokenScreen { receiveFiatAmount.assertTextContains("%", substring = true) }
            }
            step("Assert receive amount information icon is displayed") {
                onSwapTokenScreen { receiveFiatAmountInformationIcon.assertIsDisplayed() }
            }
            step("Click on receive amount information icon") {
                onSwapTokenScreen { receiveFiatAmountInformationIcon.performClick() }
            }
            step("Assert information dialog is displayed") {
                onDialog { dialogContainer.assertIsDisplayed() }
            }
            step("Assert information dialog title is displayed") {
                onDialog { title.assertTextEquals(dialogTitle) }
            }
            step("Assert information dialog text for DEX is displayed") {
                onDialog {
                    text.assertTextContains(highPriceImpactDescription, substring = true)
                    text.assertTextContains(swappingAlertDEXDescription, substring = true)
                    text.assertTextContains(swappingAlertDEXDescriptionWithSlippage, substring = true)
                }
            }
            step("Assert dialog 'OK' button is displayed") {
                onDialog { okButton.assertIsDisplayed() }
            }
            step("Click on 'OK' button") {
                onDialog { okButton.performClick() }
            }
        }
    }

    @AllureId("2831")
    @DisplayName("Swap: warning is not displayed, if remaining balance is equal to 0")
    @Test
    fun solanaRemainingBalanceEqualToZeroWarningTest() {
        val tokenTitle = "Solana"
        val inputAmount = "0.00168933"
        val tokensScenarioState = "SolanaUSDC"
        val rentAmount = "SOL 0.00089088"
        val notificationTitle = getResourceString(R.string.send_notification_invalid_amount_title)
        val notificationMessage = getResourceString(
            R.string.send_notification_invalid_amount_rent_fee,
            rentAmount
        )

        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(USER_TOKENS_API_SCENARIO)
                resetWireMockScenarioState(QUOTES_API_SCENARIO)
            }
        ).run {

            step("Set WireMock scenario: '$USER_TOKENS_API_SCENARIO' to state: '$tokensScenarioState'") {
                setWireMockScenarioState(scenarioName = USER_TOKENS_API_SCENARIO, state = tokensScenarioState)
            }
            step("Set WireMock scenario: '$QUOTES_API_SCENARIO' to state: $tokensScenarioState") {
                setWireMockScenarioState(scenarioName = QUOTES_API_SCENARIO, state = tokensScenarioState)
            }

            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Click on token with name: '$tokenTitle'") {
                onMainScreen { tokenWithTitleAndAddress(tokenTitle).clickWithAssertion() }
            }
            step("Assert title: '$tokenTitle' is displayed") {
                onTokenDetailsScreen { title.assertIsDisplayed() }
            }
            step("Open 'Swap' screen") {
                openSwapScreen(from = SwapEntryPoint.TokenDetails)
            }
            step("Assert 'You swap' block is displayed") {
                onSwapTokenScreen { youSwapBlock.assertIsDisplayed() }
            }
            step("Input swap amount = '$inputAmount'") {
                waitForIdle()
                onSwapTokenScreen {
                    textInput.clickWithAssertion()
                    textInput.performTextReplacement(inputAmount)
                }
            }
            step("Assert 'Invalid amount' warning is not displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    checkSwapWarning(
                        title = notificationTitle,
                        message = notificationMessage,
                        isDisplayed = false
                    )
                }
            }
        }
    }

    @AllureId("2832")
    @DisplayName("Swap: warning is not displayed, if remaining balance is equal to rent amount")
    @Test
    fun solanaRemainingBalanceEqualToRentAmountTest() {
        val tokenTitle = "Solana"
        val inputAmount = "0.001689338"
        val tokensScenarioState = "SolanaUSDC"
        val rentAmount = "SOL 0.00089088"
        val notificationTitle = getResourceString(R.string.send_notification_invalid_amount_title)
        val notificationMessage = getResourceString(
            R.string.send_notification_invalid_amount_rent_fee,
            rentAmount
        )

        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(USER_TOKENS_API_SCENARIO)
                resetWireMockScenarioState(QUOTES_API_SCENARIO)
            }
        ).run {

            step("Set WireMock scenario: '$USER_TOKENS_API_SCENARIO' to state: '$tokensScenarioState'") {
                setWireMockScenarioState(scenarioName = USER_TOKENS_API_SCENARIO, state = tokensScenarioState)
            }
            step("Set WireMock scenario: '$QUOTES_API_SCENARIO' to state: $tokensScenarioState") {
                setWireMockScenarioState(scenarioName = QUOTES_API_SCENARIO, state = tokensScenarioState)
            }

            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Click on token with name: '$tokenTitle'") {
                onMainScreen { tokenWithTitleAndAddress(tokenTitle).clickWithAssertion() }
            }
            step("Assert title: '$tokenTitle' is displayed") {
                onTokenDetailsScreen { title.assertIsDisplayed() }
            }
            step("Open 'Swap' screen") {
                openSwapScreen(from = SwapEntryPoint.TokenDetails)
            }
            step("Assert 'You swap' block is displayed") {
                onSwapTokenScreen { youSwapBlock.assertIsDisplayed() }
            }
            step("Input swap amount = '$inputAmount'") {
                waitForIdle()
                onSwapTokenScreen {
                    textInput.clickWithAssertion()
                    textInput.performTextReplacement(inputAmount)
                }
            }
            step("Assert 'Invalid amount' warning is not displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    checkSwapWarning(
                        title = notificationTitle,
                        message = notificationMessage,
                        isDisplayed = false
                    )
                }

            }
        }
    }

    @AllureId("2833")
    @DisplayName("Swap: warning is not displayed, if remaining balance more than rent amount")
    @Test
    fun solanaRemainingBalanceMoreThanRentAmountTest() {
        val tokenTitle = "Solana"
        val inputAmount = "0.0000941"
        val tokensScenarioState = "SolanaUSDC"
        val rentAmount = "SOL 0.00089088"
        val notificationTitle = getResourceString(R.string.send_notification_invalid_amount_title)
        val notificationMessage = getResourceString(
            R.string.send_notification_invalid_amount_rent_fee,
            rentAmount
        )

        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(USER_TOKENS_API_SCENARIO)
                resetWireMockScenarioState(QUOTES_API_SCENARIO)
            }
        ).run {

            step("Set WireMock scenario: '$USER_TOKENS_API_SCENARIO' to state: '$tokensScenarioState'") {
                setWireMockScenarioState(scenarioName = USER_TOKENS_API_SCENARIO, state = tokensScenarioState)
            }
            step("Set WireMock scenario: '$QUOTES_API_SCENARIO' to state: $tokensScenarioState") {
                setWireMockScenarioState(scenarioName = QUOTES_API_SCENARIO, state = tokensScenarioState)
            }

            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Click on token with name: '$tokenTitle'") {
                onMainScreen { tokenWithTitleAndAddress(tokenTitle).clickWithAssertion() }
            }
            step("Assert title: '$tokenTitle' is displayed") {
                onTokenDetailsScreen { title.assertIsDisplayed() }
            }
            step("Open 'Swap' screen") {
                openSwapScreen(from = SwapEntryPoint.TokenDetails)
            }
            step("Assert 'You swap' block is displayed") {
                onSwapTokenScreen { youSwapBlock.assertIsDisplayed() }
            }
            step("Input swap amount = '$inputAmount'") {
                waitForIdle()
                onSwapTokenScreen {
                    textInput.clickWithAssertion()
                    textInput.performTextReplacement(inputAmount)
                }
            }
            step("Assert 'Invalid amount' warning is not displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    checkSwapWarning(
                        title = notificationTitle,
                        message = notificationMessage,
                        isDisplayed = false
                    )
                }
            }
        }
    }

    @AllureId("2830")
    @DisplayName("Swap: warning is displayed, if remaining balance less than rent amount")
    @Test
    fun solanaRemainingBalanceLessThanRentAmountTest() {
        val tokenTitle = "Solana"
        val inputAmount = "0.0016941"
        val tokensScenarioState = "SolanaUSDC"
        val rentAmount = "SOL 0.00089088"
        val notificationTitle = getResourceString(R.string.send_notification_invalid_amount_title)
        val notificationMessage = getResourceString(
            R.string.send_notification_invalid_amount_rent_fee,
            rentAmount
        )

        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(USER_TOKENS_API_SCENARIO)
                resetWireMockScenarioState(QUOTES_API_SCENARIO)
            }
        ).run {

            step("Set WireMock scenario: '$USER_TOKENS_API_SCENARIO' to state: '$tokensScenarioState'") {
                setWireMockScenarioState(scenarioName = USER_TOKENS_API_SCENARIO, state = tokensScenarioState)
            }
            step("Set WireMock scenario: '$QUOTES_API_SCENARIO' to state: $tokensScenarioState") {
                setWireMockScenarioState(scenarioName = QUOTES_API_SCENARIO, state = tokensScenarioState)
            }

            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Click on token with name: '$tokenTitle'") {
                onMainScreen { tokenWithTitleAndAddress(tokenTitle).clickWithAssertion() }
            }
            step("Assert title: '$tokenTitle' is displayed") {
                onTokenDetailsScreen { title.assertIsDisplayed() }
            }
            step("Open 'Swap' screen") {
                openSwapScreen(from = SwapEntryPoint.TokenDetails)
            }
            step("Assert 'You swap' block is displayed") {
                onSwapTokenScreen { youSwapBlock.assertIsDisplayed() }
            }
            step("Input swap amount = '$inputAmount'") {
                waitForIdle()
                onSwapTokenScreen {
                    textInput.clickWithAssertion()
                    textInput.performTextReplacement(inputAmount)
                }
            }
            step("Assert 'Invalid amount' warning is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    checkSwapWarning(
                        title = notificationTitle,
                        message = notificationMessage
                    )
                }
            }
        }
    }
}