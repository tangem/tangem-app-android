package com.tangem.tests.yield

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.longClick
import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.HOLD_DURATION_MS
import com.tangem.common.constants.TestConstants.SVS_SEED_PHRASE_12
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_LONG
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.extensions.isDisplayedSafely
import com.tangem.common.extensions.pullToRefresh
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.scenarios.openMainScreenWithExistingHotWallet
import com.tangem.screens.onMainScreen
import com.tangem.screens.onTokenDetailsScreen
import com.tangem.screens.onTokenDetailsTopBar
import com.tangem.screens.onTxHistoryScreen
import com.tangem.screens.onYieldSupplyActiveScreen
import com.tangem.screens.onYieldSupplyApproveScreen
import com.tangem.screens.onYieldSupplyPromoScreen
import com.tangem.screens.onYieldSupplyStartEarningScreen
import com.tangem.screens.onYieldSupplyStopEarningScreen
import com.tangem.core.res.R as CoreResR
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.kakaocup.kakao.common.utilities.getResourceString
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
@HiltAndroidTest
class YieldModeTest : BaseTestCase() {

    @AllureId("7957")
    @DisplayName("Yield mode: first-time landing activation with zero balance")
    @Test
    fun firstTimeLandingActivationZeroBalanceTest() {
        val tokenTitle = "USDC"
        val apy = "5.24"

        val portfolioScenario = "user_tokens_api"
        val portfolioState = "YieldUSDCEthereumZeroBalance"
        val balancesScenario = "moralis_evm_token_balances_api"
        val balancesState = "ZeroUsdcEvmBalances"
        val yieldScenario = "yield_supply_status"
        val yieldNotActiveState = "NotActive"
        val yieldActiveState = "Active"

        setupYieldHooks().run {

            step("Set WireMock scenario: '$portfolioScenario' to state: '$portfolioState'") {
                setWireMockScenarioState(scenarioName = portfolioScenario, state = portfolioState)
            }
            step("Set WireMock scenario: '$balancesScenario' to state: '$balancesState'") {
                setWireMockScenarioState(scenarioName = balancesScenario, state = balancesState)
            }
            step("Set WireMock scenario: '$yieldScenario' to state: '$yieldNotActiveState'") {
                setWireMockScenarioState(scenarioName = yieldScenario, state = yieldNotActiveState)
            }

            step("Open 'Main Screen' with existing hot wallet") {
                openMainScreenWithExistingHotWallet(seedPhrase = SVS_SEED_PHRASE_12)
            }

            step("Click on token with name: '$tokenTitle'") {
                onMainScreen { tokenWithTitleAndAddress(tokenTitle).clickWithAssertion() }
            }
            step("Assert 'Token details' screen is displayed") {
                onTokenDetailsScreen { screenContainer.assertIsDisplayed() }
            }
            step("Assert 'Available yield block' is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onTokenDetailsScreen { yieldSupplyAvailableBlock.assertIsDisplayed() }
                }
            }

            step("Click on 'Available yield block'") {
                onTokenDetailsScreen { yieldSupplyAvailableBlock.clickWithAssertion() }
            }
            step("Click on 'Continue' button") {
                onYieldSupplyPromoScreen { continueButton.clickWithAssertion() }
            }
            step("Hold 'Start earning' button to confirm activation") {
                // The hold is swallowed whenever the button's enabled state flips mid-gesture (its
                // pointerInput restarts), so repeat it until the sheet closes — and only while the
                // button is still there, otherwise the retry dies on a node that is already gone.
                // assertIsNotDisplayed also passes for a node that has not rendered yet.
                awaitSuccess { onYieldSupplyStartEarningScreen { startEarningButton.assertIsDisplayed() } }
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onYieldSupplyStartEarningScreen {
                        if (startEarningButton.isDisplayedSafely()) {
                            startEarningButton.performTouchInput { longClick(durationMillis = HOLD_DURATION_MS) }
                        }
                        startEarningButton.assertIsNotDisplayed()
                    }
                }
            }
            step("Set WireMock scenario: '$yieldScenario' to state: '$yieldActiveState'") {
                setWireMockScenarioState(scenarioName = yieldScenario, state = yieldActiveState)
            }
            step("Assert 'Yield mode enabled' block is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onTokenDetailsScreen { yieldModeConnectedTitle.assertIsDisplayed() }
                }
            }
            step("Assert 'Average APY' is displayed in yield block") {
                onTokenDetailsScreen { yieldModeApy(apy).assertIsDisplayed() }
            }
        }
    }

    @AllureId("4938")
    @DisplayName("Yield mode: first-time landing activation")
    @Test
    fun firstTimeLandingActivationTest() {
        val tokenTitle = "USDC"
        val apy = "5.24"
        val enterTransactionTitle = getResourceString(CoreResR.string.yield_module_transaction_enter)

        val portfolioScenario = "user_tokens_api"
        val portfolioState = "YieldUSDCEthereum"
        val balancesScenario = "moralis_evm_token_balances_api"
        val balancesState = "NonZeroEvmBalances"
        val yieldScenario = "yield_supply_status"
        val yieldNotActiveState = "NotActive"
        val yieldActiveState = "Active"

        setupYieldHooks().run {

            step("Set WireMock scenario: '$portfolioScenario' to state: '$portfolioState'") {
                setWireMockScenarioState(scenarioName = portfolioScenario, state = portfolioState)
            }
            step("Set WireMock scenario: '$balancesScenario' to state: '$balancesState'") {
                setWireMockScenarioState(scenarioName = balancesScenario, state = balancesState)
            }
            step("Set WireMock scenario: '$yieldScenario' to state: '$yieldNotActiveState'") {
                setWireMockScenarioState(scenarioName = yieldScenario, state = yieldNotActiveState)
            }

            step("Open 'Main Screen' with existing hot wallet") {
                openMainScreenWithExistingHotWallet(seedPhrase = SVS_SEED_PHRASE_12)
            }

            step("Click on token with name: '$tokenTitle'") {
                onMainScreen { tokenWithTitleAndAddress(tokenTitle).clickWithAssertion() }
            }
            step("Assert 'Token details' screen is displayed") {
                onTokenDetailsScreen { screenContainer.assertIsDisplayed() }
            }
            step("Assert 'Available yield block' is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onTokenDetailsScreen { yieldSupplyAvailableBlock.assertIsDisplayed() }
                }
            }

            step("Click on 'Available yield block'") {
                onTokenDetailsScreen { yieldSupplyAvailableBlock.clickWithAssertion() }
            }
            step("Click on 'Continue' button") {
                onYieldSupplyPromoScreen { continueButton.clickWithAssertion() }
            }
            step("Hold 'Start earning' button to confirm activation") {
                // The hold is swallowed whenever the button's enabled state flips mid-gesture (its
                // pointerInput restarts), so repeat it until the sheet closes — and only while the
                // button is still there, otherwise the retry dies on a node that is already gone.
                // assertIsNotDisplayed also passes for a node that has not rendered yet.
                awaitSuccess { onYieldSupplyStartEarningScreen { startEarningButton.assertIsDisplayed() } }
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onYieldSupplyStartEarningScreen {
                        if (startEarningButton.isDisplayedSafely()) {
                            startEarningButton.performTouchInput { longClick(durationMillis = HOLD_DURATION_MS) }
                        }
                        startEarningButton.assertIsNotDisplayed()
                    }
                }
            }
            step("Set WireMock scenario: '$yieldScenario' to state: '$yieldActiveState'") {
                setWireMockScenarioState(scenarioName = yieldScenario, state = yieldActiveState)
            }
            step("Perform pull to refresh") {
                pullToRefresh()
            }
            step("Assert 'Yield mode enabled' block is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onTokenDetailsScreen { yieldModeConnectedTitle.assertIsDisplayed() }
                }
            }
            step("Assert 'Average APY' is displayed in yield block") {
                onTokenDetailsScreen { yieldModeApy(apy).assertIsDisplayed() }
            }
            step("Assert 'Yield mode enabled' transaction is displayed in history") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onTxHistoryScreen {
                        transactionItem(enterTransactionTitle).assertIsDisplayed()
                    }
                }
            }
        }
    }

    @AllureId("5473")
    @DisplayName("Yield mode: top-up for active landing")
    @Test
    fun topUpActiveLandingTest() {
        val tokenTitle = "USDC"
        val topUpAmount = "600.00"
        val incomingTransactionTitle = getResourceString(CoreResR.string.common_transferred)
        val topUpTransactionTitle = getResourceString(CoreResR.string.yield_module_transaction_topup)
        val supplyingNotificationTitle = getResourceString(
            CoreResR.string.yield_module_amount_not_transfered_to_aave_title,
            topUpAmount,
            tokenTitle,
        )

        val portfolioScenario = "user_tokens_api"
        val portfolioState = "YieldUSDCEthereum"
        val balancesScenario = "moralis_evm_token_balances_api"
        val balancesState = "NonZeroEvmBalances"
        val yieldScenario = "yield_supply_status"
        val yieldTopUpState = "TopUp"

        setupYieldHooks().run {

            step("Set WireMock scenario: '$portfolioScenario' to state: '$portfolioState'") {
                setWireMockScenarioState(scenarioName = portfolioScenario, state = portfolioState)
            }
            step("Set WireMock scenario: '$balancesScenario' to state: '$balancesState'") {
                setWireMockScenarioState(scenarioName = balancesScenario, state = balancesState)
            }
            step("Set WireMock scenario: '$yieldScenario' to state: '$yieldTopUpState'") {
                setWireMockScenarioState(scenarioName = yieldScenario, state = yieldTopUpState)
            }

            step("Open 'Main Screen' with existing hot wallet") {
                openMainScreenWithExistingHotWallet(seedPhrase = SVS_SEED_PHRASE_12)
            }

            step("Click on token with name: '$tokenTitle'") {
                onMainScreen { tokenWithTitleAndAddress(tokenTitle).clickWithAssertion() }
            }
            step("Assert 'Token details' screen is displayed") {
                onTokenDetailsScreen { screenContainer.assertIsDisplayed() }
            }
            step("Assert 'Not supplied' info icon is displayed in yield block") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onTokenDetailsScreen { earnBlockTitleIcon.assertIsDisplayed() }
                }
            }
            step("Assert 'Transferred' transaction is displayed in history") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onTxHistoryScreen { transactionItem(incomingTransactionTitle).assertIsDisplayed() }
                }
            }
            step("Assert 'Supply to Aave' transaction is displayed in history") {
                onTxHistoryScreen { transactionItem(topUpTransactionTitle).assertIsDisplayed() }
            }

            step("Click on 'Yield mode enabled' block") {
                onTokenDetailsScreen { yieldSupplyActiveBlock.clickWithAssertion() }
            }
            step("Assert 'Supplying to Aave' notification is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onYieldSupplyActiveScreen { notificationTitle(supplyingNotificationTitle).assertIsDisplayed() }
                }
            }
        }
    }

    @AllureId("7960")
    @DisplayName("Yield mode: granting approval")
    @Test
    fun grantApprovalTest() {
        val tokenTitle = "USDC"
        val approveNeededTitle = getResourceString(CoreResR.string.yield_module_approve_needed_notification_title)

        val portfolioScenario = "user_tokens_api"
        val portfolioState = "YieldUSDCEthereum"
        val balancesScenario = "moralis_evm_token_balances_api"
        val balancesState = "NonZeroEvmBalances"
        val yieldScenario = "yield_supply_status"
        val yieldApproveNeededState = "Active"
        val yieldApproveGrantedState = "ApproveGranted"

        setupYieldHooks().run {

            step("Set WireMock scenario: '$portfolioScenario' to state: '$portfolioState'") {
                setWireMockScenarioState(scenarioName = portfolioScenario, state = portfolioState)
            }
            step("Set WireMock scenario: '$balancesScenario' to state: '$balancesState'") {
                setWireMockScenarioState(scenarioName = balancesScenario, state = balancesState)
            }
            step("Set WireMock scenario: '$yieldScenario' to state: '$yieldApproveNeededState'") {
                setWireMockScenarioState(scenarioName = yieldScenario, state = yieldApproveNeededState)
            }

            step("Open 'Main Screen' with existing hot wallet") {
                openMainScreenWithExistingHotWallet(seedPhrase = SVS_SEED_PHRASE_12)
            }

            step("Click on token with name: '$tokenTitle'") {
                onMainScreen { tokenWithTitleAndAddress(tokenTitle).clickWithAssertion() }
            }
            step("Assert 'Token details' screen is displayed") {
                onTokenDetailsScreen { screenContainer.assertIsDisplayed() }
            }
            step("Assert 'Approve needed' info icon is displayed in yield block") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onTokenDetailsScreen { earnBlockTitleIcon.assertIsDisplayed() }
                }
            }

            step("Click on 'Yield mode enabled' block") {
                onTokenDetailsScreen { yieldSupplyActiveBlock.clickWithAssertion() }
            }
            step("Assert 'Approve needed' notification is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onYieldSupplyActiveScreen { notificationTitle(approveNeededTitle).assertIsDisplayed() }
                }
            }
            step("Click on 'Approve' button") {
                onYieldSupplyActiveScreen { approveButton.clickWithAssertion() }
            }
            step("Hold 'Confirm' button to confirm approval") {
                // The hold is swallowed whenever the button's enabled state flips mid-gesture (its
                // pointerInput restarts), so repeat it until the sheet closes — and only while the
                // button is still there, otherwise the retry dies on a node that is already gone.
                // assertIsNotDisplayed also passes for a node that has not rendered yet.
                awaitSuccess { onYieldSupplyApproveScreen { confirmButton.assertIsDisplayed() } }
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onYieldSupplyApproveScreen {
                        if (confirmButton.isDisplayedSafely()) {
                            confirmButton.performTouchInput { longClick(durationMillis = HOLD_DURATION_MS) }
                        }
                        confirmButton.assertIsNotDisplayed()
                    }
                }
            }
            step("Set WireMock scenario: '$yieldScenario' to state: '$yieldApproveGrantedState'") {
                setWireMockScenarioState(scenarioName = yieldScenario, state = yieldApproveGrantedState)
            }
            step("Perform pull to refresh") {
                pullToRefresh()
            }
            step("Assert 'Approve needed' info icon is not displayed in yield block") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onTokenDetailsScreen { earnBlockTitleIcon.assertIsNotDisplayed() }
                }
            }
        }
    }

    @AllureId("4940")
    @DisplayName("Yield mode: reopening landing")
    @Test
    fun reopenLandingActivationTest() {
        val tokenTitle = "USDC"
        val nativeCoinTitle = "Ethereum"
        val apy = "5.24"
        val reactivateTransactionTitle = getResourceString(CoreResR.string.yield_module_transaction_reactivate)
        val enterTransactionTitle = getResourceString(CoreResR.string.yield_module_transaction_enter)
        val portfolioScenario = "user_tokens_api"
        val portfolioState = "YieldUSDCEthereum"
        val balancesScenario = "moralis_evm_token_balances_api"
        val balancesState = "NonZeroEvmBalances"
        val yieldScenario = "yield_supply_status"
        val yieldNotActiveState = "NotActive"
        val yieldActiveState = "Active"

        setupYieldHooks().run {

            step("Set WireMock scenario: '$portfolioScenario' to state: '$portfolioState'") {
                setWireMockScenarioState(scenarioName = portfolioScenario, state = portfolioState)
            }
            step("Set WireMock scenario: '$balancesScenario' to state: '$balancesState'") {
                setWireMockScenarioState(scenarioName = balancesScenario, state = balancesState)
            }
            step("Set WireMock scenario: '$yieldScenario' to state: '$yieldNotActiveState'") {
                setWireMockScenarioState(scenarioName = yieldScenario, state = yieldNotActiveState)
            }

            step("Open 'Main Screen' with existing hot wallet") {
                openMainScreenWithExistingHotWallet(seedPhrase = SVS_SEED_PHRASE_12)
            }

            step("Click on token with name: '$tokenTitle'") {
                onMainScreen { tokenWithTitleAndAddress(tokenTitle).clickWithAssertion() }
            }
            step("Assert 'Token details' screen is displayed") {
                onTokenDetailsScreen { screenContainer.assertIsDisplayed() }
            }
            step("Assert 'Available yield block' is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onTokenDetailsScreen { yieldSupplyAvailableBlock.assertIsDisplayed() }
                }
            }

            step("Click on 'Available yield block'") {
                onTokenDetailsScreen { yieldSupplyAvailableBlock.clickWithAssertion() }
            }
            step("Click on 'Continue' button") {
                onYieldSupplyPromoScreen { continueButton.clickWithAssertion() }
            }
            step("Hold 'Start earning' button to confirm activation") {
                // The hold is swallowed whenever the button's enabled state flips mid-gesture (its
                // pointerInput restarts), so repeat it until the sheet closes — and only while the
                // button is still there, otherwise the retry dies on a node that is already gone.
                // assertIsNotDisplayed also passes for a node that has not rendered yet.
                awaitSuccess { onYieldSupplyStartEarningScreen { startEarningButton.assertIsDisplayed() } }
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onYieldSupplyStartEarningScreen {
                        if (startEarningButton.isDisplayedSafely()) {
                            startEarningButton.performTouchInput { longClick(durationMillis = HOLD_DURATION_MS) }
                        }
                        startEarningButton.assertIsNotDisplayed()
                    }
                }
            }
            step("Set WireMock scenario: '$yieldScenario' to state: '$yieldActiveState'") {
                setWireMockScenarioState(scenarioName = yieldScenario, state = yieldActiveState)
            }
            step("Perform pull to refresh") {
                pullToRefresh()
            }
            step("Assert 'Yield mode enabled' block is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onTokenDetailsScreen { yieldModeConnectedTitle.assertIsDisplayed() }
                }
            }
            step("Assert 'Average APY' is displayed in yield block") {
                onTokenDetailsScreen { yieldModeApy(apy).assertIsDisplayed() }
            }
            step("Assert 'Yield mode enabled' transaction is displayed in history") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onTxHistoryScreen { transactionItem(enterTransactionTitle).assertIsDisplayed() }
                }
            }

            step("Click 'Back' button to return to 'Main Screen'") {
                onTokenDetailsTopBar { backButton.clickWithAssertion() }
            }
            step("Click on native coin with name: '$nativeCoinTitle'") {
                onMainScreen { tokenWithTitleAndAddress(nativeCoinTitle).clickWithAssertion() }
            }
            step("Assert 'Token details' screen is displayed") {
                onTokenDetailsScreen { screenContainer.assertIsDisplayed() }
            }
            step("Assert 'Reactivate Token' transaction is displayed in history") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onTxHistoryScreen { transactionItem(reactivateTransactionTitle).assertIsDisplayed() }
                }
            }
            step("Assert 'Enter protocol' transaction is displayed in history") {
                onTxHistoryScreen { transactionItem(enterTransactionTitle).assertIsDisplayed() }
            }
        }
    }

    @AllureId("4937")
    @DisplayName("Yield mode: closing active landing")
    @Test
    fun closeActiveLandingTest() {
        val tokenTitle = "USDC"
        val nativeCoinTitle = "Ethereum"
        val exitTransactionTitle = getResourceString(CoreResR.string.yield_module_transaction_exit)
        val portfolioScenario = "user_tokens_api"
        val portfolioState = "YieldUSDCEthereum"
        val balancesScenario = "moralis_evm_token_balances_api"
        val balancesState = "NonZeroEvmBalances"
        val yieldScenario = "yield_supply_status"
        val yieldActiveState = "Active"
        val yieldExitedState = "Exited"

        setupYieldHooks().run {

            step("Set WireMock scenario: '$portfolioScenario' to state: '$portfolioState'") {
                setWireMockScenarioState(scenarioName = portfolioScenario, state = portfolioState)
            }
            step("Set WireMock scenario: '$balancesScenario' to state: '$balancesState'") {
                setWireMockScenarioState(scenarioName = balancesScenario, state = balancesState)
            }
            step("Set WireMock scenario: '$yieldScenario' to state: '$yieldActiveState'") {
                setWireMockScenarioState(scenarioName = yieldScenario, state = yieldActiveState)
            }

            step("Open 'Main Screen' with existing hot wallet") {
                openMainScreenWithExistingHotWallet(seedPhrase = SVS_SEED_PHRASE_12)
            }

            step("Click on token with name: '$tokenTitle'") {
                onMainScreen { tokenWithTitleAndAddress(tokenTitle).clickWithAssertion() }
            }
            step("Assert 'Token details' screen is displayed") {
                onTokenDetailsScreen { screenContainer.assertIsDisplayed() }
            }
            step("Assert 'Yield mode enabled' block is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onTokenDetailsScreen { yieldModeConnectedTitle.assertIsDisplayed() }
                }
            }

            step("Click on 'Yield mode enabled' block") {
                onTokenDetailsScreen { yieldSupplyActiveBlock.clickWithAssertion() }
            }
            step("Click on 'Disable Yield Mode' button") {
                onYieldSupplyActiveScreen { stopEarningButton.clickWithAssertion() }
            }
            step("Hold 'Confirm' button to confirm exit") {
                // The hold is swallowed whenever the button's enabled state flips mid-gesture (its
                // pointerInput restarts), so repeat it until the sheet closes — and only while the
                // button is still there, otherwise the retry dies on a node that is already gone.
                // assertIsNotDisplayed also passes for a node that has not rendered yet.
                awaitSuccess { onYieldSupplyStopEarningScreen { confirmButton.assertIsDisplayed() } }
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onYieldSupplyStopEarningScreen {
                        if (confirmButton.isDisplayedSafely()) {
                            confirmButton.performTouchInput { longClick(durationMillis = HOLD_DURATION_MS) }
                        }
                        confirmButton.assertIsNotDisplayed()
                    }
                }
            }
            step("Set WireMock scenario: '$yieldScenario' to state: '$yieldExitedState'") {
                setWireMockScenarioState(scenarioName = yieldScenario, state = yieldExitedState)
            }

            step("Assert 'Available yield block' is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onTokenDetailsScreen { yieldSupplyAvailableBlock.assertIsDisplayed() }
                }
            }
            step("Click 'Back' button to return to 'Main Screen'") {
                onTokenDetailsTopBar { backButton.clickWithAssertion() }
            }
            step("Click on native coin with name: '$nativeCoinTitle'") {
                onMainScreen { tokenWithTitleAndAddress(nativeCoinTitle).clickWithAssertion() }
            }
            step("Assert 'Token details' screen is displayed") {
                onTokenDetailsScreen { screenContainer.assertIsDisplayed() }
            }
            step("Assert 'Yield mode disabled' transaction is displayed in history") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onTxHistoryScreen { transactionItem(exitTransactionTitle).assertIsDisplayed() }
                }
            }
        }
    }

    /**
     * The SDK only trusts the yield module address when at least two providers return the same value
     * (EthereumYieldModuleAddressResolver.MIN_CONFIRMATIONS), and falls back to the zero address otherwise —
     * activation then silently does nothing. The default mock leaves Ethereum with a single provider, so
     * point it at the pair of mocked hosts. networks_providers is read at launch, hence before the activity starts.
     */
    private fun setupYieldHooks() = setupHooks(
        additionalBeforeAppLaunchSection = {
            check(setWireMockScenarioState(NETWORKS_PROVIDERS_SCENARIO, ETHEREUM_PROVIDER_PAIR_STATE)) {
                "Failed to set '$NETWORKS_PROVIDERS_SCENARIO' to '$ETHEREUM_PROVIDER_PAIR_STATE' — Ethereum " +
                    "would then have a single provider and yield activation would silently do nothing."
            }
        },
    )

    private companion object {
        const val NETWORKS_PROVIDERS_SCENARIO = "networks_providers"
        const val ETHEREUM_PROVIDER_PAIR_STATE = "EthereumProviderPair"
    }
}