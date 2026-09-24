package com.tangem.tests.swap

import androidx.test.InstrumentationRegistry.getTargetContext
import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.QUOTES_API_SCENARIO
import com.tangem.common.constants.TestConstants.SVS_SEED_PHRASE_12
import com.tangem.common.constants.TestConstants.USER_TOKENS_API_SCENARIO
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_LONG
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_VERY_LONG
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.extensions.disableMobileData
import com.tangem.common.extensions.disableWiFi
import com.tangem.common.extensions.enableMobileData
import com.tangem.common.extensions.enableWiFi
import com.tangem.common.extensions.stopApp
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.scenarios.openTokenDetailsFromTokenList
import com.tangem.scenarios.awaitCryptoPortfolioAccount
import com.tangem.scenarios.closeFirstTimeSwapStoriesIfShown
import com.tangem.scenarios.openAppByDeepLink
import com.tangem.scenarios.openMainScreenWithExistingHotWallet
import com.tangem.screens.onSwapTokenScreen
import com.tangem.screens.onTokenDetailsScreen
import dagger.hilt.android.testing.HiltAndroidTest
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Test

@HiltAndroidTest
class DeeplinksTest : BaseTestCase() {

    private val fromTokenSymbol = "POL"
    private val toTokenSymbol = "ETH"
    private val providerName = "1Inch"
    private val userTokensState = "HotWalletSvS"
    private val quotesState = "PolygonUSDC"
    private val assetsScenarioName = "express_api_assets"
    private val assetsExchangeEnabledState = "BitcoinExchangeEnabled"
    private val pairsScenarioName = "ethereum_from_pairs"
    private val dexProviderState = "DexProvider"
    private val quoteScenarioName = "exchange_quote_ethereum"
    private val quoteUnreachableState = "Unreachable"

    @AllureId("10913")
    @DisplayName("Deeplinks: full set of valid params opens Swap prefilled")
    @Test
    fun fullValidParamsTest() {
        val amount = "0.0008"

        setupHooks().run {
            step("Set WireMock scenario '$USER_TOKENS_API_SCENARIO' to '$userTokensState'") {
                setWireMockScenarioState(scenarioName = USER_TOKENS_API_SCENARIO, state = userTokensState)
            }
            step("Set WireMock scenario '$QUOTES_API_SCENARIO' to '$quotesState'") {
                setWireMockScenarioState(scenarioName = QUOTES_API_SCENARIO, state = quotesState)
            }
            step("Set WireMock scenario '$assetsScenarioName' to '$assetsExchangeEnabledState'") {
                setWireMockScenarioState(scenarioName = assetsScenarioName, state = assetsExchangeEnabledState)
            }
            step("Set WireMock scenario '$pairsScenarioName' to '$dexProviderState'") {
                setWireMockScenarioState(scenarioName = pairsScenarioName, state = dexProviderState)
            }
            step("Open 'Main' screen with existing hot wallet") {
                openMainScreenWithExistingHotWallet(SVS_SEED_PHRASE_12)
            }

            var deeplinkUri = ""
            step("Resolve the wallet and its main account id from the domain model") {
                val userWallet = requireNotNull(getSelectedWalletSyncUseCase().getOrNull()) {
                    "No selected wallet found"
                }
                val account = awaitCryptoPortfolioAccount(derivationIndex = 0)
                deeplinkUri = "tangem://swap" +
                    "?from_token_id=polygon-ecosystem-token&from_network_id=ethereum" +
                    "&to_token_id=ethereum&to_network_id=ethereum" +
                    "&from_amount=$amount&provider_id=1inch" +
                    "&user_wallet_id=${userWallet.walletId.stringValue}" +
                    "&from_user_account_id=${account.accountId.value}" +
                    "&to_user_account_id=${account.accountId.value}"
            }
            step("Open the deeplink with the full set of valid params") {
                openAppByDeepLink(deeplinkUri)
            }
            step("Close 'Stories' screen if shown") { closeFirstTimeSwapStoriesIfShown() }
            step("Assert Swap screen opened with FROM token '$fromTokenSymbol'") {
                onSwapTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { swapTokenSymbol(fromTokenSymbol).assertIsDisplayed() }
                }
            }
            step("Assert TO token '$toTokenSymbol'") {
                onSwapTokenScreen { receiveTokenSymbol(toTokenSymbol).assertIsDisplayed() }
            }
            step("Assert amount is prefilled with '$amount'") {
                onSwapTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { textInput.assertTextEquals(amount) }
                }
            }
            step("Assert provider '$providerName' is preselected") {
                onSwapTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { providerWithName(providerName).assertIsDisplayed() }
                }
            }
        }
    }

    @AllureId("10914")
    @DisplayName("Deeplinks: FROM/TO pair without amount opens Swap with an empty amount field")
    @Test
    fun pairWithoutAmountTest() {
        val emptyAmountPlaceholder = "0.00"

        setupHooks().run {
            step("Set WireMock scenario '$USER_TOKENS_API_SCENARIO' to '$userTokensState'") {
                setWireMockScenarioState(scenarioName = USER_TOKENS_API_SCENARIO, state = userTokensState)
            }
            step("Set WireMock scenario '$QUOTES_API_SCENARIO' to '$quotesState'") {
                setWireMockScenarioState(scenarioName = QUOTES_API_SCENARIO, state = quotesState)
            }
            step("Set WireMock scenario '$assetsScenarioName' to '$assetsExchangeEnabledState'") {
                setWireMockScenarioState(scenarioName = assetsScenarioName, state = assetsExchangeEnabledState)
            }
            step("Set WireMock scenario '$pairsScenarioName' to '$dexProviderState'") {
                setWireMockScenarioState(scenarioName = pairsScenarioName, state = dexProviderState)
            }
            step("Open 'Main' screen with existing hot wallet") {
                openMainScreenWithExistingHotWallet(SVS_SEED_PHRASE_12)
            }
            step("Open the deeplink with only the FROM/TO pair, no amount") {
                openAppByDeepLink(
                    "tangem://swap" +
                        "?from_token_id=polygon-ecosystem-token&from_network_id=ethereum" +
                        "&to_token_id=ethereum&to_network_id=ethereum",
                )
            }
            step("Close 'Stories' screen if shown") { closeFirstTimeSwapStoriesIfShown() }
            step("Assert Swap screen opened with FROM token '$fromTokenSymbol'") {
                onSwapTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { swapTokenSymbol(fromTokenSymbol).assertIsDisplayed() }
                }
            }
            step("Assert TO token '$toTokenSymbol'") {
                onSwapTokenScreen { receiveTokenSymbol(toTokenSymbol).assertIsDisplayed() }
            }
            step("Assert amount field shows the empty '$emptyAmountPlaceholder' placeholder") {
                onSwapTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { textInput.assertTextEquals(emptyAmountPlaceholder) }
                }
            }
        }
    }

    @AllureId("10917")
    @DisplayName("Deeplinks: from_amount greater than balance shows the 'Insufficient funds' warning")
    @Test
    fun amountGreaterThanBalanceTest() {
        val amount = "999999"
        val formattedAmount = "999,999"

        setupHooks().run {
            step("Set WireMock scenario '$USER_TOKENS_API_SCENARIO' to '$userTokensState'") {
                setWireMockScenarioState(scenarioName = USER_TOKENS_API_SCENARIO, state = userTokensState)
            }
            step("Set WireMock scenario '$QUOTES_API_SCENARIO' to '$quotesState'") {
                setWireMockScenarioState(scenarioName = QUOTES_API_SCENARIO, state = quotesState)
            }
            step("Set WireMock scenario '$assetsScenarioName' to '$assetsExchangeEnabledState'") {
                setWireMockScenarioState(scenarioName = assetsScenarioName, state = assetsExchangeEnabledState)
            }
            step("Set WireMock scenario '$pairsScenarioName' to '$dexProviderState'") {
                setWireMockScenarioState(scenarioName = pairsScenarioName, state = dexProviderState)
            }
            step("Open 'Main' screen with existing hot wallet") {
                openMainScreenWithExistingHotWallet(SVS_SEED_PHRASE_12)
            }
            step("Open the deeplink with 'from_amount' greater than the wallet balance") {
                openAppByDeepLink(
                    "tangem://swap" +
                        "?from_token_id=polygon-ecosystem-token&from_network_id=ethereum" +
                        "&to_token_id=ethereum&to_network_id=ethereum" +
                        "&from_amount=$amount",
                )
            }
            step("Close 'Stories' screen if shown") { closeFirstTimeSwapStoriesIfShown() }
            step("Assert Swap screen opened with FROM token '$fromTokenSymbol'") {
                onSwapTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { swapTokenSymbol(fromTokenSymbol).assertIsDisplayed() }
                }
            }
            step("Assert TO token '$toTokenSymbol'") {
                onSwapTokenScreen { receiveTokenSymbol(toTokenSymbol).assertIsDisplayed() }
            }
            step("Assert amount is prefilled with '$formattedAmount'") {
                onSwapTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { textInput.assertTextEquals(formattedAmount) }
                }
            }
            step("Assert 'Insufficient funds' warning is displayed") {
                onSwapTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { insufficientFundsErrorTitle.assertIsDisplayed() }
                }
            }
        }
    }

    @AllureId("10921")
    @DisplayName("Deeplinks: invalid user_wallet_id is treated as absent, Swap opens for the current wallet")
    @Test
    fun invalidUserWalletIdTest() {
        setupHooks().run {
            step("Set WireMock scenario '$USER_TOKENS_API_SCENARIO' to '$userTokensState'") {
                setWireMockScenarioState(scenarioName = USER_TOKENS_API_SCENARIO, state = userTokensState)
            }
            step("Set WireMock scenario '$assetsScenarioName' to '$assetsExchangeEnabledState'") {
                setWireMockScenarioState(scenarioName = assetsScenarioName, state = assetsExchangeEnabledState)
            }
            step("Open 'Main' screen with existing hot wallet") {
                openMainScreenWithExistingHotWallet(SVS_SEED_PHRASE_12)
            }
            step("Open the deeplink with a non-existent 'user_wallet_id'") {
                openAppByDeepLink(
                    "tangem://swap?user_wallet_id=C10EC74B93FD8195BC0A0ACF7D6C2795C056485C47DC706698F0461CCBFD9EF5",
                )
            }
            step("Close 'Stories' screen if shown") { closeFirstTimeSwapStoriesIfShown() }
            step("Assert Swap screen opened for the current wallet without a crash or an error") {
                onSwapTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { title.assertIsDisplayed() }
                    errorNotificationTitle.assertDoesNotExist()
                }
            }
        }
    }

    @AllureId("10925")
    @DisplayName("Deeplinks: opening a deeplink while the app is fully closed cold-starts into Swap prefilled")
    @Test
    fun deeplinkWhileAppKilledTest() {
        val amount = "0.0008"

        setupHooks().run {
            step("Set WireMock scenario '$USER_TOKENS_API_SCENARIO' to '$userTokensState'") {
                setWireMockScenarioState(scenarioName = USER_TOKENS_API_SCENARIO, state = userTokensState)
            }
            step("Set WireMock scenario '$QUOTES_API_SCENARIO' to '$quotesState'") {
                setWireMockScenarioState(scenarioName = QUOTES_API_SCENARIO, state = quotesState)
            }
            step("Set WireMock scenario '$assetsScenarioName' to '$assetsExchangeEnabledState'") {
                setWireMockScenarioState(scenarioName = assetsScenarioName, state = assetsExchangeEnabledState)
            }
            step("Set WireMock scenario '$pairsScenarioName' to '$dexProviderState'") {
                setWireMockScenarioState(scenarioName = pairsScenarioName, state = dexProviderState)
            }
            step("Open 'Main' screen with existing hot wallet") {
                openMainScreenWithExistingHotWallet(SVS_SEED_PHRASE_12)
            }
            step("Kill the app") {
                stopApp(getTargetContext().packageName)
            }
            step("Open the deeplink while the app is fully closed (cold start)") {
                openAppByDeepLink(
                    "tangem://swap" +
                        "?from_token_id=polygon-ecosystem-token&from_network_id=ethereum" +
                        "&to_token_id=ethereum&to_network_id=ethereum" +
                        "&from_amount=$amount&provider_id=1inch",
                )
            }
            step("Close 'Stories' screen if shown") { closeFirstTimeSwapStoriesIfShown() }
            step("Assert Swap screen opened with FROM token '$fromTokenSymbol'") {
                onSwapTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_VERY_LONG) { swapTokenSymbol(fromTokenSymbol).assertIsDisplayed() }
                }
            }
            step("Assert TO token '$toTokenSymbol'") {
                onSwapTokenScreen { receiveTokenSymbol(toTokenSymbol).assertIsDisplayed() }
            }
            step("Assert amount is prefilled with '$amount'") {
                onSwapTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_VERY_LONG) { textInput.assertTextEquals(amount) }
                }
            }
            step("Assert provider '$providerName' is preselected") {
                onSwapTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_VERY_LONG) { providerWithName(providerName).assertIsDisplayed() }
                }
            }
        }
    }

    @AllureId("10928")
    @DisplayName("Deeplinks: opening a deeplink while already on Swap updates the screen in place")
    @Test
    fun deeplinkFromSwapScreenUpdatesInPlaceTest() {
        val firstAmount = "0.0008"
        val secondAmount = "0.002"

        setupHooks().run {
            step("Set WireMock scenario '$USER_TOKENS_API_SCENARIO' to '$userTokensState'") {
                setWireMockScenarioState(scenarioName = USER_TOKENS_API_SCENARIO, state = userTokensState)
            }
            step("Set WireMock scenario '$QUOTES_API_SCENARIO' to '$quotesState'") {
                setWireMockScenarioState(scenarioName = QUOTES_API_SCENARIO, state = quotesState)
            }
            step("Set WireMock scenario '$assetsScenarioName' to '$assetsExchangeEnabledState'") {
                setWireMockScenarioState(scenarioName = assetsScenarioName, state = assetsExchangeEnabledState)
            }
            step("Set WireMock scenario '$pairsScenarioName' to '$dexProviderState'") {
                setWireMockScenarioState(scenarioName = pairsScenarioName, state = dexProviderState)
            }
            step("Open 'Main' screen with existing hot wallet") {
                openMainScreenWithExistingHotWallet(SVS_SEED_PHRASE_12)
            }
            step("Open the first deeplink with amount '$firstAmount'") {
                openAppByDeepLink(
                    "tangem://swap" +
                        "?from_token_id=polygon-ecosystem-token&from_network_id=ethereum" +
                        "&to_token_id=ethereum&to_network_id=ethereum" +
                        "&from_amount=$firstAmount&provider_id=1inch",
                )
            }
            step("Close 'Stories' screen if shown") { closeFirstTimeSwapStoriesIfShown() }
            step("Assert Swap screen opened with amount '$firstAmount'") {
                onSwapTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { textInput.assertTextEquals(firstAmount) }
                }
            }
            step("Open a second deeplink with amount '$secondAmount' while already on Swap") {
                openAppByDeepLink(
                    "tangem://swap" +
                        "?from_token_id=polygon-ecosystem-token&from_network_id=ethereum" +
                        "&to_token_id=ethereum&to_network_id=ethereum" +
                        "&from_amount=$secondAmount&provider_id=1inch",
                )
            }
            step("Assert the current Swap screen updated to FROM token '$fromTokenSymbol'") {
                onSwapTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { swapTokenSymbol(fromTokenSymbol).assertIsDisplayed() }
                }
            }
            step("Assert TO token '$toTokenSymbol'") {
                onSwapTokenScreen { receiveTokenSymbol(toTokenSymbol).assertIsDisplayed() }
            }
            step("Assert amount updated in place to '$secondAmount'") {
                onSwapTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { textInput.assertTextEquals(secondAmount) }
                }
            }
        }
    }

    @AllureId("10924")
    @DisplayName("Deeplinks: tapping a clickable in-app banner opens Swap the same way an external deeplink does")
    @Test
    fun deeplinkThroughClickableBannerTest() {
        val tokenDetailsTitle = "Ethereum"
        val bannerText = "Swap POL for ETH"
        val bannerScenarioName = "marketing_campaigns_token_details"
        val bannerActiveState = "TokenDetailsSwapBanner"
        val amount = "0.0008"

        setupHooks().run {
            step("Set WireMock scenario '$USER_TOKENS_API_SCENARIO' to '$userTokensState'") {
                setWireMockScenarioState(scenarioName = USER_TOKENS_API_SCENARIO, state = userTokensState)
            }
            step("Set WireMock scenario '$QUOTES_API_SCENARIO' to '$quotesState'") {
                setWireMockScenarioState(scenarioName = QUOTES_API_SCENARIO, state = quotesState)
            }
            step("Set WireMock scenario '$assetsScenarioName' to '$assetsExchangeEnabledState'") {
                setWireMockScenarioState(scenarioName = assetsScenarioName, state = assetsExchangeEnabledState)
            }
            step("Set WireMock scenario '$pairsScenarioName' to '$dexProviderState'") {
                setWireMockScenarioState(scenarioName = pairsScenarioName, state = dexProviderState)
            }
            step("Set WireMock scenario '$bannerScenarioName' to '$bannerActiveState'") {
                setWireMockScenarioState(scenarioName = bannerScenarioName, state = bannerActiveState)
            }
            step("Open 'Main' screen with existing hot wallet") {
                openMainScreenWithExistingHotWallet(SVS_SEED_PHRASE_12)
            }
            step("Open '$tokenDetailsTitle' token details") {
                openTokenDetailsFromTokenList(tokenDetailsTitle)
            }
            step("Assert the marketing banner is displayed") {
                onTokenDetailsScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { marketingBanner(bannerText).assertIsDisplayed() }
                }
            }
            step("Tap the marketing banner") {
                onTokenDetailsScreen { marketingBanner(bannerText).clickWithAssertion() }
            }
            step("Close 'Stories' screen if shown") { closeFirstTimeSwapStoriesIfShown() }
            step("Assert Swap screen opened with FROM token '$fromTokenSymbol'") {
                onSwapTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { swapTokenSymbol(fromTokenSymbol).assertIsDisplayed() }
                }
            }
            step("Assert TO token '$toTokenSymbol'") {
                onSwapTokenScreen { receiveTokenSymbol(toTokenSymbol).assertIsDisplayed() }
            }
            step("Assert amount is prefilled with '$amount'") {
                onSwapTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { textInput.assertTextEquals(amount) }
                }
            }
            step("Assert provider '$providerName' is preselected") {
                onSwapTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { providerWithName(providerName).assertIsDisplayed() }
                }
            }
        }
    }

    @AllureId("11563")
    @DisplayName("Deeplinks: opening a deeplink offline shows Swap with the prefilled pair and a network error")
    @Test
    fun deeplinkWhileOfflineTest() {
        val amount = "0.0008"

        setupHooks(
            additionalAfterSection = {
                enableWiFi()
                enableMobileData()
            },
        ).run {
            step("Set WireMock scenario '$USER_TOKENS_API_SCENARIO' to '$userTokensState'") {
                setWireMockScenarioState(scenarioName = USER_TOKENS_API_SCENARIO, state = userTokensState)
            }
            step("Set WireMock scenario '$assetsScenarioName' to '$assetsExchangeEnabledState'") {
                setWireMockScenarioState(scenarioName = assetsScenarioName, state = assetsExchangeEnabledState)
            }
            // adb reverse keeps WireMock up with the radios off; only the quote may fail, or the pair stops rendering.
            step("Set WireMock scenario '$quoteScenarioName' to '$quoteUnreachableState'") {
                setWireMockScenarioState(scenarioName = quoteScenarioName, state = quoteUnreachableState)
            }
            step("Open 'Main' screen with existing hot wallet") {
                openMainScreenWithExistingHotWallet(SVS_SEED_PHRASE_12)
            }
            step("Turn off Wi-Fi and Mobile Data") {
                disableWiFi()
                disableMobileData()
            }
            step("Open the deeplink while offline") {
                openAppByDeepLink(
                    "tangem://swap" +
                        "?from_token_id=polygon-ecosystem-token&from_network_id=ethereum" +
                        "&to_token_id=ethereum&to_network_id=ethereum" +
                        "&from_amount=$amount",
                )
            }
            step("Close 'Stories' screen if shown") { closeFirstTimeSwapStoriesIfShown() }
            step("Assert Swap screen opened with FROM token '$fromTokenSymbol'") {
                onSwapTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { swapTokenSymbol(fromTokenSymbol).assertIsDisplayed() }
                }
            }
            step("Assert TO token '$toTokenSymbol'") {
                onSwapTokenScreen { receiveTokenSymbol(toTokenSymbol).assertIsDisplayed() }
            }
            step("Assert amount is prefilled with '$amount'") {
                onSwapTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { textInput.assertTextEquals(amount) }
                }
            }
            step("Assert network error notification is displayed") {
                onSwapTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { errorNotificationTitle.assertIsDisplayed() }
                }
            }
            step("Assert 'Refresh' button is displayed") {
                onSwapTokenScreen { refreshButton.assertIsDisplayed() }
            }
        }
    }
}