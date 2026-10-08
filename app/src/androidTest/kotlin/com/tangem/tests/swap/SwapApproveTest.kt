package com.tangem.tests.swap

import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.QUOTES_API_SCENARIO
import com.tangem.common.constants.TestConstants.SVS_SEED_PHRASE_12
import com.tangem.common.constants.TestConstants.USER_TOKENS_API_SCENARIO
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_LONG
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.utils.UNLIMITED_APPROVE_AMOUNT
import com.tangem.common.utils.assertUrlOpenedStartingWith
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.common.utils.stubExternalUrlOpening
import com.tangem.scenarios.SwapEntryPoint
import com.tangem.scenarios.chooseReceiveToken
import com.tangem.scenarios.confirmSwapAndAssertApproveBroadcast
import com.tangem.scenarios.inputAmount
import com.tangem.scenarios.openMainScreenWithExistingHotWallet
import com.tangem.scenarios.openSwapScreen
import com.tangem.scenarios.openTokenDetailsFromTokenList
import com.tangem.scenarios.waitForFeeDisplayed
import com.tangem.screens.onChooseProviderBottomSheet
import com.tangem.screens.onSelectApprovalTypeBottomSheet
import com.tangem.screens.onSwapSuccessScreen
import com.tangem.screens.onSwapTokenScreen
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.kakaocup.kakao.common.utilities.getResourceString
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Test
import java.math.BigInteger
import com.tangem.core.ui.R as CoreUiR

@HiltAndroidTest
class SwapApproveTest : BaseTestCase() {

    private val fromTokenName = "POL (ex-MATIC)"
    private val fromTokenSymbol = "POL"
    private val receiveTokenName = "Ethereum"
    private val inputAmount = "50"
    private val inputAmountInWei = BigInteger.TEN.pow(18).multiply(BigInteger(inputAmount))
    private val dexProviderName = "1Inch"
    private val dexSpenderAddress = "0x1111111254eeb25477b68fb85ed929f73a960582"
    private val ethereumRpcPathPattern = "/ethereum/?"
    private val permissionArticleUrl = "https://tangem.com/embed/blog/post/give-revoke-permission"
    private val limitedApproveTitle = getResourceString(CoreUiR.string.give_permission_current_transaction)
    private val unlimitedApproveTitle = getResourceString(CoreUiR.string.give_permission_unlimited)
    private val userTokensState = "HotWalletSvS"
    private val quotesState = "PolygonUSDC"
    private val assetsScenarioName = "express_api_assets"
    private val assetsExchangeEnabledState = "BitcoinExchangeEnabled"
    private val quoteScenarioName = "exchange_quote_ethereum"
    private val approvalRequiredQuoteState = "OneInchApprovalRequired"
    private val allowanceScenarioName = "swap_pol_1inch_allowance"
    private val notApprovedState = "NotApproved"

    @AllureId("11519")
    @DisplayName("Swap: the approve settings of a DEX provider offer an approve for the operation amount or unlimited")
    @Test
    fun approveSettingsUiTest() {
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
            step("Set WireMock scenario '$quoteScenarioName' to '$approvalRequiredQuoteState'") {
                setWireMockScenarioState(scenarioName = quoteScenarioName, state = approvalRequiredQuoteState)
            }
            step("Set WireMock scenario '$allowanceScenarioName' to '$notApprovedState'") {
                setWireMockScenarioState(scenarioName = allowanceScenarioName, state = notApprovedState)
            }
            step("Open 'Main' screen with existing hot wallet") {
                openMainScreenWithExistingHotWallet(SVS_SEED_PHRASE_12)
            }
            step("Click on token with name: '$fromTokenName'") {
                openTokenDetailsFromTokenList(fromTokenName)
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
            step("Wait for the '$dexProviderName' quote and fee to load") {
                waitForFeeDisplayed()
            }
            step("Click on 'Providers' block") {
                onSwapTokenScreen { providersBlock.performClick() }
            }
            step("Assert the approve settings button of '$dexProviderName' is displayed") {
                onChooseProviderBottomSheet {
                    flakySafely(WAIT_UNTIL_TIMEOUT) { approvalSettingsButton(dexProviderName).assertIsDisplayed() }
                }
            }
            step("Click on the approve settings button of '$dexProviderName'") {
                onChooseProviderBottomSheet { approvalSettingsButton(dexProviderName).performClick() }
            }
            step("Assert the 'Give Permission' title is displayed") {
                onSelectApprovalTypeBottomSheet {
                    flakySafely(WAIT_UNTIL_TIMEOUT) { title.assertIsDisplayed() }
                }
            }
            step("Assert the DEX permission explanation is displayed") {
                onSelectApprovalTypeBottomSheet { subtitle.assertIsDisplayed() }
            }
            step("Assert the 'Learn more' link is displayed") {
                onSelectApprovalTypeBottomSheet { learnMoreLink.assertIsDisplayed() }
            }
            step("Stub opening external URLs") {
                stubExternalUrlOpening()
            }
            step("Click on 'Learn more' link") {
                onSelectApprovalTypeBottomSheet { clickLearnMoreLink() }
            }
            step("Assert the article about permissions is opened") {
                flakySafely(WAIT_UNTIL_TIMEOUT) { assertUrlOpenedStartingWith(permissionArticleUrl) }
            }
            step("Assert the approve amount row for '$fromTokenSymbol' is displayed") {
                onSelectApprovalTypeBottomSheet { amountRow(fromTokenSymbol).assertIsDisplayed() }
            }
            step("Assert '$limitedApproveTitle' approve type is selected by default") {
                onSelectApprovalTypeBottomSheet {
                    selectedApproveType(fromTokenSymbol, limitedApproveTitle).assertIsDisplayed()
                }
            }
            step("Assert 'Confirm' button is displayed") {
                onSelectApprovalTypeBottomSheet { confirmButton.assertIsDisplayed() }
            }
            step("Click on the approve amount row for '$fromTokenSymbol'") {
                onSelectApprovalTypeBottomSheet { amountRow(fromTokenSymbol).performClick() }
            }
            step("Assert '$limitedApproveTitle' approve type is displayed in the list") {
                onSelectApprovalTypeBottomSheet {
                    flakySafely(WAIT_UNTIL_TIMEOUT) { approveTypeMenuItem(limitedApproveTitle).assertIsDisplayed() }
                }
            }
            step("Assert '$unlimitedApproveTitle' approve type is displayed in the list") {
                onSelectApprovalTypeBottomSheet { approveTypeMenuItem(unlimitedApproveTitle).assertIsDisplayed() }
            }
            step("Assert '$limitedApproveTitle' approve type is checked") {
                onSelectApprovalTypeBottomSheet { approveTypeMenuCheckbox(limitedApproveTitle).assertIsOn() }
            }
            step("Assert '$unlimitedApproveTitle' approve type is not checked") {
                onSelectApprovalTypeBottomSheet { approveTypeMenuCheckbox(unlimitedApproveTitle).assertIsOff() }
            }
        }
    }

    @AllureId("582")
    @DisplayName("Swap: by default the DEX gets an approve for the operation amount, approve and swap are signed once")
    @Test
    fun approveForOperationAmountTest() {
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
            step("Set WireMock scenario '$quoteScenarioName' to '$approvalRequiredQuoteState'") {
                setWireMockScenarioState(scenarioName = quoteScenarioName, state = approvalRequiredQuoteState)
            }
            step("Set WireMock scenario '$allowanceScenarioName' to '$notApprovedState'") {
                setWireMockScenarioState(scenarioName = allowanceScenarioName, state = notApprovedState)
            }
            step("Open 'Main' screen with existing hot wallet") {
                openMainScreenWithExistingHotWallet(SVS_SEED_PHRASE_12)
            }
            step("Click on token with name: '$fromTokenName'") {
                openTokenDetailsFromTokenList(fromTokenName)
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
            step("Wait for the '$dexProviderName' quote and fee to load") {
                waitForFeeDisplayed()
            }
            step("Click on 'Providers' block") {
                onSwapTokenScreen { providersBlock.performClick() }
            }
            step("Assert the approve settings button of '$dexProviderName' is displayed") {
                onChooseProviderBottomSheet {
                    flakySafely(WAIT_UNTIL_TIMEOUT) { approvalSettingsButton(dexProviderName).assertIsDisplayed() }
                }
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
            step("Confirm the swap and assert the approve and the swap were sent after a single signature") {
                confirmSwapAndAssertApproveBroadcast(
                    rpcPathPattern = ethereumRpcPathPattern,
                    spenderAddress = dexSpenderAddress,
                    expectedApproveAmount = inputAmountInWei,
                )
            }
        }
    }

    @AllureId("581")
    @DisplayName("Swap: an unlimited approve chosen for a DEX is kept for the next swaps through this DEX")
    @Test
    fun unlimitedApproveTest() {
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
            step("Set WireMock scenario '$quoteScenarioName' to '$approvalRequiredQuoteState'") {
                setWireMockScenarioState(scenarioName = quoteScenarioName, state = approvalRequiredQuoteState)
            }
            step("Set WireMock scenario '$allowanceScenarioName' to '$notApprovedState'") {
                setWireMockScenarioState(scenarioName = allowanceScenarioName, state = notApprovedState)
            }
            step("Open 'Main' screen with existing hot wallet") {
                openMainScreenWithExistingHotWallet(SVS_SEED_PHRASE_12)
            }
            step("Click on token with name: '$fromTokenName'") {
                openTokenDetailsFromTokenList(fromTokenName)
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
            step("Wait for the '$dexProviderName' quote and fee to load") {
                waitForFeeDisplayed()
            }
            step("Click on 'Providers' block") {
                onSwapTokenScreen { providersBlock.performClick() }
            }
            step("Click on the approve settings button of '$dexProviderName'") {
                onChooseProviderBottomSheet {
                    flakySafely(WAIT_UNTIL_TIMEOUT) { approvalSettingsButton(dexProviderName).performClick() }
                }
            }
            step("Click on the approve amount row for '$fromTokenSymbol'") {
                onSelectApprovalTypeBottomSheet {
                    flakySafely(WAIT_UNTIL_TIMEOUT) { amountRow(fromTokenSymbol).performClick() }
                }
            }
            step("Click on '$unlimitedApproveTitle' approve type") {
                onSelectApprovalTypeBottomSheet {
                    flakySafely(WAIT_UNTIL_TIMEOUT) { approveTypeMenuItem(unlimitedApproveTitle).performClick() }
                }
            }
            step("Assert '$unlimitedApproveTitle' approve type is selected") {
                onSelectApprovalTypeBottomSheet {
                    flakySafely(WAIT_UNTIL_TIMEOUT) {
                        selectedApproveType(fromTokenSymbol, unlimitedApproveTitle).assertIsDisplayed()
                    }
                }
            }
            step("Click on 'Confirm' button") {
                onSelectApprovalTypeBottomSheet { confirmButton.performClick() }
            }
            step("Click on provider '$dexProviderName'") {
                onChooseProviderBottomSheet {
                    flakySafely(WAIT_UNTIL_TIMEOUT) { providerItem(dexProviderName).clickWithAssertion() }
                }
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
            step("Confirm the swap and assert the approve and the swap were sent after a single signature") {
                confirmSwapAndAssertApproveBroadcast(
                    rpcPathPattern = ethereumRpcPathPattern,
                    spenderAddress = dexSpenderAddress,
                    expectedApproveAmount = UNLIMITED_APPROVE_AMOUNT,
                )
            }
            step("Click on 'Close' button") {
                onSwapSuccessScreen { closeButton.performClick() }
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
            step("Wait for the '$dexProviderName' quote and fee to load") {
                waitForFeeDisplayed()
            }
            step("Click on 'Providers' block") {
                onSwapTokenScreen { providersBlock.performClick() }
            }
            step("Assert provider '$dexProviderName' is displayed") {
                onChooseProviderBottomSheet {
                    flakySafely(WAIT_UNTIL_TIMEOUT) { providerItem(dexProviderName).assertIsDisplayed() }
                }
            }
            step("Assert the approve settings button of '$dexProviderName' is not displayed") {
                onChooseProviderBottomSheet { approvalSettingsButton(dexProviderName).assertIsNotDisplayed() }
            }
        }
    }
}