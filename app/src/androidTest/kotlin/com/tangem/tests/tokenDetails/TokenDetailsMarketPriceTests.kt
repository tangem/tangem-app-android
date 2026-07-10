package com.tangem.tests.tokenDetails

import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.QUOTES_API_SCENARIO
import com.tangem.common.constants.TestConstants.USER_TOKENS_API_SCENARIO
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_LONG
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.utils.resetWireMockScenarioState
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.core.ui.R
import com.tangem.scenarios.openMainScreen
import com.tangem.scenarios.synchronizeAddresses
import com.tangem.screens.onMainScreen
import com.tangem.screens.onTokenDetailsScreen
import com.tangem.screens.onTokenMarketBlock
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.kakaocup.kakao.common.utilities.getResourceString
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Test

@HiltAndroidTest
class TokenDetailsMarketPriceTests : BaseTestCase() {

    @AllureId("301")
    @DisplayName("Token details: Market Price block data")
    @Test
    fun marketPriceBlockDataTest() {
        val tokenName = "Dogecoin"
        val marketPriceTitle = getResourceString(R.string.markets_common_market_price)

        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(USER_TOKENS_API_SCENARIO)
                resetWireMockScenarioState(QUOTES_API_SCENARIO)
            }
        ).run {
            step("Set WireMock scenario: '$USER_TOKENS_API_SCENARIO' to state: '$tokenName'") {
                setWireMockScenarioState(scenarioName = USER_TOKENS_API_SCENARIO, state = tokenName)
            }
            step("Set WireMock scenario: '$QUOTES_API_SCENARIO' to state: '$tokenName'") {
                setWireMockScenarioState(scenarioName = QUOTES_API_SCENARIO, state = tokenName)
            }
            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Click on token with name: '$tokenName'") {
                onMainScreen { tokenWithTitleAndAddress(tokenName).clickWithAssertion() }
            }
            step("Assert 'Token details' screen is displayed") {
                onTokenDetailsScreen { screenContainer.assertIsDisplayed() }
            }
            step("Assert 'Market Price' block is displayed with title '$marketPriceTitle'") {
                onTokenMarketBlock {
                    block.assertIsDisplayed()
                    title.assertTextEquals(marketPriceTitle)
                }
            }
            step("Assert price rate is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onTokenMarketBlock { price.assertIsDisplayed() }
                }
            }
            step("Assert 24h price change is displayed") {
                onTokenMarketBlock { priceChange.assertIsDisplayed() }
            }
            step("Assert mini chart is displayed") {
                onTokenMarketBlock { chart.assertIsDisplayed() }
            }
        }
    }
}