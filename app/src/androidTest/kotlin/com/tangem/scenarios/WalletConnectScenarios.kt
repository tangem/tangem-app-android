package com.tangem.scenarios

import android.content.Context
import androidx.compose.ui.test.onAllNodesWithText
import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_SHORT
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.utils.setClipboardText
import com.tangem.core.ui.R
import com.tangem.screens.onScanQrScreen
import com.tangem.screens.onWalletConnectBottomSheet
import com.tangem.screens.onWalletConnectDetailsBottomSheet
import com.tangem.screens.onWalletConnectScreen
import com.tangem.screens.onWarningBottomSheet
import io.github.kakaocup.kakao.common.utilities.getResourceString
import io.qameta.allure.kotlin.Allure.step

fun BaseTestCase.checkWalletConnectBottomSheet() {
    waitForIdle()
    step("Assert 'Wallet Connect' bottom sheet title is displayed") {
        onWalletConnectBottomSheet { title.assertIsDisplayed() }
    }
    step("Assert 'Wallet Connect' bottom sheet app icon is displayed") {
        onWalletConnectBottomSheet { appIcon.assertIsDisplayed() }
    }
    step("Assert 'Wallet Connect' bottom sheet app name is displayed") {
        onWalletConnectBottomSheet { appName.assertIsDisplayed() }
    }
    step("Assert 'Wallet Connect' bottom sheet app URL is displayed") {
        onWalletConnectBottomSheet { appUrl.assertIsDisplayed() }
    }
    step("Assert 'Wallet Connect' bottom sheet connection request icon is displayed") {
        onWalletConnectBottomSheet { connectionRequestIcon.assertIsDisplayed() }
    }
    step("Assert 'Wallet Connect' bottom sheet connection request text is displayed") {
        onWalletConnectBottomSheet { connectionRequestText.assertIsDisplayed() }
    }
    step("Assert 'Wallet Connect' bottom sheet connection request chevron is displayed") {
        onWalletConnectBottomSheet { connectionRequestChevron.assertIsDisplayed() }
    }
    step("Assert 'Wallet Connect' bottom sheet wallet icon is displayed") {
        onWalletConnectBottomSheet { walletIcon.assertIsDisplayed() }
    }
    step("Assert 'Wallet Connect' bottom sheet wallet title is displayed") {
        onWalletConnectBottomSheet { walletNameTitle.assertIsDisplayed() }
    }
    step("Assert 'Wallet Connect' bottom sheet wallet name is displayed") {
        onWalletConnectBottomSheet { walletName.assertIsDisplayed() }
    }
    step("Assert 'Wallet Connect' bottom sheet networks icon is displayed") {
        onWalletConnectBottomSheet { networksIcon.assertIsDisplayed() }
    }
    step("Assert 'Wallet Connect' bottom sheet networks title is displayed") {
        onWalletConnectBottomSheet { networksTitle.assertIsDisplayed() }
    }
    step("Assert 'Wallet Connect' bottom sheet right networks icons is displayed") {
        onWalletConnectBottomSheet { networksIcons.assertIsDisplayed() }
    }
    step("'Wallet Connect' bottom sheet networks selector icon is displayed") {
        onWalletConnectBottomSheet { networksSelectorIcon.assertIsDisplayed() }
    }
    step("Assert 'Wallet Connect' bottom sheet 'Cancel' button is displayed") {
        onWalletConnectBottomSheet { cancelButton.assertIsDisplayed() }
    }
    step("Assert 'Wallet Connect' bottom sheet 'Connect' button is displayed") {
        onWalletConnectBottomSheet { connectButton.assertIsDisplayed() }
    }
}

fun BaseTestCase.checkWalletConnectScreen(withConnections: Boolean) {
    waitForIdle()
    step("Assert 'Wallet Connect' title is displayed") {
        onWalletConnectScreen { title.assertIsDisplayed() }
    }
    step("Assert 'New Connection' button is displayed") {
        onWalletConnectScreen { newConnectionButton.assertIsDisplayed() }
    }
    if (withConnections) {
        step("Assert 'More' button is displayed") {
            onWalletConnectScreen { moreButton.assertIsDisplayed() }
        }
        step("Assert wallet name is displayed") {
            onWalletConnectScreen { walletName.assertIsDisplayed() }
        }
        step("Assert app icon is displayed") {
            onWalletConnectScreen { appIcon.assertIsDisplayed() }
        }
        step("Assert app name is displayed") {
            onWalletConnectScreen { appName.assertIsDisplayed() }
        }
        step("Assert app URL is displayed") {
            onWalletConnectScreen { appUrl.assertIsDisplayed() }
        }
    } else {
        step("Assert wallet name is not displayed") {
            onWalletConnectScreen { walletName.assertIsNotDisplayed() }
        }
        step("Assert app icon is not displayed") {
            onWalletConnectScreen { appIcon.assertIsNotDisplayed() }
        }
        step("Assert app name is not displayed") {
            onWalletConnectScreen { appName.assertIsNotDisplayed() }
        }
        step("Assert approve icon is not displayed") {
            onWalletConnectScreen { approveIcon.assertIsNotDisplayed() }
        }
        step("Assert app URL is not displayed") {
            onWalletConnectScreen { appUrl.assertIsNotDisplayed() }
        }
        step("Assert 'Wallet Connect' image is displayed") {
            onWalletConnectScreen { walletConnectImage.assertIsDisplayed() }
        }
        step("Assert 'No session' title is displayed") {
            onWalletConnectScreen { noSessionTitle.assertIsDisplayed() }
        }
        step("Assert 'No session' text is displayed") {
            onWalletConnectScreen { noSessionText.assertIsDisplayed() }
        }
    }

}

fun BaseTestCase.establishAndDisconnectWcSession(
    context: Context,
    deepLinkUri: String?,
    dAppName: String,
) {
    step("Set URI to clipboard") {
        setClipboardText(context, deepLinkUri)
    }
    step("Create connection via 'Paste from clipboard' button") {
        createConnectionViaPasteFromClipboardButton()
    }
    step("Check 'Wallet Connect' bottom sheet") {
        composeTestRule.waitUntil(timeoutMillis = TestConstants.WAIT_UNTIL_TIMEOUT) {
            runCatching { checkWalletConnectBottomSheet() }.isSuccess
        }
    }
    step("Click on 'Connect' button and dismiss 'Unknown domain' alert if shown") {
        confirmWcConnection()
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
}

/**
 * Clicks 'Connect' in the WalletConnect bottom sheet and dismisses the 'Unknown domain' security
 * alert if it appears.
 *
 * qa-tools URIs are not registered with Reown Verify API, so Reown returns validation=UNKNOWN —
 * after the production change in DefaultWcPairUseCase that maps UNKNOWN to FAILED_TO_VERIFY, the
 * app shows a Security Alert before establishing the session. Tests that drive qa-tools URIs go
 * through this helper to consistently accept the warning.
 */
fun BaseTestCase.confirmWcConnection() {
    step("Click on 'Connect' button") {
        onWalletConnectBottomSheet { connectButton.performClick() }
    }
    waitForIdle()

    val alertText = getResourceString(R.string.wc_alert_connect_anyway)
    val alertAppeared = runCatching {
        composeTestRule.waitUntil(timeoutMillis = WAIT_UNTIL_TIMEOUT_SHORT) {
            composeTestRule.onAllNodesWithText(alertText).fetchSemanticsNodes().isNotEmpty()
        }
    }.isSuccess

    if (alertAppeared) {
        step("Click on 'Connect anyway' button") {
            onWarningBottomSheet { connectAnywayButton.clickWithAssertion() }
        }
    }

    step("Assert 'Connect' button is not displayed") {
        waitForIdle()
        onWalletConnectBottomSheet { connectButton.assertIsNotDisplayed() }
    }
}

fun BaseTestCase.checkWalletConnectDetailsBottomSheet(dAppName: String) {
    waitForIdle()
    step("Assert connection details title is displayed") {
        onWalletConnectDetailsBottomSheet { title.assertIsDisplayed() }
    }
    step("Assert date is displayed") {
        onWalletConnectDetailsBottomSheet { date.assertIsDisplayed() }
    }
    step("Assert 'Close' button is displayed") {
        onWalletConnectDetailsBottomSheet { closeButton.assertIsDisplayed() }
    }
    step("Assert app icon is displayed") {
        onWalletConnectDetailsBottomSheet { appIcon.assertIsDisplayed() }
    }
    step("Assert app name is displayed") {
        onWalletConnectDetailsBottomSheet { appName.assertIsDisplayed() }
    }
    step("Assert app URL is displayed") {
        onWalletConnectDetailsBottomSheet { appUrl.assertIsDisplayed() }
    }
    step("Assert 'Connected networks' title is displayed") {
        onWalletConnectDetailsBottomSheet { connectedNetworksTitle.assertIsDisplayed() }
    }
    step("Assert connected network item is displayed") {
        onWalletConnectDetailsBottomSheet { connectedNetworkItem.assertIsDisplayed() }
    }
    step("Assert connected network icon is displayed") {
        onWalletConnectDetailsBottomSheet { connectedNetworkIcon.assertIsDisplayed() }
    }
    step("Assert connected dApp name: '$dAppName'") {
        onWalletConnectDetailsBottomSheet { appName.assertTextContains(dAppName) }
    }
    step("Assert connected network symbol is displayed") {
        onWalletConnectDetailsBottomSheet { connectedNetworkSymbol.assertIsDisplayed() }
    }
    step("Assert 'Disconnect button' is displayed") {
        onWalletConnectDetailsBottomSheet { disconnectButton.assertIsDisplayed() }
    }
}

fun BaseTestCase.createConnectionViaPasteFromClipboardButton() {
    step("Click on 'New connection' button") {
        onWalletConnectScreen { newConnectionButton.performClick() }
    }
    step("Click on 'Paste from clipboard' button") {
        onScanQrScreen { pasteFromClipboardButton.clickWithAssertion() }
    }
}