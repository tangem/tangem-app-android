package com.tangem.tests

import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.TOTAL_BALANCE
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.utils.getWcUri
import com.tangem.scenarios.*
import com.tangem.screens.*
import com.tangem.wallet.BuildConfig
import dagger.hilt.android.testing.HiltAndroidTest
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Ignore
import org.junit.Test

@HiltAndroidTest
class WalletConnectTest : BaseTestCase() {

    @AllureId("3833")
    @DisplayName("WC (React App): open session from deeplink on main screen")
    @Ignore("TODO [REDACTED_JIRA] React app deeplink doesn't work")
    @Test
    fun openWalletConnectSessionOnMainScreen() {
        val balance = TOTAL_BALANCE
        val dAppName = "React App"
        val deepLinkUri = getWcUri()

        setupHooks().run {
            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses(balance)
            }
            step("Create WC session buy deeplink") {
                openAppByDeepLink(deepLinkUri)
            }
            step("Check 'Wallet Connect' bottom sheet") {
                flakySafely(WAIT_UNTIL_TIMEOUT) {
                    checkWalletConnectBottomSheet()
                }
            }
            step("Assert 'Connect' button is enabled") {
                onWalletConnectBottomSheet { connectButton.assertIsEnabled() }
            }
            step("Click on 'Connect' button") {
                onWalletConnectBottomSheet { connectButton.performClick() }
            }
            step("Assert 'Connect' button is not displayed") {
                onWalletConnectBottomSheet { connectButton.assertIsNotDisplayed() }
            }
            step("Click 'More' button on TopBar") {
                onTopBar { moreButton.clickWithAssertion() }
            }
            step("Click on 'Wallet Connect' button") {
                onDetailsScreen { walletConnectButton.clickWithAssertion() }
            }
            step("Check 'Wallet Connect' screen") {
                flakySafely(WAIT_UNTIL_TIMEOUT) {
                    checkWalletConnectScreen()
                }
            }
            step("Click on app icon") {
                onWalletConnectScreen { appIcon.performClick() }
            }
            step("Check 'Wallet Connect' details bottom sheet") {
                checkWalletConnectDetailsBottomSheet(dAppName)
            }
            step("Click on 'Disconnect' button") {
                onWalletConnectDetailsBottomSheet { disconnectButton.performClick() }
            }
            step("Assert connection is not displayed") {
                onWalletConnectScreen { appName.assertIsNotDisplayed() }
            }
        }
    }

    @AllureId("3834")
    @DisplayName("WC (React App): open session from deeplink not on main screen")
    @Ignore("TODO [REDACTED_JIRA] React app deeplink doesn't work")
    @Test
    fun openWalletConnectSessionNotOnMainScreen() {
        val balance = TOTAL_BALANCE
        val dAppName = "React App"
        val deepLinkUri = getWcUri()

        setupHooks().run {
            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses(balance)
            }
            step("Click 'More' button on TopBar") {
                onTopBar { moreButton.clickWithAssertion() }
            }
            step("Click on 'Wallet Connect' button") {
                onDetailsScreen { walletConnectButton.clickWithAssertion() }
            }
            step("Create WC session buy deeplink") {
                openAppByDeepLink(deepLinkUri)
            }
            step("Check 'Wallet Connect' bottom sheet") {
                composeTestRule.waitForIdle()
                flakySafely(WAIT_UNTIL_TIMEOUT) {
                    checkWalletConnectBottomSheet()
                }
            }
            step("Click on 'Connect' button") {
                onWalletConnectBottomSheet { connectButton.performClick() }
            }
            step("Assert 'Connect' button is not displayed") {
                onWalletConnectBottomSheet { connectButton.assertIsNotDisplayed() }
            }
            step("Check 'Wallet Connect' screen") {
                flakySafely(WAIT_UNTIL_TIMEOUT) {
                    checkWalletConnectScreen()
                }
            }
            step("Click on app icon") {
                onWalletConnectScreen { appIcon.performClick() }
            }
            step("Check 'Wallet Connect' details bottom sheet") {
                flakySafely(WAIT_UNTIL_TIMEOUT) {
                    checkWalletConnectDetailsBottomSheet(dAppName)
                }
            }
            step("Click on 'Disconnect' button") {
                onWalletConnectDetailsBottomSheet { disconnectButton.performClick() }
            }
            step("Assert connection is not displayed") {
                onWalletConnectScreen { appName.assertIsNotDisplayed() }
            }
        }
    }

    @AllureId("886")
    @DisplayName("WC (React App): open session from deeplink ")
    @Ignore("TODO [REDACTED_JIRA] React app deeplink doesn't work")
    @Test
    fun openWalletConnectSession() {
        val balance = TOTAL_BALANCE
        val dAppName = "React App"
        val packageName = BuildConfig.APPLICATION_ID
        val deepLinkUri = getWcUri()

        setupHooks().run {
            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses(balance)
            }
            step("Kill app") {
                device.apps.kill(packageName)
            }
            step("Create WC session buy deeplink") {
                openAppByDeepLink(deepLinkUri)
            }
            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Check 'Wallet Connect' bottom sheet") {
                flakySafely(WAIT_UNTIL_TIMEOUT) {
                    checkWalletConnectBottomSheet()
                }
            }
            step("Click on 'Connect' button") {
                onWalletConnectBottomSheet { connectButton.performClick() }
            }
            step("Assert 'Connect' button is not displayed") {
                onWalletConnectBottomSheet { connectButton.assertIsNotDisplayed() }
            }
            step("Click 'More' button on TopBar") {
                onTopBar { moreButton.clickWithAssertion() }
            }
            step("Click on 'Wallet Connect' button") {
                onDetailsScreen { walletConnectButton.clickWithAssertion() }
            }
            step("Check 'Wallet Connect' screen") {
                checkWalletConnectScreen()
            }
            step("Click on app icon") {
                onWalletConnectScreen { appIcon.performClick() }
            }
            step("Check 'Wallet Connect' details bottom sheet") {
                checkWalletConnectDetailsBottomSheet(dAppName)
            }
            step("Click on 'Disconnect' button") {
                onWalletConnectDetailsBottomSheet { disconnectButton.performClick() }
            }
            step("Assert connection is not displayed") {
                onWalletConnectScreen { appName.assertIsNotDisplayed() }
            }
        }
    }
}