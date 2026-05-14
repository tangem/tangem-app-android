package com.tangem.tests.send.sendViaSwap

import com.tangem.common.BaseTestCase
import com.tangem.common.R
import com.tangem.common.constants.TestConstants.ETHEREUM_RECIPIENT_ADDRESS
import com.tangem.common.constants.TestConstants.POLYGON_RECIPIENT_ADDRESS
import com.tangem.common.constants.TestConstants.QUOTES_API_SCENARIO
import com.tangem.common.constants.TestConstants.SOLANA_RECIPIENT_ADDRESS
import com.tangem.common.constants.TestConstants.USER_TOKENS_API_SCENARIO
import com.tangem.common.constants.TestConstants.SVS_SEED_PHRASE_12
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_LONG
import com.tangem.common.extensions.extractText
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.utils.resetWireMockScenarioState
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.scenarios.*
import com.tangem.screens.*
import com.tangem.tap.domain.sdk.mocks.content.Wallet2WithDerivationsMockContent
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
        val userTokensScenarioState = "Wallet2"
        val warningTitle = getResourceString(R.string.express_swap_not_supported_title, stellar)
        val warningMessage = getResourceString(R.string.express_swap_not_supported_text)

        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(bitcoinBalanceScenarioName)
                resetWireMockScenarioState(assetsScenarioName)
                resetWireMockScenarioState(USER_TOKENS_API_SCENARIO)
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
            step("Open 'Send' screen") {
                openSendScreen(tokenName, mockContent = Wallet2WithDerivationsMockContent)
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
                onWarningBottomSheet { okGotItButton.performClick() }
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
        val userTokensScenarioState = "Wallet2"

        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(bitcoinBalanceScenarioName)
                resetWireMockScenarioState(assetsScenarioName)
                resetWireMockScenarioState(USER_TOKENS_API_SCENARIO)
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

            step("Open 'Send' screen") {
                openSendScreen(tokenName, mockContent = Wallet2WithDerivationsMockContent)
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
            step("Open fee selector bottom sheet via click on fee selector icon") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onSendConfirmScreen { feeSelectorIcon.performClick() }
                    onSendSelectNetworkFeeBottomSheet { regularFeeSelectorItem(fastSelectorItem).assertIsDisplayed() }
                }
            }
            step("Click on '$fastSelectorItem' selector item") {
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
        val userTokensScenarioState = "Wallet2"
        val dialogTitle = getResourceString(R.string.send_with_swap_change_token_alert_title)
        val dialogMessage = getResourceString(R.string.send_with_swap_change_token_alert_message)
        val addressHint = getResourceString(R.string.send_enter_address_field_ens)

        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(bitcoinBalanceScenarioName)
                resetWireMockScenarioState(assetsScenarioName)
                resetWireMockScenarioState(USER_TOKENS_API_SCENARIO)
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

            step("Open 'Send' screen") {
                openSendScreen(tokenName, mockContent = Wallet2WithDerivationsMockContent)
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

    @AllureId("3967")
    @DisplayName("Send via Swap: full successful send via swap flow")
    @Test
    fun sendViaSwapSuccessfulFlowTest() {
        val tokenName = "Bitcoin"
        val swapTokenName = "Ethereum"
        val main = "MAIN"
        val inputAmount = "0.001"
        val providerName = "SimpleSwap"
        val expressStatusItemTitle = getResourceString(R.string.express_exchange_by, providerName)
        val bitcoinBalanceScenarioName = "bitcoin_utxo"
        val bitcoinBalanceScenarioState = "BalanceHotWalletSvS"
        val assetsScenarioName = "express_api_assets"
        val assetsScenarioState = "BitcoinExchangeEnabled"
        val hotWalletScenarioState = "HotWalletSvS"
        val providersScenarioName = "networks_providers"
        val providersScenarioState = "HotWalletSvS"

        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(bitcoinBalanceScenarioName)
                resetWireMockScenarioState(assetsScenarioName)
                resetWireMockScenarioState(USER_TOKENS_API_SCENARIO)
                resetWireMockScenarioState(QUOTES_API_SCENARIO)
                resetWireMockScenarioState(providersScenarioName)
            }
        ).run {

            step("Set WireMock scenario: '$USER_TOKENS_API_SCENARIO' to state: '$hotWalletScenarioState'") {
                setWireMockScenarioState(scenarioName = USER_TOKENS_API_SCENARIO, state = hotWalletScenarioState)
            }
            step("Set WireMock scenario: '$QUOTES_API_SCENARIO' to state: '$tokenName'") {
                setWireMockScenarioState(scenarioName = QUOTES_API_SCENARIO, state = tokenName)
            }
            step("Set WireMock scenario: '$bitcoinBalanceScenarioName' to state: '$bitcoinBalanceScenarioState'") {
                setWireMockScenarioState(scenarioName = bitcoinBalanceScenarioName, state = bitcoinBalanceScenarioState)
            }
            step("Set WireMock scenario: '$assetsScenarioName' to state: '$assetsScenarioState'") {
                setWireMockScenarioState(scenarioName = assetsScenarioName, state = assetsScenarioState)
            }
            step("Set WireMock scenario: '$providersScenarioName' to state: '$providersScenarioState'") {
                setWireMockScenarioState(scenarioName = providersScenarioName, state = providersScenarioState)
            }

            step("Open 'Main Screen' with existing hot wallet") {
                openMainScreenWithExistingHotWallet(SVS_SEED_PHRASE_12)
            }
            step("Click on token with name: '$tokenName'") {
                onMainScreen { tokenWithTitleAndAddress(tokenName).clickWithAssertion() }
            }
            step("Select token to Send via Swap") {
                selectTokenToSendViaSwap(swapTokenName = swapTokenName, networkName = swapTokenName, networkType = main)
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
            step("Open 'Send via swap success' screen") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    openSendSuccessScreenViaLongClickOnSendButton()
                }
            }
            step("Check 'Send via swap' screen") {
                checkSendViaSwapSuccessScreen()
            }
            step("Click on 'Explore' button") {
                onSendSuccessScreen { exploreButton.performClick() }
            }
            step("Assert Chrome Browser is opened") {
                ThirdPartyAppPageObject { assertChromeIsOpened() }
            }
            step("Press 'Back' button to close 'Chrome' browser") {
                device.uiDevice.pressBack()
            }
            step("Click on 'Close' button") {
                onSendSuccessScreen { closeButton.performClick() }
            }
            step("Assert 'Express status' item is displayed with title: '$expressStatusItemTitle'") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onTokenDetailsScreen { expressStatusItem(expressStatusItemTitle).assertIsDisplayed() }
                }
            }
        }
    }

    @AllureId("4017")
    @DisplayName("Send via Swap: send same token in different network")
    @Test
    fun sendSameTokenInDifferentNetworkTest() {
        val tokenName = "Tether"
        val swapTokenName = "Tether"
        val networkName = "Polygon"
        val inputAmount = "0.001"
        val ethCallScenarioName = "eth_call_api"
        val ethCallScenarioState = "Started"
        val hotWalletScenarioState = "USDTHotWalletSvS"
        val ethNetworkBalanceScenarioName = "eth_network_balance"
        val ethNetworkBalanceScenarioState = "Started"
        val providerName = "Changelly"
        val expressStatusItemTitle = getResourceString(R.string.express_exchange_by, providerName)

        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(ethCallScenarioName)
                resetWireMockScenarioState(USER_TOKENS_API_SCENARIO)
                resetWireMockScenarioState(QUOTES_API_SCENARIO)
                resetWireMockScenarioState(ethNetworkBalanceScenarioName)
            }
        ).run {

            step("Set WireMock scenario: '$USER_TOKENS_API_SCENARIO' to state: '$hotWalletScenarioState'") {
                setWireMockScenarioState(scenarioName = USER_TOKENS_API_SCENARIO, state = hotWalletScenarioState)
            }
            step("Set WireMock scenario: '$QUOTES_API_SCENARIO' to state: '$hotWalletScenarioState'") {
                setWireMockScenarioState(scenarioName = QUOTES_API_SCENARIO, state = hotWalletScenarioState)
            }
            step("Set WireMock scenario: '$ethCallScenarioName' to state: '$ethCallScenarioState'") {
                setWireMockScenarioState(scenarioName = ethCallScenarioName, state = ethCallScenarioState)
            }
            step("Set WireMock scenario: '$ethNetworkBalanceScenarioName' to state: '$ethNetworkBalanceScenarioState'") {
                setWireMockScenarioState(scenarioName = ethNetworkBalanceScenarioName, state = ethNetworkBalanceScenarioState)
            }

            step("Open 'Main Screen' with existing hot wallet") {
                openMainScreenWithExistingHotWallet(SVS_SEED_PHRASE_12)
            }
            step("Click on token with name: '$tokenName'") {
                onMainScreen { tokenWithTitleAndAddress(tokenName).clickWithAssertion() }
            }
            step("Select token to Send via Swap") {
                selectTokenToSendViaSwap(swapTokenName = swapTokenName, networkName = networkName)
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
                onSendAddressScreen { addressTextField.performTextReplacement(POLYGON_RECIPIENT_ADDRESS) }
            }
            step("Open 'Send confirm' screen via 'Next' button") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    openSendConfirmScreenViaNextButton()
                }
            }
            step("Assert 'Best rate' badge is displayed") {
                onSendConfirmScreen { bestRateBadge.assertIsDisplayed() }
            }
            step("Open 'Send via swap success' screen") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    openSendSuccessScreenViaLongClickOnSendButton()
                }
            }
            step("Check 'Send via swap' screen") {
                checkSendViaSwapSuccessScreen()
            }
            step("Click on 'Close' button") {
                onSendSuccessScreen { closeButton.performClick() }
            }
            step("Assert 'Express status' item is displayed with title: '$expressStatusItemTitle'") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onTokenDetailsScreen { expressStatusItem(expressStatusItemTitle).assertIsDisplayed() }
                }
            }
        }
    }

    @AllureId("4545")
    @DisplayName("Send via Swap: token with tag/memo send via swap flow")
    @Test
    fun sendViaSwapTokenWithTagTest() {
        val tokenName = "XRP Ledger"
        val swapTokenName = "Ethereum"
        val main = "MAIN"
        val inputAmount = "0.001"
        val providerName = "Changelly"
        val expressStatusItemTitle = getResourceString(R.string.express_exchange_by, providerName)
        val hotWalletScenarioState = "XRPHotWalletSvS"
        val xrpExchangeQuoteScenarioName = "xrp_exchange_quote"
        val xrpExchangeDataScenarioName = "xrp_exchange_data"

        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(USER_TOKENS_API_SCENARIO)
                resetWireMockScenarioState(QUOTES_API_SCENARIO)
                resetWireMockScenarioState(xrpExchangeQuoteScenarioName)
                resetWireMockScenarioState(xrpExchangeDataScenarioName)
            }
        ).run {

            step("Set WireMock scenario: '$USER_TOKENS_API_SCENARIO' to state: '$hotWalletScenarioState'") {
                setWireMockScenarioState(scenarioName = USER_TOKENS_API_SCENARIO, state = hotWalletScenarioState)
            }
            step("Set WireMock scenario: '$QUOTES_API_SCENARIO' to state: '$hotWalletScenarioState'") {
                setWireMockScenarioState(scenarioName = QUOTES_API_SCENARIO, state = hotWalletScenarioState)
            }
            step("Set WireMock scenario: '$xrpExchangeQuoteScenarioName' to state: '$hotWalletScenarioState'") {
                setWireMockScenarioState(scenarioName = xrpExchangeQuoteScenarioName, state = hotWalletScenarioState)
            }
            step("Set WireMock scenario: '$xrpExchangeDataScenarioName' to state: '$hotWalletScenarioState'") {
                setWireMockScenarioState(scenarioName = xrpExchangeDataScenarioName, state = hotWalletScenarioState)
            }

            step("Open 'Main Screen' with existing hot wallet") {
                openMainScreenWithExistingHotWallet(SVS_SEED_PHRASE_12)
            }
            step("Click on token with name: '$tokenName'") {
                onMainScreen { tokenWithTitleAndAddress(tokenName).clickWithAssertion() }
            }
            step("Select token to Send via Swap") {
                selectTokenToSendViaSwap(swapTokenName = swapTokenName, networkName = swapTokenName, networkType = main)
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
                onSendAddressScreen { addressTextField.performTextReplacement(ETHEREUM_RECIPIENT_ADDRESS) }
            }
            step("Open 'Send confirm' screen via 'Next' button") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    openSendConfirmScreenViaNextButton()
                }
            }
            step("Open 'Send via swap success' screen") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    openSendSuccessScreenViaLongClickOnSendButton()
                }
            }
            step("Check 'Send via swap' screen") {
                checkSendViaSwapSuccessScreen()
            }
            step("Click on 'Close' button") {
                onSendSuccessScreen { closeButton.performClick() }
            }
            step("Assert 'Express status' item is displayed with title: '$expressStatusItemTitle'") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onTokenDetailsScreen { expressStatusItem(expressStatusItemTitle).assertIsDisplayed() }
                }
            }
        }
    }
}