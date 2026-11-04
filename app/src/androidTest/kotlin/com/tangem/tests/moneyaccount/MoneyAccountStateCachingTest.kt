package com.tangem.tests.moneyaccount

import androidx.test.InstrumentationRegistry.getTargetContext
import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.TANGEM_PAY_ELIGIBILITY_SCENARIO
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_LONG
import com.tangem.common.extensions.restartApp
import com.tangem.common.utils.resetWireMockScenarioState
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.res.R as CoreResR
import com.tangem.scenarios.assertMoneyAccountActionsDisabled
import com.tangem.scenarios.assertMoneyAccountRowShowsCachedBalance
import com.tangem.scenarios.assertTangemPaySessionExpiredBanner
import com.tangem.scenarios.clickMoneyAccountRowUntilDetailsOpen
import com.tangem.scenarios.closeFinishWalletActivationSheetIfShown
import com.tangem.scenarios.openMainWithMoneyAccountWallet
import com.tangem.scenarios.openMoneyAccount
import com.tangem.scenarios.refreshMainUntilCustomerSummaryRequested
import com.tangem.scenarios.refreshMainUntilMoneyAccountCacheIcon
import com.tangem.scenarios.returnToMainFromMoneyAccount
import com.tangem.scenarios.waitForMoneyAccountRowWithText
import com.tangem.screens.moneyaccount.onMoneyAccountScreen
import com.tangem.screens.onTxHistoryScreen
import com.tangem.screens.tangempay.onTangemPayMainScreen
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.kakaocup.kakao.common.utilities.getResourceString
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Ignore
import org.junit.Test

@HiltAndroidTest
class MoneyAccountStateCachingTest : BaseTestCase() {

    override val featureToggleOverrides = mapOf(
        FeatureToggles.TWI_1525_MONEY_ACCOUNTS_ENABLED.rawName to true,
    )

    private val eligibilityState = "PaeraCustomer"
    private val moneyAccountScenario = "tangem_pay_money_account"
    private val moneyAccountActiveState = "AccountActive"
    private val sessionScenario = "tangem_pay_session"
    private val sessionExpiredState = "SessionExpired"
    private val noConnectionState = "NoConnection"
    private val usdcHistoryScenario = "money_account_usdc_history"
    private val usdcHistoryAfterDepositState = "AfterDeposit"

    private val packageName = getTargetContext().packageName
    private val cachedBalance = "$25.00"
    private val receivedTitle = getResourceString(CoreResR.string.common_received)

    @AllureId("13569")
    @DisplayName("Money account: Main row keeps the cached balance when the session has expired")
    @Test
    fun mainRowShowsCachedBalanceWhenSessionExpiredTest() {
        setupHooks().run {
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to '$eligibilityState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
            }
            step("Set WireMock scenario '$moneyAccountScenario' to '$moneyAccountActiveState'") {
                setWireMockScenarioState(moneyAccountScenario, moneyAccountActiveState)
            }
            step("Open 'Main' screen of the wallet with the account") { openMainWithMoneyAccountWallet() }
            step("Assert 'Money account' row shows '$cachedBalance'") { waitForMoneyAccountRowWithText(cachedBalance) }
            step("Set WireMock scenario '$sessionScenario' to '$sessionExpiredState'") {
                setWireMockScenarioState(sessionScenario, sessionExpiredState)
            }
            step("Refresh 'Main' screen") { refreshMainUntilMoneyAccountCacheIcon() }
            step("Assert 'Money account' row shows the cached data") {
                assertMoneyAccountRowShowsCachedBalance(cachedBalance)
            }
        }
    }

    @AllureId("13570")
    @DisplayName("Money account: Main row keeps the cached balance without a connection")
    @Test
    fun mainRowShowsCachedBalanceWithoutConnectionTest() {
        setupHooks().run {
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to '$eligibilityState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
            }
            step("Set WireMock scenario '$moneyAccountScenario' to '$moneyAccountActiveState'") {
                setWireMockScenarioState(moneyAccountScenario, moneyAccountActiveState)
            }
            step("Open 'Main' screen of the wallet with the account") { openMainWithMoneyAccountWallet() }
            step("Assert 'Money account' row shows '$cachedBalance'") { waitForMoneyAccountRowWithText(cachedBalance) }
            step("Set WireMock scenario '$sessionScenario' to '$noConnectionState'") {
                setWireMockScenarioState(sessionScenario, noConnectionState)
            }
            step("Force-close and re-launch the app") { restartApp(packageName) }
            step("Assert 'Money account' row shows the cached data") {
                assertMoneyAccountRowShowsCachedBalance(cachedBalance)
            }
        }
    }

    @AllureId("13571")
    @DisplayName("Money account: Main hides the account until the session is renewed when the cache is empty")
    @Test
    fun mainScreenHidesAccountWhenCacheIsEmptyAndSessionExpiredTest() {
        setupHooks().run {
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to '$eligibilityState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
            }
            step("Set WireMock scenario '$moneyAccountScenario' to '$moneyAccountActiveState'") {
                setWireMockScenarioState(moneyAccountScenario, moneyAccountActiveState)
            }
            step("Set WireMock scenario '$sessionScenario' to '$sessionExpiredState'") {
                setWireMockScenarioState(sessionScenario, sessionExpiredState)
            }
            step("Open 'Main' screen of the wallet with the account") { openMainWithMoneyAccountWallet() }
            step("Assert session expired banner is displayed") { assertTangemPaySessionExpiredBanner() }
            step("Assert 'Money account' row is not displayed") {
                onMoneyAccountScreen { mainRow.assertDoesNotExist() }
            }
            step("Reset WireMock scenario '$sessionScenario'") { resetWireMockScenarioState(sessionScenario) }
            step("Click on 'Renew session' button") {
                onTangemPayMainScreen { renewSessionButton.performClick() }
            }
            step("Assert 'Money account' row shows '$cachedBalance'") { waitForMoneyAccountRowWithText(cachedBalance) }
        }
    }

    @AllureId("13572")
    @DisplayName("Money account: account screen shows the renew session banner over the cached data")
    @Test
    fun accountScreenShowsRenewSessionBannerOverCacheTest() {
        setupHooks().run {
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to '$eligibilityState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
            }
            step("Set WireMock scenario '$moneyAccountScenario' to '$moneyAccountActiveState'") {
                setWireMockScenarioState(moneyAccountScenario, moneyAccountActiveState)
            }
            step("Set WireMock scenario '$usdcHistoryScenario' to '$usdcHistoryAfterDepositState'") {
                setWireMockScenarioState(usdcHistoryScenario, usdcHistoryAfterDepositState)
            }
            step("Open 'Money account' screen") { openMoneyAccount() }
            step("Assert '$receivedTitle' transaction is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onTxHistoryScreen { transactionItem(receivedTitle).assertIsDisplayed() }
                }
            }
            step("Return to 'Main' screen") { returnToMainFromMoneyAccount() }
            step("Set WireMock scenario '$sessionScenario' to '$sessionExpiredState'") {
                setWireMockScenarioState(sessionScenario, sessionExpiredState)
            }
            step("Force-close and re-launch the app") { restartApp(packageName) }
            step("Close 'Finalize wallet setup' sheet if shown") { closeFinishWalletActivationSheetIfShown() }
            step("Click on 'Money account' row") { clickMoneyAccountRowUntilDetailsOpen() }
            step("Assert session expired banner is displayed") {
                onMoneyAccountScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { sessionExpiredBannerTitle.assertIsDisplayed() }
                    renewSessionButton.assertIsDisplayed()
                }
            }
            step("Assert 'Add funds' and 'Send' are disabled") { assertMoneyAccountActionsDisabled() }
            step("Assert '$receivedTitle' transaction is displayed") {
                onTxHistoryScreen { transactionItem(receivedTitle).assertIsDisplayed() }
            }
        }
    }

    @Ignore("[REDACTED_JIRA]")
    @AllureId("13573")
    @DisplayName("Money account: account screen shows the balance unavailable banner without a connection")
    @Test
    fun accountScreenShowsBalanceUnavailableBannerWithoutConnectionTest() {
        setupHooks().run {
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to '$eligibilityState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
            }
            step("Set WireMock scenario '$moneyAccountScenario' to '$moneyAccountActiveState'") {
                setWireMockScenarioState(moneyAccountScenario, moneyAccountActiveState)
            }
            step("Set WireMock scenario '$usdcHistoryScenario' to '$usdcHistoryAfterDepositState'") {
                setWireMockScenarioState(usdcHistoryScenario, usdcHistoryAfterDepositState)
            }
            step("Open 'Money account' screen") { openMoneyAccount() }
            step("Assert '$receivedTitle' transaction is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onTxHistoryScreen { transactionItem(receivedTitle).assertIsDisplayed() }
                }
            }
            step("Set WireMock scenario '$sessionScenario' to '$noConnectionState'") {
                setWireMockScenarioState(sessionScenario, noConnectionState)
            }
            step("Return to 'Main' screen") { returnToMainFromMoneyAccount() }
            step("Click on 'Money account' row") { clickMoneyAccountRowUntilDetailsOpen() }
            step("Assert 'Balance unavailable' banner is displayed") {
                onMoneyAccountScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { balanceUnavailableBannerTitle.assertIsDisplayed() }
                }
            }
            step("Assert 'Add funds' and 'Send' are disabled") { assertMoneyAccountActionsDisabled() }
            step("Assert '$receivedTitle' transaction is displayed") {
                onTxHistoryScreen { transactionItem(receivedTitle).assertIsDisplayed() }
            }
        }
    }

    @AllureId("13590")
    @DisplayName("Money account: the account row appears together with the card after Renew session")
    @Test
    fun accountRowAppearsWithCardAfterRenewSessionTest() {
        setupHooks().run {
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to '$eligibilityState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
            }
            step("Set WireMock scenario '$moneyAccountScenario' to '$moneyAccountActiveState'") {
                setWireMockScenarioState(moneyAccountScenario, moneyAccountActiveState)
            }
            step("Set WireMock scenario '$sessionScenario' to '$sessionExpiredState'") {
                setWireMockScenarioState(sessionScenario, sessionExpiredState)
            }
            step("Open 'Main' screen of the wallet with the account") { openMainWithMoneyAccountWallet() }
            step("Assert session expired banner is displayed") { assertTangemPaySessionExpiredBanner() }
            step("Reset WireMock scenario '$sessionScenario'") { resetWireMockScenarioState(sessionScenario) }
            step("Click on 'Renew session' button") {
                onTangemPayMainScreen { renewSessionButton.performClick() }
            }
            step("Assert Tangem Pay tile balance and 'Money account' row with '$cachedBalance' are displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onTangemPayMainScreen { tileBalance.assertIsDisplayed() }
                    onMoneyAccountScreen { mainRowWithText(cachedBalance).assertIsDisplayed() }
                }
            }
        }
    }

    @AllureId("13662")
    @DisplayName("Money account: Main row keeps the cached balance when customer/summary fails")
    @Test
    fun mainRowKeepsCachedBalanceWhenCustomerSummaryFailsTest() {
        val summaryScenario = "money_account_customer_summary"
        val summaryServerErrorState = "ServerError"
        val summaryConnectionResetState = "ConnectionReset"

        setupHooks().run {
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to '$eligibilityState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
            }
            step("Set WireMock scenario '$moneyAccountScenario' to '$moneyAccountActiveState'") {
                setWireMockScenarioState(moneyAccountScenario, moneyAccountActiveState)
            }
            step("Open 'Main' screen of the wallet with the account") { openMainWithMoneyAccountWallet() }
            step("Assert 'Money account' row shows '$cachedBalance'") { waitForMoneyAccountRowWithText(cachedBalance) }
            step("Set WireMock scenario '$summaryScenario' to '$summaryServerErrorState'") {
                setWireMockScenarioState(summaryScenario, summaryServerErrorState)
            }
            step("Refresh 'Main' screen") { refreshMainUntilMoneyAccountCacheIcon() }
            step("Assert 'Money account' row shows the cached data") {
                assertMoneyAccountRowShowsCachedBalance(cachedBalance)
            }
            step("Set WireMock scenario '$summaryScenario' to '$summaryConnectionResetState'") {
                setWireMockScenarioState(summaryScenario, summaryConnectionResetState)
            }
            step("Refresh 'Main' screen") { refreshMainUntilCustomerSummaryRequested() }
            step("Assert 'Money account' row shows the cached data") {
                assertMoneyAccountRowShowsCachedBalance(cachedBalance)
            }
            step("Reset WireMock scenario '$summaryScenario'") { resetWireMockScenarioState(summaryScenario) }
            step("Refresh 'Main' screen") { refreshMainUntilCustomerSummaryRequested() }
            step("Assert cached-data icon is not displayed on the 'Money account' row") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onMoneyAccountScreen { mainRowCacheIcon.assertDoesNotExist() }
                }
            }
            step("Assert 'Money account' row shows '$cachedBalance'") {
                onMoneyAccountScreen { mainRowWithText(cachedBalance).assertIsDisplayed() }
            }
        }
    }
}