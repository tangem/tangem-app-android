package com.tangem.tests.polymarket

import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_LONG
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.utils.getWireMockRequestCount
import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.scenarios.POLYMARKET_AB_TEST_ON
import com.tangem.scenarios.POLYMARKET_AB_TEST_PROPERTY
import com.tangem.scenarios.POLYMARKET_WALLET_PATH
import com.tangem.scenarios.awaitPolymarketMainScreen
import com.tangem.scenarios.clickPolymarketOnboardingActionButton
import com.tangem.scenarios.expandFeed
import com.tangem.scenarios.openMainScreenWithPolymarketHotWallet
import com.tangem.screens.onMainScreen
import com.tangem.screens.onMarketsScreen
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.kakaocup.kakao.common.utilities.getResourceString
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Assert.assertTrue
import org.junit.Test
import com.tangem.core.res.R as CoreResR

@HiltAndroidTest
class PolymarketEntryPointsTest : BaseTestCase() {

    override val featureToggleOverrides = mapOf(
        FeatureToggles.AND_16204_POLYMARKET_ENABLED.rawName to true,
    )

    @Test
    @AllureId("12894")
    @DisplayName("Polymarket: entry points. Markets banner before onboarding, banner and account after it")
    fun entryPointsTest() {
        val accountTitle = getResourceString(CoreResR.string.prediction_account_title)

        setupHooks(
            additionalBeforeAppLaunchSection = {
                System.setProperty(POLYMARKET_AB_TEST_PROPERTY, POLYMARKET_AB_TEST_ON)
            },
            additionalAfterSection = { System.clearProperty(POLYMARKET_AB_TEST_PROPERTY) },
        ).run {
            val statusRequestsBefore = getWireMockRequestCount("GET", POLYMARKET_WALLET_PATH)

            step("Open 'Main' screen with a hot wallet") {
                openMainScreenWithPolymarketHotWallet()
            }
            step("Assert the account status was requested") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    assertTrue(getWireMockRequestCount("GET", POLYMARKET_WALLET_PATH) > statusRequestsBefore)
                }
            }
            step("Assert 'Predictions account' is not displayed on 'Main' screen before the onboarding") {
                onMainScreen { findAccountSectionByName(accountTitle).assertDoesNotExist() }
            }
            step("Expand feed") {
                expandFeed()
            }
            step("Assert 'Predictions' banner is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { onMarketsScreen { predictionsBanner.assertIsDisplayed() } }
            }
            step("Click on 'Predictions' banner") {
                onMarketsScreen { predictionsBanner.clickWithAssertion() }
            }
            step("Click on 'Start predicting' button") {
                clickPolymarketOnboardingActionButton()
            }
            step("Await 'Polymarket main' screen") {
                awaitPolymarketMainScreen()
            }
            step("Go back to 'Main' screen") {
                device.uiDevice.pressBack()
            }
            step("Assert 'Predictions account' is displayed on 'Main' screen") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onMainScreen { findAccountSectionByName(accountTitle).assertIsDisplayed() }
                }
            }
            step("Expand feed again") {
                expandFeed()
            }
            step("Assert 'Predictions' banner is still displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { onMarketsScreen { predictionsBanner.assertIsDisplayed() } }
            }
        }
    }
}