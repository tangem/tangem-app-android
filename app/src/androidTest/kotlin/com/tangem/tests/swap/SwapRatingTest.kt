package com.tangem.tests.swap

import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.QUOTES_API_SCENARIO
import com.tangem.common.constants.TestConstants.SVS_SEED_PHRASE_12
import com.tangem.common.constants.TestConstants.USER_TOKENS_API_SCENARIO
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_LONG
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.scenarios.confirmSwapByHolding
import com.tangem.scenarios.openSwapAmountScreen
import com.tangem.scenarios.pullToRefreshTokenDetails
import com.tangem.screens.onRatingScreen
import com.tangem.screens.onSwapSuccessScreen
import com.tangem.screens.onSwapTokenScreen
import com.tangem.screens.onTxHistoryScreen
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.kakaocup.kakao.common.utilities.getResourceString
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Test
import com.tangem.core.ui.R as CoreUiR

@HiltAndroidTest
class SwapRatingTest : BaseTestCase() {

    private val tokenName = "USDC"
    private val receiveTokenName = "Polygon"
    private val inputAmount = "50"
    private val userTokensState = "PolygonUSDCHotWallet"
    private val quotesState = "PolygonUSDC"
    private val assetsScenarioName = "express_api_assets"
    private val assetsExchangeEnabledState = "BitcoinExchangeEnabled"
    private val exchangeStatusScenario = "exchange_status_provider"
    private val changellyStatusState = "Changelly"
    // The stars are offered only for a rateable (final) deal, so the deal is finished before its status is
    // opened; auto-disposal of a finished deal only happens once the sheet is closed, so the row stays.
    private val changellyFinishedState = "Finished"
    private val nativeBalanceScenario = "polygon_coin_balance"
    private val zeroBalanceState = "ZeroBalance"
    // Stories auto-advance forever, which keeps Compose non-idle and flakes everything after them.
    private val storiesScenario = "stories_first_time_swap_v2"
    private val storiesErrorState = "Error"
    private val historyScenario = "exchange_history"
    private val historyFinishedState = "SwapFinished"
    private val rating = 5
    private val feedbackComment = "Great swap experience"

    @AllureId("9986")
    @DisplayName("Swap: rate the swap experience with stars and feedback and see the rating in the status")
    @Test
    fun checkRateSwapExperienceTest() {
        val swapHistoryTitle = getResourceString(CoreUiR.string.common_swapped)

        setupHooks(
            additionalBeforeAppLaunchSection = { setWireMockScenarioState(storiesScenario, storiesErrorState) }
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
            step("Set WireMock scenario '$exchangeStatusScenario' to '$changellyStatusState'") {
                setWireMockScenarioState(scenarioName = exchangeStatusScenario, state = changellyStatusState)
            }
            step("Set WireMock scenario '$nativeBalanceScenario' to '$zeroBalanceState'") {
                setWireMockScenarioState(scenarioName = nativeBalanceScenario, state = zeroBalanceState)
            }

            step("Open the swap amount screen for '$tokenName' -> '$receiveTokenName' on an existing hot wallet") {
                openSwapAmountScreen(
                    fromTokenName = tokenName,
                    receiveTokenName = receiveTokenName,
                    amount = inputAmount,
                    seedPhrase = SVS_SEED_PHRASE_12,
                    storiesExist = false,
                )
            }
            step("Assert the recommended provider block is displayed") {
                onSwapTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { providersBlock.assertIsDisplayed() }
                }
            }
            step("Assert the network fee block is displayed (quote and fee are ready)") {
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
            step("Switch WireMock scenario '$exchangeStatusScenario' to '$changellyFinishedState'") {
                setWireMockScenarioState(scenarioName = exchangeStatusScenario, state = changellyFinishedState)
            }
            step("Switch WireMock scenario '$historyScenario' to '$historyFinishedState'") {
                setWireMockScenarioState(scenarioName = historyScenario, state = historyFinishedState)
            }
            // The history only re-reads on a refresh, so pull it until the finished swap row lands.
            step("Assert '$swapHistoryTitle' transaction is displayed in the history") {
                awaitAssert(timeoutMillis = WAIT_UNTIL_TIMEOUT_LONG) {
                    pullToRefreshTokenDetails()
                    onTxHistoryScreen { transactionItem(swapHistoryTitle).assertIsDisplayed() }
                }
            }
            step("Open the transaction details") {
                onTxHistoryScreen { transactionItem(swapHistoryTitle).clickWithAssertion() }
            }
            step("Assert the rating block is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT) {
                    onRatingScreen { block.assertIsDisplayed() }
                }
            }
            step("Click on the '$rating' star rating") {
                onRatingScreen { star(rating).clickWithAssertion() }
            }
            step("Assert the feedback comment field is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT) {
                    onRatingScreen { feedbackInput.assertIsDisplayed() }
                }
            }
            step("Assert the 'Send feedback' button is displayed") {
                onRatingScreen { sendFeedbackButton.assertIsDisplayed() }
            }
            step("Enter a feedback comment") {
                onRatingScreen { feedbackInput.performTextInput(feedbackComment) }
            }
            step("Click on 'Send feedback' button") {
                onRatingScreen { sendFeedbackButton.performClick() }
            }
            step("Assert the rating is shown as already rated (stars disabled) in the status block") {
                flakySafely(WAIT_UNTIL_TIMEOUT) {
                    onRatingScreen { star(rating).assertIsNotEnabled() }
                }
            }
        }
    }
}