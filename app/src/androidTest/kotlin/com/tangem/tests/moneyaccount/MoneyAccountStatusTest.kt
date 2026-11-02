package com.tangem.tests.moneyaccount

import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.TANGEM_PAY_ELIGIBILITY_SCENARIO
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.res.R as CoreResR
import com.tangem.scenarios.clickMoneyAccountRowUntilDetailsOpen
import com.tangem.scenarios.forgetLastHotWallet
import com.tangem.scenarios.openMainWithMoneyAccountWallet
import com.tangem.scenarios.waitForMoneyAccountRowWithText
import com.tangem.scenarios.waitForMoneyAccountRowWithTextNeverShowing
import com.tangem.screens.moneyaccount.onMoneyAccountScreen
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.kakaocup.kakao.common.utilities.getResourceString
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
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

    private val initialBalance = "$25.00"

    @AllureId("13367")
    @DisplayName("Money account: the issuing account row on Main turns active without a restart")
    @Test
    fun issuingAccountRowTurnsActiveWithoutRestartTest() {
        val moneyAccountIssuingState = "AccountIssuing"
        val usdcBalanceZeroState = "Zero"
        val zeroBalance = "$0.00"
        val activatingStatus = getResourceString(CoreResR.string.money_account_main_status_activating)
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
}