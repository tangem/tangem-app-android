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
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Ignore
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
        // Tether is in the market block's top-5 preview, which is capped and has no search fallback.
        val token = "Tether"

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
            step("Click on $token in the market list") {
                onAddFundsBottomSheet { trendingTokenWithTitle(token).clickWithAssertion() }
            }
            step("Click on 'Confirm' button") {
                onAddTokenBottomSheet { confirmButton.clickWithAssertion() }
            }
            step("Close 'Get token' screen") {
                onAddFundsBottomSheet { closeButton.clickWithAssertion() }
            }
            // The main-screen portfolio comes from the accounts endpoint, which the save never updates.
            step("Click on 'Add Funds' button") {
                onMainScreen { addFundsButton.clickWithAssertion() }
            }
            step("Verify token $token in Wallet list") {
                flakySafely { onAddFundsBottomSheet { userTokenWithTitle(token).assertIsDisplayed() } }
            }
        }
    }

    @AllureId("3613")
    @DisplayName("On-ramp Buy: S2C card doesn't have Buy and Sell options")
    @Test
    // Ignored due to not completed fix task and unclear requirements
    @Ignore("[REDACTED_TASK_KEY]")
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