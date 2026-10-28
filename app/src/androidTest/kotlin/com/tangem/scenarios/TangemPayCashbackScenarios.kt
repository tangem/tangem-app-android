package com.tangem.scenarios

import androidx.test.platform.app.InstrumentationRegistry
import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_LONG
import com.tangem.common.extensions.assertTextContainsSafe
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.core.res.R as CoreResR
import com.tangem.screens.tangempay.onTangemPayCashbackScreen
import com.tangem.screens.tangempay.onTangemPayMainScreen
import io.github.kakaocup.kakao.common.utilities.getResourceString
import io.qameta.allure.kotlin.Allure.step
import org.joda.time.LocalDate
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale

/** Mirrors UITEST_CASHBACK_FROM_API_KEY in MockAwareCashbackRepository — cashback comes from WireMock. */
private const val CASHBACK_FROM_API_KEY = "uitest.tangempay.cashback_from_api"

fun serveTangemPayCashbackFromWireMock() {
    System.setProperty(CASHBACK_FROM_API_KEY, "1")
}

fun restoreTangemPayCannedCashback() {
    System.clearProperty(CASHBACK_FROM_API_KEY)
}

fun BaseTestCase.assertTangemPayCashbackWidget(title: String) {
    step("Assert cashback widget '$title' is displayed") {
        // The summary is requested only once the payment account balance has loaded.
        awaitSuccess(timeoutMillis = WAIT_UNTIL_TIMEOUT_LONG) {
            onTangemPayMainScreen { cashbackWidgetWithTitle(title).assertIsDisplayed() }
        }
    }
}

fun BaseTestCase.openTangemPayCashback(widgetTitle: String) {
    assertTangemPayCashbackWidget(widgetTitle)
    step("Click on 'Cashback' widget") {
        onTangemPayMainScreen { cashbackWidget.clickWithAssertion() }
    }
}

fun BaseTestCase.openTangemPayMoreActionsMenu() {
    step("Click on 'More' button") {
        awaitSuccess { onTangemPayMainScreen { moreActionsButton.assertIsDisplayed() } }
        onTangemPayMainScreen { moreActionsButton.performClick() }
    }
    step("Assert 'Support' menu item is displayed") {
        awaitSuccess { onTangemPayMainScreen { paySupportMenuItem.assertIsDisplayed() } }
    }
}

fun BaseTestCase.assertTangemPayCashbackHeader(title: String, subtitle: String?) {
    step("Assert cashback header '$title' is displayed") {
        awaitSuccess { onTangemPayCashbackScreen { headerTitle.assertTextEquals(title) } }
    }
    if (subtitle == null) {
        step("Assert cashback header subtitle is not displayed") {
            onTangemPayCashbackScreen { headerSubtitle.assertDoesNotExist() }
        }
    } else {
        step("Assert cashback header subtitle '$subtitle' is displayed") {
            onTangemPayCashbackScreen { headerSubtitle.assertTextEquals(subtitle) }
        }
    }
}

fun BaseTestCase.assertTangemPayCashbackInfoTiles(rateTitle: String, rateSubtitle: String, accrualsTitle: String) {
    step("Assert '$rateTitle' tile with '$rateSubtitle' is displayed") {
        onTangemPayCashbackScreen {
            rateTileWithText(rateTitle).assertIsDisplayed()
            rateTileWithText(rateSubtitle).assertIsDisplayed()
        }
    }
    step("Assert '$accrualsTitle' tile is displayed") {
        onTangemPayCashbackScreen { accrualsTileWithText(accrualsTitle).assertIsDisplayed() }
    }
}

/** [amounts] lists the column values oldest month first, aligned with [months]. */
fun BaseTestCase.assertTangemPayCashbackHistogram(total: String, months: List<String>, amounts: List<String>) {
    step("Assert histogram title '$total' is displayed") {
        onTangemPayCashbackScreen {
            histogramTitle.performScrollTo()
            histogramTitle.assertTextEquals(total)
        }
    }
    months.zip(amounts).forEachIndexed { index, (month, amount) ->
        step("Assert histogram column '$month' shows '$amount'") {
            onTangemPayCashbackScreen {
                histogramMonthAt(index).assertTextEquals(month)
                histogramAmountAt(index).assertTextEquals(amount)
            }
        }
    }
    step("Assert histogram has ${months.size} columns") {
        onTangemPayCashbackScreen { histogramMonthAt(months.size).assertDoesNotExist() }
    }
}

fun BaseTestCase.assertTangemPayCashbackBanner(text: String) {
    step("Assert cashback banner '$text' is displayed") {
        onTangemPayCashbackScreen { banner.assertTextContainsSafe(text) }
    }
}

fun tangemPayCashbackWidgetTitle(amount: String, month: LocalDate): String =
    getResourceString(CoreResR.string.tangempay_cashback_widget_title, amount, tangemPayCashbackFullMonth(month))

fun tangemPayCashbackEarnedTitle(amount: String, month: LocalDate): String {
    val monthIn = InstrumentationRegistry.getInstrumentation().targetContext.resources
        .getStringArray(CoreResR.array.common_month_in)[month.monthOfYear - 1]
    return getResourceString(CoreResR.string.tangempay_cashback_earned_title, amount, monthIn)
}

fun tangemPayCashbackTotalTitle(amount: String): String =
    getResourceString(CoreResR.string.tangempay_cashback_total_earned, amount)

fun tangemPayCashbackFullMonth(date: LocalDate): String =
    Month.of(date.monthOfYear).getDisplayName(TextStyle.FULL_STANDALONE, Locale.getDefault())

fun tangemPayCashbackShortMonth(date: LocalDate): String =
    Month.of(date.monthOfYear).getDisplayName(TextStyle.SHORT_STANDALONE, Locale.getDefault())