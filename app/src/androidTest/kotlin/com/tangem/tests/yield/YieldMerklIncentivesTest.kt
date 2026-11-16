package com.tangem.tests.yield

import androidx.compose.ui.test.longClick
import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.HOLD_DURATION_MS
import com.tangem.common.constants.TestConstants.SVS_SEED_PHRASE_12
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_LONG
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.extensions.isDisplayedSafely
import com.tangem.common.utils.getWireMockRequestCount
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.scenarios.openMainScreenWithExistingHotWallet
import com.tangem.scenarios.openTokenDetailsFromTokenList
import com.tangem.scenarios.pullToRefreshTokenDetails
import com.tangem.screens.onTokenDetailsScreen
import com.tangem.screens.onYieldSupplyActiveScreen
import com.tangem.screens.onYieldSupplyClaimResultScreen
import com.tangem.screens.onYieldSupplyClaimScreen
import com.tangem.screens.onYieldSupplyPromoScreen
import com.tangem.screens.onYieldSupplyStartEarningScreen
import com.tangem.screens.onYieldSupplyStopEarningScreen
import dagger.hilt.android.testing.HiltAndroidTest
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@HiltAndroidTest
class YieldMerklIncentivesTest : BaseTestCase() {

    @AllureId("13249")
    @DisplayName("Merkl incentives: enabling Yield Mode with an active campaign")
    @Test
    fun enableYieldModeWithActiveCampaignTest() {
        val tokenTitle = "USDC"
        val rewardsApy = "1.40%"
        val totalApy = "6.64"

        val portfolioScenario = "user_tokens_api"
        val portfolioState = "YieldUSDCMonad"
        val balancesScenario = "moralis_evm_token_balances_api"
        val balancesState = "NonZeroEvmBalances"
        val yieldScenario = "yield_supply_status"
        val yieldNotActiveState = "NotActive"
        val yieldActiveState = "Active"

        setupMerklHooks(merklState = CAMPAIGN_STATE).run {
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
                openTokenDetailsFromTokenList(tokenTitle)
            }
            step("Assert 'Available yield block' is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onTokenDetailsScreen { yieldSupplyAvailableBlock.assertIsDisplayed() }
                }
            }

            step("Click on 'Available yield block'") {
                onTokenDetailsScreen { yieldSupplyAvailableBlock.clickWithAssertion() }
            }
            step("Assert 'Incentivised $rewardsApy' chip is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onYieldSupplyPromoScreen { incentiveChip(rewardsApy).assertIsDisplayed() }
                }
            }
            step("Assert title with total APY '$totalApy' is displayed") {
                onYieldSupplyPromoScreen { title(totalApy).assertIsDisplayed() }
            }
            step("Assert 'Continue' button is displayed") {
                onYieldSupplyPromoScreen { continueButton.assertIsDisplayed() }
            }

            step("Click on 'Continue' button") {
                onYieldSupplyPromoScreen { continueButton.clickWithAssertion() }
            }
            step("Hold 'Start earning' button to confirm activation") {
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
                pullToRefreshTokenDetails()
            }
            step("Assert 'Yield mode enabled' block is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onTokenDetailsScreen { yieldModeConnectedTitle.assertIsDisplayed() }
                }
            }
            step("Assert 'Average APY' is displayed in yield block") {
                onTokenDetailsScreen { yieldModeApy(totalApy).assertIsDisplayed() }
            }
        }
    }

    @AllureId("13253")
    @DisplayName("Merkl incentives: claiming rewards")
    @Test
    fun claimRewardsTest() {
        val tokenTitle = "USDC"
        val rewardsTotal = "$25.50"

        val portfolioScenario = "user_tokens_api"
        val portfolioState = "YieldUSDCMonad"
        val balancesScenario = "moralis_evm_token_balances_api"
        val balancesState = "NonZeroEvmBalances"
        val yieldScenario = "yield_supply_status"
        val yieldActiveState = "Active"
        val merklScenario = MERKL_SCENARIO
        val merklClaimedState = "Claimed"

        setupMerklHooks(merklState = REWARDS_READY_STATE).run {
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
                openTokenDetailsFromTokenList(tokenTitle)
            }
            step("Assert 'Yield mode enabled' block is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onTokenDetailsScreen { yieldModeConnectedTitle.assertIsDisplayed() }
                }
            }

            step("Click on 'Yield mode enabled' block") {
                onTokenDetailsScreen { yieldSupplyActiveBlock.clickWithAssertion() }
            }
            step("Assert rewards banner with '$rewardsTotal' is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onYieldSupplyActiveScreen { rewardsBannerTitle(rewardsTotal).assertIsDisplayed() }
                }
            }

            val claimRequestsBefore = getWireMockRequestCount(
                method = POST,
                urlPathPattern = MONAD_RPC_PATH,
                bodyContains = listOf(SEND_RAW_TRANSACTION_METHOD, CLAIM_METHOD_ID),
            )
            val claimCalldataBefore = getWireMockRequestCount(
                method = GET,
                urlPathPattern = CLAIM_CALLDATA_PATH,
            )

            step("Assert 'Claim' button is enabled") {
                onYieldSupplyActiveScreen { claimRewardsControl.assertIsEnabled() }
            }
            step("Click on 'Claim' button") {
                onYieldSupplyActiveScreen { claimRewardsButton.clickWithAssertion() }
            }
            step("Assert 'Claim' confirmation button with '$rewardsTotal' is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onYieldSupplyClaimScreen { confirmButtonText(rewardsTotal).assertIsDisplayed() }
                }
            }
            step("Hold 'Claim' button to confirm") {
                awaitSuccess { onYieldSupplyClaimScreen { confirmButton.assertIsDisplayed() } }
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onYieldSupplyClaimScreen {
                        if (confirmButton.isDisplayedSafely()) {
                            confirmButton.performTouchInput { longClick(durationMillis = HOLD_DURATION_MS) }
                        }
                        confirmButton.assertIsNotDisplayed()
                    }
                }
            }
            step("Assert 'Reward claim in progress' screen is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onYieldSupplyClaimResultScreen { inProgressTitle.assertIsDisplayed() }
                }
            }
            step("Assert one claim transaction was sent") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    val claimRequestsAfter = getWireMockRequestCount(
                        method = POST,
                        urlPathPattern = MONAD_RPC_PATH,
                        bodyContains = listOf(SEND_RAW_TRANSACTION_METHOD, CLAIM_METHOD_ID),
                    )
                    assertEquals(1, claimRequestsAfter - claimRequestsBefore)
                }
            }
            step("Assert fresh claim calldata was requested") {
                val claimCalldataAfter = getWireMockRequestCount(method = GET, urlPathPattern = CLAIM_CALLDATA_PATH)
                assertTrue(claimCalldataAfter > claimCalldataBefore)
            }

            step("Click on 'Go to yield' button") {
                onYieldSupplyClaimResultScreen { goToYieldButton.clickWithAssertion() }
            }
            step("Assert 'Disable Yield Mode' button is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onYieldSupplyActiveScreen { stopEarningButton.assertIsDisplayed() }
                }
            }
            step("Assert 'Claim' button is disabled while the claim is processed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onYieldSupplyActiveScreen { claimRewardsControl.assertIsNotEnabled() }
                }
            }
            step("Set WireMock scenario: '$merklScenario' to state: '$merklClaimedState'") {
                setWireMockScenarioState(scenarioName = merklScenario, state = merklClaimedState)
            }
            step("Assert rewards banner with '$rewardsTotal' is not displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onYieldSupplyActiveScreen { rewardsBannerTitle(rewardsTotal).assertIsNotDisplayed() }
                }
            }
        }
    }

    @AllureId("13255")
    @DisplayName("Merkl incentives: claiming rewards when disabling Yield Mode")
    @Test
    fun claimRewardsOnDisableYieldModeTest() {
        val tokenTitle = "USDC"

        val portfolioScenario = "user_tokens_api"
        val portfolioState = "YieldUSDCMonad"
        val balancesScenario = "moralis_evm_token_balances_api"
        val balancesState = "NonZeroEvmBalances"
        val yieldScenario = "yield_supply_status"
        val yieldActiveState = "Active"
        val yieldExitedState = "Exited"

        setupMerklHooks(merklState = REWARDS_READY_STATE).run {
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
                openTokenDetailsFromTokenList(tokenTitle)
            }
            step("Assert 'Yield mode enabled' block is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onTokenDetailsScreen { yieldModeConnectedTitle.assertIsDisplayed() }
                }
            }

            step("Click on 'Yield mode enabled' block") {
                onTokenDetailsScreen { yieldSupplyActiveBlock.clickWithAssertion() }
            }

            val sentTransactionsBefore = getWireMockRequestCount(
                method = POST,
                urlPathPattern = MONAD_RPC_PATH,
                bodyContains = listOf(SEND_RAW_TRANSACTION_METHOD),
            )
            val claimTransactionsBefore = getWireMockRequestCount(
                method = POST,
                urlPathPattern = MONAD_RPC_PATH,
                bodyContains = listOf(SEND_RAW_TRANSACTION_METHOD, CLAIM_METHOD_ID),
            )

            step("Click on 'Disable Yield Mode' button") {
                onYieldSupplyActiveScreen { stopEarningButton.clickWithAssertion() }
            }
            step("Hold 'Confirm' button to confirm exit") {
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
            step("Assert claim and exit transactions were sent") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    val sentTransactionsAfter = getWireMockRequestCount(
                        method = POST,
                        urlPathPattern = MONAD_RPC_PATH,
                        bodyContains = listOf(SEND_RAW_TRANSACTION_METHOD),
                    )
                    val claimTransactionsAfter = getWireMockRequestCount(
                        method = POST,
                        urlPathPattern = MONAD_RPC_PATH,
                        bodyContains = listOf(SEND_RAW_TRANSACTION_METHOD, CLAIM_METHOD_ID),
                    )
                    assertEquals(1, claimTransactionsAfter - claimTransactionsBefore)
                    assertEquals(2, sentTransactionsAfter - sentTransactionsBefore)
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
        }
    }

    private fun setupMerklHooks(merklState: String) = setupHooks(
        additionalBeforeAppLaunchSection = {
            check(setWireMockScenarioState(NETWORKS_PROVIDERS_SCENARIO, MONAD_PROVIDER_PAIR_STATE)) {
                "Failed to set '$NETWORKS_PROVIDERS_SCENARIO' to '$MONAD_PROVIDER_PAIR_STATE'"
            }
            check(setWireMockScenarioState(QUOTES_SCENARIO, MONAD_QUOTES_STATE)) {
                "Failed to set '$QUOTES_SCENARIO' to '$MONAD_QUOTES_STATE' — without a MON quote the yield fee is unknown"
            }
            check(setWireMockScenarioState(MERKL_SCENARIO, merklState)) {
                "Failed to set '$MERKL_SCENARIO' to '$merklState' — the markets are read at app start"
            }
        },
    )

    private companion object {
        const val NETWORKS_PROVIDERS_SCENARIO = "networks_providers"
        const val MONAD_PROVIDER_PAIR_STATE = "MonadProviderPair"
        const val QUOTES_SCENARIO = "quotes_api"
        const val MONAD_QUOTES_STATE = "MonadUSDC"
        const val MERKL_SCENARIO = "yield_merkl_incentives"
        const val CAMPAIGN_STATE = "Campaign"
        const val REWARDS_READY_STATE = "RewardsReady"
        const val POST = "POST"
        const val GET = "GET"
        const val MONAD_RPC_PATH = "/monad/?"
        const val CLAIM_CALLDATA_PATH = "/api/v1/yield-modules/143/.*/claim-calldata"
        const val SEND_RAW_TRANSACTION_METHOD = "eth_sendRawTransaction"
        const val CLAIM_METHOD_ID = "71ee95c0"
    }
}