package com.tangem.tests

import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.TOTAL_BALANCE
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.extensions.swipeUp
import com.tangem.common.utils.getWcUri
import com.tangem.screens.*
import com.tangem.scenarios.*
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
                checkWalletConnectBottomSheet()
            }
            step("Click on 'Connect' button") {
                onWalletConnectBottomSheet { connectButton.performClick() }
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
            step("Click on 'Disconnect button' is displayed") {
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
            step("Click on 'Buy' button") {
                onMainScreen { buyButton.clickWithAssertion() }
            }
            step("Create WC session buy deeplink") {
                openAppByDeepLink(deepLinkUri)
            }
            step("Check 'Wallet Connect' bottom sheet") {
                checkWalletConnectBottomSheet()
            }
            step("Click on 'Connect' button") {
                onWalletConnectBottomSheet { connectButton.performClick() }
            }
            step("Click 'More' button on TopBar") {
                onTopBar { moreButton.clickWithAssertion() }
            }
            step("Click on 'Wallet Connect' button") {
                onDetailsScreen { walletConnectButton.clickWithAssertion() }
            }
            step("Assert 'Wallet Connect' bottom sheet is displayed") {
                onWalletConnectBottomSheet { connectButton.clickWithAssertion() }
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
            step("Click on 'Disconnect button' is displayed") {
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
        val deepLinkUri = getWcUri()

        setupHooks().run {
            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses(balance)
            }
            step("Open recent apps") {
                device.uiDevice.pressRecentApps()
            }
            step("Stop app by swipe") {
                swipeUp(startHeightRatio = 0.8f)
            }
            step("Create WC session buy deeplink") {
                openAppByDeepLink(deepLinkUri)
            }
            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Check 'Wallet Connect' bottom sheet") {
                checkWalletConnectBottomSheet()
            }
            step("Click on 'Connect' button") {
                onWalletConnectBottomSheet { connectButton.performClick() }
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
            step("Click on 'Disconnect button' is displayed") {
                onWalletConnectDetailsBottomSheet { disconnectButton.performClick() }
            }
            step("Assert connection is not displayed") {
                onWalletConnectScreen { appName.assertIsNotDisplayed() }
            }
        }
    }
}