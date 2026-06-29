package com.tangem.tests.addFunds

import com.tangem.common.BaseTestCase
import com.tangem.common.extensions.assertTextContainsSafe
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.domain.models.scan.ProductType
import com.tangem.scenarios.openMainScreen
import com.tangem.scenarios.synchronizeAddresses
import com.tangem.screens.onAddTokenBottomSheet
import com.tangem.screens.onAddFundsBottomSheet
import com.tangem.screens.onBuyTokenDetailsScreen
import com.tangem.screens.onMainScreen
import dagger.hilt.android.testing.HiltAndroidTest
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.Issue
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Test

@HiltAndroidTest
class BuyTest : BaseTestCase() {

    @AllureId("587")
    @DisplayName("Buy. Display tokens available for purchase")
    @Test
    fun buyDisplayTokensAvailableForPurchaseTest() {
        val token = "Bitcoin"

        setupHooks().run {
            step("Open 'Main' screen") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Click on 'Add Funds' button") {
                onMainScreen { addFundsButton.clickWithAssertion() }
            }
            step("Click on $token in Wallet list") {
                onAddFundsBottomSheet { userTokenWithTitle(token).clickWithAssertion() }
            }
            step("Click on 'Buy' button") {
                onAddFundsBottomSheet { buyTokenButton.clickWithAssertion() }
            }
            step("Verify 'Buy $token' title is displayed") {
                onBuyTokenDetailsScreen {
                    topBarTitle.assertTextContainsSafe("Buy $token", substring = true)
                }
            }
        }
    }

    @AllureId("590")
    @DisplayName("Buy. Adding trending token to the main screen")
    @Test
    fun buyAddingTrendingTokenToMainScreenTest() {
        val token = "Solana"

        setupHooks().run {
            step("Open 'Main' screen") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Click on 'Add Funds' button") {
                onMainScreen { addFundsButton.clickWithAssertion() }
            }
            step("Click on $token in Trending list") {
                onAddFundsBottomSheet { trendingTokenWithTitle(token).clickWithAssertion() }
            }
            step("Click on 'Add' button") {
                onAddTokenBottomSheet { addButton.clickWithAssertion() }
            }
            step("Close 'Get token' screen") {
                onAddFundsBottomSheet { closeButton.clickWithAssertion() }
            }
            step("Verify token $token exists on main screen") {
                onMainScreen { assertTokenExists(token) }
            }
            step("Click on 'Add Funds' button") {
                onMainScreen { addFundsButton.clickWithAssertion() }
            }
            step("Verify token $token in Wallet list") {
                onAddFundsBottomSheet { userTokenWithTitle(token).assertIsDisplayed() }
            }
        }
    }

    @AllureId("3613")
    @DisplayName("On-ramp Buy: S2C card doesn't have Buy and Sell options")
    @Test
    @Issue("[REDACTED_TASK_KEY]")
    fun buyAndSellIsNotAvailableForS2CCardTest() {
        setupHooks().run {
            step("Open 'Main' screen") {
                openMainScreen(productType = ProductType.Start2Coin)
            }
            step("Verify 'Add funds' button is displayed") {
                onMainScreen { addFundsButton.assertIsDisplayed() }
            }
            step("Verify Buy/Sell action buttons are hidden") {
                onMainScreen {
                    buyButton.assertDoesNotExist()
                }
            }
        }
    }
}