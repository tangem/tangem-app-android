package com.tangem.scenarios

import androidx.compose.ui.test.ExperimentalTestApi
import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_LONG
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.utils.findWireMockRequestBodies
import com.tangem.common.utils.getWireMockRequestCount
import com.tangem.common.utils.getWireMockRequestCountByQueryParam
import com.tangem.domain.pay.TangemPayEligibilityManager
import com.tangem.screens.onDetailsScreen
import com.tangem.screens.onMainScreen
import com.tangem.screens.onMainScreenTopBar
import com.tangem.screens.tangempay.focusedWaitlistEmailFieldMatcher
import com.tangem.screens.tangempay.onTangemPayMainScreen
import com.tangem.screens.tangempay.onTangemPayOnboardingScreen
import com.tangem.screens.tangempay.onTangemPayRegionUnavailableScreen
import com.tangem.screens.tangempay.onTangemPayResidencyScreen
import io.qameta.allure.kotlin.Allure.step
import kotlinx.coroutines.runBlocking
import org.json.JSONObject

private const val GEO_COUNTRIES_PATH = "/bff-v2/v1/geo/countries"
private const val ELIGIBILITY_CHECK_PATH = "/bff-v2/v1/eligibility/check"
private const val WAITLIST_PATH = "/bff-v2/v1/waitlist"

fun getTangemPayCountriesRequestCount(): Int = getWireMockRequestCount("GET", GEO_COUNTRIES_PATH)

fun getTangemPayRegionsRequestCount(countryCode: String): Int =
    getWireMockRequestCount("GET", "$GEO_COUNTRIES_PATH/$countryCode/regions")

fun getTangemPayEligibilityCheckRequestCount(regionCode: String): Int = getWireMockRequestCountByQueryParam(
    method = "GET",
    urlPath = ELIGIBILITY_CHECK_PATH,
    queryParam = "region_code",
    queryValue = regionCode,
)

fun getTangemPayWaitlistRequestBodies(): List<JSONObject> =
    findWireMockRequestBodies(method = "POST", urlPath = WAITLIST_PATH).map(::JSONObject)

fun BaseTestCase.awaitTangemPayRequestCountIncreased(name: String, countBefore: Int, currentCount: () -> Int) {
    awaitSuccess {
        val countAfter = currentCount()
        if (countAfter <= countBefore) throw AssertionError("'$name' was not requested: $countBefore → $countAfter")
    }
}

fun BaseTestCase.openTangemPayCountryStepFromDetails() {
    step("Click on 'More' button") {
        onMainScreenTopBar { moreButton.clickWithAssertion() }
    }
    step("Assert 'Get Tangem Pay' row is displayed") {
        awaitSuccess(timeoutMillis = WAIT_UNTIL_TIMEOUT_LONG) { onDetailsScreen { getTangemPayRow.assertIsDisplayed() } }
    }
    step("Click on 'Get Tangem Pay' row") {
        onDetailsScreen { getTangemPayRow.clickWithAssertion() }
    }
    step("Assert Tangem Pay onboarding screen is displayed") {
        awaitSuccess { onTangemPayOnboardingScreen { title.assertIsDisplayed() } }
    }
    step("Click on 'Get card' button") { clickGetCardOnTangemPayOnboarding() }
    step("Assert 'Country of residence' screen is displayed") {
        awaitSuccess(timeoutMillis = WAIT_UNTIL_TIMEOUT_LONG) {
            onTangemPayResidencyScreen { countryTitle.assertIsDisplayed() }
        }
    }
}

fun BaseTestCase.clickTangemPayResidencyItem(name: String) {
    awaitSuccess { onTangemPayResidencyScreen { item(name).assertIsDisplayed() } }
    onTangemPayResidencyScreen { item(name).performClick() }
}

fun BaseTestCase.searchTangemPayResidency(query: String) {
    awaitSuccess { onTangemPayResidencyScreen { searchField.assertIsDisplayed() } }
    onTangemPayResidencyScreen { searchField.performTextReplacement(query) }
}

fun getTangemPayEligibilityCheckRequestCountByCountry(countryCode: String): Int =
    getWireMockRequestCountByQueryParam(
        method = "GET",
        urlPath = ELIGIBILITY_CHECK_PATH,
        queryParam = "country_code",
        queryValue = countryCode,
    )

fun BaseTestCase.awaitTangemPayWaitlistRequestBody(countBefore: Int): JSONObject {
    lateinit var body: JSONObject
    awaitSuccess {
        val bodies = getTangemPayWaitlistRequestBodies()
        if (bodies.size <= countBefore) throw AssertionError("Waitlist was not requested")
        body = bodies.last()
    }
    return body
}

fun BaseTestCase.assertTangemPayWaitlistJoined() {
    step("Assert 'Joined waitlist' message is displayed") {
        awaitSuccess { onTangemPayMainScreen { waitlistJoinedMessage.assertIsDisplayed() } }
    }
    step("Assert Tangem Pay tile is not displayed") { assertTangemPayTileNotDisplayed() }
    step("Click on 'More' button") {
        onMainScreenTopBar { moreButton.clickWithAssertion() }
    }
    step("Assert 'Get Tangem Pay' row is displayed") {
        awaitSuccess { onDetailsScreen { getTangemPayRow.assertIsDisplayed() } }
    }
}

fun BaseTestCase.returnToMainFromTangemPayRegionUnavailableScreen() {
    step("Click on 'Close' button") {
        onTangemPayRegionUnavailableScreen { closeButton.clickWithAssertion() }
    }
    step("Assert app Details screen is displayed") {
        awaitSuccess { onDetailsScreen { appSettingsButton.assertIsDisplayed() } }
    }
    step("Click on 'Back' button") {
        onDetailsScreen { topAppBarBackButton.clickWithAssertion() }
    }
    step("Assert 'Main' screen is displayed") {
        awaitSuccess { onMainScreen { screenContainer.assertIsDisplayed() } }
    }
}

@OptIn(ExperimentalTestApi::class)
fun BaseTestCase.awaitTangemPayWaitlistEmailFieldFocused() {
    composeTestRule.waitUntilAtLeastOneExists(focusedWaitlistEmailFieldMatcher, WAIT_UNTIL_TIMEOUT)
}

fun BaseTestCase.awaitNoWalletOfferedTangemPay(eligibilityManager: TangemPayEligibilityManager) {
    awaitSuccess {
        val walletIds = runBlocking { eligibilityManager.getCompatibleWalletsIds(shouldExcludePaeraCustomers = true) }
        if (walletIds.isNotEmpty()) throw AssertionError("Tangem Pay is still offered to $walletIds")
    }
}