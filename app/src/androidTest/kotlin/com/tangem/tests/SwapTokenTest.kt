package com.tangem.tests

import androidx.compose.ui.test.hasText
import com.tangem.common.BaseTestCase
import com.tangem.common.annotations.ApiEnv
import com.tangem.common.annotations.ApiEnvConfig
import com.tangem.common.constants.TestConstants.TOTAL_BALANCE
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.datasource.api.common.config.ApiConfig
import com.tangem.datasource.api.common.config.ApiEnvironment
import com.tangem.scenarios.OpenMainScreenScenario
import com.tangem.screens.*
import dagger.hilt.android.testing.HiltAndroidTest
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Test

@HiltAndroidTest
class SwapTokenTest : BaseTestCase() {

    @ApiEnv(
        ApiEnvConfig(ApiConfig.ID.Express, ApiEnvironment.PROD)
    )
    @AllureId("3546")
    @DisplayName("Swap: network fee")
    @Test
    fun networkFeeTest() {
        val inputAmount = "100"
        setupHooks().run {
            val tokenTitle = "Polygon"
            val balance = TOTAL_BALANCE

            step("Open 'Main Screen'") {
                scenario(OpenMainScreenScenario(composeTestRule))
            }
            step("Click on 'Synchronize addresses' button") {
                onMainScreen { synchronizeAddressesButton.clickWithAssertion() }
            }
            step("Assert wallet balance = $balance") {
                onMainScreen { walletBalance().assertTextContains(balance) }
            }
            step("Click on token with name: '$tokenTitle'") {
                onMainScreen { tokenWithTitleAndAddress(tokenTitle).clickWithAssertion() }
            }
            step("Click on token with name: '$tokenTitle'") {
                onTokenDetailsScreen { title.assertIsDisplayed() }
            }
            step("Click on 'Swap' button") {
                onTokenDetailsScreen { swapButton.performClick() }
            }
            step("Close 'Stories' screen") {
                onSwapStoriesScreen { closeButton.clickWithAssertion() }
            }
            step("Assert 'Swap' screen title is displayed") {
                onSwapTokenScreen { title.assertIsDisplayed() }
            }
            step("Assert 'Close' button is displayed") {
                onSwapTokenScreen { closeButton.assertIsDisplayed() }
            }
            step("Assert 'Swap tokens on screen' button is displayed") {
                onSwapTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT) {
                        swapTokensOnscreenButton.assertIsDisplayed()
                    }
                }
            }
            step("Assert receive amount is displayed") {
                onSwapTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT) {
                        receiveAmount.assertIsDisplayed()
                    }
                }
            }
            step("Input swap amount = '$inputAmount'") {
                composeTestRule.waitForIdle()
                onSwapTokenScreen {
                    textInput.clickWithAssertion()
                    textInput.performTextReplacement(inputAmount)
                }
            }
            step("Assert input amount = '$inputAmount'") {
                onSwapTokenScreen { textInput.assertTextEquals(inputAmount) }
            }
            step("Assert 'Providers' block is displayed") {
                onSwapTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT) {
                        providersBlock.assertIsDisplayed()
                    }
                }
            }
            step("Assert 'Network fee' block is displayed") {
                onSwapTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT) {
                        networkFeeBlock.assertIsDisplayed()
                    }
                }
            }
            step("Assert receive amount is not equal to '0'") {
                onSwapTokenScreen { receiveAmount.assert(!hasText("0")) }
            }
        }
    }

    @AllureId("3549")
    @DisplayName("Swap: network error test")
    @Test
    fun networkErrorSwapTest() {
        setupHooks().run {
            val tokenTitle = "Polygon"
            val balance = TOTAL_BALANCE

            step("Open 'Main Screen'") {
                scenario(OpenMainScreenScenario(composeTestRule))
            }
            step("Click on 'Synchronize addresses' button") {
                onMainScreen { synchronizeAddressesButton.clickWithAssertion() }
            }
            step("Assert wallet balance = $balance") {
                onMainScreen { walletBalance().assertTextContains(balance) }
            }
            step("Click on token with name: '$tokenTitle'") {
                onMainScreen { tokenWithTitleAndAddress(tokenTitle).clickWithAssertion() }
            }
            step("Click on token with name: '$tokenTitle'") {
                onTokenDetailsScreen { title.assertIsDisplayed() }
            }
            step("Click on 'Swap' button") {
                onTokenDetailsScreen { swapButton.performClick() }
            }
            step("Close 'Stories' screen") {
                onSwapStoriesScreen { closeButton.clickWithAssertion() }
            }
            step("Assert 'Swap' screen title is displayed") {
                onSwapTokenScreen { title.assertIsDisplayed() }
            }
            step("Assert error notification title is displayed") {
                onSwapTokenScreen { errorNotificationTitle.assertIsDisplayed() }
            }
            step("Assert error notification text is displayed") {
                onSwapTokenScreen { errorNotificationText.assertIsDisplayed() }
            }
            step("Assert 'Refresh' button is displayed") {
                onSwapTokenScreen { refreshButton.assertIsDisplayed() }
            }
        }
    }

    @ApiEnv(
        ApiEnvConfig(ApiConfig.ID.Express, ApiEnvironment.PROD)
    )
    @AllureId("3546")
    @DisplayName("Swap: change network fee")
    @Test
    fun changeNetworkFeeTest() {
        val inputAmount = "100"
        setupHooks().run {
            val tokenTitle = "Polygon"
            val balance = TOTAL_BALANCE

            step("Open 'Main Screen'") {
                scenario(OpenMainScreenScenario(composeTestRule))
            }
            step("Click on 'Synchronize addresses' button") {
                onMainScreen { synchronizeAddressesButton.clickWithAssertion() }
            }
            step("Assert wallet balance = $balance") {
                onMainScreen { walletBalance().assertTextContains(balance) }
            }
            step("Click on token with name: '$tokenTitle'") {
                onMainScreen { tokenWithTitleAndAddress(tokenTitle).clickWithAssertion() }
            }
            step("Click on token with name: '$tokenTitle'") {
                onTokenDetailsScreen { title.assertIsDisplayed() }
            }
            step("Click on 'Swap' button") {
                onTokenDetailsScreen { swapButton.performClick() }
            }
            step("Close 'Stories' screen") {
                onSwapStoriesScreen { closeButton.clickWithAssertion() }
            }
            step("Assert 'Swap' screen title is displayed") {
                onSwapTokenScreen { title.assertIsDisplayed() }
            }
            step("Assert 'Swap tokens on screen' button is displayed") {
                onSwapTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT) {
                        swapTokensOnscreenButton.assertIsDisplayed()
                    }
                }
            }
            step("Assert receive amount is displayed") {
                onSwapTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT) {
                        receiveAmount.assertIsDisplayed()
                    }
                }
            }
            step("Input swap amount = '$inputAmount'") {
                composeTestRule.waitForIdle()
                onSwapTokenScreen {
                    textInput.clickWithAssertion()
                    textInput.performTextReplacement(inputAmount)
                }
            }
            step("Assert input amount = '$inputAmount'") {
                onSwapTokenScreen { textInput.assertTextEquals(inputAmount) }
            }
            step("Click on 'Network fee' block") {
                onSwapTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT) {
                        networkFeeBlock.clickWithAssertion()
                    }
                }
            }
            step("Assert 'Select fee' bottom sheet title is displayed") {
                onSelectNetworkFeeBottomSheet { title.assertIsDisplayed() }
            }
            step("Assert 'Market' item is displayed") {
                onSelectNetworkFeeBottomSheet { marketSelectorItem.assertIsDisplayed() }
            }
            step("Assert 'Fast' item is displayed") {
                onSelectNetworkFeeBottomSheet { fastSelectorItem.assertIsDisplayed() }
            }
            step("Assert 'Read more' text block is displayed") {
                onSelectNetworkFeeBottomSheet { readMoreTextBlock.assertIsDisplayed() }
            }
            step("Click on 'Fast' item") {
                onSelectNetworkFeeBottomSheet { fastSelectorItem.assertIsDisplayed() }
            }
            step("Assert 'Network fee' block is displayed") {
                onSwapTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT) {
                        networkFeeBlock.assertIsDisplayed()
                    }
                }
            }
        }
    }
}