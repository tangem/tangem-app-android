package com.tangem.scenarios

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_LONG
import com.tangem.common.extensions.SwipeDirection
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.extensions.extractText
import com.tangem.common.extensions.swipeVertical
import com.tangem.core.ui.test.MainScreenTestTags
import com.tangem.screens.MainScreenPageObject
import com.tangem.screens.onAddAndManageBottomSheet
import com.tangem.screens.onMainScreen
import com.tangem.screens.onOrganizeTokensScreen
import com.tangem.screens.onWarningBottomSheet
import io.qameta.allure.kotlin.Allure.step
import org.junit.Assert.assertEquals
import java.math.BigDecimal

private const val ACCOUNT_EXPAND_ATTEMPTS = 3
private const val ACCOUNT_EXPAND_TIMEOUT_MILLIS = 5_000L
private const val BALANCE_RELOAD_TIMEOUT_MILLIS = 10_000L
private val CRYPTO_AMOUNT_REGEX = """\d[\d,]*(\.\d+)?""".toRegex()

fun BaseTestCase.openOrganizeTokensScreen() {
    step("Swipe to 'Add & Manage' button") {
        swipeVertical(SwipeDirection.UP)
        swipeVertical(SwipeDirection.UP)
    }
    step("Click on 'Add & Manage' button") {
        onMainScreen { addAndManageButton().clickWithAssertion() }
    }
    step("Click on 'Organize tokens' button in bottom sheet") {
        onAddAndManageBottomSheet { organizeTokensButton.clickWithAssertion() }
    }
}

fun BaseTestCase.getMainScreenTokensOrder(): List<String> {
    var tokens: List<String> = emptyList()
    step("Read displayed token titles from 'Main' screen") {
        awaitSuccess(timeoutMillis = WAIT_UNTIL_TIMEOUT_LONG) {
            onMainScreen { tokens = getDisplayedTokenTitles() }
            require(tokens.isNotEmpty()) { "No token titles found on the main screen" }
        }
    }
    return tokens
}

fun BaseTestCase.waitUntilMainScreenTokenBalanceLoaded(tokenTitle: String) {
    awaitSuccess(timeoutMillis = WAIT_UNTIL_TIMEOUT_LONG) {
        var balance = ""
        onMainScreen { balance = tokenFiatAmountText(tokenTitle).extractText() }
        require(balance.any(Char::isDigit)) { "Balance for '$tokenTitle' is not loaded yet: '$balance'" }
    }
}

fun BaseTestCase.assertOrganizeTokensMatch(expectedTokens: List<String>) {
    step("Open 'Organize tokens' bottom-sheet") {
        onMainScreen { clickDisplayedAddAndManageButton() }
        onAddAndManageBottomSheet { organizeTokensButton.clickWithAssertion() }
    }
    step("Assert 'Organize tokens' list matches the main screen order") {
        onOrganizeTokensScreen {
            // 'Organize tokens' appends the currency symbol to the name ("Bitcoin BTC"), the main screen doesn't.
            val organizeTokens = getDisplayedTokenTitles().map { it.substringBeforeLast(delimiter = ' ') }
            assertEquals(expectedTokens, organizeTokens)
        }
    }
    step("Return to 'Main' screen") {
        onOrganizeTokensScreen { cancelButton.clickWithAssertion() }
    }
}

/**
 * Pulls the 'Main' screen to refresh.
 *
 * Compose Test gesture on purpose: the shared UiAutomator-based `pullToRefresh()` does not reach the
 * refresh container, so `onRefresh` never fires and nothing is re-fetched.
 */
fun BaseTestCase.pullToRefreshMainScreen() {
    composeTestRule.onNode(hasTestTag(MainScreenTestTags.SCREEN_CONTAINER))
        .performTouchInput {
            swipeDown(startY = 0f, endY = visibleSize.height.toFloat() * 6f, durationMillis = 800)
        }
    waitForIdle()
}

// A new hot wallet gets this one-off sheet once its balance turns positive, so it shows up only when that load wins.
fun BaseTestCase.closeFinishWalletActivationSheetIfShown() {
    var isShown = false
    onWarningBottomSheet { isShown = isFinishWalletActivationShown() }
    if (!isShown) return
    step("Click on 'Later' button") {
        onWarningBottomSheet { finishWalletActivationLaterButton.performClick() }
    }
    step("Assert 'Finalize wallet setup' sheet is not displayed") {
        awaitSuccess { onWarningBottomSheet { finishWalletActivationTitle.assertDoesNotExist() } }
    }
}

fun BaseTestCase.revealTokenInAccountOnMain(tokenName: String, accountName: String) {
    revealAccountContentOnMain(accountName, "token '$tokenName'") { isTokenOnScreenPage(tokenName) }
}

fun BaseTestCase.revealAccountTokensOnMain(accountName: String) {
    revealAccountContentOnMain(accountName, "its tokens") { hasTokenRowsOnScreenPage() }
}

// The expanded state of an account is persisted, so a click can collapse it instead; retry until the content shows.
private fun BaseTestCase.revealAccountContentOnMain(
    accountName: String,
    content: String,
    isRevealed: MainScreenPageObject.() -> Boolean,
) {
    fun isContentShown(): Boolean {
        var isShown = false
        onMainScreen { isShown = isRevealed() }
        return isShown
    }
    step("Collapse header") {
        onMainScreen { collapseOnScreenPageHeader() }
    }
    step("Expand account '$accountName' and reveal $content") {
        repeat(ACCOUNT_EXPAND_ATTEMPTS) {
            if (isContentShown()) return@step
            runCatching { onMainScreen { clickOnScreenAccountSection(accountName) } }
            runCatching { composeTestRule.waitUntil(ACCOUNT_EXPAND_TIMEOUT_MILLIS) { isContentShown() } }
        }
        if (!isContentShown()) throw AssertionError("Account '$accountName' does not show $content")
    }
}

fun BaseTestCase.readDisplayedTokenBalance(tokenName: String): BigDecimal {
    var balance: BigDecimal? = null
    step("Read '$tokenName' balance on 'Main' screen") {
        awaitAssert(timeoutMillis = WAIT_UNTIL_TIMEOUT_LONG) {
            balance = readDisplayedTokenBalanceOrNull(tokenName)
                ?: throw AssertionError("Balance of '$tokenName' is not loaded on 'Main' screen")
        }
    }
    return checkNotNull(balance)
}

fun BaseTestCase.pullToRefreshDisplayedWallet() {
    onMainScreen { pullToRefreshOnScreenPage() }
    waitForIdle()
}

fun BaseTestCase.refreshMainUntilTokenBalance(
    tokenName: String,
    expectation: String,
    condition: (BigDecimal) -> Boolean,
) {
    step("Refresh 'Main' until '$tokenName' balance $expectation") {
        // One pull can land before the switched balance mock reaches the app, so pull until the value changes.
        composeTestRule.waitUntil(timeoutMillis = WAIT_UNTIL_TIMEOUT_LONG) {
            closeFinishWalletActivationSheetIfShown()
            runCatching { pullToRefreshDisplayedWallet() }
            runCatching {
                composeTestRule.waitUntil(BALANCE_RELOAD_TIMEOUT_MILLIS) {
                    readDisplayedTokenBalanceOrNull(tokenName)?.let(condition) == true
                }
            }.isSuccess
        }
    }
}

private fun BaseTestCase.readDisplayedTokenBalanceOrNull(tokenName: String): BigDecimal? {
    var amountText: String? = null
    onMainScreen { amountText = onScreenTokenCryptoAmount(tokenName) }
    return amountText
        ?.let { CRYPTO_AMOUNT_REGEX.find(it)?.value }
        ?.replace(",", "")
        ?.toBigDecimalOrNull()
}