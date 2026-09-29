package com.tangem.tests.tangempay

import androidx.test.platform.app.InstrumentationRegistry
import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.TANGEM_PAY_ELIGIBILITY_SCENARIO
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_LONG
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.core.res.R as CoreResR
import com.tangem.core.ui.utils.DateTimeFormatters
import com.tangem.scenarios.*
import com.tangem.screens.tangempay.onTangemPayCashbackScreen
import com.tangem.screens.tangempay.onTangemPayMainScreen
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.kakaocup.kakao.common.utilities.getResourceString
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.joda.time.DateTimeZone
import org.joda.time.LocalDate
import org.junit.Test
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale

@HiltAndroidTest
class TangemPayCashbackTest : BaseTestCase() {

    private val cashbackScenario = "tangem_pay_cashback"
    private val historyScenario = "tangem_pay_transaction_history"
    private val eligibilityState = "PaeraCustomer"
    private val positiveState = "Positive"
    private val emptyState = "Empty"
    private val refundState = "Refund"
    private val altBlockState = "AltBlock"
    private val fraudState = "Fraud"
    private val payoutState = "Payout"
    private val historyFailedState = "HistoryFailed"
    private val cashbackTransactionsState = "CashbackTransactions"

    // Amounts, the plan and the promotion come from the WireMock cashback pack. Widget and chart columns keep
    // two decimals; the header, chart total and payout banner drop them for whole amounts.
    private val positiveAmount = "$32.15"
    private val positiveTotal = "$153"
    private val fourMonthsAgoAmount = "$10.40"
    private val threeMonthsAgoAmount = "$25.10"
    private val previousMonthAmount = "$85.35"
    private val zeroAmount = "$0.00"
    private val refundAmount = "-$3.20"
    private val payoutAmount = "$5.40"
    private val previousPayoutAmount = "$12.80"
    private val cashbackRate = "1"
    private val cashbackPlan = "Basic"
    private val spendMerchant = "Cashback Coffee"
    private val spendCashback = "+$0.12"
    private val refundMerchant = "Cashback Books"
    private val refundCashback = "-$0.08"

    private val emptyTitle = getResourceString(CoreResR.string.tangempay_cashback_empty_title)
    private val emptySubtitle = getResourceString(CoreResR.string.tangempay_cashback_empty_subtitle)
    private val refundBanner = getResourceString(CoreResR.string.tangempay_cashback_refund_banner)
    private val rateTitle = getResourceString(CoreResR.string.tangempay_cashback_rate_title, cashbackRate)
    private val rateSubtitle = getResourceString(CoreResR.string.tangempay_cashback_rate_subtitle, cashbackPlan)
    private val accrualsTitle = getResourceString(CoreResR.string.tangempay_cashback_accruals_title)

    // Mocks stamp every period with the UTC run date. Read lazily — Joda's timezone provider is initialized by
    // the app, so a field initializer would run too early.
    private val today: LocalDate
        get() = LocalDate.now(DateTimeZone.UTC)

    private val currentMonth: String
        get() = fullMonth(today)

    private val payoutWindow: String
        get() {
            val nextMonth = today.plusMonths(1)
            val window = DateTimeFormatters.formatDateRange(
                start = nextMonth.withDayOfMonth(PAYOUT_START_DAY),
                end = nextMonth.withDayOfMonth(PAYOUT_END_DAY),
                skeleton = "MMMMd",
            )
            return getResourceString(CoreResR.string.tangempay_cashback_deposited_on, window)
        }

    @AllureId("10934")
    @DisplayName("Tangem Pay: non-EU cashback widget shows a positive amount and no menu item")
    @Test
    fun nonEuWidgetPositiveAmountTest() {
        setupHooks(
            additionalBeforeSection = { serveTangemPayCashbackFromWireMock() },
            additionalAfterSection = { restoreTangemPayCannedCashback() },
        ).run {
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to '$eligibilityState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
            }
            step("Set WireMock scenario '$cashbackScenario' to '$positiveState'") {
                setWireMockScenarioState(cashbackScenario, positiveState)
            }
            step("Open Tangem Pay") { openTangemPay() }
            step("Assert cashback widget with '$positiveAmount' is displayed") {
                assertTangemPayCashbackWidget(widgetTitle(positiveAmount))
            }
            step("Open 'More' menu") { openTangemPayMoreActionsMenu() }
            step("Assert 'Cashback in $currentMonth' menu item is not displayed") {
                onTangemPayMainScreen { cashbackMenuItem(currentMonth).assertDoesNotExist() }
            }
        }
    }

    @AllureId("10935")
    @DisplayName("Tangem Pay: non-EU cashback widget shows zero for the current month")
    @Test
    fun nonEuWidgetZeroAmountTest() {
        setupHooks(
            additionalBeforeSection = { serveTangemPayCashbackFromWireMock() },
            additionalAfterSection = { restoreTangemPayCannedCashback() },
        ).run {
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to '$eligibilityState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
            }
            step("Set WireMock scenario '$cashbackScenario' to '$emptyState'") {
                setWireMockScenarioState(cashbackScenario, emptyState)
            }
            step("Open Tangem Pay") { openTangemPay() }
            step("Assert cashback widget with '$zeroAmount' is displayed") {
                assertTangemPayCashbackWidget(widgetTitle(zeroAmount))
            }
        }
    }

    @AllureId("10936")
    @DisplayName("Tangem Pay: non-EU cashback widget shows a negative amount with a minus")
    @Test
    fun nonEuWidgetNegativeAmountTest() {
        setupHooks(
            additionalBeforeSection = { serveTangemPayCashbackFromWireMock() },
            additionalAfterSection = { restoreTangemPayCannedCashback() },
        ).run {
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to '$eligibilityState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
            }
            step("Set WireMock scenario '$cashbackScenario' to '$refundState'") {
                setWireMockScenarioState(cashbackScenario, refundState)
            }
            step("Open Tangem Pay") { openTangemPay() }
            step("Assert cashback widget with '$refundAmount' is displayed") {
                assertTangemPayCashbackWidget(widgetTitle(refundAmount))
            }
        }
    }

    @AllureId("10938")
    @DisplayName("Tangem Pay: EU layout moves cashback from the widget into the menu")
    @Test
    fun euLayoutShowsCashbackInMenuTest() {
        setupHooks(
            additionalBeforeSection = { serveTangemPayCashbackFromWireMock() },
            additionalAfterSection = { restoreTangemPayCannedCashback() },
        ).run {
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to '$eligibilityState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
            }
            step("Set WireMock scenario '$cashbackScenario' to '$altBlockState'") {
                setWireMockScenarioState(cashbackScenario, altBlockState)
            }
            step("Open Tangem Pay") { openTangemPay() }
            step("Open 'More' menu") { openTangemPayMoreActionsMenu() }
            step("Assert 'Cashback in $currentMonth' menu item is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onTangemPayMainScreen { cashbackMenuItem(currentMonth).assertIsDisplayed() }
                }
            }
            step("Click on 'Back' button") { device.uiDevice.pressBack() }
            step("Assert cashback widget is not displayed") {
                onTangemPayMainScreen { cashbackWidget.assertDoesNotExist() }
            }
        }
    }

    @AllureId("10944")
    @DisplayName("Tangem Pay: Cashback screen shows the empty state")
    @Test
    fun cashbackScreenEmptyStateTest() {
        setupHooks(
            additionalBeforeSection = { serveTangemPayCashbackFromWireMock() },
            additionalAfterSection = { restoreTangemPayCannedCashback() },
        ).run {
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to '$eligibilityState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
            }
            step("Set WireMock scenario '$cashbackScenario' to '$emptyState'") {
                setWireMockScenarioState(cashbackScenario, emptyState)
            }
            step("Open Tangem Pay") { openTangemPay() }
            step("Open 'Cashback' screen") { openTangemPayCashback(widgetTitle(zeroAmount)) }
            step("Assert empty state header is displayed") {
                assertTangemPayCashbackHeader(title = emptyTitle, subtitle = emptySubtitle)
            }
            step("Assert info tiles are displayed") {
                assertTangemPayCashbackInfoTiles(rateTitle, rateSubtitle, accrualsTitle)
            }
            step("Assert histogram is not displayed") {
                onTangemPayCashbackScreen { histogramTitle.assertDoesNotExist() }
            }
            step("Assert 'Additional cashback' section is displayed") {
                onTangemPayCashbackScreen {
                    additionalCashbackTitle.performScrollTo()
                    additionalCashbackTitle.assertIsDisplayed()
                }
            }
        }
    }

    @AllureId("10946")
    @DisplayName("Tangem Pay: Cashback screen shows the current month accruals and the chart")
    @Test
    fun cashbackScreenCurrentMonthAccrualsTest() {
        setupHooks(
            additionalBeforeSection = { serveTangemPayCashbackFromWireMock() },
            additionalAfterSection = { restoreTangemPayCannedCashback() },
        ).run {
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to '$eligibilityState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
            }
            step("Set WireMock scenario '$cashbackScenario' to '$positiveState'") {
                setWireMockScenarioState(cashbackScenario, positiveState)
            }
            step("Open Tangem Pay") { openTangemPay() }
            step("Open 'Cashback' screen") { openTangemPayCashback(widgetTitle(positiveAmount)) }
            step("Assert earned header with the payout window is displayed") {
                assertTangemPayCashbackHeader(title = earnedTitle(positiveAmount), subtitle = payoutWindow)
            }
            step("Assert info tiles are displayed") {
                assertTangemPayCashbackInfoTiles(rateTitle, rateSubtitle, accrualsTitle)
            }
            step("Assert histogram with the earned months is displayed") {
                // The mock skips two months ago, and the chart draws only the months the backend returns.
                assertTangemPayCashbackHistogram(
                    total = totalTitle(positiveTotal),
                    months = listOf(-4, -3, -1, 0).map { shortMonth(today.plusMonths(it)) },
                    amounts = listOf(fourMonthsAgoAmount, threeMonthsAgoAmount, previousMonthAmount, positiveAmount),
                )
            }
        }
    }

    @AllureId("10947")
    @DisplayName("Tangem Pay: Cashback screen shows a negative amount and the refund banner")
    @Test
    fun cashbackScreenRefundTest() {
        setupHooks(
            additionalBeforeSection = { serveTangemPayCashbackFromWireMock() },
            additionalAfterSection = { restoreTangemPayCannedCashback() },
        ).run {
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to '$eligibilityState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
            }
            step("Set WireMock scenario '$cashbackScenario' to '$refundState'") {
                setWireMockScenarioState(cashbackScenario, refundState)
            }
            step("Open Tangem Pay") { openTangemPay() }
            step("Open 'Cashback' screen") { openTangemPayCashback(widgetTitle(refundAmount)) }
            step("Assert earned header without the payout window is displayed") {
                assertTangemPayCashbackHeader(title = earnedTitle(refundAmount), subtitle = null)
            }
            step("Assert refund banner is displayed") { assertTangemPayCashbackBanner(refundBanner) }
            step("Assert histogram with the refund is displayed") {
                assertTangemPayCashbackHistogram(
                    total = totalTitle(refundAmount),
                    months = listOf(shortMonth(today)),
                    amounts = listOf(refundAmount),
                )
            }
        }
    }

    @AllureId("10948")
    @DisplayName("Tangem Pay: Cashback screen shows the failure state and recovers on reload")
    @Test
    fun cashbackScreenFailureStateTest() {
        setupHooks(
            additionalBeforeSection = { serveTangemPayCashbackFromWireMock() },
            additionalAfterSection = { restoreTangemPayCannedCashback() },
        ).run {
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to '$eligibilityState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
            }
            step("Set WireMock scenario '$cashbackScenario' to '$historyFailedState'") {
                setWireMockScenarioState(cashbackScenario, historyFailedState)
            }
            step("Open Tangem Pay") { openTangemPay() }
            step("Open 'Cashback' screen") { openTangemPayCashback(widgetTitle(positiveAmount)) }
            step("Assert failure message is displayed") {
                flakySafely {
                    onTangemPayCashbackScreen {
                        errorText.assertTextEquals(getResourceString(CoreResR.string.tangempay_cashback_error_title))
                    }
                }
            }
            step("Set WireMock scenario '$cashbackScenario' to '$positiveState'") {
                setWireMockScenarioState(cashbackScenario, positiveState)
            }
            step("Click on 'Reload' button") {
                onTangemPayCashbackScreen { reloadButton.clickWithAssertion() }
            }
            step("Assert earned header is displayed") {
                assertTangemPayCashbackHeader(title = earnedTitle(positiveAmount), subtitle = payoutWindow)
            }
        }
    }

    @AllureId("10949")
    @DisplayName("Tangem Pay: fraud status shows the deactivation banner until 'Got it'")
    @Test
    fun fraudStatusDeactivationBannerTest() {
        setupHooks(
            additionalBeforeSection = { serveTangemPayCashbackFromWireMock() },
            additionalAfterSection = { restoreTangemPayCannedCashback() },
        ).run {
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to '$eligibilityState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
            }
            step("Set WireMock scenario '$cashbackScenario' to '$fraudState'") {
                setWireMockScenarioState(cashbackScenario, fraudState)
            }
            step("Open Tangem Pay") { openTangemPay() }
            step("Assert 'Cashback deactivated' banner is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onTangemPayMainScreen { cashbackDeactivatedBannerTitle.assertIsDisplayed() }
                }
            }
            step("Assert cashback widget is not displayed") {
                onTangemPayMainScreen { cashbackWidget.assertDoesNotExist() }
            }
            step("Click on 'Got it' button") {
                onTangemPayMainScreen { cashbackDeactivatedGotItButton.clickWithAssertion() }
            }
            step("Assert 'Cashback deactivated' banner is not displayed") {
                flakySafely { onTangemPayMainScreen { cashbackDeactivatedBannerTitle.assertDoesNotExist() } }
            }
            step("Open 'More' menu") { openTangemPayMoreActionsMenu() }
            step("Assert 'Cashback in $currentMonth' menu item is not displayed") {
                onTangemPayMainScreen { cashbackMenuItem(currentMonth).assertDoesNotExist() }
            }
        }
    }

    @AllureId("10953")
    @DisplayName("Tangem Pay: Cashback screen shows the payout banner")
    @Test
    fun cashbackScreenPayoutBannerTest() {
        setupHooks(
            additionalBeforeSection = { serveTangemPayCashbackFromWireMock() },
            additionalAfterSection = { restoreTangemPayCannedCashback() },
        ).run {
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to '$eligibilityState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
            }
            step("Set WireMock scenario '$cashbackScenario' to '$payoutState'") {
                setWireMockScenarioState(cashbackScenario, payoutState)
            }
            // The mock ends the previous payout two days after the run, which keeps the banner on screen.
            val payoutEndDate = today.plusDays(PREVIOUS_PAYOUT_END_OFFSET_DAYS)
            val depositBanner = getResourceString(
                CoreResR.string.tangempay_cashback_deposit_banner,
                previousPayoutAmount,
                fullMonth(payoutEndDate.minusMonths(1)),
                DateTimeFormatters.formatDate(payoutEndDate.toDateTimeAtStartOfDay(), DateTimeFormatters.dateDMMM),
            )

            step("Open Tangem Pay") { openTangemPay() }
            step("Open 'Cashback' screen") { openTangemPayCashback(widgetTitle(payoutAmount)) }
            step("Assert earned header with the payout window is displayed") {
                assertTangemPayCashbackHeader(title = earnedTitle(payoutAmount), subtitle = payoutWindow)
            }
            step("Assert payout banner is displayed") { assertTangemPayCashbackBanner(depositBanner) }
        }
    }

    @AllureId("11604")
    @DisplayName("Tangem Pay: transaction history shows the signed cashback of each transaction")
    @Test
    fun transactionHistoryShowsCashbackTest() {
        setupHooks(
            additionalBeforeSection = { serveTangemPayCashbackFromWireMock() },
            additionalAfterSection = { restoreTangemPayCannedCashback() },
        ).run {
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to '$eligibilityState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
            }
            step("Set WireMock scenario '$cashbackScenario' to '$positiveState'") {
                setWireMockScenarioState(cashbackScenario, positiveState)
            }
            step("Set WireMock scenario '$historyScenario' to '$cashbackTransactionsState'") {
                setWireMockScenarioState(historyScenario, cashbackTransactionsState)
            }
            step("Open Tangem Pay") { openTangemPay() }
            step("Assert '$spendMerchant' transaction row is displayed") {
                assertTangemPayTransactionRowDisplayed(spendMerchant)
            }
            step("Assert cashback badge '$spendCashback' is displayed") {
                onTangemPayMainScreen { transactionCashbackBadge(spendCashback).assertIsDisplayed() }
            }
            step("Assert '$refundMerchant' transaction row is displayed") {
                assertTangemPayTransactionRowDisplayed(refundMerchant)
            }
            step("Assert cashback badge '$refundCashback' is displayed") {
                onTangemPayMainScreen { transactionCashbackBadge(refundCashback).assertIsDisplayed() }
            }
        }
    }

    private fun widgetTitle(amount: String): String =
        getResourceString(CoreResR.string.tangempay_cashback_widget_title, amount, currentMonth)

    private fun earnedTitle(amount: String): String {
        val monthIn = InstrumentationRegistry.getInstrumentation().targetContext.resources
            .getStringArray(CoreResR.array.common_month_in)[today.monthOfYear - 1]
        return getResourceString(CoreResR.string.tangempay_cashback_earned_title, amount, monthIn)
    }

    private fun totalTitle(amount: String): String =
        getResourceString(CoreResR.string.tangempay_cashback_total_earned, amount)

    private fun fullMonth(date: LocalDate): String =
        Month.of(date.monthOfYear).getDisplayName(TextStyle.FULL_STANDALONE, Locale.getDefault())

    private fun shortMonth(date: LocalDate): String =
        Month.of(date.monthOfYear).getDisplayName(TextStyle.SHORT_STANDALONE, Locale.getDefault())

    private companion object {
        const val PAYOUT_START_DAY = 2
        const val PAYOUT_END_DAY = 5
        const val PREVIOUS_PAYOUT_END_OFFSET_DAYS = 2
    }
}