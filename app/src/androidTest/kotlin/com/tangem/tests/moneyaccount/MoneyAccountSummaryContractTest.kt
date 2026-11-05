package com.tangem.tests.moneyaccount

import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.TANGEM_PAY_ELIGIBILITY_SCENARIO
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT
import com.tangem.common.extensions.getQuantityString
import com.tangem.common.utils.getWireMockRequestCountByQueryParam
import com.tangem.common.utils.resetWireMockScenarioState
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.res.R as CoreResR
import com.tangem.scenarios.openMainWithMoneyAccountWallet
import com.tangem.scenarios.refreshMainUntilCustomerSummaryRequested
import com.tangem.scenarios.refreshMainUntilMoneyAccountBalance
import com.tangem.scenarios.waitForMoneyAccountRowWithText
import com.tangem.screens.moneyaccount.onMoneyAccountScreen
import com.tangem.screens.onMainScreen
import com.tangem.screens.onWarningBottomSheet
import com.tangem.screens.tangempay.onTangemPayMainScreen
import com.tangem.screens.tangempay.onTangemPayVerifiedScreen
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.kakaocup.kakao.common.utilities.getResourceString
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Assert.assertTrue
import org.junit.Test

@HiltAndroidTest
class MoneyAccountSummaryContractTest : BaseTestCase() {

    override val featureToggleOverrides = mapOf(
        FeatureToggles.TWI_1525_MONEY_ACCOUNTS_ENABLED.rawName to true,
    )

    private val eligibilityState = "PaeraCustomer"
    private val moneyAccountScenario = "tangem_pay_money_account"
    private val moneyAccountActiveState = "AccountActive"
    private val kycApprovedNotStartedState = "KycApprovedNotStarted"

    private val initialBalance = "$25.00"
    private val oneCardSubtitle = getQuantityString(CoreResR.plurals.tangempay_cards_count, 1, 1)
    private val actionRequiredText = getResourceString(CoreResR.string.tangempay_action_required_title)

    @AllureId("13648")
    @DisplayName("Money account: NOT_STARTED with approved KYC shows no account row and 'Action required'")
    @Test
    fun notStartedAccountWithApprovedKycShowsActionRequiredTest() {
        setupHooks().run {
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to '$eligibilityState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
            }
            step("Set WireMock scenario '$moneyAccountScenario' to '$kycApprovedNotStartedState'") {
                setWireMockScenarioState(moneyAccountScenario, kycApprovedNotStartedState)
            }
            step("Open 'Main' screen of the wallet with Tangem Pay") { openMainWithMoneyAccountWallet() }
            step("Assert Tangem Pay tile with '$actionRequiredText' is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT) {
                    onTangemPayMainScreen { tileWithSubtitle(actionRequiredText).assertIsDisplayed() }
                }
            }
            step("Assert 'Money account' row is not displayed") {
                onMoneyAccountScreen { mainRow.assertDoesNotExist() }
            }
            step("Click on Tangem Pay tile") { onTangemPayMainScreen { clickMainScreenTileViaOnClickAction() } }
            step("Assert 'You're verified' screen is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT) { onTangemPayVerifiedScreen { title.assertIsDisplayed() } }
            }
        }
    }

    @AllureId("13650")
    @DisplayName("Money account: FAILED / ISSUING / RAIN_REJECTED shows a failed row and the activation error")
    @Test
    fun rainRejectedAccountShowsActivationErrorTest() {
        val moneyAccountRainRejectedState = "AccountFailedRainRejected"
        val failedStatus = getResourceString(CoreResR.string.money_account_main_status_failed)
        val activationFailedTitle = getResourceString(CoreResR.string.money_account_main_status_dialog_failed_title)
        val activationFailedDescription =
            getResourceString(CoreResR.string.money_account_main_status_dialog_failed_description)

        setupHooks().run {
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to '$eligibilityState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
            }
            step("Set WireMock scenario '$moneyAccountScenario' to '$moneyAccountRainRejectedState'") {
                setWireMockScenarioState(moneyAccountScenario, moneyAccountRainRejectedState)
            }
            step("Open 'Main' screen of the wallet with the account") { openMainWithMoneyAccountWallet() }
            step("Assert 'Money account' row shows '$failedStatus'") {
                waitForMoneyAccountRowWithText(failedStatus)
            }
            step("Click on 'Money account' row") { onMoneyAccountScreen { clickMainRow() } }
            step("Assert '$activationFailedTitle' title is displayed") {
                onMoneyAccountScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT) { modalStatusTitle(activationFailedTitle).assertIsDisplayed() }
                }
            }
            step("Assert '$activationFailedDescription' description is displayed") {
                onMoneyAccountScreen { modalStatusDescription(activationFailedDescription).assertIsDisplayed() }
            }
            step("Assert 'Contact support' button is displayed") {
                onMoneyAccountScreen { contactSupportButton.assertIsDisplayed() }
            }
        }
    }

    @AllureId("13653")
    @DisplayName("Money account: UNAVAILABLE shows no account row")
    @Test
    fun unavailableAccountShowsNoRowTest() {
        setupHooks().run {
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to '$eligibilityState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
            }
            step("Open 'Main' screen of the wallet with Tangem Pay") { openMainWithMoneyAccountWallet() }
            step("Assert Tangem Pay tile with '$oneCardSubtitle' is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT) {
                    onTangemPayMainScreen { tileWithSubtitle(oneCardSubtitle).assertIsDisplayed() }
                }
            }
            step("Assert 'Money account' row is not displayed") {
                onMoneyAccountScreen { mainRow.assertDoesNotExist() }
            }
        }
    }

    @AllureId("13654")
    @DisplayName("Money account: 'Action required' checks eligibility of both products")
    @Test
    fun actionRequiredChecksEligibilityOfBothProductsTest() {
        val eligibilityCheckPath = "/bff-v2/v1/eligibility/check"
        val productParam = "product"
        val bankingAccountProduct = "BANKING_ACCOUNT"
        val paymentAccountProduct = "PAYMENT_ACCOUNT"
        var bankingChecksBefore = 0
        var paymentChecksBefore = 0

        setupHooks().run {
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to '$eligibilityState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
            }
            step("Set WireMock scenario '$moneyAccountScenario' to '$kycApprovedNotStartedState'") {
                setWireMockScenarioState(moneyAccountScenario, kycApprovedNotStartedState)
            }
            step("Open 'Main' screen of the wallet with Tangem Pay") { openMainWithMoneyAccountWallet() }
            step("Assert Tangem Pay tile with '$actionRequiredText' is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT) {
                    onTangemPayMainScreen { tileWithSubtitle(actionRequiredText).assertIsDisplayed() }
                }
            }
            step("Read 'eligibility/check' request counts") {
                bankingChecksBefore = getWireMockRequestCountByQueryParam(
                    method = "GET",
                    urlPath = eligibilityCheckPath,
                    queryParam = productParam,
                    queryValue = bankingAccountProduct,
                )
                paymentChecksBefore = getWireMockRequestCountByQueryParam(
                    method = "GET",
                    urlPath = eligibilityCheckPath,
                    queryParam = productParam,
                    queryValue = paymentAccountProduct,
                )
            }
            step("Click on Tangem Pay tile") { onTangemPayMainScreen { clickMainScreenTileViaOnClickAction() } }
            step("Assert 'You're verified' screen is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT) { onTangemPayVerifiedScreen { title.assertIsDisplayed() } }
            }
            step("Assert 'eligibility/check' is requested for '$bankingAccountProduct'") {
                val bankingChecksAfter = getWireMockRequestCountByQueryParam(
                    method = "GET",
                    urlPath = eligibilityCheckPath,
                    queryParam = productParam,
                    queryValue = bankingAccountProduct,
                )
                assertTrue(
                    "Expected a new '$bankingAccountProduct' check: before=$bankingChecksBefore, after=$bankingChecksAfter",
                    bankingChecksAfter > bankingChecksBefore,
                )
            }
            step("Assert 'eligibility/check' is requested for '$paymentAccountProduct'") {
                val paymentChecksAfter = getWireMockRequestCountByQueryParam(
                    method = "GET",
                    urlPath = eligibilityCheckPath,
                    queryParam = productParam,
                    queryValue = paymentAccountProduct,
                )
                assertTrue(
                    "Expected a new '$paymentAccountProduct' check: before=$paymentChecksBefore, after=$paymentChecksAfter",
                    paymentChecksAfter > paymentChecksBefore,
                )
            }
        }
    }

    @AllureId("13657")
    @DisplayName("Money account: FORMER customer with a deactivated account shows no account row")
    @Test
    fun formerCustomerShowsNoAccountRowTest() {
        val customerFormerState = "CustomerFormer"
        val accountClosedStatus = getResourceString(CoreResR.string.tangempay_status_deactivated)

        setupHooks().run {
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to '$eligibilityState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
            }
            step("Set WireMock scenario '$moneyAccountScenario' to '$customerFormerState'") {
                setWireMockScenarioState(moneyAccountScenario, customerFormerState)
            }
            step("Open 'Main' screen of the wallet with Tangem Pay") { openMainWithMoneyAccountWallet() }
            step("Assert Tangem Pay tile with '$accountClosedStatus' is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT) {
                    onTangemPayMainScreen { tileWithSubtitle(accountClosedStatus).assertIsDisplayed() }
                }
            }
            step("Assert 'Money account' row is not displayed") {
                onMoneyAccountScreen { mainRow.assertDoesNotExist() }
            }
        }
    }

    @AllureId("13658")
    @DisplayName("Money account: the 'hidden' field of customer/summary does not change Main")
    @Test
    fun hiddenFieldDoesNotAffectMainTest() {
        val moneyAccountHiddenState = "AccountHidden"

        setupHooks().run {
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to '$eligibilityState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
            }
            step("Set WireMock scenario '$moneyAccountScenario' to '$moneyAccountHiddenState'") {
                setWireMockScenarioState(moneyAccountScenario, moneyAccountHiddenState)
            }
            step("Open 'Main' screen of the wallet with the account") { openMainWithMoneyAccountWallet() }
            step("Assert 'Money account' row shows '$initialBalance'") {
                waitForMoneyAccountRowWithText(initialBalance)
            }
            step("Assert Tangem Pay tile with '$oneCardSubtitle' is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT) {
                    onTangemPayMainScreen { tileWithSubtitle(oneCardSubtitle).assertIsDisplayed() }
                }
            }
            step("Switch WireMock scenario '$moneyAccountScenario' to '$moneyAccountActiveState'") {
                setWireMockScenarioState(moneyAccountScenario, moneyAccountActiveState)
            }
            step("Refresh 'Main' screen") { refreshMainUntilCustomerSummaryRequested() }
            step("Assert 'Money account' row still shows '$initialBalance'") {
                flakySafely(WAIT_UNTIL_TIMEOUT) {
                    onMoneyAccountScreen { mainRowWithText(initialBalance).assertIsDisplayed() }
                }
            }
            step("Assert Tangem Pay tile still shows '$oneCardSubtitle'") {
                onTangemPayMainScreen { tileWithSubtitle(oneCardSubtitle).assertIsDisplayed() }
            }
        }
    }

    @AllureId("13661")
    @DisplayName("Money account: a customer/summary without a product object does not crash Main")
    @Test
    fun summaryWithoutProductObjectDoesNotCrashTest() {
        val withoutBankingAccountState = "SummaryWithoutBankingAccount"
        val withoutPaymentAccountState = "SummaryWithoutPaymentAccount"
        val withoutCardsState = "SummaryWithoutCards"

        setupHooks().run {
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to '$eligibilityState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
            }
            step("Set WireMock scenario '$moneyAccountScenario' to '$withoutBankingAccountState'") {
                setWireMockScenarioState(moneyAccountScenario, withoutBankingAccountState)
            }
            step("Open 'Main' screen of the wallet with Tangem Pay") { openMainWithMoneyAccountWallet() }
            step("Refresh 'Main' screen") { refreshMainUntilCustomerSummaryRequested() }
            step("Assert 'Main' screen is displayed") { onMainScreen { screenContainer.assertIsDisplayed() } }
            step("Assert 'Money account' row is not displayed") {
                onMoneyAccountScreen { mainRow.assertDoesNotExist() }
            }
            step("Switch WireMock scenario '$moneyAccountScenario' to '$withoutPaymentAccountState'") {
                setWireMockScenarioState(moneyAccountScenario, withoutPaymentAccountState)
            }
            step("Refresh 'Main' screen") { refreshMainUntilCustomerSummaryRequested() }
            step("Assert 'Main' screen is displayed") { onMainScreen { screenContainer.assertIsDisplayed() } }
            step("Switch WireMock scenario '$moneyAccountScenario' to '$withoutCardsState'") {
                setWireMockScenarioState(moneyAccountScenario, withoutCardsState)
            }
            step("Refresh 'Main' screen") { refreshMainUntilCustomerSummaryRequested() }
            step("Assert 'Main' screen is displayed") { onMainScreen { screenContainer.assertIsDisplayed() } }
            step("Assert 'Money account' row is not displayed") {
                onMoneyAccountScreen { mainRow.assertDoesNotExist() }
            }
        }
    }

    @AllureId("13662")
    @DisplayName("Money account: a failing customer/summary keeps the cached account row")
    @Test
    fun failingSummaryKeepsCachedAccountRowTest() {
        val summaryErrorScenario = "money_account_customer_summary"
        val serverErrorState = "ServerError"
        val connectionResetState = "ConnectionReset"
        val usdcBalanceScenario = "money_account_usdc_balance"
        val usdcBalanceAfterDepositState = "AfterDeposit"
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
            step("Set WireMock scenario '$summaryErrorScenario' to '$serverErrorState'") {
                setWireMockScenarioState(summaryErrorScenario, serverErrorState)
            }
            step("Refresh 'Main' screen") { refreshMainUntilCustomerSummaryRequested() }
            step("Assert 'Money account' row shows the cache icon") {
                flakySafely(WAIT_UNTIL_TIMEOUT) { onMoneyAccountScreen { mainRowCacheIcon.assertIsDisplayed() } }
            }
            step("Assert 'Money account' row shows the cached '$initialBalance'") {
                onMoneyAccountScreen { mainRowWithText(initialBalance).assertIsDisplayed() }
            }
            step("Set WireMock scenario '$summaryErrorScenario' to '$connectionResetState'") {
                setWireMockScenarioState(summaryErrorScenario, connectionResetState)
            }
            step("Refresh 'Main' screen") { refreshMainUntilCustomerSummaryRequested() }
            step("Assert 'Money account' row shows the cache icon") {
                flakySafely(WAIT_UNTIL_TIMEOUT) { onMoneyAccountScreen { mainRowCacheIcon.assertIsDisplayed() } }
            }
            step("Assert 'Money account' row shows the cached '$initialBalance'") {
                onMoneyAccountScreen { mainRowWithText(initialBalance).assertIsDisplayed() }
            }
            step("Reset WireMock scenario '$summaryErrorScenario'") { resetWireMockScenarioState(summaryErrorScenario) }
            step("Set WireMock scenario '$usdcBalanceScenario' to '$usdcBalanceAfterDepositState'") {
                setWireMockScenarioState(usdcBalanceScenario, usdcBalanceAfterDepositState)
            }
            step("Refresh 'Main' screen") { refreshMainUntilMoneyAccountBalance(refreshedBalance) }
            step("Assert 'Money account' row shows no cache icon") {
                onMoneyAccountScreen { mainRowCacheIcon.assertDoesNotExist() }
            }
        }
    }

    @AllureId("13667")
    @DisplayName("Money account: tapping the row that waits for the plan deposit opens 'Creating your banking account'")
    @Test
    fun awaitingDepositAccountRowOpensCreatingAccountSheetTest() {
        val moneyAccountAwaitingDepositState = "AccountAwaitingDeposit"
        val awaitingDepositStatus = getResourceString(CoreResR.string.money_account_main_status_awaiting_deposit)
        val creatingAccountSheetTitle =
            getResourceString(CoreResR.string.money_account_main_status_dialog_activating_title)

        setupHooks().run {
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to '$eligibilityState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
            }
            step("Set WireMock scenario '$moneyAccountScenario' to '$moneyAccountAwaitingDepositState'") {
                setWireMockScenarioState(moneyAccountScenario, moneyAccountAwaitingDepositState)
            }
            step("Open 'Main' screen of the wallet with the account") { openMainWithMoneyAccountWallet() }
            step("Assert 'Money account' row shows '$awaitingDepositStatus'") {
                waitForMoneyAccountRowWithText(awaitingDepositStatus)
            }
            step("Click on 'Money account' row") { onMoneyAccountScreen { clickMainRow() } }
            step("Assert '$creatingAccountSheetTitle' sheet is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT) {
                    onWarningBottomSheet { title(creatingAccountSheetTitle).assertIsDisplayed() }
                }
            }
        }
    }
}