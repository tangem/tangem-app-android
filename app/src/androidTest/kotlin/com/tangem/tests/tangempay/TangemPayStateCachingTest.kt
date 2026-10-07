package com.tangem.tests.tangempay

import androidx.test.InstrumentationRegistry.getTargetContext
import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.SVS_SEED_PHRASE_12
import com.tangem.common.constants.TestConstants.TANGEM_PAY_ACCESS_CODE
import com.tangem.common.constants.TestConstants.TANGEM_PAY_ELIGIBILITY_SCENARIO
import com.tangem.common.constants.TestConstants.TANGEM_PAY_KYC_STATUS_SCENARIO
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_LONG
import com.tangem.common.extensions.assertTextContainsSafe
import com.tangem.common.extensions.getQuantityString
import com.tangem.common.extensions.restartApp
import com.tangem.common.utils.getWireMockRequestCount
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.core.res.R as CoreResR
import com.tangem.scenarios.assertTangemPayConnectionIssuesBanner
import com.tangem.scenarios.assertTangemPaySessionExpiredBanner
import com.tangem.scenarios.assertTangemPayStaleState
import com.tangem.scenarios.assertTangemPayTileShowsCachedData
import com.tangem.scenarios.assertTangemPayTransactionRowDisplayed
import com.tangem.scenarios.openAndCloseTangemPayKycInProgressSheet
import com.tangem.scenarios.openMainScreenWithExistingHotWallet
import com.tangem.scenarios.openMainScreenWithTangemPayTile
import com.tangem.scenarios.openTangemPay
import com.tangem.screens.tangempay.onTangemPayMainScreen
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.kakaocup.kakao.common.utilities.getResourceString
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Ignore
import org.junit.Test

@HiltAndroidTest
class TangemPayStateCachingTest : BaseTestCase() {

    private val eligibilityState = "PaeraCustomer"
    private val sessionScenario = "tangem_pay_session"
    private val sessionExpiredState = "SessionExpired"
    private val serviceUnavailableState = "ServiceUnavailable"
    private val historyScenario = "tangem_pay_transaction_history"
    private val historyLoadedState = "SpendCompleted"
    private val historyErrorState = "HistoryError"

    private val packageName = getTargetContext().packageName
    private val cachedBalance = "10"
    private val cachedTransaction = "Tangem Coffee"

    // The Main tile summarises the cached cards as a count, not as the "*XXXX" mask the card page shows.
    private val cachedCardsSubtitle = getQuantityString(CoreResR.plurals.tangempay_cards_count, 1, 1)
    private val sessionExpiredSubtitle = getResourceString(CoreResR.string.tangempay_payment_account_sync_needed)

    @AllureId("9753")
    @DisplayName("Tangem Pay: Main tile keeps the cached account data when the session has expired")
    @Test
    fun mainScreenShowsCachedDataWhenSessionExpiredTest() {
        setupHooks(
            additionalBeforeSection = {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
            }
        ).run {
            step("Open Main screen with the Tangem Pay tile") { openMainScreenWithTangemPayTile() }
            step("Assert Main tile balance contains '$cachedBalance'") {
                onTangemPayMainScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                        tileBalance.assertTextContainsSafe(cachedBalance, substring = true)
                    }
                }
            }
            step("Set WireMock scenario '$sessionScenario' to '$sessionExpiredState'") {
                setWireMockScenarioState(sessionScenario, sessionExpiredState)
            }
            step("Force-close and re-launch the app") { restartApp(packageName) }
            step("Assert Main tile shows the cached account data") {
                assertTangemPayTileShowsCachedData(cachedBalance, cachedCardsSubtitle)
            }
            step("Assert 'Payment account session expired' banner is not displayed") {
                onTangemPayMainScreen { sessionExpiredBannerTitle.assertDoesNotExist() }
            }
        }
    }

    @AllureId("9754")
    @DisplayName("Tangem Pay: Main tile keeps the cached account data when the service is unavailable")
    @Test
    fun mainScreenShowsCachedDataWhenServiceUnavailableTest() {
        setupHooks(
            additionalBeforeSection = {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
            }
        ).run {
            step("Open Main screen with the Tangem Pay tile") { openMainScreenWithTangemPayTile() }
            step("Assert Main tile balance contains '$cachedBalance'") {
                onTangemPayMainScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                        tileBalance.assertTextContainsSafe(cachedBalance, substring = true)
                    }
                }
            }
            step("Set WireMock scenario '$sessionScenario' to '$serviceUnavailableState'") {
                setWireMockScenarioState(sessionScenario, serviceUnavailableState)
            }
            step("Force-close and re-launch the app") { restartApp(packageName) }
            step("Assert Main tile shows the cached account data") {
                assertTangemPayTileShowsCachedData(cachedBalance, cachedCardsSubtitle)
            }
            step("Assert 'Connection issues' banner is not displayed") {
                onTangemPayMainScreen { connectionIssuesBannerTitle.assertDoesNotExist() }
            }
        }
    }

    @AllureId("9755")
    @DisplayName("Tangem Pay: Payment account shows the session expired banner over the cached data")
    @Test
    fun paymentAccountShowsSessionExpiredBannerTest() {
        setupHooks(
            additionalBeforeSection = {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
                setWireMockScenarioState(historyScenario, historyLoadedState)
            }
        ).run {
            step("Open Tangem Pay payment account") { openTangemPay() }
            step("Assert the transaction history is loaded") {
                assertTangemPayTransactionRowDisplayed(cachedTransaction)
            }
            step("Set WireMock scenario '$sessionScenario' to '$sessionExpiredState'") {
                setWireMockScenarioState(sessionScenario, sessionExpiredState)
            }
            step("Set WireMock scenario '$historyScenario' to '$historyErrorState'") {
                setWireMockScenarioState(historyScenario, historyErrorState)
            }
            // Decompose restores its child stack from saved state, so a relaunch lands back on Payment account.
            step("Force-close and re-launch the app") { restartApp(packageName) }
            step("Assert session expired banner is displayed") { assertTangemPaySessionExpiredBanner() }
            step("Assert payment account still shows its cached state") {
                assertTangemPayStaleState(cachedBalance, cachedTransaction)
            }
        }
    }

    @AllureId("9756")
    @DisplayName("Tangem Pay: Payment account shows the connection issues banner over the cached data")
    @Test
    fun paymentAccountShowsConnectionIssuesBannerTest() {
        setupHooks(
            additionalBeforeSection = {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
                setWireMockScenarioState(historyScenario, historyLoadedState)
            }
        ).run {
            step("Open Tangem Pay payment account") { openTangemPay() }
            step("Assert the transaction history is loaded") {
                assertTangemPayTransactionRowDisplayed(cachedTransaction)
            }
            step("Set WireMock scenario '$sessionScenario' to '$serviceUnavailableState'") {
                setWireMockScenarioState(sessionScenario, serviceUnavailableState)
            }
            step("Set WireMock scenario '$historyScenario' to '$historyErrorState'") {
                setWireMockScenarioState(historyScenario, historyErrorState)
            }
            // Decompose restores its child stack from saved state, so a relaunch lands back on Payment account.
            step("Force-close and re-launch the app") { restartApp(packageName) }
            step("Assert connection issues banner is displayed") { assertTangemPayConnectionIssuesBanner() }
            step("Assert payment account still shows its cached state") {
                assertTangemPayStaleState(cachedBalance, cachedTransaction)
            }
        }
    }

    @AllureId("9757")
    @DisplayName("Tangem Pay: Main screen asks to renew the session when the cache is empty")
    @Test
    fun mainScreenShowsSessionExpiredWhenCacheIsEmptyTest() {
        setupHooks(
            additionalBeforeSection = {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
                setWireMockScenarioState(sessionScenario, sessionExpiredState)
            }
        ).run {
            step("Open Main screen with the Tangem Pay tile") { openMainScreenWithTangemPayTile() }
            step("Assert Main tile shows the '$sessionExpiredSubtitle' status") {
                onTangemPayMainScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                        tileWithSubtitle(sessionExpiredSubtitle).assertIsDisplayed()
                    }
                }
            }
            step("Assert Main tile shows no balance") {
                onTangemPayMainScreen { tileBalance.assertDoesNotExist() }
            }
            step("Assert session expired banner is displayed") { assertTangemPaySessionExpiredBanner() }
        }
    }

    @Ignore("[REDACTED_JIRA]")
    @AllureId("9586")
    @DisplayName("Tangem Pay: Main keeps the cached KYC in progress status when customer/me fails")
    @Test
    fun mainScreenKeepsCachedKycInProgressWhenCustomerRequestFailsTest() {
        val kycInProgressState = "InProgress"
        val kycInProgressText = getResourceString(CoreResR.string.tangempay_kyc_in_progress)
        val issuingCardText = getResourceString(CoreResR.string.tangempay_issuing_your_card)
        val customerInfoEndpoint = "/bff-v2/v1/customer/me"

        setupHooks(
            additionalBeforeSection = {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
                setWireMockScenarioState(TANGEM_PAY_KYC_STATUS_SCENARIO, kycInProgressState)
            }
        ).run {
            step("Open 'Main' screen with existing hot wallet") {
                openMainScreenWithExistingHotWallet(SVS_SEED_PHRASE_12, accessCode = TANGEM_PAY_ACCESS_CODE)
            }
            step("Assert Tangem Pay tile with '$kycInProgressText' is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onTangemPayMainScreen { tileWithSubtitle(kycInProgressText).assertIsDisplayed() }
                }
            }
            step("Open and close the KYC in progress sheet") { openAndCloseTangemPayKycInProgressSheet() }
            step("Set WireMock scenario '$sessionScenario' to '$serviceUnavailableState'") {
                setWireMockScenarioState(sessionScenario, serviceUnavailableState)
            }
            val customerInfoBefore = getWireMockRequestCount("GET", customerInfoEndpoint)
            step("Force-close and re-launch the app") { restartApp(packageName) }
            step("Assert the relaunch requested customer info") {
                awaitSuccess {
                    if (getWireMockRequestCount("GET", customerInfoEndpoint) <= customerInfoBefore) {
                        throw AssertionError("The relaunch should request $customerInfoEndpoint")
                    }
                }
            }
            step("Assert Tangem Pay tile with '$kycInProgressText' is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onTangemPayMainScreen { tileWithSubtitle(kycInProgressText).assertIsDisplayed() }
                }
            }
            step("Assert Tangem Pay tile with '$issuingCardText' is not displayed") {
                onTangemPayMainScreen { tileWithSubtitle(issuingCardText).assertDoesNotExist() }
            }
        }
    }
}