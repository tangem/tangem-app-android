package com.tangem.scenarios

import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.HOLD_DURATION_MS
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_LONG
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_VERY_LONG
import com.tangem.common.extensions.assertVisibility
import com.tangem.common.extensions.clickAndWaitFor
import com.tangem.common.extensions.clickWhenEnabled
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.extensions.extractText
import com.tangem.common.extensions.isDisplayedSafely
import com.tangem.core.ui.R as CoreUiR
import com.tangem.core.ui.test.BaseButtonTestTags
import com.tangem.core.ui.test.HotWalletAccessCodeTestTags
import com.tangem.screens.*
import com.tangem.tap.domain.sdk.mocks.MockContent
import io.github.kakaocup.kakao.common.utilities.getResourceString
import io.qameta.allure.kotlin.Allure.step
import com.tangem.common.ui.R as CommonUiR

private val firstStoryIndex = 0
private val firstStoryTitle = getResourceString(CommonUiR.string.swap_story_first_title_v2)
private val firstStorySubtitle = getResourceString(CommonUiR.string.swap_story_first_subtitle_v2)
private val secondStoryIndex = 1
private val secondStoryTitle = getResourceString(CommonUiR.string.swap_story_second_title_v2)
private val secondStorySubtitle = getResourceString(CommonUiR.string.swap_story_second_subtitle_v2)
private val thirdStoryIndex = 2
private val thirdStoryTitle = getResourceString(CommonUiR.string.swap_story_third_title_v2)
private val thirdStorySubtitle = getResourceString(CommonUiR.string.swap_story_third_subtitle_v2)
private val forthStoryIndex = 3
private val forthStoryTitle = getResourceString(CommonUiR.string.swap_story_forth_title_v2)
private val forthStorySubtitle = getResourceString(CommonUiR.string.swap_story_forth_subtitle_v2)

fun BaseTestCase.openSwapScreen(
    from: SwapEntryPoint,
    storiesExist: Boolean = true,
) {
    when (from) {
        SwapEntryPoint.MainScreen -> step("Click on 'Swap' button on 'Main' screen") {
            onMainScreen { swapButton.performClick() }
        }

        SwapEntryPoint.TokenDetails -> step("Click on 'Swap' button on 'Token details' screen") {
            onTokenDetailsScreen { swapButton.clickWhenEnabled() }
            }

        SwapEntryPoint.MarketsTokenDetails -> step("Click on 'Swap' button on 'Markets' token details screen") {
            onMarketsTokenDetailsScreen { swapPortfolioQuickActionButton.performClick() }
        }

        SwapEntryPoint.TokenActionsBottomSheet -> step("Click on 'Swap' button on token actions bottom sheet") {
            onTokenActionsBottomSheet { swapButton.performClick() }
        }
    }

    if (storiesExist) {
        // Whether stories show at all is decided by a request that can still be in flight here.
        step("Close 'Stories' screen") {
            val storiesShown = runCatching {
                awaitSuccess { onSwapStoriesScreen { closeButton.assertIsDisplayed() } }
            }.isSuccess

            if (storiesShown) {
                onSwapStoriesScreen { closeButton.performClick() }
            }
        }
    } else {
        step("Assert 'Stories' screen is not displayed") {
            onSwapStoriesScreen { container.assertDoesNotExist() }
        }
    }

    step("Assert 'Swap' screen title is displayed") {
        onSwapTokenScreen { title.assertIsDisplayed() }
    }
}

fun BaseTestCase.checkStoriesContent(
    storyIndex: Int,
    storyTitle: String,
    storySubtitle: String,
    ) {
    step("Assert 'Close' button is displayed") {
        onSwapStoriesScreen { closeButton.assertIsDisplayed() }
    }
    step("Assert progress bar item №${storyIndex + 1} is displayed") {
        onSwapStoriesScreen { progressBarItem(storyIndex).assertIsDisplayed() }
    }
    step("Assert story title is $storyTitle") {
        onSwapStoriesScreen { title.assertTextContains(storyTitle) }
    }
    step("Assert story subtitle is $storySubtitle") {
        onSwapStoriesScreen { subtitle.assertTextContains(storySubtitle) }
    }
}

fun BaseTestCase.checkStoriesChanges() {
    step("Check title and subtitle for story №${firstStoryIndex + 1}") {
        checkStoriesContent(
            storyIndex = firstStoryIndex,
            storyTitle = firstStoryTitle,
            storySubtitle = firstStorySubtitle
        )
    }
    step("Click on right side") {
        onSwapStoriesScreen { container.performTouchInput { click(centerRight) } }
    }
    step("Check title and subtitle for story №${secondStoryIndex + 1}") {
        checkStoriesContent(
            storyIndex = secondStoryIndex,
            storyTitle = secondStoryTitle,
            storySubtitle = secondStorySubtitle
        )
    }
    step("Click on right side") {
        onSwapStoriesScreen { container.performTouchInput { click(centerRight) } }
    }
    step("Check title and subtitle for story №${thirdStoryIndex + 1}") {
        checkStoriesContent(
            storyIndex = thirdStoryIndex,
            storyTitle = thirdStoryTitle,
            storySubtitle = thirdStorySubtitle
        )
    }
    step("Click on right side") {
        onSwapStoriesScreen { container.performTouchInput { click(centerRight) } }
    }
    step("Check title and subtitle for story №${forthStoryIndex + 1}") {
        checkStoriesContent(
            storyIndex = forthStoryIndex,
            storyTitle = forthStoryTitle,
            storySubtitle = forthStorySubtitle
        )
    }
    step("Click on left side") {
        onSwapStoriesScreen { container.performTouchInput { click(centerLeft) } }
    }
    step("Check title and subtitle for story №${thirdStoryIndex + 1}") {
        checkStoriesContent(
            storyIndex = thirdStoryIndex,
            storyTitle = thirdStoryTitle,
            storySubtitle = thirdStorySubtitle
        )
    }
    step("Click on left side") {
        onSwapStoriesScreen { container.performTouchInput { click(centerLeft) } }
    }
    step("Check title and subtitle for story №${secondStoryIndex + 1}") {
        checkStoriesContent(
            storyIndex = secondStoryIndex,
            storyTitle = secondStoryTitle,
            storySubtitle = secondStorySubtitle
        )
    }
    step("Click on left side") {
        onSwapStoriesScreen { container.performTouchInput { click(centerLeft) } }
    }
    step("Check title and subtitle for story №${firstStoryIndex + 1}") {
        checkStoriesContent(
            storyIndex = firstStoryIndex,
            storyTitle = firstStoryTitle,
            storySubtitle = firstStorySubtitle
        )
    }
}

fun BaseTestCase.skipSwapStories() {
    step("Skip 'Swap stories' screen if displayed") {
        onSwapStoriesScreen {
            if (closeButton.isDisplayedSafely()) {
                closeButton.performClick()
            }
        }
    }
}

fun BaseTestCase.selectFeeType(feeType: FeeType, selectedFeeAmount: String) {
    step("Click on 'Select fee' icon") {
        onSwapTokenScreen { selectFeeIcon.performClick() }
    }

    when (feeType) {
        FeeType.Market -> {
            step("Click on 'Market' item") {
                onSwapSelectNetworkFeeBottomSheet { marketSelectorItem.clickWithAssertion() }
            }
            step("Assert fee amount is equal to 'Market' fee:'$selectedFeeAmount'") {
                onSwapTokenScreen { feeAmount.assertTextContains(selectedFeeAmount, substring = true) }
            }
        }
        FeeType.Fast -> {
            step("Click on 'Fast' item") {
                onSwapSelectNetworkFeeBottomSheet { fastSelectorItem.clickWithAssertion() }
            }
            step("Assert fee amount is equal to 'Fast' fee:'$selectedFeeAmount'") {
                onSwapTokenScreen { feeAmount.assertTextContains(selectedFeeAmount, substring = true) }
            }
        }
    }
}

fun BaseTestCase.selectFeeTypeAndReadFee(feeType: FeeType): String {
    step("Click on 'Select fee' icon") {
        onSwapTokenScreen { selectFeeIcon.performClick() }
    }
    step("Click on '$feeType' item") {
        onSwapSelectNetworkFeeBottomSheet {
            when (feeType) {
                FeeType.Market -> marketSelectorItem.clickWithAssertion()
                FeeType.Fast -> fastSelectorItem.clickWithAssertion()
            }
        }
    }
    var fee = ""
    step("Read displayed '$feeType' fee amount") {
        onSwapTokenScreen { fee = feeAmount.extractText() }
    }
    return fee
}

fun BaseTestCase.chackUnableToCoverFeeNotification(networkName: String, currencySymbol: String) {
    step("Assert 'Unable to cover '$networkName' fee notification title is displayed'") {
        onSwapTokenScreen { unableToCoverFeeNotificationTitle(networkName).assertIsDisplayed() }
    }
    step("Assert 'Unable to cover '$networkName' fee notification text is displayed'") {
        onSwapTokenScreen {
            unableToCoverFeeNotificationText(
                currencyName = networkName,
                currencySymbol = currencySymbol
            ).assertIsDisplayed()
        }
    }
    step("Assert 'Unable to cover '$networkName' fee notification icon is displayed'") {
        onSwapTokenScreen { unableToCoverFeeNotificationIcon(networkName).assertIsDisplayed() }
    }
}

fun BaseTestCase.checkSwapWarning(
    title: String,
    message: String,
    isDisplayed: Boolean = true,
    swapButtonIsDisabled: Boolean = isDisplayed,
) {
    val assertDisplay = if (isDisplayed) "displayed" else "not displayed"

    step("Assert warning title is $assertDisplay") {
        onSwapTokenScreen {
            warningTitle(title).assertVisibility(isDisplayed)
        }
    }
    step("Assert warning icon is $assertDisplay") {
        onSwapTokenScreen {
            warningIcon(message).assertVisibility(isDisplayed)
        }
    }
    step("Assert warning message is $assertDisplay") {
        onSwapTokenScreen {
            warningMessage(message).assertVisibility(isDisplayed)
        }
    }

    if (swapButtonIsDisabled)
        step("Assert 'Swap' button is disabled") {
            onSwapTokenScreen {
                swapButton.assertIsNotEnabled()
            }
        }
    else
        step("Assert 'Swap' button is enabled") {
            onSwapTokenScreen {
                swapButton.assertIsEnabled()
            }
        }
}

/** Scans a card wallet and opens Swap for [tokenName] in [fromAccountName] without choosing the receive token yet. */
fun BaseTestCase.openSwapForTokenInAccount(
    tokenName: String,
    fromAccountName: String = "Account 1",
    mockContent: MockContent? = null,
) {
    step("Open 'Main' screen") {
        openMainScreen(mockContent = mockContent)
    }
    step("Synchronize addresses") {
        synchronizeAddresses(assertBalance = false)
    }
    step("Wait for addresses to be generated") {
        waitForAddressesGenerated()
    }
    navigateToSwapForToken(tokenName, fromAccountName)
}

/** Opens Swap for [tokenName] in [fromAccountName] and picks it again in [toAccountName] to enter Transfer mode; needs a two-accounts-same-token mock. */
fun BaseTestCase.openSwapInTransferMode(
    tokenName: String,
    fromAccountName: String = "Account 1",
    toAccountName: String = "Account 2",
    mockContent: MockContent? = null,
) {
    openSwapForTokenInAccount(tokenName, fromAccountName, mockContent)
    step("Choose identical receive token '$tokenName' from '$toAccountName'") {
        chooseIdenticalReceiveToken(tokenName = tokenName, receiveAccountName = toAccountName)
    }
}

/** Like [openSwapInTransferMode] but imports a hot wallet first — required for broadcasting flows (the mock card can't sign). */
fun BaseTestCase.openSwapInTransferModeWithHotWallet(
    tokenName: String,
    seedPhrase: String,
    fromAccountName: String = "Account 1",
    toAccountName: String = "Account 2",
) {
    step("Open 'Main' screen with existing hot wallet") {
        openMainScreenWithExistingHotWallet(seedPhrase)
    }
    step("Generate missing addresses") {
        generateMissingHotWalletAddresses()
    }
    step("Wait for addresses to be generated") {
        waitForAddressesGenerated()
    }
    navigateToSwapForToken(tokenName, fromAccountName)
    step("Choose identical receive token '$tokenName' from '$toAccountName'") {
        chooseIdenticalReceiveToken(tokenName = tokenName, receiveAccountName = toAccountName)
    }
}

private fun BaseTestCase.navigateToSwapForToken(tokenName: String, fromAccountName: String) {
    step("Collapse header") {
        onMainScreen { collapseHeader() }
    }
    step("Scroll '$fromAccountName' into view (semantics, not touch — avoids the Markets sheet)") {
        onMainScreen { scrollToAccount(fromAccountName) }
    }
    step("Expand account '$fromAccountName' and reveal token '$tokenName'") {
        onMainScreen {
            findAccountSectionByName(fromAccountName).clickAndWaitFor(
                rule = composeTestRule,
                expectedCondition = {
                    onMainScreen { findTokenInAnyAccountByName(tokenName).assertIsDisplayed() }
                },
            )
        }
    }
    step("Click on token with name: '$tokenName'") {
        onMainScreen { findTokenInAnyAccountByName(tokenName).clickWithAssertion() }
    }
    step("Open 'Swap' screen") {
        openSwapScreen(from = SwapEntryPoint.TokenDetails, storiesExist = false)
    }
}

/**
 * Opens Swap from [tokenName]'s zero-balance token-details screen. At zero balance the Buy / Swap /
 * Receive block replaces the action buttons, and its 'Swap' row routes through TO-position swap, so the
 * entry token lands in the receive block. [accountName] groups the token and is collapsed by default.
 */
fun BaseTestCase.openSwapFromZeroBalanceToken(tokenName: String, accountName: String) {
    step("Collapse balance header") {
        onMainScreen { collapseHeader() }
    }
    // Multi-account wallets group the token under a collapsed account section; a single-account
    // wallet has none, so only scroll/expand when that section is actually present.
    var hasAccountSection = false
    step("Check whether the '$accountName' account section is present") {
        onMainScreen { hasAccountSection = findAccountSectionByName(accountName).isDisplayedSafely() }
    }
    if (hasAccountSection) {
        step("Scroll '$accountName' into view") {
            onMainScreen { scrollToAccount(accountName) }
        }
        step("Expand '$accountName' and reveal token '$tokenName'") {
            onMainScreen {
                findAccountSectionByName(accountName).clickAndWaitFor(
                    rule = composeTestRule,
                    expectedCondition = {
                        onMainScreen { findTokenInAnyAccountByName(tokenName).assertIsDisplayed() }
                    },
                )
            }
        }
    }
    step("Click on token with name: '$tokenName'") {
        onMainScreen { findTokenInAnyAccountByName(tokenName).clickWithAssertion() }
    }
    step("Assert 'Token details' screen is displayed") {
        onTokenDetailsScreen { screenContainer.assertIsDisplayed() }
    }
    // assertHasClickAction gates on the loaded (Content) row — the Loading row has no onClick, so a click would throw.
    step("Assert 'Swap' button is active for the zero-balance token") {
        awaitSuccess {
            onTokenDetailsScreen {
                zeroBalanceSwapButton.assertHasClickAction()
                zeroBalanceSwapButton.assertIsEnabled()
            }
        }
    }
    step("Click on 'Swap' button") {
        onTokenDetailsScreen { zeroBalanceSwapButton.clickWhenEnabled() }
    }
    step("Close 'Stories' screen") {
        onSwapStoriesScreen { closeButton.clickWithAssertion() }
    }
    step("Assert 'Swap' screen title is displayed") {
        onSwapTokenScreen { title.assertIsDisplayed() }
    }
}

// Hot wallets derive locally, so the second account's missing addresses are generated without a card scan when prompted.
fun BaseTestCase.generateMissingHotWalletAddresses() {
    var notificationShown = false
    onMainScreen { notificationShown = synchronizeAddressesButton.isDisplayedSafely() }
    if (notificationShown) {
        onMainScreen { synchronizeAddressesButton.performClick() }
    }
}

// The receive selector shows "No address" until the second account's derivation lands; the prompt disappears when it does.
fun BaseTestCase.waitForAddressesGenerated() {
    composeTestRule.waitUntil(timeoutMillis = WAIT_UNTIL_TIMEOUT_LONG) {
        var generated = false
        onMainScreen { generated = !synchronizeAddressesButton.isDisplayedSafely() }
        generated
    }
}

/** Picks the identical [tokenName] in [receiveAccountName]; the receive list collapses the other account, so its header is expanded first. */
fun BaseTestCase.chooseIdenticalReceiveToken(tokenName: String, receiveAccountName: String) {
    step("Click on 'Choose token' button") {
        onSwapTokenScreen { chooseTokenButton.performClick() }
    }
    step("Expand account '$receiveAccountName' in receive selector") {
        onSwapSelectTokenScreen { tokenWithName(receiveAccountName).performClick() }
    }
    step("Click on token with name '$tokenName'") {
        onSwapSelectTokenScreen { tokenWithName(tokenName).performClick() }
    }
}

fun BaseTestCase.chooseReceiveToken(tokenName: String) {
    step("Click on 'Choose token' button") {
        onSwapTokenScreen { chooseTokenButton.performClick() }
    }
    step("Click on token with name '$tokenName'") {
        onSwapSelectTokenScreen { tokenWithName(tokenName).performClick() }
    }
}

/** Reopens the receive selector via the receive-card icon and picks [tokenName] directly — the reopened selector keeps the account expanded. */
fun BaseTestCase.changeReceiveToken(tokenName: String) {
    step("Open receive token selector") {
        onSwapTokenScreen { receiveSelectTokenIcon.performClick() }
    }
    step("Click on token with name '$tokenName'") {
        onSwapSelectTokenScreen { tokenWithName(tokenName).performClick() }
    }
}

/**
 * From a clean start: open the main screen (cold by default, or an existing hot wallet when
 * [seedPhrase] is given), open Swap for [fromTokenName], choose [receiveTokenName] to receive and
 * enter [amount]. Scenario states stay in the test body.
 */
fun BaseTestCase.openSwapAmountScreen(
    fromTokenName: String,
    receiveTokenName: String,
    amount: String,
    seedPhrase: String? = null,
    storiesExist: Boolean = true,
) {
    if (seedPhrase == null) {
        step("Open 'Main' screen") {
            openMainScreen()
        }
        step("Synchronize addresses") {
            synchronizeAddresses()
        }
    } else {
        step("Open 'Main' screen with existing hot wallet") {
            openMainScreenWithExistingHotWallet(seedPhrase)
        }
    }
    step("Click on token with name: '$fromTokenName'") {
        onMainScreen { tokenWithTitleAndAddress(fromTokenName).clickWithAssertion() }
    }
    step("Open 'Swap' screen") {
        openSwapScreen(from = SwapEntryPoint.TokenDetails, storiesExist = storiesExist)
    }
    step("Choose receive token '$receiveTokenName'") {
        chooseReceiveToken(receiveTokenName)
    }
    step("Input swap amount '$amount'") {
        waitForIdle()
        onSwapTokenScreen {
            textInput.clickWithAssertion()
            textInput.performTextReplacement(amount)
        }
    }
    step("Wait for the receive amount to load") {
        awaitSuccess(timeoutMillis = WAIT_UNTIL_TIMEOUT_LONG) {
            onSwapTokenScreen { receiveAmount.assertIsDisplayed() }
        }
    }
}

/**
 * Opens the swap 'Network fee' bottom sheet, retrying the click until the fee selector shows.
 * Single action without its own step — wrap the call in a `step(...)`.
 */
fun BaseTestCase.openSwapNetworkFeeSelector() {
    awaitSuccess(timeoutMillis = WAIT_UNTIL_TIMEOUT_VERY_LONG) {
        runCatching { onSwapTokenScreen { networkFeeBlock.performClick() } }
        onSendFeeSelectorBottomSheet { networkFeeTitle.assertIsDisplayed() }
    }
}

/**
 * Opens the fee selector and switches the fee-paying token from [currentFeeToken] to [newFeeToken],
 * then applies. Works both ways — coin -> stablecoin and back.
 */
fun BaseTestCase.switchFeeTokenAndApply(currentFeeToken: String, newFeeToken: String) {
    step("Open the 'Network fee' bottom sheet") {
        openSwapNetworkFeeSelector()
    }
    step("Click on '$currentFeeToken' fee token to open 'Choose token'") {
        onSendFeeSelectorBottomSheet { feeTokenItem(currentFeeToken).performClick() }
    }
    step("Select '$newFeeToken' as the fee-paying token") {
        awaitSuccess(timeoutMillis = WAIT_UNTIL_TIMEOUT_LONG) {
            onSendFeeSelectorBottomSheet { feeTokenItem(newFeeToken).performClick() }
        }
    }
    step("Click on 'Apply' button") {
        waitForIdle()
        onSendFeeSelectorBottomSheet { applyButton.performClick() }
    }
}

private const val HOLD_ATTEMPTS = 3
private const val HOLD_CONFIRMATION_TIMEOUT = 10_000L

/** Holds the last BASE_BUTTON; enters [accessCode] if a hot wallet prompts for it. */
fun BaseTestCase.confirmSwapByHolding(accessCode: String? = null) {
    val buttonMatcher = hasTestTag(BaseButtonTestTags.BUTTON)
    val accessCodeInput = hasTestTag(HotWalletAccessCodeTestTags.ACCESS_CODE_INPUT)
    val swapInProgressText = hasText(getResourceString(CoreUiR.string.swap_in_progress))

    fun confirmationStarted(): Boolean =
        composeTestRule.onAllNodes(accessCodeInput).fetchSemanticsNodes().isNotEmpty() ||
            composeTestRule.onAllNodes(swapInProgressText, useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()

    // holdToConfirmGestures swallows the hold while the quote is being re-fetched, with no feedback.
    var attemptsLeft = HOLD_ATTEMPTS
    while (attemptsLeft > 0 && !confirmationStarted()) {
        attemptsLeft--
        val buttons = composeTestRule.onAllNodes(buttonMatcher)
        // HoldToConfirm is always last — withdraw renders an extra BASE_BUTTON for notifications.
        val swapButton = buttons[buttons.fetchSemanticsNodes().lastIndex]
        swapButton.performTouchInput { longClick(durationMillis = HOLD_DURATION_MS) }
        waitForIdle()

        val isLastAttempt = attemptsLeft == 0
        if (isLastAttempt) {
            composeTestRule.waitUntil(timeoutMillis = WAIT_UNTIL_TIMEOUT_LONG) { confirmationStarted() }
        } else {
            runCatching {
                composeTestRule.waitUntil(timeoutMillis = HOLD_CONFIRMATION_TIMEOUT) { confirmationStarted() }
            }
        }
    }

    val needsAccessCode =
        composeTestRule.onAllNodes(accessCodeInput).fetchSemanticsNodes().isNotEmpty()
    if (needsAccessCode && accessCode != null) {
        composeTestRule.onNode(accessCodeInput).performTextInput(accessCode)
        waitForIdle()
    }
}

// Caller asserts the outcome — transfer mode has no in-progress marker to wait on.
fun BaseTestCase.holdToConfirmTransfer() {
    composeTestRule.onNode(
        hasTestTag(BaseButtonTestTags.BUTTON) and
            hasText(getResourceString(CoreUiR.string.swapping_transfer_action)),
    ).performTouchInput { longClick(durationMillis = HOLD_DURATION_MS) }
    waitForIdle()
}

sealed class SwapEntryPoint {
    object MainScreen : SwapEntryPoint()
    object TokenDetails : SwapEntryPoint()
    object MarketsTokenDetails : SwapEntryPoint()
    object TokenActionsBottomSheet : SwapEntryPoint()
}

enum class FeeType {
    Market,
    Fast
}

fun BaseTestCase.inputAmount(amount: String) {
    // No waitForIdle(): the transfer screen recalculates the fee continuously and never reaches idle.
    awaitSuccess(timeoutMillis = WAIT_UNTIL_TIMEOUT_LONG) {
        onSwapTokenScreen { textInput.assertIsDisplayed() }
    }
    onSwapTokenScreen {
        textInput.clickWithAssertion()
        textInput.performTextReplacement(amount)
    }
}

fun BaseTestCase.assertTransferReady() {
    awaitSuccess(timeoutMillis = WAIT_UNTIL_TIMEOUT_LONG) {
        onSwapTokenScreen { transferButton.assertIsDisplayed() }
    }
    onSwapTokenScreen { providersBlock.assertIsNotDisplayed() }
}

fun BaseTestCase.waitForFeeDisplayed() {
    awaitSuccess(timeoutMillis = WAIT_UNTIL_TIMEOUT_LONG) {
        onSwapTokenScreen { feeAmount.assertIsDisplayed() }
    }
}

fun BaseTestCase.swapFeeDiffersFrom(previousFee: String): Boolean {
    var current = ""
    onSwapTokenScreen { current = feeAmount.extractText() }
    return current.isNotEmpty() && current != previousFee
}

/** Opens the swap 'more' menu and selects the layout [mode] (Simple / Detailed). */
fun BaseTestCase.switchSwapMode(mode: String) {
    step("Open the swap mode menu") {
        onSwapTokenScreen { moreButton.clickWithAssertion() }
    }
    step("Select '$mode'") {
        onSwapTokenScreen { swapModeMenuItem(mode).clickWithAssertion() }
    }
}




private const val PROVIDER_SHEET_OPEN_ATTEMPTS = 3

/**
 * Opens the providers bottom sheet, reopening it until the ALL/CEX/DEX segments render.
 *
 * The segments exist only when the provider list already holds both a CEX and a DEX quote, and that set is
 * captured once, when the sheet config is built (`StateBuilder.showSelectProviderBottomSheet`). A DEX quote
 * arriving later never adds them to an open sheet, so a slow quote is waited out by reopening, not by a
 * longer assert — hence the mutating retry instead of a plain awaitSuccess.
 */
fun BaseTestCase.openProviderSheetWithTypeFilter(allFilter: String) {
    repeat(PROVIDER_SHEET_OPEN_ATTEMPTS - 1) {
        step("Click on 'Providers' block") {
            onSwapTokenScreen { providersBlock.performClick() }
        }
        val filtersRendered = runCatching {
            awaitSuccess { onChooseProviderBottomSheet { filterButton(allFilter).assertIsDisplayed() } }
        }.isSuccess
        if (filtersRendered) return

        step("Close the providers bottom sheet and wait for the next quote") {
            device.uiDevice.pressBack()
            awaitSuccess { onSwapTokenScreen { providersBlock.assertIsDisplayed() } }
        }
    }

    step("Click on 'Providers' block") {
        onSwapTokenScreen { providersBlock.performClick() }
    }
    step("Assert the '$allFilter' filter is displayed") {
        awaitSuccess { onChooseProviderBottomSheet { filterButton(allFilter).assertIsDisplayed() } }
    }
}