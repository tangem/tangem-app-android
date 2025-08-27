package com.tangem.tests

import android.content.Intent
import android.net.Uri
import androidx.test.InstrumentationRegistry.getTargetContext
import androidx.test.core.app.ApplicationProvider
import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.TOTAL_BALANCE
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.extensions.swipeUp
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
        val deeplinkScheme = "tangem://wc?uri="
        val deepLinkUri = deeplinkScheme + getWcUri()
        // val deepLinkUri = "tangem://wc?uri=wc:34f2df69e24b03c54eccb14324a6ac3881fb8cdf225880ca45670de4feefb4d5@2?relay-protocol=irn&symKey=067bcdb119852ed684fe81f183428ccd51a357342b4c40249f1455a928792405&expiryTimestamp=1756223736"

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

            step("Create WC session buy deeplink") {
                val context = ApplicationProvider.getApplicationContext<android.content.Context>()
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(deepLinkUri)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

                context.startActivity(intent)
            }
            // step("Assert 'Wallet Connect' pop up is displayed") {
            //     WalletConnectPopUpPageObject().popUp.isDisplayed()
            // }
            // step("Assert pop up title is displayed") {
            //     WalletConnectPopUpPageObject().title.isDisplayed()
            // }
            // step("Assert pop up message is displayed") {
            //     WalletConnectPopUpPageObject().message.isDisplayed()
            // }
            // step("Assert pop up 'Start' button is displayed") {
            //     WalletConnectPopUpPageObject().startButton.isDisplayed()
            // }
            // step("Assert pop up 'Reject' button is displayed") {
            //     WalletConnectPopUpPageObject().rejectButton.isDisplayed()
            // }
            // step("Click 'Start' button") {
            //     WalletConnectPopUpPageObject().startButton.click()
            // }
            step("Click 'Scan' button") {
                // onStoriesScreen { scanButton.clickWithAssertion() }
            }
            step("") {
                onWalletConnectBottomSheet { title.assertIsDisplayed() }
            }
            // step("Click 'More' button on TopBar") {
            //     onTopBar { moreButton.clickWithAssertion() }
            // }
            // step("Click on 'Wallet Connect' button") {
            //     onDetailsScreen { walletConnectButton.clickWithAssertion() }
            // }
            // step("Assert 'Wallet Connect' title is displayed") {
            //     onWalletConnectScreen { title.assertIsDisplayed() }
            // }
            // step("Assert session name title: '$sessionName' is displayed") {
            //     onWalletConnectScreen { sessionTitle(sessionName).assertIsDisplayed() }
            // }
            // step("Assert session name 'Close' button is displayed") {
            //     onWalletConnectScreen { sessionCloseButton(sessionName).assertIsDisplayed() }
            // }
            // step("Assert 'Add session button' is displayed") {
            //     onWalletConnectScreen { addSessionButton.assertIsDisplayed() }
            // }
        }
    }

    @AllureId("3834")
    @DisplayName("WC: open session from deeplink not on main screen")
    @Test
    fun openWalletConnectSessionNotOnMainScreen() {
        val balance = TOTAL_BALANCE
        val sessionName = "React App"
        val deeplinkScheme = "tangem://wc?uri="
        // val deepLinkUri = deeplinkScheme + getWcUri()
        val deepLinkUri = "tangem://wc?uri=wc:84747e38249c1cec7aa19249d0578d46ade55afdb220c3e180de9e5951068784@2?relay-protocol=irn&symKey=c734d88cf27390dbc0bf4812f189e0b498d74f394c5e7da6f18fc8fbe27824dc&expiryTimestamp=1756139555"

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
            step("Click 'Scan' button") {
                onStoriesScreen { scanButton.clickWithAssertion() }
            }
            // step("Assert pop up title is displayed") {
            //     WalletConnectPopUpPageObject().title.isDisplayed()
            // }
            // step("Assert pop up message is displayed") {
            //     WalletConnectPopUpPageObject().message.isDisplayed()
            // }
            // step("Assert pop up 'Start' button is displayed") {
            //     WalletConnectPopUpPageObject().startButton.isDisplayed()
            // }
            // step("Assert pop up 'Reject' button is displayed") {
            //     WalletConnectPopUpPageObject().rejectButton.isDisplayed()
            // }
            // step("Click 'Start' button") {
            //     WalletConnectPopUpPageObject().startButton.click()
            // }
            step("Click 'More' button on TopBar") {
                onMainScreen { buyButton.assertIsDisplayed() }
                onTopBar { moreButton.clickWithAssertion() }
            }
            step("Click on 'Wallet Connect' button") {
                onDetailsScreen { walletConnectButton.clickWithAssertion() }
            }
            step("Assert 'Wallet Connect' bottom sheet is displayed") {
                onWalletConnectBottomSheet { connectButton.clickWithAssertion() }
            }
            step("Assert 'Wallet Connect' title is displayed") {
                onWalletConnectScreen { title.assertIsDisplayed() }
            }
            // step("Assert session name title: '$sessionName' is displayed") {
            //     onWalletConnectScreen {
            //         flakySafely(timeoutMs = 50_000) { sessionTitle(sessionName).assertIsDisplayed() }
            //
            //     }
            // }
            // step("Assert session name 'Close' button is displayed") {
            //     onWalletConnectScreen { sessionCloseButton(sessionName).assertIsDisplayed() }
            // }
            // step("Assert 'Add session button' is displayed") {
            //     onWalletConnectScreen { addSessionButton.assertIsDisplayed() }
            // }
        }
    }

    @AllureId("886")
    @DisplayName("WC: open session from deeplink ")
    @Test
    fun openWalletConnectSession() {
        val balance = TOTAL_BALANCE
        val sessionName = "React App"
        val deeplinkScheme = "tangem://wc?uri="
        // val deepLinkUri = deeplinkScheme + getWcUri()
        val deepLinkUri = "tangem://wc?uri=wc:25babc1e5e359a96cede9114647fa7a192227496d6c2521a84405160032499bd@2?relay-protocol=irn&symKey=2d84d7997bcf318ae09824f4fb6bdd4c155d536824840b59483f679445875c6f&expiryTimestamp=1756230216"

        setupHooks().run {

            step("Open 'Main Screen'") {
                scenario(OpenMainScreenScenario(composeTestRule))
            }
            step("Click on 'Synchronize addresses' button") {
                onMainScreen { synchronizeAddressesButton.clickWithAssertion() }
            }
            step("Assert wallet balance = '$balance'") {
                onMainScreen { walletBalance().assertTextContains(balance) }
                // device.apps.kill(getTargetContext().packageName)
            }
            step("Open recent apps") {
                device.uiDevice.pressRecentApps()
            }
            Thread.sleep(2_000)
            step("Stop app by swipe") {
                swipeUp(startHeightRatio = 0.8f)
            }

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
            step("") {
                onWalletConnectBottomSheet { title.assertIsDisplayed() }
            }
            // step("Assert 'Wallet Connect' pop up is displayed") {
            //     WalletConnectPopUpPageObject().popUp.isDisplayed()
            // }
            // step("Assert pop up title is displayed") {
            //     WalletConnectPopUpPageObject().title.isDisplayed()
            // }
            // step("Assert pop up message is displayed") {
            //     WalletConnectPopUpPageObject().message.isDisplayed()
            // }
            // step("Assert pop up 'Start' button is displayed") {
            //     WalletConnectPopUpPageObject().startButton.isDisplayed()
            // }
            // step("Assert pop up 'Reject' button is displayed") {
            //     WalletConnectPopUpPageObject().rejectButton.isDisplayed()
            // }
            // step("Click 'Start' button") {
            //     WalletConnectPopUpPageObject().startButton.click()
            // }
            // step("Click 'More' button on TopBar") {
            //     onTopBar { moreButton.clickWithAssertion() }
            // }
            // step("Click on 'Wallet Connect' button") {
            //     onDetailsScreen { walletConnectButton.clickWithAssertion() }
            // }
            // step("Assert 'Wallet Connect' title is displayed") {
            //     onWalletConnectScreen { title.assertIsDisplayed() }
            // }
            // step("Assert session name title: '$sessionName' is displayed") {
            //     onWalletConnectScreen { sessionTitle(sessionName).assertIsDisplayed() }
            // }
            // step("Assert session name 'Close' button is displayed") {
            //     onWalletConnectScreen { sessionCloseButton(sessionName).assertIsDisplayed() }
            // }
            // step("Assert 'Add session button' is displayed") {
            //     onWalletConnectScreen { addSessionButton.assertIsDisplayed() }
            // }
        }
    }

}