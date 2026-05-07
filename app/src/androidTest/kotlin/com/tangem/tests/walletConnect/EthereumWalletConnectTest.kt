package com.tangem.tests.walletConnect

import android.Manifest
import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.utils.getWcUri
import com.tangem.common.utils.setClipboardText
import com.tangem.scenarios.checkWalletConnectBottomSheet
import com.tangem.scenarios.checkWalletConnectDetailsBottomSheet
import com.tangem.scenarios.checkWalletConnectScreen
import com.tangem.scenarios.openAppByDeepLink
import com.tangem.scenarios.openMainScreen
import com.tangem.scenarios.openWalletConnectScreen
import com.tangem.scenarios.synchronizeAddresses
import com.tangem.screens.onScanQrScreen
import com.tangem.screens.onWalletConnectBottomSheet
import com.tangem.screens.onWalletConnectDetailsBottomSheet
import com.tangem.screens.onWalletConnectScreen
import com.tangem.wallet.BuildConfig
import dagger.hilt.android.testing.HiltAndroidTest
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Test

@HiltAndroidTest
class EthereumWalletConnectTest : BaseTestCase() {

    @AllureId("3958")
    @DisplayName("WC (React App): open session from deeplink on main screen")
    @Test
    fun openWalletConnectSessionOnMainScreenTest() {
        val dAppName = "Tangem QA Tools"
        val deepLinkUri = getWcUri()

        setupHooks().run {
            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Create WC session by deeplink") {
                openAppByDeepLink(deepLinkUri)
            }
            step("Check 'Wallet Connect' bottom sheet") {
                flakySafely(TestConstants.WAIT_UNTIL_TIMEOUT) {
                    checkWalletConnectBottomSheet()
                }
            }
            step("Assert 'Connect' button is enabled") {
                onWalletConnectBottomSheet { connectButton.assertIsEnabled() }
            }
            step("Click on 'Connect' button") {
                waitForIdle()
                onWalletConnectBottomSheet { connectButton.performClick() }
            }
            step("Assert 'Connect' button is not displayed") {
                waitForIdle()
                onWalletConnectBottomSheet { connectButton.assertIsNotDisplayed() }
            }
            step("Open 'Wallet Connect' screen") {
                openWalletConnectScreen()
            }
            step("Check 'Wallet Connect' screen with connections") {
                flakySafely(TestConstants.WAIT_UNTIL_TIMEOUT) {
                    checkWalletConnectScreen(withConnections = true)
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
            step("Check 'Wallet Connect' screen without connections") {
                checkWalletConnectScreen(withConnections = false)
            }
        }
    }

    @AllureId("3959")
    @DisplayName("WC (React App): open session from deeplink not on main screen")
    @Test
    fun openWalletConnectSessionNotOnMainScreenTest() {
        val dAppName = "Tangem QA Tools"
        val deepLinkUri = getWcUri()

        setupHooks().run {
            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Open 'Wallet Connect' screen") {
                openWalletConnectScreen()
                checkWalletConnectScreen(false)
            }
            step("Create WC session by deeplink") {
                openAppByDeepLink(deepLinkUri)
            }
            step("Check 'Wallet Connect' bottom sheet") {
                flakySafely(TestConstants.WAIT_UNTIL_TIMEOUT) {
                    checkWalletConnectBottomSheet()
                }
            }
            step("Click on 'Connect' button") {
                waitForIdle()
                onWalletConnectBottomSheet { connectButton.performClick() }
            }
            step("Assert 'Connect' button is not displayed") {
                waitForIdle()
                onWalletConnectBottomSheet { connectButton.assertIsNotDisplayed() }
            }
            step("Check 'Wallet Connect' screen with connections") {
                flakySafely(TestConstants.WAIT_UNTIL_TIMEOUT) {
                    checkWalletConnectScreen(withConnections = true)
                }
            }
            step("Click on app icon") {
                onWalletConnectScreen { appIcon.performClick() }
            }
            step("Check 'Wallet Connect' details bottom sheet") {
                flakySafely(TestConstants.WAIT_UNTIL_TIMEOUT) {
                    checkWalletConnectDetailsBottomSheet(dAppName)
                }
            }
            step("Click on 'Disconnect' button") {
                onWalletConnectDetailsBottomSheet { disconnectButton.performClick() }
            }
            step("Check 'Wallet Connect' screen without connections") {
                checkWalletConnectScreen(withConnections = false)
            }
        }
    }

    @AllureId("3957")
    @DisplayName("WC (React App): open session from deeplink")
    @Test
    fun openWalletConnectSessionTest() {
        val dAppName = "Tangem QA Tools"
        val packageName = BuildConfig.APPLICATION_ID
        val deepLinkUri = getWcUri()

        setupHooks().run {
            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Kill app") {
                device.apps.kill(packageName)
            }
            step("Create WC session by deeplink") {
                openAppByDeepLink(deepLinkUri)
            }
            step("Check 'Wallet Connect' bottom sheet") {
                flakySafely(TestConstants.WAIT_UNTIL_TIMEOUT) {
                    checkWalletConnectBottomSheet()
                }
            }
            step("Click on 'Connect' button") {
                onWalletConnectBottomSheet { connectButton.performClick() }
            }
            step("Assert 'Connect' button is not displayed") {
                onWalletConnectBottomSheet { connectButton.assertIsNotDisplayed() }
            }
            step("Open 'Wallet Connect' screen") {
                openWalletConnectScreen()
            }
            step("Check 'Wallet Connect' screen with connections") {
                checkWalletConnectScreen(withConnections = true)
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
            step("Check 'Wallet Connect' screen without connections") {
                checkWalletConnectScreen(withConnections = false)
            }
        }
    }

    @AllureId("887")
    @DisplayName("WC: open session by 'Paste from clipboard' button")
    @Test
    fun openWalletConnectSessionByClipboardLinkTest() {
        val dAppName = "Tangem QA Tools"
        val context = device.context
        val deepLinkUri = getWcUri()
        val packageName = BuildConfig.APPLICATION_ID
        val permissionName = Manifest.permission.CAMERA

        setupHooks(
            additionalBeforeSection = {
                device.uiDevice.executeShellCommand("pm grant $packageName $permissionName")
            },
        ).run {
            step("Set URI to clipboard") {
                setClipboardText(context, deepLinkUri)
            }
            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Open 'Wallet Connect' screen") {
                openWalletConnectScreen()
            }
            step("Click 'New connection' button") {
                onWalletConnectScreen { newConnectionButton.performClick() }
            }
            step("CLick 'Paste from clipboard' button") {
                onScanQrScreen { pasteFromClipboardButton.clickWithAssertion() }
            }
            step("Check 'Wallet Connect' bottom sheet") {
                waitForIdle()
                flakySafely(TestConstants.WAIT_UNTIL_TIMEOUT) {
                    checkWalletConnectBottomSheet()
                }
            }
            step("Click on 'Connect' button") {
                waitForIdle()
                onWalletConnectBottomSheet { connectButton.performClick() }
            }
            step("Assert 'Connect' button is not displayed") {
                waitForIdle()
                onWalletConnectBottomSheet { connectButton.assertIsNotDisplayed() }
            }
            step("Check 'Wallet Connect' screen with connections") {
                checkWalletConnectScreen(withConnections = true)
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
            step("Check 'Wallet Connect' screen without connections") {
                checkWalletConnectScreen(withConnections = false)
            }
        }
    }
}