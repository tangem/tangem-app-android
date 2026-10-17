package com.tangem.tests.send.sendViaSwap

import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.ETHEREUM_RECIPIENT_ADDRESS
import com.tangem.common.constants.TestConstants.SOLANA_RECIPIENT_ADDRESS
import com.tangem.common.constants.TestConstants.USER_TOKENS_API_SCENARIO
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_LONG
import com.tangem.common.extensions.extractText
import com.tangem.common.extensions.parseNumericBalance
import com.tangem.common.utils.assertUrlOpened
import com.tangem.common.utils.findWireMockRequestTimestampsByQueryParam
import com.tangem.common.utils.getWireMockRequestCountByQueryParam
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.common.utils.stubUrlOpening
import com.tangem.core.res.R
import com.tangem.scenarios.*
import com.tangem.screens.*
import com.tangem.tap.domain.sdk.mocks.content.Wallet2WithDerivationsMockContent
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.kakaocup.kakao.common.utilities.getResourceString
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Assert.assertTrue
import org.junit.Test

@HiltAndroidTest
class SendViaSwapQuotesTest : BaseTestCase() {

    @AllureId("11533")
    @DisplayName("Send via Swap: offer is recalculated after the amount change")
    @Test
    fun offerIsRecalculatedAfterAmountChangeTest() {
        val tokenName = "Bitcoin"
        val swapTokenName = "Ethereum"
        val main = "MAIN"
        val firstInputAmount = "0.001"
        val secondInputAmount = "0.002"
        val secondInputAmountInSatoshi = "200000"
        val recalculatedReceiveAmount = "0.0856"
        val bestRateProviderName = "SimpleSwap"
        val exchangeQuotePath = "/v1/exchange-quote"
        val fromAmountQueryParam = "fromAmount"
        val bitcoinBalanceScenarioName = "bitcoin_utxo"
        val bitcoinBalanceScenarioState = "Balance"
        val assetsScenarioName = "express_api_assets"
        val assetsScenarioState = "BitcoinExchangeEnabled"
        val userTokensScenarioState = "Wallet2"

        setupHooks().run {

            step("Set WireMock scenario: '$bitcoinBalanceScenarioName' to state: '$bitcoinBalanceScenarioState'") {
                setWireMockScenarioState(scenarioName = bitcoinBalanceScenarioName, state = bitcoinBalanceScenarioState)
            }
            step("Set WireMock scenario: '$assetsScenarioName' to state: '$assetsScenarioState'") {
                setWireMockScenarioState(scenarioName = assetsScenarioName, state = assetsScenarioState)
            }
            step("Set WireMock scenario: '$USER_TOKENS_API_SCENARIO' to state: '$userTokensScenarioState'") {
                setWireMockScenarioState(scenarioName = USER_TOKENS_API_SCENARIO, state = userTokensScenarioState)
            }

            step("Open 'Send' screen") {
                openSendScreen(tokenName, mockContent = Wallet2WithDerivationsMockContent)
            }
            step("Open 'Send via Swap' confirm screen") {
                openSendViaSwapConfirmScreen(
                    sendTokenName = tokenName,
                    swapTokenName = swapTokenName,
                    networkType = main,
                    amount = firstInputAmount,
                    recipientAddress = ETHEREUM_RECIPIENT_ADDRESS,
                )
            }
            step("Click on send amount block") {
                onSendConfirmScreen { swapSendAmount.performClick() }
            }
            step("Assert amount editing screen is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT) { onSendScreen { continueButton.assertIsDisplayed() } }
            }
            step("Click on '$tokenName' send amount block") {
                onSendScreen { swapTokenName(tokenName).performClick() }
            }
            val quoteRequestsBefore = getWireMockRequestCountByQueryParam(
                method = "GET",
                urlPath = exchangeQuotePath,
                queryParam = fromAmountQueryParam,
                queryValue = secondInputAmountInSatoshi,
            )
            step("Type '$secondInputAmount' in text field") {
                onSendScreen { amountInputTextField.performTextReplacement(secondInputAmount) }
            }
            step("Assert receive amount is recalculated to '$recalculatedReceiveAmount'") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onSendScreen { swapReceiveAmount.assertTextContains(recalculatedReceiveAmount, substring = true) }
                }
            }
            step("Assert quote is requested for '$secondInputAmount'") {
                flakySafely(WAIT_UNTIL_TIMEOUT) {
                    val quoteRequestsAfter = getWireMockRequestCountByQueryParam(
                        method = "GET",
                        urlPath = exchangeQuotePath,
                        queryParam = fromAmountQueryParam,
                        queryValue = secondInputAmountInSatoshi,
                    )
                    assertTrue(
                        "No '$exchangeQuotePath' request with $fromAmountQueryParam=$secondInputAmountInSatoshi was sent",
                        quoteRequestsAfter > quoteRequestsBefore,
                    )
                }
            }
            step("Click on 'Continue' button") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { onSendScreen { continueButton.assertIsEnabled() } }
                onSendScreen { continueButton.performClick() }
            }
            step("Assert 'Send confirm' screen is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT) { onSendConfirmScreen { sendButton.assertIsDisplayed() } }
            }
            step("Assert send amount is '$secondInputAmount'") {
                flakySafely(WAIT_UNTIL_TIMEOUT) {
                    onSendConfirmScreen { swapSendAmount.assertTextContains(secondInputAmount, substring = true) }
                }
            }
            step("Assert receive amount is '$recalculatedReceiveAmount'") {
                onSendConfirmScreen { swapReceiveAmount.assertTextContains(recalculatedReceiveAmount, substring = true) }
            }
            step("Assert 'Best rate' badge is displayed") {
                onSendConfirmScreen { bestRateBadge.assertIsDisplayed() }
            }
            step("Assert provider name is '$bestRateProviderName'") {
                onSendConfirmScreen { providerName.assertTextContains(bestRateProviderName) }
            }
        }
    }

    @AllureId("11532")
    @DisplayName("Send via Swap: network fee speed selection")
    @Test
    fun networkFeeSpeedSelectionTest() {
        val tokenName = "Bitcoin"
        val swapTokenName = "Ethereum"
        val main = "MAIN"
        val inputAmount = "0.001"
        val marketOption = getResourceString(R.string.common_fee_selector_option_market)
        val fastOption = getResourceString(R.string.common_fee_selector_option_fast)
        val bitcoinBalanceScenarioName = "bitcoin_utxo"
        val bitcoinBalanceScenarioState = "Balance"
        val assetsScenarioName = "express_api_assets"
        val assetsScenarioState = "BitcoinExchangeEnabled"
        val userTokensScenarioState = "Wallet2"
        var confirmFeeBefore = ""

        setupHooks().run {

            step("Set WireMock scenario: '$bitcoinBalanceScenarioName' to state: '$bitcoinBalanceScenarioState'") {
                setWireMockScenarioState(scenarioName = bitcoinBalanceScenarioName, state = bitcoinBalanceScenarioState)
            }
            step("Set WireMock scenario: '$assetsScenarioName' to state: '$assetsScenarioState'") {
                setWireMockScenarioState(scenarioName = assetsScenarioName, state = assetsScenarioState)
            }
            step("Set WireMock scenario: '$USER_TOKENS_API_SCENARIO' to state: '$userTokensScenarioState'") {
                setWireMockScenarioState(scenarioName = USER_TOKENS_API_SCENARIO, state = userTokensScenarioState)
            }

            step("Open 'Send' screen") {
                openSendScreen(tokenName, mockContent = Wallet2WithDerivationsMockContent)
            }
            step("Open 'Send via Swap' confirm screen") {
                openSendViaSwapConfirmScreen(
                    sendTokenName = tokenName,
                    swapTokenName = swapTokenName,
                    networkType = main,
                    amount = inputAmount,
                    recipientAddress = ETHEREUM_RECIPIENT_ADDRESS,
                )
            }
            step("Wait until network fee is stable") {
                waitUntilNetworkFeeIsStable { readNetworkFeeAmount() }
            }
            step("Capture network fee on 'Send confirm' screen") {
                confirmFeeBefore = readNetworkFeeAmount()
            }
            step("Click on network fee block") {
                onSendConfirmScreen { feeSelectorBlock.performClick() }
            }
            step("Assert 'Choose speed' bottom sheet is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT) { onSendSelectNetworkFeeBottomSheet { chooseSpeedTitle.assertIsDisplayed() } }
            }
            step("Assert '$marketOption' option with crypto and fiat fee is displayed") {
                onSendSelectNetworkFeeBottomSheet { regularFeeSelectorItem(marketOption).assertIsDisplayed() }
            }
            step("Assert '$fastOption' option with crypto and fiat fee is displayed") {
                onSendSelectNetworkFeeBottomSheet { regularFeeSelectorItem(fastOption).assertIsDisplayed() }
            }
            step("Assert '$marketOption' option is selected") {
                onSendSelectNetworkFeeBottomSheet { regularFeeOption(marketOption).assertIsSelected() }
            }
            step("Assert '$fastOption' option is not selected") {
                onSendSelectNetworkFeeBottomSheet { regularFeeOption(fastOption).assertIsNotSelected() }
            }
            step("Assert '$fastOption' fee is higher than '$marketOption' fee") {
                var marketFee = ""
                var fastFee = ""
                onSendSelectNetworkFeeBottomSheet {
                    marketFee = regularFeeTokenAmount(marketOption).extractText()
                    fastFee = regularFeeTokenAmount(fastOption).extractText()
                }
                assertTrue(
                    "'$fastOption' fee '$fastFee' is not higher than '$marketOption' fee '$marketFee'",
                    fastFee.parseNumericBalance() > marketFee.parseNumericBalance(),
                )
            }
            step("Click on '$fastOption' option") {
                onSendSelectNetworkFeeBottomSheet { regularFeeSelectorItem(fastOption).performClick() }
            }
            step("Assert 'Choose speed' bottom sheet is not displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT) { onSendSelectNetworkFeeBottomSheet { chooseSpeedTitle.assertDoesNotExist() } }
            }
            step("Assert network fee on 'Send confirm' screen is higher than '$confirmFeeBefore'") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    val confirmFeeAfter = readNetworkFeeAmount()
                    if (confirmFeeAfter.isEmpty() ||
                        confirmFeeAfter.parseNumericBalance() <= confirmFeeBefore.parseNumericBalance()
                    ) {
                        throw AssertionError("Network fee '$confirmFeeAfter' is not higher than '$confirmFeeBefore'")
                    }
                }
            }
            step("Click on network fee block") {
                onSendConfirmScreen { feeSelectorBlock.performClick() }
            }
            step("Assert '$fastOption' option is selected") {
                flakySafely(WAIT_UNTIL_TIMEOUT) {
                    onSendSelectNetworkFeeBottomSheet { regularFeeOption(fastOption).assertIsSelected() }
                }
            }
        }
    }

    @AllureId("11534")
    @DisplayName("Send via Swap: quote is updated automatically")
    @Test
    fun quoteIsUpdatedAutomaticallyTest() {
        val tokenName = "Bitcoin"
        val swapTokenName = "Ethereum"
        val main = "MAIN"
        val inputAmount = "0.001"
        val selectedProviderName = "ChangeHero"
        val selectedProviderId = "changehero"
        val cexType = "CEX"
        val autoUpdatesCount = 2
        val minQuoteUpdateIntervalMillis = 9_500L
        val maxQuoteUpdateIntervalMillis = 15_000L
        val exchangeQuotePath = "/v1/exchange-quote"
        val providerIdQueryParam = "providerId"
        val bitcoinBalanceScenarioName = "bitcoin_utxo"
        val bitcoinBalanceScenarioState = "Balance"
        val assetsScenarioName = "express_api_assets"
        val assetsScenarioState = "BitcoinExchangeEnabled"
        val userTokensScenarioState = "Wallet2"
        val exchangeQuoteScenarioName = "exchange_quote_bitcoin"
        val exchangeQuoteScenarioState = "AutoRefresh"
        var receiveAmount = ""

        setupHooks().run {

            step("Set WireMock scenario: '$bitcoinBalanceScenarioName' to state: '$bitcoinBalanceScenarioState'") {
                setWireMockScenarioState(scenarioName = bitcoinBalanceScenarioName, state = bitcoinBalanceScenarioState)
            }
            step("Set WireMock scenario: '$assetsScenarioName' to state: '$assetsScenarioState'") {
                setWireMockScenarioState(scenarioName = assetsScenarioName, state = assetsScenarioState)
            }
            step("Set WireMock scenario: '$USER_TOKENS_API_SCENARIO' to state: '$userTokensScenarioState'") {
                setWireMockScenarioState(scenarioName = USER_TOKENS_API_SCENARIO, state = userTokensScenarioState)
            }
            step("Set WireMock scenario: '$exchangeQuoteScenarioName' to state: '$exchangeQuoteScenarioState'") {
                setWireMockScenarioState(scenarioName = exchangeQuoteScenarioName, state = exchangeQuoteScenarioState)
            }

            step("Open 'Send' screen") {
                openSendScreen(tokenName, mockContent = Wallet2WithDerivationsMockContent)
            }
            step("Open 'Send via Swap' confirm screen") {
                openSendViaSwapConfirmScreen(
                    sendTokenName = tokenName,
                    swapTokenName = swapTokenName,
                    networkType = main,
                    amount = inputAmount,
                    recipientAddress = ETHEREUM_RECIPIENT_ADDRESS,
                    isFloatingRate = true,
                )
            }
            step("Click on 'Provider'") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { onSendConfirmScreen { bestRateBadge.assertIsDisplayed() } }
                onSendConfirmScreen { provider.performClick() }
            }
            step("Click on '$selectedProviderName'") {
                onSelectSwapProviderBottomSheet {
                    providerItem(name = selectedProviderName, type = cexType, isBestRate = false).performClick()
                }
            }
            step("Assert provider name is '$selectedProviderName'") {
                flakySafely(WAIT_UNTIL_TIMEOUT) {
                    onSendConfirmScreen { providerName.assertTextContains(selectedProviderName) }
                }
            }
            step("Capture receive amount") {
                onSendConfirmScreen { receiveAmount = swapReceiveAmount.extractText() }
            }
            val quoteRequestsBefore = findWireMockRequestTimestampsByQueryParam(
                method = "GET",
                urlPath = exchangeQuotePath,
                queryParam = providerIdQueryParam,
                queryValue = selectedProviderId,
            ).size
            repeat(autoUpdatesCount) { index ->
                val updateNumber = index + 1
                step("Assert receive amount is updated automatically (update $updateNumber)") {
                    val previousReceiveAmount = receiveAmount
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                        onSendConfirmScreen {
                            val currentReceiveAmount = swapReceiveAmount.extractText()
                            if (currentReceiveAmount == previousReceiveAmount) {
                                throw AssertionError("Receive amount is still '$previousReceiveAmount'")
                            }
                            receiveAmount = currentReceiveAmount
                        }
                    }
                }
                step("Assert provider name is still '$selectedProviderName' (update $updateNumber)") {
                    onSendConfirmScreen { providerName.assertTextContains(selectedProviderName) }
                }
                step("Assert 'Best rate' badge is not displayed (update $updateNumber)") {
                    onSendConfirmScreen { bestRateBadge.assertIsNotDisplayed() }
                }
                step("Assert 'Send' button is enabled (update $updateNumber)") {
                    onSendConfirmScreen { sendButton.assertIsEnabled() }
                }
            }
            step("Assert quotes are requested every ~10 seconds") {
                val quoteRequestTimestamps = findWireMockRequestTimestampsByQueryParam(
                    method = "GET",
                    urlPath = exchangeQuotePath,
                    queryParam = providerIdQueryParam,
                    queryValue = selectedProviderId,
                ).drop(quoteRequestsBefore)
                val intervals = quoteRequestTimestamps.zipWithNext { previous, next -> next - previous }
                assertTrue(
                    "Expected at least one interval between automatic quote updates, timestamps: $quoteRequestTimestamps",
                    intervals.isNotEmpty(),
                )
                assertTrue(
                    "Quote update intervals $intervals ms are not within " +
                        "$minQuoteUpdateIntervalMillis..$maxQuoteUpdateIntervalMillis ms",
                    intervals.all { it in minQuoteUpdateIntervalMillis..maxQuoteUpdateIntervalMillis },
                )
            }
        }
    }

    @AllureId("11536")
    @DisplayName("Send via Swap: confirm the receive token change")
    @Test
    fun confirmReceiveTokenChangeTest() {
        val tokenName = "Bitcoin"
        val firstSwapTokenName = "Solana"
        val secondSwapTokenName = "Ethereum"
        val main = "MAIN"
        val inputAmount = "0.001"
        val receiveAmount = "0.05"
        val recalculatedReceiveAmount = "0.0428"
        val bitcoinBalanceScenarioName = "bitcoin_utxo"
        val bitcoinBalanceScenarioState = "Balance"
        val assetsScenarioName = "express_api_assets"
        val assetsScenarioState = "BitcoinExchangeEnabled"
        val userTokensScenarioState = "Wallet2"
        val dialogTitle = getResourceString(R.string.send_with_swap_change_token_alert_title)
        val dialogMessage = getResourceString(R.string.send_with_swap_change_token_alert_message)
        val addressHint = getResourceString(R.string.send_enter_address_field_ens)

        setupHooks().run {

            step("Set WireMock scenario: '$bitcoinBalanceScenarioName' to state: '$bitcoinBalanceScenarioState'") {
                setWireMockScenarioState(scenarioName = bitcoinBalanceScenarioName, state = bitcoinBalanceScenarioState)
            }
            step("Set WireMock scenario: '$assetsScenarioName' to state: '$assetsScenarioState'") {
                setWireMockScenarioState(scenarioName = assetsScenarioName, state = assetsScenarioState)
            }
            step("Set WireMock scenario: '$USER_TOKENS_API_SCENARIO' to state: '$userTokensScenarioState'") {
                setWireMockScenarioState(scenarioName = USER_TOKENS_API_SCENARIO, state = userTokensScenarioState)
            }

            step("Open 'Send' screen") {
                openSendScreen(tokenName, mockContent = Wallet2WithDerivationsMockContent)
            }
            step("Open 'Send via Swap' confirm screen") {
                openSendViaSwapConfirmScreen(
                    sendTokenName = tokenName,
                    swapTokenName = firstSwapTokenName,
                    networkType = main,
                    amount = inputAmount,
                    recipientAddress = SOLANA_RECIPIENT_ADDRESS,
                    isFloatingRate = true,
                )
            }
            step("Click on send amount block") {
                onSendConfirmScreen { swapSendAmount.performClick() }
            }
            step("Click on '$firstSwapTokenName' receive token block to edit receive amount") {
                flakySafely(WAIT_UNTIL_TIMEOUT) { onSendScreen { swapTokenName(firstSwapTokenName).assertIsDisplayed() } }
                onSendScreen { swapTokenName(firstSwapTokenName).performClick() }
            }
            step("Click on '$firstSwapTokenName' receive token block to open token selection") {
                flakySafely(WAIT_UNTIL_TIMEOUT) { onSendScreen { fixedRateBadge.assertIsDisplayed() } }
                onSendScreen { swapTokenName(firstSwapTokenName).performClick() }
            }
            step("Click on token: '$secondSwapTokenName'") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { onSendViaSwapScreen { tokenItem(secondSwapTokenName).assertIsDisplayed() } }
                onSendViaSwapScreen { tokenItem(secondSwapTokenName).performClick() }
            }
            step("Click on '$secondSwapTokenName $main' network") {
                onChooseNetworkBottomSheet { networkItem(secondSwapTokenName, main).performClick() }
            }
            step("Assert '$dialogTitle' dialog title is displayed") {
                onDialog { title.assertTextEquals(dialogTitle) }
            }
            step("Assert '$dialogMessage' dialog message is displayed") {
                onDialog { text.assertTextEquals(dialogMessage) }
            }
            step("Click on 'Change' button") {
                onDialog { changeButton.performClick() }
            }
            step("Assert amount screen with 'Next' button is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT) { onSendScreen { nextButton.assertIsDisplayed() } }
            }
            step("Assert receive token '$secondSwapTokenName' is displayed") {
                onSendScreen { swapTokenName(secondSwapTokenName).assertIsDisplayed() }
            }
            step("Assert previously entered send amount '$inputAmount' is kept") {
                flakySafely(WAIT_UNTIL_TIMEOUT) {
                    onSendScreen { swapSendAmount.assertTextContains(inputAmount, substring = true) }
                }
            }
            step("Assert receive amount is recalculated to '$recalculatedReceiveAmount'") {
                flakySafely(WAIT_UNTIL_TIMEOUT) {
                    onSendScreen { amountInputTextField.assertTextContains(recalculatedReceiveAmount, substring = true) }
                }
            }
            step("Assert 'Rate is fixed' badge is displayed") {
                onSendScreen { fixedRateBadge.assertIsDisplayed() }
            }
            step("Type '$receiveAmount' in receive amount text field") {
                onSendScreen { amountInputTextField.performTextReplacement(receiveAmount) }
            }
            step("Click on 'Next' button") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { onSendScreen { nextButton.assertIsEnabled() } }
                onSendScreen { nextButton.performClick() }
            }
            step("Assert 'Send address' screen is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT) { onSendAddressScreen { container.assertIsDisplayed() } }
            }
            step("Assert address text field is empty and hint is displayed") {
                onSendAddressScreen { addressTextFieldHint.assertTextContains(addressHint) }
            }
        }
    }

    @AllureId("11537")
    @DisplayName("Send via Swap: switch from fixed to floating rate")
    @Test
    fun switchFromFixedToFloatingRateTest() {
        val tokenName = "Bitcoin"
        val swapTokenName = "Ethereum"
        val main = "MAIN"
        val receiveAmount = "0.0428"
        val calculatedSendAmount = "0.001"
        val changedSendAmount = "0.002"
        val changedSendAmountInSatoshi = "200000"
        val recalculatedReceiveAmount = "0.0856"
        val exchangeQuotePath = "/v1/exchange-quote"
        val fromAmountQueryParam = "fromAmount"
        val bitcoinBalanceScenarioName = "bitcoin_utxo"
        val bitcoinBalanceScenarioState = "Balance"
        val assetsScenarioName = "express_api_assets"
        val assetsScenarioState = "BitcoinExchangeEnabled"
        val userTokensScenarioState = "Wallet2"

        setupHooks().run {

            step("Set WireMock scenario: '$bitcoinBalanceScenarioName' to state: '$bitcoinBalanceScenarioState'") {
                setWireMockScenarioState(scenarioName = bitcoinBalanceScenarioName, state = bitcoinBalanceScenarioState)
            }
            step("Set WireMock scenario: '$assetsScenarioName' to state: '$assetsScenarioState'") {
                setWireMockScenarioState(scenarioName = assetsScenarioName, state = assetsScenarioState)
            }
            step("Set WireMock scenario: '$USER_TOKENS_API_SCENARIO' to state: '$userTokensScenarioState'") {
                setWireMockScenarioState(scenarioName = USER_TOKENS_API_SCENARIO, state = userTokensScenarioState)
            }

            step("Open 'Send' screen") {
                openSendScreen(tokenName, mockContent = Wallet2WithDerivationsMockContent)
            }
            step("Select '$swapTokenName $main' as receive token") {
                selectReceiveTokenForSendViaSwap(swapTokenName = swapTokenName, networkType = main)
            }
            step("Assert 'Rate is fixed' badge is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { onSendScreen { fixedRateBadge.assertIsDisplayed() } }
            }
            step("Type '$receiveAmount' in receive amount text field") {
                onSendScreen { amountInputTextField.performTextReplacement(receiveAmount) }
            }
            step("Assert send amount is calculated as '$calculatedSendAmount'") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onSendScreen { swapSendAmount.assertTextContains(calculatedSendAmount, substring = true) }
                }
            }
            step("Assert receive amount is exactly '$receiveAmount'") {
                onSendScreen { amountInputTextField.assertTextContains(receiveAmount, substring = true) }
            }
            step("Assert approximate receive amount is not displayed") {
                onSendScreen { swapReceiveAmount.assertDoesNotExist() }
            }
            step("Assert 'Next' button is enabled") {
                flakySafely(WAIT_UNTIL_TIMEOUT) { onSendScreen { nextButton.assertIsEnabled() } }
            }
            step("Click on '$tokenName' send amount block") {
                onSendScreen { swapTokenName(tokenName).performClick() }
            }
            step("Assert 'Floating rate' badge is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT) { onSendScreen { floatingRateBadge.assertIsDisplayed() } }
            }
            step("Assert calculated send amount '$calculatedSendAmount' is in text field") {
                onSendScreen { amountInputTextField.assertTextContains(calculatedSendAmount) }
            }
            val quoteRequestsBefore = getWireMockRequestCountByQueryParam(
                method = "GET",
                urlPath = exchangeQuotePath,
                queryParam = fromAmountQueryParam,
                queryValue = changedSendAmountInSatoshi,
            )
            step("Type '$changedSendAmount' in send amount text field") {
                onSendScreen { amountInputTextField.performTextReplacement(changedSendAmount) }
            }
            step("Assert quote is requested for '$changedSendAmount'") {
                flakySafely(WAIT_UNTIL_TIMEOUT) {
                    val quoteRequestsAfter = getWireMockRequestCountByQueryParam(
                        method = "GET",
                        urlPath = exchangeQuotePath,
                        queryParam = fromAmountQueryParam,
                        queryValue = changedSendAmountInSatoshi,
                    )
                    assertTrue(
                        "No '$exchangeQuotePath' request with $fromAmountQueryParam=$changedSendAmountInSatoshi was sent",
                        quoteRequestsAfter > quoteRequestsBefore,
                    )
                }
            }
            step("Assert approximate receive amount is updated to '$recalculatedReceiveAmount'") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onSendScreen { swapReceiveAmount.assertTextContains(recalculatedReceiveAmount, substring = true) }
                }
            }
            step("Assert 'Next' button is enabled") {
                flakySafely(WAIT_UNTIL_TIMEOUT) { onSendScreen { nextButton.assertIsEnabled() } }
            }
        }
    }

    @AllureId("3973")
    @DisplayName("Send via Swap: FCA warning for a CEX provider in the UK region")
    @Test
    fun fcaWarningForCexProviderInUkTest() {
        val tokenName = "Bitcoin"
        val swapTokenName = "Ethereum"
        val main = "MAIN"
        val inputAmount = "0.001"
        val cexType = "CEX"
        val defaultProviderName = "ChangeHero"
        val fcaProviderName = "Changelly"
        val anotherFcaProviderName = "SimpleSwap"
        val fcaProviderTermsOfUseUrl = "https://changelly.com/terms-of-use"
        val fcaProviderPrivacyPolicyUrl = "https://changelly.com/privacy-policy"
        val geoScenarioName = "geo"
        val geoScenarioState = "GB"
        val bitcoinBalanceScenarioName = "bitcoin_utxo"
        val bitcoinBalanceScenarioState = "Balance"
        val assetsScenarioName = "express_api_assets"
        val assetsScenarioState = "BitcoinExchangeEnabled"
        val userTokensScenarioState = "Wallet2"
        val exchangeQuoteScenarioName = "exchange_quote_bitcoin"
        val exchangeQuoteScenarioState = "ChangeHeroBestRate"

        setupHooks(
            additionalBeforeAppLaunchSection = {
                setWireMockScenarioState(scenarioName = geoScenarioName, state = geoScenarioState)
            }
        ).run {

            step("Set WireMock scenario: '$bitcoinBalanceScenarioName' to state: '$bitcoinBalanceScenarioState'") {
                setWireMockScenarioState(scenarioName = bitcoinBalanceScenarioName, state = bitcoinBalanceScenarioState)
            }
            step("Set WireMock scenario: '$assetsScenarioName' to state: '$assetsScenarioState'") {
                setWireMockScenarioState(scenarioName = assetsScenarioName, state = assetsScenarioState)
            }
            step("Set WireMock scenario: '$USER_TOKENS_API_SCENARIO' to state: '$userTokensScenarioState'") {
                setWireMockScenarioState(scenarioName = USER_TOKENS_API_SCENARIO, state = userTokensScenarioState)
            }
            step("Set WireMock scenario: '$exchangeQuoteScenarioName' to state: '$exchangeQuoteScenarioState'") {
                setWireMockScenarioState(scenarioName = exchangeQuoteScenarioName, state = exchangeQuoteScenarioState)
            }

            step("Open 'Send' screen") {
                openSendScreen(tokenName, mockContent = Wallet2WithDerivationsMockContent)
            }
            step("Select '$swapTokenName $main' as receive token") {
                selectReceiveTokenForSendViaSwap(swapTokenName = swapTokenName, networkType = main)
            }
            step("Click on '$tokenName' send amount block") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { onSendScreen { fixedRateBadge.assertIsDisplayed() } }
                onSendScreen { swapTokenName(tokenName).performClick() }
            }
            step("Assert 'Floating rate' badge is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT) { onSendScreen { floatingRateBadge.assertIsDisplayed() } }
            }
            step("Type '$inputAmount' in text field") {
                onSendScreen { amountInputTextField.performTextReplacement(inputAmount) }
            }
            step("Click on 'Next' button") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { onSendScreen { nextButton.assertIsEnabled() } }
                onSendScreen { nextButton.performClick() }
            }
            step("Type recipient address") {
                onSendAddressScreen { addressTextField.performTextReplacement(ETHEREUM_RECIPIENT_ADDRESS) }
            }
            step("Open 'Send confirm' screen via 'Next' button") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { onSendAddressScreen { nextButton.assertIsEnabled() } }
                openSendConfirmScreenViaNextButton()
            }
            step("Assert provider name is '$defaultProviderName'") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onSendConfirmScreen { providerName.assertTextContains(defaultProviderName) }
                }
            }
            step("Click on 'Provider'") {
                onSendConfirmScreen { provider.performClick() }
            }
            step("Assert 'FCA Warning List' badge is displayed for '$fcaProviderName'") {
                flakySafely(WAIT_UNTIL_TIMEOUT) {
                    onSelectSwapProviderBottomSheet { fcaWarningBadge(fcaProviderName).assertIsDisplayed() }
                }
            }
            step("Assert 'FCA Warning List' badge is displayed for '$anotherFcaProviderName'") {
                onSelectSwapProviderBottomSheet { fcaWarningBadge(anotherFcaProviderName).assertIsDisplayed() }
            }
            step("Assert 'FCA Warning List' badge is not displayed for '$defaultProviderName'") {
                onSelectSwapProviderBottomSheet { fcaWarningBadge(defaultProviderName).assertDoesNotExist() }
            }
            step("Click on '$fcaProviderName'") {
                onSelectSwapProviderBottomSheet {
                    providerItem(name = fcaProviderName, type = cexType, isBestRate = false).performClick()
                }
            }
            step("Assert provider name is '$fcaProviderName'") {
                flakySafely(WAIT_UNTIL_TIMEOUT) {
                    onSendConfirmScreen { providerName.assertTextContains(fcaProviderName) }
                }
            }
            step("Assert FCA warning is displayed") {
                onSendConfirmScreen { fcaWarning.assertIsDisplayed() }
            }
            step("Stub opening '$fcaProviderTermsOfUseUrl'") {
                stubUrlOpening(fcaProviderTermsOfUseUrl)
            }
            step("Click on 'Terms of Use' link") {
                flakySafely(WAIT_UNTIL_TIMEOUT) { onSendConfirmScreen { sendingText.assertIsDisplayed() } }
                onSendConfirmScreen { clickTermsOfUseLink() }
            }
            step("Assert '$fcaProviderTermsOfUseUrl' is opened") {
                flakySafely(WAIT_UNTIL_TIMEOUT) { assertUrlOpened(fcaProviderTermsOfUseUrl) }
            }
            step("Assert provider name is still '$fcaProviderName'") {
                onSendConfirmScreen { providerName.assertTextContains(fcaProviderName) }
            }
            step("Assert FCA warning is still displayed") {
                onSendConfirmScreen { fcaWarning.assertIsDisplayed() }
            }
            step("Stub opening '$fcaProviderPrivacyPolicyUrl'") {
                stubUrlOpening(fcaProviderPrivacyPolicyUrl)
            }
            step("Click on 'Privacy Policy' link") {
                onSendConfirmScreen { clickPrivacyPolicyLink() }
            }
            step("Assert '$fcaProviderPrivacyPolicyUrl' is opened") {
                flakySafely(WAIT_UNTIL_TIMEOUT) { assertUrlOpened(fcaProviderPrivacyPolicyUrl) }
            }
        }
    }
}