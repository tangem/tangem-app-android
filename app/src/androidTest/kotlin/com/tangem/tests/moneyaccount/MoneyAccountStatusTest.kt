package com.tangem.tests.moneyaccount

import androidx.test.InstrumentationRegistry.getTargetContext
import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.PROVIDERS_API_SCENARIO
import com.tangem.common.constants.TestConstants.TANGEM_PAY_ELIGIBILITY_SCENARIO
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_LONG
import com.tangem.common.extensions.restartApp
import com.tangem.common.utils.getWireMockRequestCount
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.res.R as CoreResR
import com.tangem.scenarios.assertMoneyAccountRowNeverShown
import com.tangem.scenarios.assertTangemPayRequestSentOnce
import com.tangem.scenarios.clickMoneyAccountRowUntilDetailsOpen
import com.tangem.scenarios.clickTangemPayTileUntilPaymentAccountOpens
import com.tangem.scenarios.forgetLastHotWallet
import com.tangem.scenarios.openMainWithMoneyAccountWallet
import com.tangem.scenarios.openTangemPayMoreActionsMenu
import com.tangem.scenarios.pullToRefreshMainScreen
import com.tangem.scenarios.refreshMainUntilMoneyAccountBalance
import com.tangem.scenarios.refreshMainUntilMoneyAccountRowHidden
import com.tangem.scenarios.waitForMoneyAccountRowWithText
import com.tangem.scenarios.waitForMoneyAccountRowWithTextNeverShowing
import com.tangem.screens.moneyaccount.onMoneyAccountScreen
import com.tangem.screens.onWarningBottomSheet
import com.tangem.screens.tangempay.onTangemPayMainScreen
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.kakaocup.kakao.common.utilities.getResourceString
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Ignore
import org.junit.Test

@HiltAndroidTest
class MoneyAccountStatusTest : BaseTestCase() {

    override val featureToggleOverrides = mapOf(
        FeatureToggles.TWI_1525_MONEY_ACCOUNTS_ENABLED.rawName to true,
    )

    private val eligibilityState = "PaeraCustomer"
    private val moneyAccountScenario = "tangem_pay_money_account"
    private val moneyAccountActiveState = "AccountActive"

    private val usdcBalanceScenario = "money_account_usdc_balance"

    private val moneyAccountIssuingState = "AccountIssuing"

    private val initialBalance = "$25.00"
    private val activatingStatus = getResourceString(CoreResR.string.money_account_main_status_activating)
    private val creatingAccountSheetTitle =
        getResourceString(CoreResR.string.money_account_main_status_dialog_activating_title)

    @AllureId("13366")
    @DisplayName("Money account: the active account tile on Main reads 'ACH'")
    @Test
    fun activeAccountTileShowsAchSubtitleTest() {
        val usdcBalanceAfterDepositState = "AfterDeposit"
        val accountName = getResourceString(CoreResR.string.money_account_main_name)
        val achSubtitle = getResourceString(CoreResR.string.money_account_main_status_active_v2)
        val refreshedBalance = "$35.00"

        setupHooks().run {
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to '$eligibilityState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
            }
            step("Set WireMock scenario '$moneyAccountScenario' to '$moneyAccountActiveState'") {
                setWireMockScenarioState(moneyAccountScenario, moneyAccountActiveState)
            }
            step("Open 'Main' screen of the wallet with the account") { openMainWithMoneyAccountWallet() }
            step("Assert 'Money account' row shows '$initialBalance'") {
                waitForMoneyAccountRowWithText(initialBalance)
            }
            step("Assert 'Money account' row shows '$accountName'") {
                onMoneyAccountScreen { mainRowWithText(accountName).assertIsDisplayed() }
            }
            step("Assert 'Money account' row shows '$achSubtitle'") {
                onMoneyAccountScreen { mainRowWithText(achSubtitle).assertIsDisplayed() }
            }
            step("Set WireMock scenario '$usdcBalanceScenario' to '$usdcBalanceAfterDepositState'") {
                setWireMockScenarioState(usdcBalanceScenario, usdcBalanceAfterDepositState)
            }
            step("Refresh 'Main' screen") { refreshMainUntilMoneyAccountBalance(refreshedBalance) }
            step("Assert 'Money account' row still shows '$achSubtitle'") {
                onMoneyAccountScreen { mainRowWithText(achSubtitle).assertIsDisplayed() }
            }
        }
    }

    @AllureId("13581")
    @DisplayName("Money account: tapping the activating account row opens 'Creating your banking account'")
    @Test
    fun activatingAccountRowOpensCreatingAccountSheetTest() {
        setupHooks().run {
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to '$eligibilityState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
            }
            step("Set WireMock scenario '$moneyAccountScenario' to '$moneyAccountIssuingState'") {
                setWireMockScenarioState(moneyAccountScenario, moneyAccountIssuingState)
            }
            step("Open 'Main' screen of the wallet with the account") { openMainWithMoneyAccountWallet() }
            step("Assert 'Money account' row shows '$activatingStatus'") {
                waitForMoneyAccountRowWithText(activatingStatus)
            }
            step("Click on 'Money account' row") { onMoneyAccountScreen { clickMainRow() } }
            step("Assert '$creatingAccountSheetTitle' sheet is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT) {
                    onWarningBottomSheet { title(creatingAccountSheetTitle).assertIsDisplayed() }
                }
            }
        }
    }

    @AllureId("13583")
    @DisplayName("Money account: tapping the failed account row opens the activation error")
    @Test
    fun failedAccountRowOpensActivationErrorTest() {
        val moneyAccountFailedState = "AccountFailed"
        val failedStatus = getResourceString(CoreResR.string.money_account_main_status_failed)
        val activationFailedTitle = getResourceString(CoreResR.string.money_account_main_status_dialog_failed_title)

        setupHooks().run {
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to '$eligibilityState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
            }
            step("Set WireMock scenario '$moneyAccountScenario' to '$moneyAccountFailedState'") {
                setWireMockScenarioState(moneyAccountScenario, moneyAccountFailedState)
            }
            step("Open 'Main' screen of the wallet with the account") { openMainWithMoneyAccountWallet() }
            step("Assert 'Money account' row shows '$failedStatus'") {
                waitForMoneyAccountRowWithText(failedStatus)
            }
            step("Click on 'Money account' row") { onMoneyAccountScreen { clickMainRow() } }
            step("Assert '$activationFailedTitle' sheet is displayed") {
                onMoneyAccountScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT) { modalStatusTitle(activationFailedTitle).assertIsDisplayed() }
                }
            }
        }
    }

    @AllureId("13594")
    @DisplayName("Money account: the account row waits for the plan deposit")
    @Test
    fun awaitingDepositAccountRowTest() {
        val moneyAccountAwaitingDepositState = "AccountAwaitingDeposit"
        val awaitingDepositStatus = getResourceString(CoreResR.string.money_account_main_status_awaiting_deposit)

        setupHooks().run {
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to '$eligibilityState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
            }
            step("Set WireMock scenario '$moneyAccountScenario' to '$moneyAccountAwaitingDepositState'") {
                setWireMockScenarioState(moneyAccountScenario, moneyAccountAwaitingDepositState)
            }
            step("Open 'Main' screen of the wallet with the account") { openMainWithMoneyAccountWallet() }
            step("Assert 'Money account' row shows '$awaitingDepositStatus' and never '$activatingStatus'") {
                waitForMoneyAccountRowWithTextNeverShowing(text = awaitingDepositStatus, forbiddenText = activatingStatus)
            }
        }
    }

    @AllureId("13367")
    @DisplayName("Money account: the issuing account row on Main turns active without a restart")
    @Test
    fun issuingAccountRowTurnsActiveWithoutRestartTest() {
        val usdcBalanceZeroState = "Zero"
        val zeroBalance = "$0.00"
        val activationPollTimeoutMillis = 45_000L
        val fiatSign = "$"

        setupHooks().run {
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to '$eligibilityState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
            }
            step("Set WireMock scenario '$moneyAccountScenario' to '$moneyAccountIssuingState'") {
                setWireMockScenarioState(moneyAccountScenario, moneyAccountIssuingState)
            }
            step("Set WireMock scenario '$usdcBalanceScenario' to '$usdcBalanceZeroState'") {
                setWireMockScenarioState(usdcBalanceScenario, usdcBalanceZeroState)
            }
            step("Open 'Main' screen of the wallet with the account") { openMainWithMoneyAccountWallet() }
            step("Assert 'Money account' row shows '$activatingStatus'") {
                waitForMoneyAccountRowWithText(activatingStatus)
            }
            step("Assert 'Money account' row shows no balance") {
                onMoneyAccountScreen { mainRowWithText(fiatSign).assertDoesNotExist() }
            }
            step("Switch WireMock scenario '$moneyAccountScenario' to '$moneyAccountActiveState'") {
                setWireMockScenarioState(moneyAccountScenario, moneyAccountActiveState)
            }
            step("Assert 'Money account' row shows '$zeroBalance' without a refresh") {
                waitForMoneyAccountRowWithText(zeroBalance, activationPollTimeoutMillis)
            }
            step("Assert 'Money account' row no longer shows '$activatingStatus'") {
                onMoneyAccountScreen { mainRowWithText(activatingStatus).assertDoesNotExist() }
            }
            step("Click on 'Money account' row") { clickMoneyAccountRowUntilDetailsOpen() }
            step("Assert 'Money account' screen shows '$zeroBalance'") {
                onMoneyAccountScreen { flakySafely(WAIT_UNTIL_TIMEOUT) { balance.assertTextEquals(zeroBalance) } }
            }
        }
    }

    @AllureId("13370")
    @DisplayName("Money account: forgetting the wallet clears the cached account status")
    @Test
    fun forgottenWalletDoesNotRestoreAccountFromCacheTest() {
        val moneyAccountActiveDelayedState = "AccountActiveDelayed"
        val usdcBalanceAfterDepositState = "AfterDeposit"
        val balanceFromBackend = "$35.00"

        setupHooks().run {
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to '$eligibilityState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
            }
            step("Set WireMock scenario '$moneyAccountScenario' to '$moneyAccountActiveState'") {
                setWireMockScenarioState(moneyAccountScenario, moneyAccountActiveState)
            }
            step("Open 'Main' screen of the wallet with the account") { openMainWithMoneyAccountWallet() }
            step("Assert 'Money account' row shows '$initialBalance'") {
                waitForMoneyAccountRowWithText(initialBalance)
            }
            step("Forget the wallet") { forgetLastHotWallet() }
            step("Switch WireMock scenario '$moneyAccountScenario' to '$moneyAccountActiveDelayedState'") {
                setWireMockScenarioState(moneyAccountScenario, moneyAccountActiveDelayedState)
            }
            step("Switch WireMock scenario '$usdcBalanceScenario' to '$usdcBalanceAfterDepositState'") {
                setWireMockScenarioState(usdcBalanceScenario, usdcBalanceAfterDepositState)
            }
            step("Import the same wallet again") {
                openMainWithMoneyAccountWallet(dismissMarketsTooltip = false)
            }
            step("Assert 'Money account' row shows '$balanceFromBackend' and never the cached '$initialBalance'") {
                waitForMoneyAccountRowWithTextNeverShowing(text = balanceFromBackend, forbiddenText = initialBalance)
            }
        }
    }

    @Ignore("[REDACTED_JIRA]")
    @AllureId("13585")
    @DisplayName("Money account: pulling Main to refresh requests customer/summary once")
    @Test
    fun mainPullToRefreshRequestsCustomerSummaryOnceTest() {
        val customerSummaryPath = "/bff-v2/v1/customer/summary"
        val usdcBalanceAfterDepositState = "AfterDeposit"
        val refreshedBalance = "$35.00"
        var summaryRequestsBefore = 0

        setupHooks().run {
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to '$eligibilityState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
            }
            step("Set WireMock scenario '$moneyAccountScenario' to '$moneyAccountActiveState'") {
                setWireMockScenarioState(moneyAccountScenario, moneyAccountActiveState)
            }
            step("Open 'Main' screen of the wallet with the account") { openMainWithMoneyAccountWallet() }
            step("Assert 'Money account' row shows '$initialBalance'") {
                waitForMoneyAccountRowWithText(initialBalance)
            }
            step("Set WireMock scenario '$usdcBalanceScenario' to '$usdcBalanceAfterDepositState'") {
                setWireMockScenarioState(usdcBalanceScenario, usdcBalanceAfterDepositState)
            }
            step("Count 'GET $customerSummaryPath' requests sent so far") {
                summaryRequestsBefore = getWireMockRequestCount("GET", customerSummaryPath)
            }
            step("Pull 'Main' screen down to refresh") { pullToRefreshMainScreen() }
            // The new balance is read after the summary, so once it shows every summary request of the pull is sent.
            step("Assert 'Money account' row shows '$refreshedBalance'") {
                waitForMoneyAccountRowWithText(refreshedBalance)
            }
            step("Assert 'GET $customerSummaryPath' is requested once for the pull") {
                assertTangemPayRequestSentOnce("GET", customerSummaryPath, summaryRequestsBefore)
            }
        }
    }

    @AllureId("13670")
    @DisplayName("Money account: a closed account stays hidden on Main and is not offered again")
    @Test
    fun closedAccountStaysHiddenOnMainTest() {
        val moneyAccountDeactivatedState = "AccountDeactivated"
        val moneyAccountDeactivatedDelayedState = "AccountDeactivatedDelayed"
        val cachedStateWindowMillis = 10_000L
        val providersPolygonMockOnlyState = "PolygonMockOnly"
        val packageName = getTargetContext().packageName

        setupHooks(
            additionalBeforeAppLaunchSection = {
                setWireMockScenarioState(PROVIDERS_API_SCENARIO, providersPolygonMockOnlyState)
            },
        ).run {
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to '$eligibilityState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
            }
            step("Set WireMock scenario '$moneyAccountScenario' to '$moneyAccountActiveState'") {
                setWireMockScenarioState(moneyAccountScenario, moneyAccountActiveState)
            }
            step("Open 'Main' screen of the wallet with the account") { openMainWithMoneyAccountWallet() }
            step("Assert 'Money account' row shows '$initialBalance'") {
                waitForMoneyAccountRowWithText(initialBalance)
            }
            step("Switch WireMock scenario '$moneyAccountScenario' to '$moneyAccountDeactivatedState'") {
                setWireMockScenarioState(moneyAccountScenario, moneyAccountDeactivatedState)
            }
            refreshMainUntilMoneyAccountRowHidden()
            // The closed status reaches the persisted cache after the row hides; a kill right away restores 'Active'.
            step("Assert 'Money account' row stays hidden") { assertMoneyAccountRowNeverShown() }
            step("Switch WireMock scenario '$moneyAccountScenario' to '$moneyAccountDeactivatedDelayedState'") {
                setWireMockScenarioState(moneyAccountScenario, moneyAccountDeactivatedDelayedState)
            }
            step("Force-close and re-launch the app") { restartApp(packageName) }
            step("Assert 'Money account' row with the cached '$initialBalance' never shows") {
                assertMoneyAccountRowNeverShown(durationMillis = cachedStateWindowMillis)
            }
            step("Assert Tangem Pay tile is displayed") {
                awaitSuccess(WAIT_UNTIL_TIMEOUT_LONG) { onTangemPayMainScreen { tileBalance.assertIsDisplayed() } }
            }
            step("Click on Tangem Pay tile") { clickTangemPayTileUntilPaymentAccountOpens() }
            openTangemPayMoreActionsMenu()
            step("Assert 'Remove account' menu item is not displayed") {
                onTangemPayMainScreen { removeAccountMenuItem.assertDoesNotExist() }
            }
        }
    }
}