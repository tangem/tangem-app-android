package com.tangem.scenarios

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import com.tangem.common.BaseTestCase
import com.tangem.common.constants.E2eCatalog
import com.tangem.core.ui.test.ManageTokensScreenTestTags
import com.tangem.core.ui.test.SwitchTestTags
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_LONG
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_VERY_LONG
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.extensions.extractEditableText
import com.tangem.common.extensions.extractText
import com.tangem.common.extensions.isDisplayedNow
import com.tangem.common.extensions.parseNumericBalance
import com.tangem.common.utils.lastSharedChooserText
import com.tangem.common.utils.stubShareChooserIntent
import com.tangem.tests.e2e.utils.E2ePortfolioResetter
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.runBlocking
import com.tangem.screens.onAddAndManageBottomSheet
import com.tangem.screens.onAddToPortfolioScreen
import com.tangem.screens.onMainScreen
import com.tangem.screens.onManageTokensScreen
import com.tangem.screens.onMarketsScreen
import com.tangem.screens.onPortfolioSelectorSheet
import com.tangem.screens.onSendAddressScreen
import com.tangem.screens.onSendConfirmScreen
import com.tangem.screens.onSendScreen
import com.tangem.screens.onSendSelectNetworkFeeBottomSheet
import com.tangem.screens.onSendSuccessScreen
import com.tangem.screens.onTokenDetailsScreen
import com.tangem.screens.onTransferBottomSheet
import com.tangem.utils.StringsSigns.DASH_SIGN
import io.github.kakaocup.kakao.common.utilities.getResourceString
import io.qameta.allure.kotlin.Allure.step
import java.math.BigDecimal
import java.math.RoundingMode
import com.tangem.core.ui.R as CoreUiR

fun BaseTestCase.waitForMainScreenLoaded() {
    step("Wait until the main screen is loaded") {
        awaitSuccess { onMainScreen { screenContainer.assertIsDisplayed() } }
        // Wait for a real total balance, not merely "not a dash": while loading, the balance is a shimmer
        // with no digits, and `!hasText(DASH_SIGN)` passes on it, letting the flow open Markets mid-load.
        awaitSuccess(timeoutMillis = WAIT_UNTIL_TIMEOUT_VERY_LONG) {
            var balance = ""
            onMainScreen { balance = totalBalanceText.extractText() }
            require(balance.any(Char::isDigit)) { "Total balance is not loaded yet: '$balance'" }
        }
    }
}

fun BaseTestCase.resetE2eWalletTokens(walletId: String) {
    val resetter = E2ePortfolioResetter.from(ApplicationProvider.getApplicationContext())
    try {
        runBlocking { resetter.clearTokens(walletId) }
    } catch (e: Exception) {
        // A silently-skipped reset lets the add-network tests pass on a dirty wallet, so log and rethrow.
        TangemLogger.e("E2E portfolio reset failed", e)
        throw e
    }
}

@OptIn(ExperimentalTestApi::class)
fun BaseTestCase.addNetworkToMainViaMarkets(network: E2eCatalog.E2eNetwork) {
    step("Open 'Markets' screen") {
        onMainScreen { marketsSheetDragHandle.clickWithAssertion() }
        waitForIdle()
    }
    step("Search for '${network.searchQuery}' in Markets") {
        searchTokenInMarkets(network.searchQuery)
    }
    step("Click on '${network.tokenName}' token") {
        awaitSuccess { onMarketsScreen { tokenWithTitle(network.tokenName).clickWithAssertion() } }
        waitForIdle()
    }
    step("Add '${network.networkName}' to the portfolio") {
        // The 'Add to portfolio' button sits in a sticky bottom bar, not inside the scrollable token details,
        // so a scroll-to-node can't reach it — wait for it to appear (once the details and portfolio have
        // loaded) and click it directly. The portfolio is reset to empty, so the token is never already added.
        awaitSuccess(timeoutMillis = WAIT_UNTIL_TIMEOUT_LONG) { onMarketsScreen { addButton.assertIsDisplayed() } }
        onMarketsScreen { addButton.clickWithAssertion() }
        awaitSuccess { onAddToPortfolioScreen { confirmButton.assertIsDisplayed() } }
        selectAddToPortfolioNetwork(network)
        onAddToPortfolioScreen { confirmButton.clickWithAssertion() }
    }
    returnToMainScreen()
}

// Multi-network coins (e.g. ETH on Arbitrum/Base) default the 'Add to portfolio' sheet to another chain, so the
// catalog network must be chosen explicitly. A single-network coin shows no selector — hence the guard.
private fun BaseTestCase.selectAddToPortfolioNetwork(network: E2eCatalog.E2eNetwork) {
    // Probe instantly: an absent row (single-network coin) would otherwise cost the full ~10s flaky-safety retry.
    var hasSelector = false
    onAddToPortfolioScreen { hasSelector = networkRow.isDisplayedNow() }
    if (!hasSelector) return
    // The selector is shown, so the catalog network MUST be selectable; failing explicitly here beats silently
    // confirming the preselected chain, which for a multi-network coin is a false pass on the wrong network.
    onAddToPortfolioScreen {
        networkRow.performClick()
        networkOption(network.networkName).clickWithAssertion()
    }
}

fun BaseTestCase.addNetworkToMainViaManageTokens(network: E2eCatalog.E2eNetwork) {
    openManageTokensForE2e()
    enableInManageTokensAndReturn(network.searchQuery, network.tokenName, network.manageTokensNetworkLabel)
}

fun BaseTestCase.addNetworkToSecondAccountViaManageTokens(network: E2eCatalog.E2eNetwork) {
    openManageTokensForSecondAccount()
    enableInManageTokensAndReturn(network.searchQuery, network.tokenName, network.manageTokensNetworkLabel)
}

// Token variants (for the token Send E2E test): enable the ERC-20 token on the same network. The add-token
// flow is identical to the native coin — only the search query / row title / network label come from the token.
fun BaseTestCase.addTokenToMainViaManageTokens(token: E2eCatalog.E2eToken) {
    openManageTokensForE2e()
    enableInManageTokensAndReturn(token.searchQuery, token.tokenName, token.manageTokensNetworkLabel)
}

fun BaseTestCase.addTokenToSecondAccountViaManageTokens(token: E2eCatalog.E2eToken) {
    openManageTokensForSecondAccount()
    enableInManageTokensAndReturn(token.searchQuery, token.tokenName, token.manageTokensNetworkLabel)
}

private fun BaseTestCase.openManageTokensForSecondAccount() {
    val secondAccountName = secondAccountName()
    step("Open 'Manage Tokens' for the '$secondAccountName' account via its 'Add tokens' button") {
        onMainScreen { collapseOnScreenPageHeader() }
        repeat(times = ACCOUNT_REVEAL_ATTEMPTS) {
            var shown = false
            onMainScreen { shown = emptyAccountAddTokensButton.isDisplayedNow() }
            if (shown) return@repeat
            runCatching { onMainScreen { clickOnScreenAccountSection(secondAccountName) } }
            waitForIdle()
        }
        awaitSuccess { onMainScreen { emptyAccountAddTokensButton.assertIsDisplayed() } }
        onMainScreen { emptyAccountAddTokensButton.performClick() }
        awaitSuccess { onManageTokensScreen { searchField.assertIsDisplayed() } }
    }
}

private fun BaseTestCase.secondAccountName(): String {
    val mainName = getResourceString(CoreUiR.string.account_main_account_title)
    var name = ""
    awaitSuccess {
        onMainScreen { name = accountTitles().firstOrNull { it != mainName }.orEmpty() }
        require(name.isNotBlank()) { "No non-main account found on the main screen" }
    }
    return name
}

private const val ACCOUNT_REVEAL_ATTEMPTS = 5

/**
 * Expands [accountName] until its [tokenName] row is on screen (scoped to that account positionally). The
 * account's expanded state is persisted, so a tap can collapse it — retry. Account-scoped so a token present
 * in another account never satisfies the reveal while the target stays collapsed.
 */
private fun BaseTestCase.revealAccountTokenOnMain(accountName: String, tokenName: String) {
    onMainScreen { collapseOnScreenPageHeader() }
    repeat(times = ACCOUNT_REVEAL_ATTEMPTS) {
        var shown = false
        onMainScreen { shown = isTokenInAccountOnScreen(accountName, tokenName) }
        if (shown) return
        runCatching { onMainScreen { clickOnScreenAccountSection(accountName) } }
        waitForIdle()
    }
    awaitSuccess(timeoutMillis = WAIT_UNTIL_TIMEOUT_LONG) {
        var shown = false
        onMainScreen { shown = isTokenInAccountOnScreen(accountName, tokenName) }
        require(shown) { "'$tokenName' is not shown in account '$accountName'" }
    }
}

private fun BaseTestCase.enableInManageTokensAndReturn(searchQuery: String, tokenName: String, networkLabel: String) {
    // A symbol like "USDC" recurs across dozens of result rows (bridged variants, meme tokens that reuse the
    // ticker), and each row's text includes that symbol — so matching by text alone is ambiguous. The genuine
    // coin is the only row whose *title* (the row's first text) equals the token name; every other row has a
    // different title ("UpSideDownCat", "USD CoinVertible", …).
    val genuineToken = {
        composeTestRule.onNode(
            SemanticsMatcher("TOKEN_ITEM titled '$tokenName'") { node ->
                val config = node.config
                val isTokenItem = SemanticsProperties.TestTag in config &&
                    config[SemanticsProperties.TestTag] == ManageTokensScreenTestTags.TOKEN_ITEM
                val title = if (SemanticsProperties.Text in config) {
                    config[SemanticsProperties.Text].firstOrNull()?.text
                } else {
                    null
                }
                isTokenItem && title == tokenName
            },
        )
    }
    // Network switches render only under an expanded token, so their presence is a reliable "expanded" signal.
    val anyNetworkSwitchPresent = {
        composeTestRule.onAllNodes(hasTestTag(SwitchTestTags.SWITCH)).fetchSemanticsNodes().isNotEmpty()
    }
    // Scroll to index 0 first: the genuine coin is the first result and the LazyColumn renders behind the sticky
    // search bar, so a slightly-scrolled list leaves the top row clipped under the bar and the tap would miss.
    val expandGenuineToken = {
        composeTestRule.onNode(hasScrollToIndexAction()).performScrollToIndex(0)
        waitForIdle()
        genuineToken().performClick()
        waitForIdle()
    }

    step("Search for '$searchQuery' in Manage Tokens") {
        onManageTokensScreen {
            searchField.performClick()
            searchField.performTextInput(searchQuery)
        }
        // Dismiss the IME specifically — pressBack() would navigate out of Manage Tokens if the keyboard never opened.
        Espresso.closeSoftKeyboard()
        waitForIdle()
    }
    step("Expand '$tokenName' and enable the '$networkLabel' network") {
        // The token list keeps refreshing while the portfolio fetches balances for the token just added to the
        // previous account, and every refresh collapses the expanded row. So (re-)expand and locate the target
        // network switch in a single retry loop that survives those recompositions — performScrollTo throws when
        // the switch is absent (row collapsed or switch below the fold), which drives another expand+scroll pass.
        awaitSuccess {
            if (!anyNetworkSwitchPresent()) expandGenuineToken()
            onManageTokensScreen { networkSwitch(networkLabel).performScrollTo() }
            onManageTokensScreen { networkSwitch(networkLabel).assertIsDisplayed() }
        }
        // Toggle only when OFF — never disable an already-enabled network.
        val alreadyEnabled = runCatching {
            onManageTokensScreen { networkSwitch(networkLabel).assertIsOn() }
        }.isSuccess
        if (!alreadyEnabled) {
            onManageTokensScreen { networkSwitch(networkLabel).performClick() }
            awaitSuccess { onManageTokensScreen { saveButton.assertIsDisplayed() } }
            onManageTokensScreen { saveButton.performClick() }
            waitForIdle()
        }
    }
    returnToMainScreen()
}

private fun BaseTestCase.openManageTokensForE2e() {
    step("Open 'Manage Tokens'") {
        val accountName = getResourceString(CoreUiR.string.account_main_account_title)
        // Non-empty portfolio opens it via 'Add & Manage'; an empty account via its 'Add tokens' button.
        val viaAddAndManage = runCatching { onMainScreen { clickDisplayedAddAndManageButton() } }.isSuccess
        if (viaAddAndManage) {
            awaitSuccess { onAddAndManageBottomSheet { addTokensButton.assertIsDisplayed() } }
            onAddAndManageBottomSheet { addTokensButton.performClick() }
            var hasPicker = false
            onPortfolioSelectorSheet { hasPicker = portfolioOption(accountName).isDisplayedNow() }
            if (hasPicker) {
                onPortfolioSelectorSheet { portfolioOption(accountName).performClick() }
            }
        } else {
            awaitSuccess { onMainScreen { emptyAccountAddTokensButton.assertIsDisplayed() } }
            onMainScreen { emptyAccountAddTokensButton.performClick() }
        }
        awaitSuccess { onManageTokensScreen { searchField.assertIsDisplayed() } }
    }
}

private fun BaseTestCase.returnToMainScreen() {
    step("Return to main screen") {
        // The Markets shtorka is a persistent bottom sheet: back only navigates within it (details -> search ->
        // feed) and can re-open a trending token, so it is collapsed with a downward drag — the same gesture that
        // raises it. Manage Tokens is a normal screen and is dismissed with back. Stop once neither is on screen;
        // the Markets search-results screen carries neither the feed nor the details tag, so it is detected by its
        // token list.
        repeat(times = 6) {
            var marketsOpen = false
            onMarketsScreen {
                marketsOpen = tokenDetailsContent.isDisplayedNow() ||
                    feedContent.isDisplayedNow() ||
                    tokensList.isDisplayedNow()
            }
            if (marketsOpen) {
                val ui = device.uiDevice
                ui.swipe(
                    ui.displayWidth / 2,
                    (ui.displayHeight * 0.12f).toInt(),
                    ui.displayWidth / 2,
                    (ui.displayHeight * 0.95f).toInt(),
                    STEPS_FOR_SHEET_COLLAPSE_SWIPE,
                )
                waitForIdle()
            } else {
                var manageTokensOpen = false
                onManageTokensScreen { manageTokensOpen = searchField.isDisplayedNow() }
                if (!manageTokensOpen) return@step
                device.uiDevice.pressBack()
                waitForIdle()
            }
        }
    }
}

private const val STEPS_FOR_SHEET_COLLAPSE_SWIPE = 25

fun BaseTestCase.revealTokenOnMain(tokenTitle: String) {
    step("Expand accounts to reveal '$tokenTitle' if it is hidden") {
        repeat(times = ACCOUNT_REVEAL_ATTEMPTS) {
            var visible = false
            onMainScreen { visible = isTokenOnScreenPage(tokenTitle) }
            if (visible) return@step
            // Account expanded state is persisted, so expand a header only while the token is still hidden and
            // re-check after each one — a blind single pass could collapse an already-open account (same
            // self-correcting approach as revealAccountTokenOnMain).
            var count = 0
            onMainScreen { count = accountSectionCount() }
            for (index in 0 until count) {
                onMainScreen { if (!isTokenOnScreenPage(tokenTitle)) expandAccountSection(index) }
                waitForIdle()
            }
        }
    }
    step("Assert '$tokenTitle' is displayed on main") {
        awaitSuccess(timeoutMillis = WAIT_UNTIL_TIMEOUT) {
            onMainScreen { findTokenInAnyAccountByName(tokenTitle).assertIsDisplayed() }
        }
    }
}

fun BaseTestCase.assertTokenBalanceLoadedAndNonNegative(tokenTitle: String) {
    val unreachable = getResourceString(CoreUiR.string.common_unreachable)
    step("Assert '$tokenTitle' balance is loaded (not '$DASH_SIGN', not '$unreachable') and >= 0") {
        awaitSuccess(timeoutMillis = WAIT_UNTIL_TIMEOUT_LONG) {
            lateinit var fiat: String
            onMainScreen { fiat = tokenFiatAmountInAnyAccount(tokenTitle).extractText() }
            if (fiat == DASH_SIGN || fiat.contains(unreachable, ignoreCase = true)) {
                throw AssertionError("Balance for '$tokenTitle' is not loaded yet: '$fiat'")
            }
            if (fiat.parseNumericBalance() < BigDecimal.ZERO) {
                throw AssertionError("Balance for '$tokenTitle' is negative: '$fiat'")
            }
        }
    }
}

// Small native-coin reserve required on top of the send amount to cover the network fee. A single constant is
// enough while fees are tiny (≈0.006–0.008 POL/send); make it a per-network catalog field if a network needs more.
private val FEE_RESERVE = BigDecimal("0.01")

// Mirrors the iOS SendBetweenAccountsE2ETests flow: always a real on-chain broadcast.
fun BaseTestCase.sendFromRicherAccountToOther(network: E2eCatalog.E2eNetwork, amount: String) {
    performSendFromRicherAccount(
        tokenTitle = network.mainScreenToken,
        symbol = network.searchQuery,
        amount = amount,
        feeReserve = FEE_RESERVE,
        accountAddresses = network.accountAddresses,
        networkName = network.networkName,
    )
}

// Gas is paid in the native coin, so the token balance only needs to cover the amount (no native fee reserve).
fun BaseTestCase.sendTokenFromRicherAccountToOther(network: E2eCatalog.E2eNetwork, token: E2eCatalog.E2eToken) {
    performSendFromRicherAccount(
        tokenTitle = token.mainScreenToken,
        symbol = token.searchQuery,
        amount = token.sendAmount,
        feeReserve = BigDecimal.ZERO,
        accountAddresses = network.accountAddresses,
        networkName = network.networkName,
    )
}

fun BaseTestCase.sendWithCustomFeeFromRicherAccountToOther(network: E2eCatalog.E2eNetwork, percentAboveMarket: Int) {
    performSendFromRicherAccount(
        tokenTitle = network.mainScreenToken,
        symbol = network.searchQuery,
        amount = network.sendAmount,
        feeReserve = FEE_RESERVE,
        accountAddresses = network.accountAddresses,
        networkName = network.networkName,
        beforeBroadcast = { setCustomFeeAboveMarket(percentAboveMarket) },
    )
}

private fun BaseTestCase.performSendFromRicherAccount(
    tokenTitle: String,
    symbol: String,
    amount: String,
    feeReserve: BigDecimal,
    accountAddresses: List<String>,
    networkName: String,
    beforeBroadcast: BaseTestCase.() -> Unit = {},
) {
    val token = tokenTitle
    var accountNames = emptyList<String>()
    step("Read the account list") {
        awaitSuccess(timeoutMillis = WAIT_UNTIL_TIMEOUT_LONG) {
            onMainScreen { accountNames = accountTitles() }
            require(accountNames.size >= 2) { "Expected at least two accounts, found: $accountNames" }
        }
    }
    // Compare per-account *crypto* balances of the token, not the header fiat totals: fiat is rounded to cents,
    // so "<$0.01" and "$0.01" tie and could pick an account that can't cover the amount + fee. Reading the crypto
    // amount needs the token row revealed, so reveal each account's row first.
    val balances = accountNames.mapIndexed { index, name ->
        var balance = BigDecimal.ZERO
        step("Read the '$token' balance of account '$name'") {
            revealAccountTokenOnMain(name, token)
            awaitSuccess(timeoutMillis = WAIT_UNTIL_TIMEOUT_LONG) {
                var raw: String? = null
                onMainScreen { raw = tokenCryptoAmountInAccount(name, token) }
                require(raw != null && raw!!.any(Char::isDigit)) { "'$token' balance not loaded for '$name': '$raw'" }
                balance = raw!!.parseNumericBalance()
            }
        }
        balance
    }
    val senderIndex = if (balances[0] >= balances[1]) 0 else 1
    val senderName = accountNames[senderIndex]
    // Fail here with a clear top-up message when the wallet runs dry, rather than timing out later on a disabled
    // Next button (the app blocks it on insufficient funds) — on a scheduled run that is the expected dry-wallet
    // failure, and a bare "Next not enabled" would read like an app bug.
    val required = amount.toBigDecimal() + feeReserve
    require(balances[senderIndex] >= required) {
        "E2E wallet is out of funds on $networkName: richer account '$senderName' holds " +
            "${balances[senderIndex]} $symbol, needs >= $required (amount $amount + fee reserve). " +
            "Top up ${accountAddresses.joinToString()}"
    }
    // The on-screen account order is reorderable (ApplyAccountListSortingUseCase), so map the recipient by account
    // identity, not display position: the catalog addresses are ordered by account index (0 = main, 1 = second).
    // This keeps the recipient the *other* account and rules out a self-transfer that would pass trivially.
    val mainName = getResourceString(CoreUiR.string.account_main_account_title)
    val recipientAddress =
        if (senderName == mainName) accountAddresses[1] else accountAddresses[0]
    TangemLogger.i(
        "E2E-SEND accounts=$accountNames balances=$balances senderIndex=$senderIndex " +
            "sender=$senderName recipient=$recipientAddress",
    )

    step("Open Send from '$token' in the richer account '$senderName' (balances=$balances)") {
        revealAccountTokenOnMain(senderName, token)
        onMainScreen { clickTokenInAccount(senderName, token) }
        onTokenDetailsScreen { transferButton.clickWithAssertion() }
        onTransferBottomSheet { sendButton.clickWithAssertion() }
    }
    step("Enter amount '$amount'") {
        awaitSuccess { onSendScreen { amountInputTextField.assertIsDisplayed() } }
        onSendScreen { amountInputTextField.performTextInput(amount) }
        awaitSuccess(timeoutMillis = WAIT_UNTIL_TIMEOUT_LONG) { onSendScreen { nextButton.assertIsEnabled() } }
        onSendScreen { nextButton.performClick() }
    }
    step("Enter recipient address '$recipientAddress'") {
        awaitSuccess { onSendAddressScreen { container.assertIsDisplayed() } }
        awaitSuccess(timeoutMillis = WAIT_UNTIL_TIMEOUT_LONG) {
            onSendAddressScreen { addressesShimmer.assertIsNotDisplayed() }
        }
        onSendAddressScreen { addressTextField.performTextReplacement(recipientAddress) }
    }
    openSendConfirmScreenViaNextButton()
    beforeBroadcast()
    // The confirm 'Send' button is hold-to-confirm (a tap does nothing) — broadcast via the shared long-click
    // helper, which holds and asserts the success screen.
    openSendSuccessScreenViaLongClickOnSendButton()
    step("Assert the sent-transaction explorer link is available") {
        onSendSuccessScreen { exploreButton.assertIsDisplayed() }
    }
    step("Capture the sent-transaction explorer link via the 'Share' button") {
        stubShareChooserIntent()
        onSendSuccessScreen { shareButton.clickWithAssertion() }
        val txUrl = requireNotNull(lastSharedChooserText()) {
            "The 'Share' button did not share a transaction explorer link"
        }
        require(txUrl.startsWith("http")) { "Shared transaction text is not a URL: '$txUrl'" }
        TangemLogger.i("E2E-SEND txUrl=$txUrl")
        // Report the link as a step name, not an Allure link/attachment: a step is part of the execution tree
        // the CI converter keeps, so the URL stays visible in TestOps (mirrors the iOS E2E report).
        step("Sent transaction: $txUrl")
    }
}

// Opens the fee selector on the confirm screen, switches to Custom and raises the EVM max fee per gas to
// [percent]% above the prefilled market value. maxFeePerGas is the fee cap — the on-chain charge still follows
// EIP-1559; the increase is asserted on the field value (the confirm-screen fiat fee rounds to "<$0.01" on cheap
// networks and would not show the change). Market/custom values are reported as step names for the Allure report.
private fun BaseTestCase.setCustomFeeAboveMarket(percent: Int) {
    val maxFeeTitle = getResourceString(CoreUiR.string.send_custom_evm_max_fee)
    step("Open the fee selector and select Custom") {
        // The fee keeps computing after the confirm screen appears; the selector is a no-op until it is ready.
        // The ⇕ select-fee icon renders only once the fee is loaded and selectable, so wait for it before tapping.
        awaitSuccess { onSendConfirmScreen { selectFeeIcon.assertIsDisplayed() } }
        onSendConfirmScreen { feeSelectorIcon.performClick() }
        awaitSuccess { onSendSelectNetworkFeeBottomSheet { chooseSpeedTitle.assertIsDisplayed() } }
        awaitSuccess { onSendSelectNetworkFeeBottomSheet { customSelectorItem.assertIsDisplayed() } }
        onSendSelectNetworkFeeBottomSheet { customSelectorItem.performClick() }
    }
    step("Set the custom max fee $percent% above market") {
        awaitSuccess { onSendSelectNetworkFeeBottomSheet { inputTextFieldValue(maxFeeTitle).assertIsDisplayed() } }
        var marketRaw = ""
        onSendSelectNetworkFeeBottomSheet { marketRaw = inputTextFieldValue(maxFeeTitle).extractEditableText() }
        val market = numericPart(marketRaw)
        require(market > BigDecimal.ZERO) { "Prefilled market max fee is not a positive number: '$marketRaw'" }
        // maxFeePerGas is in gwei and can be a fraction of a gwei on cheap networks; keep the field's full
        // precision and round UP so the bump is always a representable value strictly above market (rounding to
        // 2 dp could erase a small bump — 0.01→0.01 — or floor a low fee to 0, failing the assertion below).
        val bumped = market.multiply(BigDecimal(100 + percent)).divide(BigDecimal(100))
            .setScale(9, RoundingMode.UP).stripTrailingZeros().toPlainString()
        onSendSelectNetworkFeeBottomSheet {
            inputTextFieldValue(maxFeeTitle).performClick()
            inputTextFieldValue(maxFeeTitle).performTextReplacement(bumped)
        }
        var appliedRaw = ""
        onSendSelectNetworkFeeBottomSheet { appliedRaw = inputTextFieldValue(maxFeeTitle).extractEditableText() }
        require(numericPart(appliedRaw) > market) {
            "Custom max fee did not increase above market: market='$marketRaw' applied='$appliedRaw'"
        }
        step("Market max fee: $marketRaw; custom (+$percent%): $bumped")
    }
    step("Apply the custom fee") {
        onSendSelectNetworkFeeBottomSheet { doneButton.performClick() }
    }
}

private fun numericPart(raw: String): BigDecimal =
    Regex("[0-9]+([.,][0-9]+)?").find(raw)?.value?.replace(",", ".")?.toBigDecimal() ?: BigDecimal.ZERO