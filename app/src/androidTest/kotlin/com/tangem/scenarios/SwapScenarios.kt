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
        step("Close 'Stories' screen") {
            onSwapStoriesScreen { closeButton.clickWithAssertion() }
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

/** Opens Swap for [tokenName] in [fromAccountName] and picks it again in [toAccountName] to enter Transfer mode; needs a two-accounts-same-token mock. */
fun BaseTestCase.openSwapInTransferMode(
    tokenName: String,
    fromAccountName: String = "Account 1",
    toAccountName: String = "Account 2",
    mockContent: MockContent? = null,
) {
    step("Open 'Main' screen") {
        openMainScreen(mockContent = mockContent)
    }
    step("Synchronize addresses") {
        synchronizeAddresses()
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
    step("Choose identical receive token '$tokenName' from '$toAccountName'") {
        chooseIdenticalReceiveToken(tokenName = tokenName, receiveAccountName = toAccountName)
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
        openSwapScreen(from = SwapEntryPoint.TokenDetails)
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
        composeTestRule.waitUntil(timeoutMillis = WAIT_UNTIL_TIMEOUT_LONG) {
            runCatching { onSwapTokenScreen { receiveAmount.assertIsDisplayed() } }.isSuccess
        }
    }
}

/**
 * Opens the swap 'Network fee' bottom sheet, retrying the click until the fee selector shows.
 * Single action without its own step — wrap the call in a `step(...)`.
 */
fun BaseTestCase.openSwapNetworkFeeSelector() {
    composeTestRule.waitUntil(timeoutMillis = WAIT_UNTIL_TIMEOUT_VERY_LONG) {
        runCatching { onSwapTokenScreen { networkFeeBlock.performClick() } }
        runCatching { onSendFeeSelectorBottomSheet { networkFeeTitle.assertIsDisplayed() } }.isSuccess
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
        composeTestRule.waitUntil(timeoutMillis = WAIT_UNTIL_TIMEOUT_LONG) {
            runCatching { onSendFeeSelectorBottomSheet { feeTokenItem(newFeeToken).performClick() } }.isSuccess
        }
    }
    step("Click on 'Apply' button") {
        waitForIdle()
        onSendFeeSelectorBottomSheet { applyButton.performClick() }
    }
}

/** Holds the last BASE_BUTTON; enters [accessCode] if a hot wallet prompts for it. */
fun BaseTestCase.confirmSwapByHolding(accessCode: String? = null) {
    val buttonMatcher = hasTestTag(BaseButtonTestTags.BUTTON)
    val buttons = composeTestRule.onAllNodes(buttonMatcher)
    // HoldToConfirm is always last — withdraw renders an extra BASE_BUTTON for notifications.
    val swapButton = buttons[buttons.fetchSemanticsNodes().lastIndex]
    swapButton.performTouchInput { longClick(durationMillis = HOLD_DURATION_MS) }
    waitForIdle()
    val accessCodeInput = hasTestTag(HotWalletAccessCodeTestTags.ACCESS_CODE_INPUT)
    val swapInProgressText = hasText(getResourceString(CoreUiR.string.swap_in_progress))
    composeTestRule.waitUntil(timeoutMillis = WAIT_UNTIL_TIMEOUT_LONG) {
        composeTestRule.onAllNodes(accessCodeInput).fetchSemanticsNodes().isNotEmpty() ||
            composeTestRule.onAllNodes(swapInProgressText, useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
    }
    val needsAccessCode =
        composeTestRule.onAllNodes(accessCodeInput).fetchSemanticsNodes().isNotEmpty()
    if (needsAccessCode && accessCode != null) {
        composeTestRule.onNode(accessCodeInput).performTextInput(accessCode)
        waitForIdle()
    }
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
    composeTestRule.waitUntil(timeoutMillis = WAIT_UNTIL_TIMEOUT_LONG) {
        runCatching { onSwapTokenScreen { textInput.assertIsDisplayed() } }.isSuccess
    }
    onSwapTokenScreen {
        textInput.clickWithAssertion()
        textInput.performTextReplacement(amount)
    }
}

// composeTestRule.waitUntil rather than flakySafely — the latter is unavailable in extensions on BaseTestCase.
fun BaseTestCase.assertTransferReady() {
    composeTestRule.waitUntil(timeoutMillis = WAIT_UNTIL_TIMEOUT_LONG) {
        runCatching { onSwapTokenScreen { transferButton.assertIsDisplayed() } }.isSuccess
    }
    onSwapTokenScreen { providersBlock.assertIsNotDisplayed() }
}

fun BaseTestCase.waitForFeeDisplayed() {
    composeTestRule.waitUntil(timeoutMillis = WAIT_UNTIL_TIMEOUT_LONG) {
        runCatching { onSwapTokenScreen { feeAmount.assertIsDisplayed() } }.isSuccess
    }
}

fun BaseTestCase.swapFeeDiffersFrom(previousFee: String): Boolean {
    var current = ""
    onSwapTokenScreen { current = feeAmount.extractText() }
    return current.isNotEmpty() && current != previousFee
}


