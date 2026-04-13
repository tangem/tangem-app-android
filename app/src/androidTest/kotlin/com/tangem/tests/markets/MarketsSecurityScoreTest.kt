package com.tangem.tests.markets

import com.tangem.common.BaseTestCase
import com.tangem.common.annotations.ApiEnv
import com.tangem.common.annotations.ApiEnvConfig
import com.tangem.common.extensions.SwipeDirection
import com.tangem.common.extensions.assertVisibility
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.extensions.swipeVertical
import com.tangem.datasource.api.common.config.ApiConfig
import com.tangem.datasource.api.common.config.ApiEnvironment
import com.tangem.scenarios.openMarketsScreen
import com.tangem.scenarios.openMarketsTokenDetails
import com.tangem.scenarios.searchInMarkets
import com.tangem.screens.onMarketsSecurityScoreDetailsScreen
import com.tangem.screens.onMarketsTokenDetailsScreen
import dagger.hilt.android.testing.HiltAndroidTest
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Test

@HiltAndroidTest
class MarketsSecurityScoreTest : BaseTestCase() {

    @Test
    @ApiEnv(ApiEnvConfig(ApiConfig.ID.TangemTech, ApiEnvironment.PROD))
    @AllureId("63")
    @DisplayName("Markets: verify Security Score block is displayed for Bitcoin")
    fun marketsSecurityScoreBlockDisplayedTest() {
        val tokenName = "Bitcoin"
        setupHooks().run {
            step("Open 'Markets' screen") { openMarketsScreen() }
            step("Search for '$tokenName'") { searchInMarkets(tokenName) }
            step("Open '$tokenName' token details") { openMarketsTokenDetails(tokenName) }
            step("Scroll to Security Score block") {
                swipeVertical(SwipeDirection.UP)
                waitForIdle()
            }
            step("Assert Security Score block is displayed") {
                onMarketsTokenDetailsScreen { securityScoreBlock.assertIsDisplayed() }
            }
            step("Assert Security Score value is displayed") {
                onMarketsTokenDetailsScreen { securityScoreValue.assertIsDisplayed() }
            }
            step("Assert reviews count is displayed") {
                onMarketsTokenDetailsScreen { securityScoreReviewsCount.assertIsDisplayed() }
            }
            step("Assert rating stars are displayed") {
                onMarketsTokenDetailsScreen { securityScoreStars.assertIsDisplayed() }
            }
        }
    }

    @Test
    @ApiEnv(ApiEnvConfig(ApiConfig.ID.TangemTech, ApiEnvironment.PROD))
    @AllureId("64")
    @DisplayName("Markets: verify Security Score details navigation and provider link")
    fun marketsSecurityScoreDetailsNavigationTest() {
        val tokenName = "Bitcoin"
        setupHooks().run {
            step("Open 'Markets' screen") { openMarketsScreen() }
            step("Search for '$tokenName'") { searchInMarkets(tokenName) }
            step("Open '$tokenName' token details") { openMarketsTokenDetails(tokenName) }
            step("Scroll to Security Score block") {
                swipeVertical(SwipeDirection.UP)
                waitForIdle()
            }
            step("Tap Security Score info button") {
                onMarketsTokenDetailsScreen { securityScoreInfoButton.clickWithAssertion() }
                waitForIdle()
            }
            step("Assert Security Score details title is displayed") {
                onMarketsSecurityScoreDetailsScreen { detailsTitle.assertIsDisplayed() }
            }
            step("Tap first provider link") {
                onMarketsSecurityScoreDetailsScreen { firstProviderLink.clickWithAssertion() }
                waitForIdle()
            }
        }
    }

    @Test
    @ApiEnv(ApiEnvConfig(ApiConfig.ID.TangemTech, ApiEnvironment.PROD))
    @AllureId("65")
    @DisplayName("Markets: verify Security Score block is hidden for tokens without security data")
    fun marketsSecurityScoreBlockHiddenTest() {
        val tokenName = "PepeTopia"
        setupHooks().run {
            step("Open 'Markets' screen") { openMarketsScreen() }
            step("Search for '$tokenName'") { searchInMarkets(tokenName) }
            step("Open '$tokenName' token details") { openMarketsTokenDetails(tokenName) }
            step("Scroll down inside token details") {
                swipeVertical(SwipeDirection.UP)
                waitForIdle()
            }
            step("Assert Security Score block is NOT displayed") {
                onMarketsTokenDetailsScreen { securityScoreBlock.assertVisibility(shouldBeDisplayed = false) }
            }
        }
    }
}