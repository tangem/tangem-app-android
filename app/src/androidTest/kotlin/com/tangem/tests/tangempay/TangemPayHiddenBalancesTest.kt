package com.tangem.tests.tangempay

import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TangemPayBankAccounts.JOHN
import com.tangem.common.constants.TestConstants.TANGEM_PAY_ELIGIBILITY_CHANNELS_SCENARIO
import com.tangem.common.constants.TestConstants.TANGEM_PAY_ELIGIBILITY_SCENARIO
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.utils.hideBalances
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.core.res.R as CoreResR
import com.tangem.scenarios.*
import com.tangem.screens.onMainScreen
import com.tangem.screens.onSwapSelectTokenScreen
import com.tangem.screens.onSwapTokenScreen
import com.tangem.screens.tangempay.*
import com.tangem.utils.StringsSigns.THREE_STARS
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.kakaocup.kakao.common.utilities.getResourceString
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.joda.time.DateTimeZone
import org.joda.time.LocalDate
import org.junit.Test

@HiltAndroidTest
class TangemPayHiddenBalancesTest : BaseTestCase() {

    private val balanceScenario = "tangem_pay_balance_update"
    private val cashbackScenario = "tangem_pay_cashback"
    private val historyScenario = "tangem_pay_transaction_history"
    private val swapStoriesScenario = "stories_first_time_swap_v2"
    private val eligibilityState = "PaeraCustomer"
    private val initialBalanceState = "InitialBalance"
    private val positiveCashbackState = "Positive"
    private val cashbackTransactionsState = "CashbackTransactions"
    private val swapStoriesErrorState = "Error"
    private val bankOfframpChannelsState = "BankOfframp"
    private val usdc = "USDC"
    private val usdt = "USDT"
    private val polygon = "Polygon"
    private val arbitrum = "Arbitrum One"
    private val bnbSmartChain = "BNB Smart Chain"

    private val today: LocalDate
        get() = LocalDate.now(DateTimeZone.UTC)

    @AllureId("12742")
    @DisplayName("Tangem Pay: hidden balances mask the Payment account balance as on Main")
    @Test
    fun paymentAccountBalanceMaskedTest() {
        setupHooks(
            additionalPreferencesSection = { appPreferencesStore.hideBalances(it) },
        ).run {
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to '$eligibilityState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
            }
            step("Set WireMock scenario '$balanceScenario' to '$initialBalanceState'") {
                setWireMockScenarioState(balanceScenario, initialBalanceState)
            }
            step("Open 'Main' screen with Tangem Pay tile") { openMainScreenWithTangemPayTile() }
            step("Assert wallet balance is '$THREE_STARS'") {
                onMainScreen { totalBalanceText.assertTextEquals(THREE_STARS) }
            }
            step("Assert Tangem Pay tile balance is '$THREE_STARS'") {
                onTangemPayMainScreen { tileBalance.assertTextEquals(THREE_STARS) }
            }
            step("Click on Tangem Pay tile") {
                onTangemPayMainScreen { mainScreenTile.performClick() }
            }
            step("Assert Payment account balance is '$THREE_STARS'") {
                flakySafely { onTangemPayMainScreen { balance.assertTextEquals(THREE_STARS) } }
            }
        }
    }

    @AllureId("12743")
    @DisplayName("Tangem Pay: hidden balances mask the transaction amount and cashback in the history and details")
    @Test
    fun transactionAmountAndCashbackMaskedTest() {
        val spendMerchant = "Cashback Coffee"

        setupHooks(
            additionalPreferencesSection = { appPreferencesStore.hideBalances(it) },
            additionalBeforeSection = { serveTangemPayCashbackFromWireMock() },
            additionalAfterSection = { restoreTangemPayCannedCashback() },
        ).run {
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to '$eligibilityState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
            }
            step("Set WireMock scenario '$cashbackScenario' to '$positiveCashbackState'") {
                setWireMockScenarioState(cashbackScenario, positiveCashbackState)
            }
            step("Set WireMock scenario '$historyScenario' to '$cashbackTransactionsState'") {
                setWireMockScenarioState(historyScenario, cashbackTransactionsState)
            }
            step("Open Tangem Pay") { openTangemPay() }
            step("Assert '$spendMerchant' cashback badge is '$THREE_STARS'") {
                flakySafely {
                    onTangemPayMainScreen {
                        transactionCashbackBadgeInRow(spendMerchant, THREE_STARS).assertIsDisplayed()
                    }
                }
            }
            step("Open '$spendMerchant' transaction details") { openTangemPayTransactionDetails(spendMerchant) }
            step("Assert transaction amount is '$THREE_STARS'") {
                onTangemPayTransactionDetailsSheet { amount.assertTextEquals(THREE_STARS) }
            }
            step("Assert 'Cashback' field is '$THREE_STARS'") {
                flakySafely {
                    onTangemPayTransactionDetailsSheet { cashbackFieldValue(THREE_STARS).assertIsDisplayed() }
                }
            }
        }
    }

    @AllureId("12745")
    @DisplayName("Tangem Pay: hidden balances mask the cashback widget amount")
    @Test
    fun cashbackWidgetAmountMaskedTest() {
        setupHooks(
            additionalPreferencesSection = { appPreferencesStore.hideBalances(it) },
            additionalBeforeSection = { serveTangemPayCashbackFromWireMock() },
            additionalAfterSection = { restoreTangemPayCannedCashback() },
        ).run {
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to '$eligibilityState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
            }
            step("Set WireMock scenario '$cashbackScenario' to '$positiveCashbackState'") {
                setWireMockScenarioState(cashbackScenario, positiveCashbackState)
            }
            step("Open Tangem Pay") { openTangemPay() }
            step("Assert cashback widget amount is '$THREE_STARS'") {
                assertTangemPayCashbackWidget(tangemPayCashbackWidgetTitle(THREE_STARS, today))
            }
        }
    }

    @AllureId("12746")
    @DisplayName("Tangem Pay: hidden balances mask the Payment account balance in Add funds from Tangem Wallet")
    @Test
    fun addFundsFromTangemWalletBalanceMaskedTest() {
        setupHooks(
            additionalBeforeAppLaunchSection = { setWireMockScenarioState(swapStoriesScenario, swapStoriesErrorState) },
            additionalPreferencesSection = { appPreferencesStore.hideBalances(it) },
        ).run {
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to '$eligibilityState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
            }
            step("Set WireMock scenario '$balanceScenario' to '$initialBalanceState'") {
                setWireMockScenarioState(balanceScenario, initialBalanceState)
            }
            step("Open Tangem Pay") { openTangemPay() }
            step("Open 'Add funds' sheet") { openTangemPayAddFunds() }
            step("Click on 'From your Tangem Wallet' option") {
                onTangemPayAddFundsSheet { swapOption.clickWithAssertion() }
            }
            step("Assert 'Add funds' Swap screen is displayed") {
                flakySafely { onSwapTokenScreen { addFundsTitle.assertIsDisplayed() } }
            }
            step("Assert Payment account balance is '$THREE_STARS'") {
                flakySafely { onSwapTokenScreen { receiveCardBalance.assertTextEquals(THREE_STARS) } }
            }
        }
    }

    @AllureId("12747")
    @DisplayName("Tangem Pay: hidden balances mask the balances in Withdraw within the portfolio")
    @Test
    fun withdrawWithinPortfolioBalancesMaskedTest() {
        setupHooks(
            additionalBeforeAppLaunchSection = { setWireMockScenarioState(swapStoriesScenario, swapStoriesErrorState) },
            additionalPreferencesSection = { appPreferencesStore.hideBalances(it) },
        ).run {
            step("Set WireMock scenarios for a multichain Tangem Pay withdrawal") {
                setTangemPayMultiNetworkWithdrawScenarios()
            }
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_CHANNELS_SCENARIO' to '$bankOfframpChannelsState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_CHANNELS_SCENARIO, bankOfframpChannelsState)
            }
            step("Open Tangem Pay 'Withdraw' hub") { openTangemPayWithdrawHub() }
            step("Click on 'Within your portfolio' card") {
                onTangemPayWithdrawHubScreen { withinPortfolioCard.performClick() }
            }
            step("Assert 'Withdraw' Swap screen is displayed") {
                flakySafely { onSwapTokenScreen { withdrawTitle.assertIsDisplayed() } }
            }
            step("Assert Payment account balance is '$THREE_STARS'") {
                flakySafely { onSwapTokenScreen { swapCardBalance.assertTextEquals(THREE_STARS) } }
            }
            step("Open the source token selector") { openWithdrawSourceTokenSelector() }
            step("Assert '$usdc' on '$polygon' amount is '$THREE_STARS'") {
                flakySafely {
                    onSwapSelectTokenScreen {
                        tokenWithNameNetworkAndAmount(usdc, polygon, THREE_STARS).assertIsDisplayed()
                    }
                }
            }
            step("Assert '$usdt' on '$bnbSmartChain' amount is '$THREE_STARS'") {
                onSwapSelectTokenScreen {
                    tokenWithNameNetworkAndAmount(usdt, bnbSmartChain, THREE_STARS).assertIsDisplayed()
                }
            }
        }
    }

    @AllureId("12748")
    @DisplayName("Tangem Pay: hidden balances mask the source token balances of the bank transfer")
    @Test
    fun bankTransferSourceBalancesMaskedTest() {
        setupHooks(
            additionalPreferencesSection = { appPreferencesStore.hideBalances(it) },
        ).run {
            step("Set WireMock scenarios for a Tangem Pay bank transfer") { setTangemPayBankOfframpTransferScenarios() }
            step("Open Tangem Pay") { openTangemPay() }
            step("Assert Payment account balance is '$THREE_STARS'") {
                flakySafely { onTangemPayMainScreen { balance.assertTextEquals(THREE_STARS) } }
            }
            step("Open 'Withdraw' hub") { openWithdrawHubFromPaymentAccount() }
            step("Click on 'To personal account' button") { openTangemPayBankWithdrawToPersonalAccount() }
            step("Assert '$usdc' on '$polygon' source balance is '$THREE_STARS'") {
                flakySafely {
                    onTangemPayBankWithdrawAmountScreen {
                        sourceRowWith(usdc, maskedSourceSubtitle(polygon)).assertIsDisplayed()
                    }
                }
            }
            step("Open the source token selector") { openTangemPayBankWithdrawSourceSelector() }
            step("Assert '$usdc' on '$polygon' balance is '$THREE_STARS'") {
                onTangemPayBankWithdrawAmountScreen {
                    selectedTokenRow(usdc, maskedSourceSubtitle(polygon)).assertIsDisplayed()
                }
            }
            step("Assert '$usdc' on '$arbitrum' balance is '$THREE_STARS'") {
                onTangemPayBankWithdrawAmountScreen {
                    tokenRow(usdc, maskedSourceSubtitle(arbitrum)).assertIsDisplayed()
                }
            }
        }
    }

    @AllureId("12750")
    @DisplayName("Tangem Pay: hidden balances mask the balance on the bank transfer confirmation to a recent recipient")
    @Test
    fun bankTransferConfirmationBalanceMaskedTest() {
        val bankAccountsScenario = "tangem_pay_bank_accounts"
        val twoAccountsState = "TwoAccounts"
        val amount = "5"

        setupHooks(
            additionalPreferencesSection = { appPreferencesStore.hideBalances(it) },
        ).run {
            step("Set WireMock scenarios for a Tangem Pay bank transfer") { setTangemPayBankOfframpTransferScenarios() }
            step("Set WireMock scenario '$bankAccountsScenario' to '$twoAccountsState'") {
                setWireMockScenarioState(bankAccountsScenario, twoAccountsState)
            }
            step("Open Tangem Pay") { openTangemPay() }
            step("Assert Payment account balance is '$THREE_STARS'") {
                flakySafely { onTangemPayMainScreen { balance.assertTextEquals(THREE_STARS) } }
            }
            step("Open 'Withdraw' hub") { openWithdrawHubFromPaymentAccount() }
            step("Open recent recipient '${JOHN.shortName}'") { openTangemPayRecentRecipient(JOHN) }
            step("Enter amount '$amount'") { enterTangemPayBankWithdrawAmount(amount) }
            step("Click on 'Continue' button") { clickTangemPayBankWithdrawAmountContinue() }
            step("Assert 'Confirm transfer' screen is displayed") {
                flakySafely { onTangemPayBankWithdrawPreviewScreen { title.assertIsDisplayed() } }
            }
            step("Assert amount block '$$amount' shows the balance as '$THREE_STARS'") {
                flakySafely {
                    onTangemPayBankWithdrawPreviewScreen { amountCard("$$amount", THREE_STARS).assertIsDisplayed() }
                }
            }
        }
    }

    @AllureId("12751")
    @DisplayName("Tangem Pay: hidden balances mask the Cashback screen header and chart amounts")
    @Test
    fun cashbackScreenAmountsMaskedTest() {
        val histogramMonthOffsets = listOf(-4, -3, -1, 0)

        setupHooks(
            additionalPreferencesSection = { appPreferencesStore.hideBalances(it) },
            additionalBeforeSection = { serveTangemPayCashbackFromWireMock() },
            additionalAfterSection = { restoreTangemPayCannedCashback() },
        ).run {
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to '$eligibilityState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
            }
            step("Set WireMock scenario '$cashbackScenario' to '$positiveCashbackState'") {
                setWireMockScenarioState(cashbackScenario, positiveCashbackState)
            }
            step("Open Tangem Pay") { openTangemPay() }
            step("Open 'Cashback' screen") { openTangemPayCashback(tangemPayCashbackWidgetTitle(THREE_STARS, today)) }
            step("Assert cashback header amount is '$THREE_STARS'") {
                flakySafely {
                    onTangemPayCashbackScreen {
                        headerTitle.assertTextEquals(tangemPayCashbackEarnedTitle(THREE_STARS, today))
                    }
                }
            }
            step("Assert histogram amounts are '$THREE_STARS'") {
                assertTangemPayCashbackHistogram(
                    total = tangemPayCashbackTotalTitle(THREE_STARS),
                    months = histogramMonthOffsets.map { tangemPayCashbackShortMonth(today.plusMonths(it)) },
                    amounts = histogramMonthOffsets.map { THREE_STARS },
                )
            }
        }
    }

    private fun maskedSourceSubtitle(network: String): String =
        getResourceString(CoreResR.string.tangempay_withdraw_bank_source_subtitle, network, THREE_STARS)
}