package com.tangem.tests.tangempay

import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.TANGEM_PAY_ELIGIBILITY_SCENARIO
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_LONG
import com.tangem.common.extensions.assertTextContainsSafe
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.utils.getWireMockRequestCount
import com.tangem.common.utils.resetWireMockScenarioState
import com.tangem.common.utils.resetWireMockScenarios
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.scenarios.*
import com.tangem.screens.tangempay.*
import dagger.hilt.android.testing.HiltAndroidTest
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Assert.assertTrue
import org.junit.Test

@HiltAndroidTest
class TangemPayBalanceSyncTest : BaseTestCase() {

    private val eligibilityState = "PaeraCustomer"
    private val balanceScenario = "tangem_pay_balance_update"
    private val balanceInitialState = "InitialBalance"
    private val balanceAfterTransactionState = "AfterTransaction"

    // Android refreshes the payment-account balance via /customer/me; /customer/balance is reissue-only.
    private val customerInfoEndpointPattern = "/bff-v2/v1/customer/me"
    private val initialBalance = "10"
    private val updatedBalance = "9"

    @AllureId("9529")
    @DisplayName("Tangem Pay: pull-to-refresh updates details balance and syncs to the Main tile")
    @Test
    fun balanceRefreshesViaPullToRefreshAndSyncsToMainTest() {
        setupHooks(
            additionalBeforeSection = {
                resetWireMockScenarios()
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
                setWireMockScenarioState(balanceScenario, balanceInitialState)
            },
            additionalAfterSection = {
                resetWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO)
                resetWireMockScenarioState(balanceScenario)
            },
        ).run {
            step("Open Tangem Pay payment account") { openTangemPay() }
            step("Assert initial balance contains '$initialBalance'") {
                onTangemPayMainScreen { balance.assertTextContainsSafe(initialBalance, substring = true) }
            }
            step("Switch WireMock scenario '$balanceScenario' to '$balanceAfterTransactionState'") {
                setWireMockScenarioState(balanceScenario, balanceAfterTransactionState)
            }
            val customerInfoBefore = getWireMockRequestCount("GET", customerInfoEndpointPattern)
            step("Pull to refresh Tangem Pay") { pullToRefreshTangemPay() }
            step("Assert details balance updated to '$updatedBalance'") {
                onTangemPayMainScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                        balance.assertTextContainsSafe(updatedBalance, substring = true)
                    }
                }
            }
            step("Assert pull-to-refresh requested customer info") {
                val customerInfoAfter = getWireMockRequestCount("GET", customerInfoEndpointPattern)
                assertTrue(
                    "Pull-to-refresh should request $customerInfoEndpointPattern",
                    customerInfoAfter > customerInfoBefore,
                )
            }
            step("Click on 'Back' button") { device.uiDevice.pressBack() }
            step("Assert Main tile balance updated to '$updatedBalance'") {
                onTangemPayMainScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                        tileBalance.assertTextContainsSafe(updatedBalance, substring = true)
                    }
                }
            }
        }
    }

    @AllureId("9549")
    @DisplayName("Tangem Pay: leaving the payment account screen refreshes the Main balance via customer info")
    @Test
    fun balanceRefreshesOnLeavingDetailsTest() {
        setupHooks(
            additionalBeforeSection = {
                resetWireMockScenarios()
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
                setWireMockScenarioState(balanceScenario, balanceInitialState)
            },
            additionalAfterSection = {
                resetWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO)
                resetWireMockScenarioState(balanceScenario)
            },
        ).run {
            step("Open Tangem Pay payment account") { openTangemPay() }
            step("Assert initial balance contains '$initialBalance'") {
                onTangemPayMainScreen { balance.assertTextContainsSafe(initialBalance, substring = true) }
            }
            step("Switch WireMock scenario '$balanceScenario' to '$balanceAfterTransactionState'") {
                setWireMockScenarioState(balanceScenario, balanceAfterTransactionState)
            }
            val customerInfoBefore = getWireMockRequestCount("GET", customerInfoEndpointPattern)
            step("Click on 'Back' button") { device.uiDevice.pressBack() }
            step("Assert Main tile balance updated to '$updatedBalance'") {
                onTangemPayMainScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                        tileBalance.assertTextContainsSafe(updatedBalance, substring = true)
                    }
                }
            }
            step("Assert returning to Main requested customer info") {
                val customerInfoAfter = getWireMockRequestCount("GET", customerInfoEndpointPattern)
                assertTrue(
                    "Returning to Main should request $customerInfoEndpointPattern",
                    customerInfoAfter > customerInfoBefore,
                )
            }
        }
    }

    @AllureId("9550")
    @DisplayName("Tangem Pay: balance stays in sync between details and the Main tile")
    @Test
    fun balanceStaysInSyncBetweenDetailsAndMainTest() {
        setupHooks(
            additionalBeforeSection = {
                resetWireMockScenarios()
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
                setWireMockScenarioState(balanceScenario, balanceInitialState)
            },
            additionalAfterSection = {
                resetWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO)
                resetWireMockScenarioState(balanceScenario)
            },
        ).run {
            step("Open Tangem Pay payment account") { openTangemPay() }
            step("Assert initial balance contains '$initialBalance'") {
                onTangemPayMainScreen { balance.assertTextContainsSafe(initialBalance, substring = true) }
            }
            step("Switch WireMock scenario '$balanceScenario' to '$balanceAfterTransactionState'") {
                setWireMockScenarioState(balanceScenario, balanceAfterTransactionState)
            }
            step("Pull to refresh Tangem Pay") { pullToRefreshTangemPay() }
            step("Assert details balance updated to '$updatedBalance'") {
                onTangemPayMainScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                        balance.assertTextContainsSafe(updatedBalance, substring = true)
                    }
                }
            }
            step("Click on 'Back' button") { device.uiDevice.pressBack() }
            step("Assert Main tile balance updated to '$updatedBalance'") {
                onTangemPayMainScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                        tileBalance.assertTextContainsSafe(updatedBalance, substring = true)
                    }
                }
            }
            step("Click on Tangem Pay tile") {
                onTangemPayMainScreen { mainScreenTile.clickWithAssertion() }
            }
            step("Assert details balance still contains '$updatedBalance'") {
                onTangemPayMainScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                        balance.assertTextContainsSafe(updatedBalance, substring = true)
                    }
                }
            }
        }
    }
}