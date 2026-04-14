package com.tangem.scenarios

import android.view.KeyEvent
import androidx.test.core.app.ApplicationProvider
import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_VERY_LONG
import com.tangem.common.utils.AddressComparisonHelper
import com.tangem.common.utils.getClipboardText
import com.tangem.screens.onMainScreen
import com.tangem.screens.onTesterMenuScreen
import com.tangem.utils.StringsSigns.DASH_SIGN
import com.tangem.utils.logging.TangemLogger
import io.qameta.allure.kotlin.Allure.step
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertNotNull

private const val WALLET_MANAGERS_SETTLE_MS = 5_000L
private const val WALLET_MANAGERS_POLL_INTERVAL_MS = 500L

fun BaseTestCase.verifyAddresses(seedPhrase: String, apiAddressesJson: String) {
    var appAddressesJson: String? = null

    step("Open 'Main Screen' with existing hot wallet") {
        openMainScreenWithExistingHotWallet(seedPhrase)
    }
    step("Assert wallet balance = '$DASH_SIGN'") {
        composeTestRule.waitUntil(timeoutMillis = WAIT_UNTIL_TIMEOUT_VERY_LONG) {
            runCatching { onMainScreen { totalBalanceText.assertTextContains(DASH_SIGN) } }.isSuccess
        }
    }
    step("Assert 'Organize tokens' button is enabled") {
        composeTestRule.waitUntil(timeoutMillis = WAIT_UNTIL_TIMEOUT_VERY_LONG) {
            runCatching { onMainScreen { organizeTokensButton().assertIsEnabled() } }.isSuccess
        }
    }
    step("Wait for all wallet managers to initialize") {
        awaitWalletManagersStabilized()
    }
    step("Open tester menu") {
        openTesterMenu()
    }
    step("Click on 'Addresses info' button") {
        onTesterMenuScreen { addressesInfoButton.performClick() }
    }
    step("Click on 'JSON' tab") {
        onTesterMenuScreen { jsonTab.performClick() }
    }
    step("Click on 'Copy' button") {
        onTesterMenuScreen { copyButton.performClick() }
    }
    step("Get addresses JSON from clipboard") {
        appAddressesJson = getClipboardText(ApplicationProvider.getApplicationContext())
        assertNotNull("Clipboard is empty after copying addresses", appAddressesJson)
    }
    step("Compare app addresses with API reference") {
        AddressComparisonHelper.compareAddresses(
            appJson = requireNotNull(appAddressesJson),
            apiJson = apiAddressesJson,
        )
    }
}

private const val TESTER_MENU_MAX_ATTEMPTS = 3

/**
 * Presses 'Volume Down' twice to open tester menu.
 * Retries up to [TESTER_MENU_MAX_ATTEMPTS] times if the menu doesn't appear.
 */
private fun BaseTestCase.openTesterMenu() {
    repeat(TESTER_MENU_MAX_ATTEMPTS) { attempt ->
        waitForIdle()
        device.uiDevice.pressKeyCode(KeyEvent.KEYCODE_VOLUME_DOWN)
        device.uiDevice.pressKeyCode(KeyEvent.KEYCODE_VOLUME_DOWN)

        val opened = runCatching {
            onTesterMenuScreen { addressesInfoButton.assertIsDisplayed() }
        }.isSuccess

        if (opened) {
            TangemLogger.i("Tester menu opened on attempt ${attempt + 1}")
            return
        }
        TangemLogger.w("Tester menu not opened on attempt ${attempt + 1}, retrying...")
    }
    error("Failed to open tester menu after $TESTER_MENU_MAX_ATTEMPTS attempts")
}

/**
 * Polls [walletManagersStore] until the wallet manager count stops growing for [WALLET_MANAGERS_SETTLE_MS].

 *
 * Uses [getAllSync] with a polling interval instead of Flow, because the Flow only emits on changes —
 * if the count stabilizes, there would be no new emission to check the settle timeout against.
 */
private fun BaseTestCase.awaitWalletManagersStabilized() {
    val walletId = getSelectedWalletSyncUseCase().getOrNull()?.walletId
        ?: error("No selected wallet found")
    var lastSize = -1
    var stableStart = System.currentTimeMillis()

    runBlocking {
        withTimeout(WAIT_UNTIL_TIMEOUT_VERY_LONG) {
            while (true) {
                val currentSize = walletManagersStore.getAllSync(walletId).size
                val now = System.currentTimeMillis()

                if (currentSize != lastSize) {
                    TangemLogger.i("Wallet managers count: $currentSize (was $lastSize)")
                    lastSize = currentSize
                    stableStart = now
                } else if (now - stableStart >= WALLET_MANAGERS_SETTLE_MS) {
                    TangemLogger.i("Wallet managers stabilized at $currentSize entries")
                    return@withTimeout
                }

                delay(WALLET_MANAGERS_POLL_INTERVAL_MS)
            }
        }
    }
}