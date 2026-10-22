package com.tangem.tests.addFunds

import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.scenarios.openBuyScreenFromAddFunds
import com.tangem.scenarios.openBuyScreenFromMarkets
import com.tangem.scenarios.openBuyScreenFromTokenContextMenu
import com.tangem.scenarios.openBuyScreenFromTokenDetails
import com.tangem.scenarios.openMainScreen
import com.tangem.scenarios.returnFromProviderToOnrampInProgress
import com.tangem.scenarios.synchronizeAddresses
import com.tangem.screens.onMainScreen
import com.tangem.screens.onMarketsTokenDetailsScreen
import com.tangem.screens.onOnrampSuccessScreen
import com.tangem.screens.onTokenDetailsScreen
import dagger.hilt.android.testing.HiltAndroidTest
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Test

@HiltAndroidTest
class BuyReturnToAppTest : BaseTestCase() {

    private val tokenTitle = "Polygon"
    private val marketsTokenTitle = "Ethereum"
    private val fiatAmount = "1"
    private val providerName = "Mercuryo"
    private val onrampTransactionId = "mock-onramp-tx-001"
    private val providerPageUrl = "https://example.com/onramp-widget"
    private val purchaseDate = "Oct, 2026"
    private val fiatAmountText = "1.00"
    private val cryptoAmountText = "488.24938338"

    @AllureId("2573")
    @DisplayName("Buy. Purchase completion. Return to the app. Token details")
    @Test
    fun returnToAppFromTokenDetailsTest() {
        setupHooks().run {
            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Open 'Buy' screen from token details and write amount") {
                openBuyScreenFromTokenDetails(tokenTitle = tokenTitle, fiatAmount = fiatAmount)
            }
            step("Return from the provider to the 'In progress' screen") {
                returnFromProviderToOnrampInProgress(
                    providerPageUrl = providerPageUrl,
                    onrampTransactionId = onrampTransactionId,
                    providerName = providerName,
                    purchaseDate = purchaseDate,
                    fiatAmountText = fiatAmountText,
                    cryptoAmountText = cryptoAmountText,
                )
            }
            step("Click on 'Close' button") {
                onOnrampSuccessScreen { closeButton.clickWithAssertion() }
            }
            step("Assert token details screen is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT) { onTokenDetailsScreen { screenContainer.assertIsDisplayed() } }
            }
        }
    }

    @AllureId("12739")
    @DisplayName("Buy. Purchase completion. Return to the app. Main screen, token context menu")
    @Test
    fun returnToAppFromTokenContextMenuTest() {
        setupHooks().run {
            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Open 'Buy' screen from the token context menu and write amount") {
                openBuyScreenFromTokenContextMenu(tokenTitle = tokenTitle, fiatAmount = fiatAmount)
            }
            step("Return from the provider to the 'In progress' screen") {
                returnFromProviderToOnrampInProgress(
                    providerPageUrl = providerPageUrl,
                    onrampTransactionId = onrampTransactionId,
                    providerName = providerName,
                    purchaseDate = purchaseDate,
                    fiatAmountText = fiatAmountText,
                    cryptoAmountText = cryptoAmountText,
                )
            }
            step("Click on 'Close' button") {
                onOnrampSuccessScreen { closeButton.clickWithAssertion() }
            }
            step("Assert main screen is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT) { onMainScreen { screenContainer.assertIsDisplayed() } }
            }
        }
    }

    @AllureId("12740")
    @DisplayName("Buy. Purchase completion. Return to the app. Main screen, Add Funds, token selection")
    @Test
    fun returnToAppFromAddFundsTest() {
        setupHooks().run {
            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Open 'Buy' screen from 'Add Funds' and write amount") {
                openBuyScreenFromAddFunds(tokenTitle = tokenTitle, fiatAmount = fiatAmount)
            }
            step("Return from the provider to the 'In progress' screen") {
                returnFromProviderToOnrampInProgress(
                    providerPageUrl = providerPageUrl,
                    onrampTransactionId = onrampTransactionId,
                    providerName = providerName,
                    purchaseDate = purchaseDate,
                    fiatAmountText = fiatAmountText,
                    cryptoAmountText = cryptoAmountText,
                )
            }
            step("Click on 'Close' button") {
                onOnrampSuccessScreen { closeButton.clickWithAssertion() }
            }
            step("Assert main screen is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT) { onMainScreen { screenContainer.assertIsDisplayed() } }
            }
        }
    }

    @AllureId("12741")
    @DisplayName("Buy. Purchase completion. Return to the app. Markets")
    @Test
    fun returnToAppFromMarketsTest() {
        setupHooks().run {
            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Open 'Buy' screen from 'Markets' and write amount") {
                openBuyScreenFromMarkets(tokenName = marketsTokenTitle, fiatAmount = fiatAmount)
            }
            step("Return from the provider to the 'In progress' screen") {
                returnFromProviderToOnrampInProgress(
                    providerPageUrl = providerPageUrl,
                    onrampTransactionId = onrampTransactionId,
                    providerName = providerName,
                    purchaseDate = purchaseDate,
                    fiatAmountText = fiatAmountText,
                    cryptoAmountText = cryptoAmountText,
                )
            }
            step("Click on 'Close' button") {
                onOnrampSuccessScreen { closeButton.clickWithAssertion() }
            }
            step("Assert 'Markets' token details screen is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT) { onMarketsTokenDetailsScreen { inYourPortfolioBlock.assertIsDisplayed() } }
            }
        }
    }
}