package com.tangem.tests.swap

import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.MORALIS_EVM_TOKEN_BALANCES_API_SCENARIO
import com.tangem.common.constants.TestConstants.PROVIDERS_API_SCENARIO
import com.tangem.common.constants.TestConstants.SVS_SEED_PHRASE_12
import com.tangem.common.constants.TestConstants.USER_TOKENS_API_SCENARIO
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_LONG
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.scenarios.SwapEntryPoint
import com.tangem.scenarios.chooseReceiveToken
import com.tangem.scenarios.confirmSwapByHolding
import com.tangem.scenarios.inputAmount
import com.tangem.scenarios.openMainScreenWithExistingHotWallet
import com.tangem.scenarios.openSwapScreen
import com.tangem.scenarios.openTokenDetailsFromTokenList
import com.tangem.scenarios.waitForFeeDisplayed
import com.tangem.screens.onChooseProviderBottomSheet
import com.tangem.screens.onMainScreen
import com.tangem.screens.onSwapSuccessScreen
import com.tangem.screens.onSwapTokenScreen
import com.tangem.screens.onTokenDetailsScreen
import dagger.hilt.android.testing.HiltAndroidTest
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Test

@HiltAndroidTest
class SwapYieldModeTest : BaseTestCase() {

    private val fromTokenName = "USDC"
    private val receiveTokenName = "Ethereum"
    private val inputAmount = "100"
    private val dexProviderName = "1Inch"
    private val cexProviderName = "Changelly"
    private val providersPairState = "EthereumProviderPair"
    private val userTokensState = "YieldUSDCEthereum"
    private val balancesState = "NonZeroEvmBalances"
    private val yieldScenarioName = "yield_supply_status"
    private val yieldActiveState = "ApproveGranted"
    private val assetsScenarioName = "express_api_assets"
    private val assetsState = "YieldUSDCEthereumExchangeEnabled"
    private val pairsScenarioName = "ethereum_pairs"
    private val quoteScenarioName = "exchange_quote_ethereum"
    private val yieldUsdcState = "YieldUSDC"
    private val yieldSwapScenarioName = "yield_swap"
    private val yieldSwapEnabledState = "Enabled"
    private val fromTokenSymbol = "USDC"
    private val fiatSign = "$"
    private val gaslessTokensScenarioName = "gasless_tokens"
    private val gaslessEthereumUsdcState = "WithEthereumUSDC"
    private val nativeBalanceScenarioName = "eth_network_balance"
    private val zeroNativeBalanceState = "Empty"
    private val zeroFiatBalance = "$0.00"

    @AllureId("11521")
    @DisplayName("Swap: a token with Yield mode enabled is swapped via CEX or DEX without the approve settings")
    @Test
    fun swapYieldModeTokenTest() {
        setupHooks(
            additionalBeforeAppLaunchSection = {
                setWireMockScenarioState(scenarioName = PROVIDERS_API_SCENARIO, state = providersPairState)
            },
        ).run {
            step("Set WireMock scenario '$USER_TOKENS_API_SCENARIO' to '$userTokensState'") {
                setWireMockScenarioState(scenarioName = USER_TOKENS_API_SCENARIO, state = userTokensState)
            }
            step("Set WireMock scenario '$MORALIS_EVM_TOKEN_BALANCES_API_SCENARIO' to '$balancesState'") {
                setWireMockScenarioState(scenarioName = MORALIS_EVM_TOKEN_BALANCES_API_SCENARIO, state = balancesState)
            }
            step("Set WireMock scenario '$yieldScenarioName' to '$yieldActiveState'") {
                setWireMockScenarioState(scenarioName = yieldScenarioName, state = yieldActiveState)
            }
            step("Set WireMock scenario '$assetsScenarioName' to '$assetsState'") {
                setWireMockScenarioState(scenarioName = assetsScenarioName, state = assetsState)
            }
            step("Set WireMock scenario '$pairsScenarioName' to '$yieldUsdcState'") {
                setWireMockScenarioState(scenarioName = pairsScenarioName, state = yieldUsdcState)
            }
            step("Set WireMock scenario '$quoteScenarioName' to '$yieldUsdcState'") {
                setWireMockScenarioState(scenarioName = quoteScenarioName, state = yieldUsdcState)
            }
            step("Set WireMock scenario '$yieldSwapScenarioName' to '$yieldSwapEnabledState'") {
                setWireMockScenarioState(scenarioName = yieldSwapScenarioName, state = yieldSwapEnabledState)
            }
            step("Open 'Main' screen with existing hot wallet") {
                openMainScreenWithExistingHotWallet(SVS_SEED_PHRASE_12)
            }
            step("Click on token with name: '$fromTokenName'") {
                openTokenDetailsFromTokenList(fromTokenName)
            }
            step("Assert Yield mode is enabled for '$fromTokenName'") {
                onTokenDetailsScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { yieldModeConnectedTitle.assertIsDisplayed() }
                }
            }
            step("Open 'Swap' screen") {
                openSwapScreen(from = SwapEntryPoint.TokenDetails)
            }
            step("Choose receive token '$receiveTokenName'") {
                chooseReceiveToken(receiveTokenName)
            }
            step("Input swap amount '$inputAmount'") {
                inputAmount(inputAmount)
            }
            step("Wait for the quote and fee to load") {
                waitForFeeDisplayed()
            }
            step("Click on 'Providers' block") {
                onSwapTokenScreen { providersBlock.performClick() }
            }
            step("Assert CEX provider '$cexProviderName' is displayed") {
                onChooseProviderBottomSheet {
                    flakySafely(WAIT_UNTIL_TIMEOUT) { providerItem(cexProviderName).assertIsDisplayed() }
                }
            }
            step("Assert DEX provider '$dexProviderName' is displayed") {
                onChooseProviderBottomSheet { providerItem(dexProviderName).assertIsDisplayed() }
            }
            step("Assert the approve settings button of '$dexProviderName' is not displayed") {
                onChooseProviderBottomSheet { approvalSettingsButton(dexProviderName).assertIsNotDisplayed() }
            }
            step("Click on provider '$dexProviderName'") {
                onChooseProviderBottomSheet { providerItem(dexProviderName).clickWithAssertion() }
            }
            step("Assert the provider '$dexProviderName' is selected") {
                onSwapTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { providerNameText.assertTextContains(dexProviderName) }
                }
            }
            step("Assert 'Swap' button is available") {
                onSwapTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { swapButtonWithLabel.assertIsEnabled() }
                }
            }
            step("Confirm the swap by holding the 'Swap' button and sign") {
                confirmSwapByHolding()
            }
            step("Assert the 'Swap in progress' screen is displayed") {
                onSwapSuccessScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { title.assertIsDisplayed() }
                }
            }
        }
    }

    @AllureId("11522")
    @DisplayName("Swap: a Yield mode token is swapped without the native coin, the fee is paid in the sent token via Gasless")
    @Test
    fun swapYieldModeTokenWithGaslessFeeTest() {
        setupHooks(
            additionalBeforeAppLaunchSection = {
                setWireMockScenarioState(scenarioName = PROVIDERS_API_SCENARIO, state = providersPairState)
                setWireMockScenarioState(scenarioName = gaslessTokensScenarioName, state = gaslessEthereumUsdcState)
            },
        ).run {
            step("Set WireMock scenario '$USER_TOKENS_API_SCENARIO' to '$userTokensState'") {
                setWireMockScenarioState(scenarioName = USER_TOKENS_API_SCENARIO, state = userTokensState)
            }
            step("Set WireMock scenario '$MORALIS_EVM_TOKEN_BALANCES_API_SCENARIO' to '$balancesState'") {
                setWireMockScenarioState(scenarioName = MORALIS_EVM_TOKEN_BALANCES_API_SCENARIO, state = balancesState)
            }
            step("Set WireMock scenario '$nativeBalanceScenarioName' to '$zeroNativeBalanceState'") {
                setWireMockScenarioState(scenarioName = nativeBalanceScenarioName, state = zeroNativeBalanceState)
            }
            step("Set WireMock scenario '$yieldScenarioName' to '$yieldActiveState'") {
                setWireMockScenarioState(scenarioName = yieldScenarioName, state = yieldActiveState)
            }
            step("Set WireMock scenario '$assetsScenarioName' to '$assetsState'") {
                setWireMockScenarioState(scenarioName = assetsScenarioName, state = assetsState)
            }
            step("Set WireMock scenario '$pairsScenarioName' to '$yieldUsdcState'") {
                setWireMockScenarioState(scenarioName = pairsScenarioName, state = yieldUsdcState)
            }
            step("Set WireMock scenario '$quoteScenarioName' to '$yieldUsdcState'") {
                setWireMockScenarioState(scenarioName = quoteScenarioName, state = yieldUsdcState)
            }
            step("Set WireMock scenario '$yieldSwapScenarioName' to '$yieldSwapEnabledState'") {
                setWireMockScenarioState(scenarioName = yieldSwapScenarioName, state = yieldSwapEnabledState)
            }
            step("Open 'Main' screen with existing hot wallet") {
                openMainScreenWithExistingHotWallet(SVS_SEED_PHRASE_12)
            }
            step("Assert '$receiveTokenName' balance is '$zeroFiatBalance'") {
                onMainScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                        tokenFiatAmountText(receiveTokenName).assertTextEquals(zeroFiatBalance)
                    }
                }
            }
            step("Click on token with name: '$fromTokenName'") {
                openTokenDetailsFromTokenList(fromTokenName)
            }
            step("Assert Yield mode is enabled for '$fromTokenName'") {
                onTokenDetailsScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { yieldModeConnectedTitle.assertIsDisplayed() }
                }
            }
            step("Open 'Swap' screen") {
                openSwapScreen(from = SwapEntryPoint.TokenDetails)
            }
            step("Choose receive token '$receiveTokenName'") {
                chooseReceiveToken(receiveTokenName)
            }
            step("Input swap amount '$inputAmount'") {
                inputAmount(inputAmount)
            }
            step("Wait for the quote and fee to load") {
                waitForFeeDisplayed()
            }
            step("Click on 'Providers' block") {
                onSwapTokenScreen { providersBlock.performClick() }
            }
            step("Click on provider '$cexProviderName'") {
                onChooseProviderBottomSheet {
                    flakySafely(WAIT_UNTIL_TIMEOUT) { providerItem(cexProviderName).clickWithAssertion() }
                }
            }
            step("Assert the provider '$cexProviderName' is selected") {
                onSwapTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { providerNameText.assertTextContains(cexProviderName) }
                }
            }
            step("Assert the network fee is paid in '$fromTokenSymbol'") {
                onSwapTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { feeBlockCurrency(fromTokenSymbol).assertIsDisplayed() }
                }
            }
            step("Assert the network fee amount is displayed") {
                onSwapTokenScreen { feeAmount.assertTextContains(fiatSign, substring = true) }
            }
            step("Assert 'Swap' button is available without the native coin balance") {
                onSwapTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { swapButtonWithLabel.assertIsEnabled() }
                }
            }
            step("Confirm the swap by holding the 'Swap' button and sign") {
                confirmSwapByHolding()
            }
            step("Assert the 'Swap in progress' screen is displayed") {
                onSwapSuccessScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { title.assertIsDisplayed() }
                }
            }
        }
    }
}