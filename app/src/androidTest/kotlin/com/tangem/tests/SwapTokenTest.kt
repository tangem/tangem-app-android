package com.tangem.tests

import androidx.compose.ui.test.hasText
import com.tangem.common.BaseTestCase
import com.tangem.common.annotations.ApiEnv
import com.tangem.common.annotations.ApiEnvConfig
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_LONG
import com.tangem.common.extensions.*
import com.tangem.common.utils.resetWireMockScenarios
import com.tangem.datasource.api.common.config.ApiConfig
import com.tangem.datasource.api.common.config.ApiEnvironment
import com.tangem.scenarios.openMainScreen
import com.tangem.scenarios.synchronizeAddresses
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
        val tokenTitle = "Polygon"

        setupHooks().run {

            resetWireMockScenarios()
            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Click on token with name: '$tokenTitle'") {
                onMainScreen { tokenWithTitleAndAddress(tokenTitle).clickWithAssertion() }
            }
            step("Click on token with name: '$tokenTitle'") {
                onTokenDetailsScreen { title.assertIsDisplayed() }
            }
            step("Click on 'Swap' button") {
                onTokenDetailsScreen { swapButton().performClick() }
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
                waitForIdle()
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
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
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
        setupHooks(
            additionalAfterSection = {
                enableWiFi()
                enableMobileData()
            }
        ).run {
            val tokenTitle = "Polygon"

            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Click on token with name: '$tokenTitle'") {
                onMainScreen { tokenWithTitleAndAddress(tokenTitle).clickWithAssertion() }
            }
            step("Click on token with name: '$tokenTitle'") {
                onTokenDetailsScreen { title.assertIsDisplayed() }
            }
            step("Turn off Wi-Fi and Mobile Data") {
                disableWiFi()
                disableMobileData()
            }
            step("Click on 'Swap' button") {
                onTokenDetailsScreen { swapButton().performClick() }
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
    @AllureId("3547")
    @DisplayName("Swap: change network fee")
    @Test
    fun changeNetworkFeeTest() {
        val inputAmount = "100"
        setupHooks().run {
            val tokenTitle = "Polygon"

            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Click on token with name: '$tokenTitle'") {
                onMainScreen { tokenWithTitleAndAddress(tokenTitle).clickWithAssertion() }
            }
            step("Click on token with name: '$tokenTitle'") {
                onTokenDetailsScreen { title.assertIsDisplayed() }
            }
            step("Click on 'Swap' button") {
                onTokenDetailsScreen { swapButton().performClick() }
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
                waitForIdle()
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
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                        networkFeeBlock.clickWithAssertion()
                    }
                }
            }
            step("Assert 'Select fee' bottom sheet title is displayed") {
                onSwapSelectNetworkFeeBottomSheet { title.assertIsDisplayed() }
            }
            step("Assert 'Market' item is displayed") {
                onSwapSelectNetworkFeeBottomSheet { marketSelectorItem.assertIsDisplayed() }
            }
            step("Assert 'Fast' item is displayed") {
                onSwapSelectNetworkFeeBottomSheet { fastSelectorItem.assertIsDisplayed() }
            }
            step("Assert 'Read more' text block is displayed") {
                onSwapSelectNetworkFeeBottomSheet { readMoreTextBlock.assertIsDisplayed() }
            }
            step("Click on 'Fast' item") {
                onSwapSelectNetworkFeeBottomSheet { fastSelectorItem.assertIsDisplayed() }
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