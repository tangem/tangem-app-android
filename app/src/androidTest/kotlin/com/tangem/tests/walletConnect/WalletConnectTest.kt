package com.tangem.tests.walletConnect

import android.Manifest
import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants
import com.tangem.common.extensions.assertSnackbarWithText
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.utils.getWcUri
import com.tangem.common.utils.setClipboardText
import com.tangem.scenarios.*
import com.tangem.screens.onWarningBottomSheet
import com.tangem.wallet.BuildConfig
import dagger.hilt.android.testing.HiltAndroidTest
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Test

@HiltAndroidTest
class WalletConnectTest : BaseTestCase() {

    @AllureId("9037")
    @DisplayName("WC: invalid wallet connect link")
    @Test
    fun invalidWalletConnectLinkTest() {
        val context = device.context
        val deepLinkUri = "wc:384617d590a47f11c26311b5cf2418859682920aa0ad52"
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
            step("Create connection via 'Paste from clipboard' button") {
                createConnectionViaPasteFromClipboardButton()
            }
            step("Assert error snackbar about invalid WC URI is displayed") {
                assertSnackbarWithText("getUserInfo")
            }
        }
    }

    @AllureId("9040")
    @DisplayName("WC (React App): repeat open/close session")
    @Test
    fun repeatedConnectByWalletConnectDeeplinkScreenTest() {
        val dAppName = "Tangem QA Tools"
        val context = device.context
        val packageName = BuildConfig.APPLICATION_ID
        val permissionName = Manifest.permission.CAMERA
        val sessionsCount = 3

        setupHooks(
            additionalBeforeSection = {
                device.uiDevice.executeShellCommand("pm grant $packageName $permissionName")
            },
        ).run {
            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Open 'Wallet Connect' screen") {
                openWalletConnectScreen()
            }
            repeat(sessionsCount) { iteration ->
                step("Session #${iteration + 1}: connect and disconnect") {
                    establishAndDisconnectWcSession(
                        context = context,
                        deepLinkUri = getWcUri(),
                        dAppName = dAppName,
                    )
                }
                step("Check 'Wallet Connect' screen without connections") {
                    checkWalletConnectScreen(withConnections = false)
                }
            }
        }
    }

    @AllureId("9066")
    @DisplayName("WC: connect to unsupported dApp shows error")
    @Test
    fun connectToUnsupportedDAppShowsUnsupportedErrorTest() {
        val unsupportedDAppUrl = "https://dydx.trade/test"
        val dAppName = "dYdX"
        val context = device.context
        val packageName = BuildConfig.APPLICATION_ID
        val permissionName = Manifest.permission.CAMERA

        setupHooks(
            additionalBeforeSection = {
                device.uiDevice.executeShellCommand("pm grant $packageName $permissionName")
            },
        ).run {
            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Open 'Wallet Connect' screen") {
                openWalletConnectScreen()
            }
            step("Set unsupported dApp URI to clipboard") {
                setClipboardText(
                    context = context,
                    text = getWcUri(dAppUrl = unsupportedDAppUrl, dAppName = dAppName),
                )
            }
            step("Create connection via 'Paste from clipboard' button") {
                createConnectionViaPasteFromClipboardButton()
            }
            step("Wait for unsupported dApp error bottom sheet") {
                composeTestRule.waitUntil(timeoutMillis = TestConstants.WAIT_UNTIL_TIMEOUT) {
                    runCatching {
                        onWarningBottomSheet { gotItButton.assertIsDisplayed() }
                    }.isSuccess
                }
            }
            step("Click on 'Got it' button") {
                onWarningBottomSheet { gotItButton.clickWithAssertion() }
            }
            step("Check 'Wallet Connect' screen without connections") {
                checkWalletConnectScreen(withConnections = false)
            }
        }
    }
}