package com.tangem.tests.swap

import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.QUOTES_API_SCENARIO
import com.tangem.common.constants.TestConstants.SVS_SEED_PHRASE_12
import com.tangem.common.constants.TestConstants.USER_TOKENS_API_SCENARIO
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_LONG
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.utils.assertClipboardTextEquals
import com.tangem.common.utils.assertTextContainingSharedViaChooser
import com.tangem.common.utils.assertUrlOpened
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.common.utils.stubShareChooserIntent
import com.tangem.common.utils.stubUrlOpening
import com.tangem.scenarios.SwapEntryPoint
import com.tangem.scenarios.chooseReceiveToken
import com.tangem.scenarios.confirmSwapByHolding
import com.tangem.scenarios.openMainScreenWithExistingHotWallet
import com.tangem.scenarios.openSwapAmountScreen
import com.tangem.scenarios.openSwapScreen
import com.tangem.scenarios.openTokenDetailsFromTokenList
import com.tangem.scenarios.pullToRefreshTokenDetails
import com.tangem.screens.onSwapSuccessScreen
import com.tangem.screens.onSwapTokenScreen
import com.tangem.screens.onTxHistoryDetailsBottomSheet
import com.tangem.screens.onTxHistoryScreen
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.kakaocup.kakao.common.utilities.getResourceString
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Test
import com.tangem.core.ui.R as CoreUiR

@HiltAndroidTest
class SwapDexTest : BaseTestCase() {

    private val fromTokenName = "POL (ex-MATIC)"
    private val receiveTokenName = "Ethereum"
    private val fromTokenSymbol = "POL"
    private val receiveTokenSymbol = "ETH"
    private val inputAmount = "50"
    private val providerName = "1Inch"
    private val providerType = "DEX"
    private val expressTxId = "e1f2a3b4-c5d6-7890-abcd-ef1234567890"
    private val providerUrl = "https://blockchair.com/polygon/transaction/mock-external-tx-id-004"
    private val sentSign = "-"
    private val receivedSign = "+"
    private val swappedTitle = getResourceString(CoreUiR.string.common_swapped)
    private val rateLabel = getResourceString(CoreUiR.string.common_rate)
    private val transactionIdMenuItem = getResourceString(CoreUiR.string.common_transaction_id)
    private val transactionIdCopiedMessage = getResourceString(CoreUiR.string.express_transaction_id_copied)
    private val shareMenuItem = getResourceString(CoreUiR.string.common_share)
    private val userTokensState = "HotWalletSvS"
    private val quotesState = "PolygonUSDC"
    private val assetsScenarioName = "express_api_assets"
    private val assetsExchangeEnabledState = "BitcoinExchangeEnabled"
    private val pairsScenarioName = "ethereum_from_pairs"
    private val dexProviderState = "DexProvider"
    private val exchangeStatusScenario = "exchange_status_provider"
    private val oneInchFinishedStatusState = "OneInchFinished"
    private val historyScenario = "exchange_history"
    private val historyFinishedState = "SwapFinished"

    @AllureId("9459")
    @DisplayName("Swap: the details of a finished DEX swap show the swap data and actions")
    @Test
    fun checkDexSwapStatusBlockTest() {
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
            step("Set WireMock scenario '$exchangeStatusScenario' to '$oneInchFinishedStatusState'") {
                setWireMockScenarioState(scenarioName = exchangeStatusScenario, state = oneInchFinishedStatusState)
            }

            step("Open the swap amount screen for '$fromTokenName' -> '$receiveTokenName' on an existing hot wallet") {
                openSwapAmountScreen(
                    fromTokenName = fromTokenName,
                    receiveTokenName = receiveTokenName,
                    amount = inputAmount,
                    seedPhrase = SVS_SEED_PHRASE_12,
                )
            }
            step("Assert the DEX provider block is displayed") {
                onSwapTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { providersBlock.assertIsDisplayed() }
                }
            }
            step("Assert the network fee block is displayed") {
                onSwapTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { networkFeeBlock.assertIsDisplayed() }
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
            step("Click on 'Close' button") {
                onSwapSuccessScreen { closeButton.performClick() }
            }
            step("Set WireMock scenario '$historyScenario' to '$historyFinishedState'") {
                setWireMockScenarioState(scenarioName = historyScenario, state = historyFinishedState)
            }
            step("Assert '$swappedTitle' transaction is displayed in the history") {
                awaitAssert(timeoutMillis = WAIT_UNTIL_TIMEOUT_LONG) {
                    pullToRefreshTokenDetails()
                    onTxHistoryScreen { transactionItem(swappedTitle).assertIsDisplayed() }
                }
            }
            step("Click on '$swappedTitle' transaction") {
                onTxHistoryScreen { transactionItem(swappedTitle).clickWithAssertion() }
            }
            step("Assert the swap details title '$swappedTitle' is displayed") {
                onTxHistoryDetailsBottomSheet {
                    flakySafely(WAIT_UNTIL_TIMEOUT) { headerTitle(swappedTitle).assertIsDisplayed() }
                }
            }
            step("Assert the swap date and time are displayed") {
                onTxHistoryDetailsBottomSheet { headerDateTime.assertIsDisplayed() }
            }
            step("Assert the sent '$fromTokenSymbol' amount is displayed") {
                onTxHistoryDetailsBottomSheet { assetAmount(sentSign, fromTokenSymbol).assertIsDisplayed() }
            }
            step("Assert the received '$receiveTokenSymbol' amount is displayed") {
                onTxHistoryDetailsBottomSheet { assetAmount(receivedSign, receiveTokenSymbol).assertIsDisplayed() }
            }
            step("Assert the provider '$providerName' of type '$providerType' is displayed") {
                onTxHistoryDetailsBottomSheet { rowValueContaining("$providerName • $providerType").assertIsDisplayed() }
            }
            step("Assert the '$rateLabel' row is displayed") {
                onTxHistoryDetailsBottomSheet { rowLabel(rateLabel).assertIsDisplayed() }
            }
            step("Click on 'More' button") {
                onTxHistoryDetailsBottomSheet { moreButton.performClick() }
            }
            step("Click on '$transactionIdMenuItem' menu item") {
                onTxHistoryDetailsBottomSheet { menuItem(transactionIdMenuItem).clickWithAssertion() }
            }
            step("Assert '$transactionIdCopiedMessage' message is displayed") {
                onTxHistoryDetailsBottomSheet {
                    flakySafely(WAIT_UNTIL_TIMEOUT) { message(transactionIdCopiedMessage).assertIsDisplayed() }
                }
            }
            step("Assert the operation ID '$expressTxId' is copied to the clipboard") {
                assertClipboardTextEquals(expressTxId)
            }
            step("Stub the system share chooser") {
                stubShareChooserIntent()
            }
            step("Click on 'More' button") {
                onTxHistoryDetailsBottomSheet { moreButton.performClick() }
            }
            step("Click on '$shareMenuItem' menu item") {
                onTxHistoryDetailsBottomSheet { menuItem(shareMenuItem).clickWithAssertion() }
            }
            step("Assert the swap ID '$expressTxId' and provider '$providerName' are shared via the system chooser") {
                flakySafely(WAIT_UNTIL_TIMEOUT) {
                    assertTextContainingSharedViaChooser(
                        fragments = listOf(expressTxId, "$providerName $providerType"),
                        times = 1,
                    )
                }
            }
            step("Stub opening the provider page") {
                stubUrlOpening(providerUrl)
            }
            step("Click on the provider '$providerName' row") {
                onTxHistoryDetailsBottomSheet { clickableRowContaining("$providerName • $providerType").performClick() }
            }
            step("Assert the provider page '$providerUrl' is opened") {
                flakySafely(WAIT_UNTIL_TIMEOUT) { assertUrlOpened(providerUrl) }
            }
        }
    }

    @AllureId("11523")
    @DisplayName("Swap: successful swap E2E — the swap form and the 'Swap in progress' screen show the swap data")
    @Test
    fun successfulSwapEndToEndTest() {
        val percent25 = "PERCENT_25"
        val percent50 = "PERCENT_50"
        val percent75 = "PERCENT_75"
        val max = "MAX"
        val fiatSign = "$"
        val zeroAmount = "0.00"
        val zeroReceiveAmount = "~ 0"
        val zeroFiatAmount = "0.00"
        val approximateSign = "~"
        val rateSign = "≈"
        // The wallet's POL is the ERC-20 token, so the fee is paid in the Ethereum network coin.
        val feeCoinSymbol = "ETH"
        val marketFeeTitle = getResourceString(CoreUiR.string.common_fee_selector_option_market)

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
            step("Click on token with name: '$fromTokenName'") {
                openTokenDetailsFromTokenList(fromTokenName)
            }
            step("Open 'Swap' screen") {
                openSwapScreen(from = SwapEntryPoint.TokenDetails)
            }
            step("Assert '$fromTokenSymbol' is selected in the 'From' block") {
                onSwapTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { swapTokenSymbol(fromTokenSymbol).assertIsDisplayed() }
                }
            }
            step("Assert the receive token is not selected") {
                onSwapTokenScreen { chooseTokenButton.assertIsDisplayed() }
            }
            step("Assert the send amount is zero") {
                onSwapTokenScreen { textInput.assertTextEquals(zeroAmount) }
            }
            step("Assert the send amount fiat equivalent is zero") {
                onSwapTokenScreen { sendFiatAmount.assertTextContains(zeroFiatAmount, substring = true) }
            }
            step("Assert the receive amount is zero") {
                onSwapTokenScreen { emptyReceiveAmount.assertTextEquals(zeroReceiveAmount) }
            }
            step("Assert the receive amount fiat equivalent is zero") {
                onSwapTokenScreen { emptyReceiveFiatAmount.assertTextContains(zeroFiatAmount, substring = true) }
            }
            step("Assert 'Swap' button is not available") {
                onSwapTokenScreen { swapButtonWithLabel.assertIsNotEnabled() }
            }
            step("Choose receive token '$receiveTokenName'") {
                chooseReceiveToken(receiveTokenName)
            }
            step("Assert '$receiveTokenSymbol' is selected in the 'To' block") {
                onSwapTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT) { receiveTokenSymbol(receiveTokenSymbol).assertIsDisplayed() }
                }
            }
            step("Click on 'You send' amount field") {
                onSwapTokenScreen { textInput.clickWithAssertion() }
            }
            step("Assert the '25%' button is displayed") {
                onSwapTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { percentButton(percent25).assertIsDisplayed() }
                }
            }
            step("Assert the '50%' button is displayed") {
                onSwapTokenScreen { percentButton(percent50).assertIsDisplayed() }
            }
            step("Assert the '75%' button is displayed") {
                onSwapTokenScreen { percentButton(percent75).assertIsDisplayed() }
            }
            step("Assert the 'Max' button is displayed") {
                onSwapTokenScreen { percentButton(max).assertIsDisplayed() }
            }
            step("Input swap amount '$inputAmount'") {
                onSwapTokenScreen { textInput.performTextReplacement(inputAmount) }
            }
            step("Assert the send amount '$inputAmount' is displayed") {
                onSwapTokenScreen { textInput.assertTextEquals(inputAmount) }
            }
            step("Assert the send amount fiat equivalent is displayed") {
                onSwapTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT) { swapFiatAmount.assertTextContains(fiatSign, substring = true) }
                }
            }
            step("Assert the receive amount is displayed") {
                onSwapTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { receiveAmount.assertIsDisplayed() }
                }
            }
            step("Assert the provider '$providerName' is displayed") {
                onSwapTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { providerNameText.assertTextContains(providerName) }
                }
            }
            step("Assert the network fee block is displayed") {
                onSwapTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { feeAmount.assertIsDisplayed() }
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
            step("Assert the operation date and time are displayed") {
                onSwapSuccessScreen { transactionDate.assertIsDisplayed() }
            }
            step("Assert the send token '$fromTokenSymbol' amount '$inputAmount' is displayed") {
                onSwapSuccessScreen { sendBlockText(fromTokenSymbol).assertTextContains(inputAmount, substring = true) }
            }
            step("Assert the send amount fiat equivalent is displayed") {
                onSwapSuccessScreen { sendBlockText(fiatSign).assertIsDisplayed() }
            }
            step("Assert the approximate receive '$receiveTokenSymbol' amount is displayed") {
                onSwapSuccessScreen {
                    receiveBlockText(approximateSign).assertTextContains(receiveTokenSymbol, substring = true)
                }
            }
            step("Assert the receive amount fiat equivalent is displayed") {
                onSwapSuccessScreen { receiveBlockText(fiatSign).assertIsDisplayed() }
            }
            step("Assert the provider logo is displayed") {
                onSwapSuccessScreen { providerIcon.assertIsDisplayed() }
            }
            step("Assert the provider '$providerName' is displayed") {
                onSwapSuccessScreen { providerBlockText(providerName).assertIsDisplayed() }
            }
            step("Assert the provider type '$providerType' is displayed") {
                onSwapSuccessScreen { providerBlockText(providerType).assertIsDisplayed() }
            }
            step("Assert the provider rate is displayed") {
                onSwapSuccessScreen { providerBlockText(rateSign).assertIsDisplayed() }
            }
            step("Assert the '$marketFeeTitle' fee type is displayed") {
                onSwapSuccessScreen { feeBlockText(marketFeeTitle).assertIsDisplayed() }
            }
            step("Assert the network fee in '$feeCoinSymbol' is displayed") {
                onSwapSuccessScreen { feeBlockText(feeCoinSymbol).assertIsDisplayed() }
            }
            step("Assert the network fee fiat equivalent is displayed") {
                onSwapSuccessScreen { feeBlockText(fiatSign).assertIsDisplayed() }
            }
            step("Assert 'Explore' button is displayed") {
                onSwapSuccessScreen { exploreButton.assertIsDisplayed() }
            }
            step("Assert 'Close' button is displayed") {
                onSwapSuccessScreen { closeButton.assertIsDisplayed() }
            }
        }
    }
}