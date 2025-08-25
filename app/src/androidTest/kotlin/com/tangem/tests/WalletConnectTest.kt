package com.tangem.tests

import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.TOTAL_BALANCE
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.utils.getWcUri
import com.tangem.scenarios.OpenMainScreenScenario
import com.tangem.screens.*
import dagger.hilt.android.testing.HiltAndroidTest
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Test

@HiltAndroidTest
class WalletConnectTest : BaseTestCase() {

    @AllureId("3833")
    @DisplayName("WC: open session from deeplink on main screen")
    @Test
    fun openWalletConnectSessionOnMainScreen() {
        val balance = TOTAL_BALANCE
        val sessionName = "React App"

        setupHooks().run {
            step("Open 'Main Screen'") {
                scenario(OpenMainScreenScenario(composeTestRule))
            }
            step("Click on 'Synchronize addresses' button") {
                onMainScreen { synchronizeAddressesButton.clickWithAssertion() }
            }
            step("Assert wallet balance = '$balance'") {
                onMainScreen { walletBalance().assertTextContains(balance) }
            }

            val deepLinkUrl = "tangem://wc?uri=wc:b6c6ab3fa9872ab5274d4463e9af5fa994dfed0ee8de1cbc9897143778b3f93c@2?relay-protocol=irn&symKey=64323070ae3f347fdb996847ea6ad64bb4e5ff3418f5a0bece62c63cdf05321a&expiryTimestamp=1755713592"

            step("Create WC session buy deeplink") {
                val context = ApplicationProvider.getApplicationContext<android.content.Context>()
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(deepLinkUrl)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

                context.startActivity(intent)
            }
            step("Assert 'Wallet Connect' pop up is displayed") {
                WalletConnectPopUpPageObject().popUp.isDisplayed()
            }
            step("Assert pop up title is displayed") {
                WalletConnectPopUpPageObject().title.isDisplayed()
            }
            step("Assert pop up message is displayed") {
                WalletConnectPopUpPageObject().message.isDisplayed()
            }
            step("Assert pop up 'Start' button is displayed") {
                WalletConnectPopUpPageObject().startButton.isDisplayed()
            }
            step("Assert pop up 'Reject' button is displayed") {
                WalletConnectPopUpPageObject().rejectButton.isDisplayed()
            }
            step("Click 'Start' button") {
                WalletConnectPopUpPageObject().startButton.click()
            }
            step("Click 'Scan' button") {
                onStoriesScreen { scanButton.clickWithAssertion() }
            }
            step("Click 'More' button on TopBar") {
                onTopBar { moreButton.clickWithAssertion() }
            }
            step("Click on 'Wallet Connect' button") {
                onDetailsScreen { walletConnectButton.clickWithAssertion() }
            }
            step("Assert 'Wallet Connect' title is displayed") {
                onWalletConnectScreen { title.assertIsDisplayed() }
            }
            step("Assert session name title: '$sessionName' is displayed") {
                onWalletConnectScreen { sessionTitle(sessionName).assertIsDisplayed() }
            }
            step("Assert session name 'Close' button is displayed") {
                onWalletConnectScreen { sessionCloseButton(sessionName).assertIsDisplayed() }
            }
            step("Assert 'Add session button' is displayed") {
                onWalletConnectScreen { addSessionButton.assertIsDisplayed() }
            }
        }
    }

    @AllureId("3834")
    @DisplayName("WC: open session from deeplink not on main screen")
    @Test
    fun openWalletConnectSessionNotOnMainScreen() {
        val balance = TOTAL_BALANCE
        val sessionName = "React App"
        val deeplinkScheme = "tangem://wc?uri="
        val deepLinkUri = deeplinkScheme + getWcUri()
        // val deepLinkUri = "tangem://wc?uri=wc:7a2f98ddf772411bcc58628a76a018f209f4583d4154032718477641dc2fafc6@2?relay-protocol=irn&symKey=e8eb377cd6c288d9c7625277db0269a992b3bfeead572fc957364ac7428b30a1&expiryTimestamp=1755875607"

        setupHooks().run {
            step("Open 'Main Screen'") {
                scenario(OpenMainScreenScenario(composeTestRule))
            }
            step("Click on 'Synchronize addresses' button") {
                onMainScreen { synchronizeAddressesButton.clickWithAssertion() }
            }
            step("Assert wallet balance = '$balance'") {
                onMainScreen { walletBalance().assertTextContains(balance) }
            }
            step("Click on 'Buy' button") {
                onMainScreen { buyButton.clickWithAssertion() }
            }
            step("Create WC session buy deeplink") {
                val context = ApplicationProvider.getApplicationContext<android.content.Context>()
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(deepLinkUri)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

                context.startActivity(intent)
            }
            step("Assert 'Wallet Connect' pop up is displayed") {
                WalletConnectPopUpPageObject().popUp.isDisplayed()
            }
            step("Assert pop up title is displayed") {
                WalletConnectPopUpPageObject().title.isDisplayed()
            }
            step("Assert pop up message is displayed") {
                WalletConnectPopUpPageObject().message.isDisplayed()
            }
            step("Assert pop up 'Start' button is displayed") {
                WalletConnectPopUpPageObject().startButton.isDisplayed()
            }
            step("Assert pop up 'Reject' button is displayed") {
                WalletConnectPopUpPageObject().rejectButton.isDisplayed()
            }
            step("Click 'Start' button") {
                WalletConnectPopUpPageObject().startButton.click()
            }
            step("Click 'Scan' button") {
                onStoriesScreen { scanButton.clickWithAssertion() }
            }
            step("Click 'More' button on TopBar") {
                onTopBar { moreButton.clickWithAssertion() }
            }
            step("Click on 'Wallet Connect' button") {
                onDetailsScreen { walletConnectButton.clickWithAssertion() }
            }
            step("Assert 'Wallet Connect' title is displayed") {
                onWalletConnectScreen { title.assertIsDisplayed() }
            }
            step("Assert session name title: '$sessionName' is displayed") {
                onWalletConnectScreen {
                    flakySafely(timeoutMs = 50_000) { sessionTitle(sessionName).assertIsDisplayed() }

                }
            }
            step("Assert session name 'Close' button is displayed") {
                onWalletConnectScreen { sessionCloseButton(sessionName).assertIsDisplayed() }
            }
            step("Assert 'Add session button' is displayed") {
                onWalletConnectScreen { addSessionButton.assertIsDisplayed() }
            }
        }
    }

    @AllureId("886")
    @DisplayName("WC: open session from deeplink ")
    @Test
    fun openWalletConnectSession() {
        val balance = TOTAL_BALANCE
        val sessionName = "React App"
        val deeplinkScheme = "tangem://wc?uri="
        val deepLinkUri = deeplinkScheme + getWcUri()

        setupHooks().run {
            step("Create WC session buy deeplink") {
                val context = ApplicationProvider.getApplicationContext<android.content.Context>()
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(deepLinkUri)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

                context.startActivity(intent)
            }

            step("Open 'Main Screen'") {
                scenario(OpenMainScreenScenario(composeTestRule))
            }
            step("Click on 'Synchronize addresses' button") {
                onMainScreen { synchronizeAddressesButton.clickWithAssertion() }
            }
            step("Assert wallet balance = '$balance'") {
                onMainScreen { walletBalance().assertTextContains(balance) }
            }
            step("Assert 'Wallet Connect' pop up is displayed") {
                WalletConnectPopUpPageObject().popUp.isDisplayed()
            }
            step("Assert pop up title is displayed") {
                WalletConnectPopUpPageObject().title.isDisplayed()
            }
            step("Assert pop up message is displayed") {
                WalletConnectPopUpPageObject().message.isDisplayed()
            }
            step("Assert pop up 'Start' button is displayed") {
                WalletConnectPopUpPageObject().startButton.isDisplayed()
            }
            step("Assert pop up 'Reject' button is displayed") {
                WalletConnectPopUpPageObject().rejectButton.isDisplayed()
            }
            step("Click 'Start' button") {
                WalletConnectPopUpPageObject().startButton.click()
            }
            step("Click 'More' button on TopBar") {
                onTopBar { moreButton.clickWithAssertion() }
            }
            step("Click on 'Wallet Connect' button") {
                onDetailsScreen { walletConnectButton.clickWithAssertion() }
            }
            step("Assert 'Wallet Connect' title is displayed") {
                onWalletConnectScreen { title.assertIsDisplayed() }
            }
            step("Assert session name title: '$sessionName' is displayed") {
                onWalletConnectScreen { sessionTitle(sessionName).assertIsDisplayed() }
            }
            step("Assert session name 'Close' button is displayed") {
                onWalletConnectScreen { sessionCloseButton(sessionName).assertIsDisplayed() }
            }
            step("Assert 'Add session button' is displayed") {
                onWalletConnectScreen { addSessionButton.assertIsDisplayed() }
            }
        }
    }

}