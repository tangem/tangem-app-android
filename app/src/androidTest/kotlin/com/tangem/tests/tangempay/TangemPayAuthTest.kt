package com.tangem.tests.tangempay

import androidx.test.InstrumentationRegistry.getTargetContext
import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.TANGEM_PAY_ELIGIBILITY_SCENARIO
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_LONG
import com.tangem.common.extensions.assertTextContainsSafe
import com.tangem.common.extensions.bringAppToForeground
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.extensions.collapseAppByHomeButton
import com.tangem.common.utils.findWireMockRequestBodies
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.scenarios.expireTangemPayAccessToken
import com.tangem.scenarios.openMainScreenWithTangemPayTile
import com.tangem.scenarios.restoreTangemPayAccessToken
import com.tangem.screens.tangempay.onTangemPayMainScreen
import dagger.hilt.android.testing.HiltAndroidTest
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@HiltAndroidTest
class TangemPayAuthTest : BaseTestCase() {

    private val eligibilityState = "PaeraCustomer"
    private val balanceScenario = "tangem_pay_balance_update"
    private val balanceInitialState = "InitialBalance"
    private val balanceAfterTransactionState = "AfterTransaction"

    private val refreshTokenEndpoint = "/auth/token/refresh"
    private val refreshToken = "mock-refresh-token"
    private val initialBalance = "10.00"
    private val updatedBalance = "9.00"

    @AllureId("9561")
    @DisplayName("Tangem Pay: an access token expired in background is refreshed on return to foreground")
    @Test
    fun expiredAccessTokenIsRefreshedOnForegroundTest() {
        val packageName = getTargetContext().packageName

        setupHooks(
            additionalBeforeSection = {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
                setWireMockScenarioState(balanceScenario, balanceInitialState)
            },
            additionalAfterSection = { restoreTangemPayAccessToken() },
        ).run {
            val refreshCountBefore = countRefreshRequestsWithRefreshToken()

            step("Open Main screen with the Tangem Pay tile") { openMainScreenWithTangemPayTile() }
            step("Assert Main tile balance contains '$initialBalance'") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onTangemPayMainScreen { tileBalance.assertTextContainsSafe(initialBalance, substring = true) }
                }
            }
            step("Assert the access token was not refreshed before going to background") {
                val refreshCount = countRefreshRequestsWithRefreshToken()
                assertEquals("A valid access token should not be refreshed", refreshCountBefore, refreshCount)
            }
            step("Click on 'Home' button") { collapseAppByHomeButton() }
            step("Switch WireMock scenario '$balanceScenario' to '$balanceAfterTransactionState'") {
                setWireMockScenarioState(balanceScenario, balanceAfterTransactionState)
            }
            step("Expire the access token while the app is in background") { expireTangemPayAccessToken() }
            step("Return to the app") { bringAppToForeground(packageName) }
            step("Assert Main tile balance updated to '$updatedBalance'") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onTangemPayMainScreen { tileBalance.assertTextContainsSafe(updatedBalance, substring = true) }
                }
            }
            step("Assert the access token was refreshed with the refresh token") {
                assertTrue(
                    "Returning to foreground should call $refreshTokenEndpoint with '$refreshToken'",
                    countRefreshRequestsWithRefreshToken() > refreshCountBefore,
                )
            }
            step("Click on Tangem Pay tile") {
                onTangemPayMainScreen { mainScreenTile.clickWithAssertion() }
            }
            step("Assert payment account balance contains '$updatedBalance'") {
                flakySafely(WAIT_UNTIL_TIMEOUT) {
                    onTangemPayMainScreen { balance.assertTextContainsSafe(updatedBalance, substring = true) }
                }
            }
        }
    }

    private fun countRefreshRequestsWithRefreshToken(): Int =
        findWireMockRequestBodies("POST", refreshTokenEndpoint).count { it.contains(refreshToken) }
}