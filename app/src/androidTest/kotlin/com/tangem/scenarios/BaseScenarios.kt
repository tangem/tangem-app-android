package com.tangem.scenarios

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_LONG
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_WALLET_CREATION
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.extensions.isDisplayedSafely
import com.tangem.core.res.R as CommonR
import com.tangem.core.ui.R as CoreUiR
import com.tangem.domain.models.scan.ProductType
import com.tangem.screens.*
import com.tangem.tap.domain.sdk.mocks.MockContent
import com.tangem.tap.domain.sdk.mocks.MockProvider
import com.tangem.utils.StringsSigns.DASH_SIGN
import io.github.kakaocup.kakao.common.utilities.getResourceString
import io.qameta.allure.kotlin.Allure.step

fun BaseTestCase.scanCard(
    productType: ProductType? = null,
    mockContent: MockContent? = null,
    isTwinsCard: Boolean = false,
) {
    when {
        mockContent != null -> MockProvider.setMocks(mockContent)
        productType != null -> MockProvider.setMocks(productType)
        else -> MockProvider.setMocks(ProductType.Wallet)
    }
    step("Click on 'I have a wallet' button") {
        onIntroductionScreen { iHaveWalletButton.clickWithAssertion() }
    }
    step("Click on 'Tangem Card or Ring' option") {
        onIntroductionScreen { cardOrRingOption.clickWithAssertion() }
    }
    if (isTwinsCard) {
        step("Click on 'Continue' button") {
            onOnboardingScreen { continueButton.clickWithAssertion() }
        }
        step("Click on 'Continue to my wallet' button") {
            onOnboardingScreen { continueToMyWalletButton.clickWithAssertion() }
        }
    }
}

fun BaseTestCase.openMainScreen(
    productType: ProductType? = null,
    mockContent: MockContent? = null,
    isTwinsCard: Boolean = false,
) {
    step("Scan card") {
        scanCard(
            productType = productType,
            mockContent = mockContent,
            isTwinsCard = isTwinsCard,
        )
    }
    step("Assert 'Main' screen is displayed") {
        onMainScreen { screenContainer.assertIsDisplayed() }
    }
    step("Dismiss Market Tooltip by clicking close button") {
        onMarketsTooltipScreen { closeButton.clickWithAssertion() }
    }
}

/** Opens the main screen, synchronizes addresses, and opens the details of the token with [tokenName]. */
fun BaseTestCase.openTokenDetails(tokenName: String) {
    step("Open 'Main Screen'") {
        openMainScreen()
    }
    step("Synchronize addresses") {
        synchronizeAddresses()
    }
    step("Click on token with name: '$tokenName'") {
        openTokenDetailsFromTokenList(tokenName)
    }
}

/** Creates a brand-new mobile (hot) wallet from the Welcome screen and lands on Main. */
fun BaseTestCase.openMainScreenWithNewHotWallet() {
    step("Click on 'Create wallet' button") {
        onIntroductionScreen { createWalletButton.clickWithAssertion() }
    }
    // The sheet row creates the wallet on the spot ([REDACTED_TASK_KEY]), so there is no separate create step any more.
    step("Click on 'Create seed phrase wallet' option") {
        onIntroductionScreen { createSeedPhraseWalletOption.clickWithAssertion() }
    }
    step("Assert 'Main' screen is displayed") {
        awaitSuccess(timeoutMillis = WAIT_UNTIL_TIMEOUT_WALLET_CREATION) {
            onMainScreen { screenContainer.assertIsDisplayed() }
        }
    }
    step("Dismiss Market Tooltip by clicking close button") {
        onMarketsTooltipScreen { closeButton.clickWithAssertion() }
    }
}

@OptIn(ExperimentalTestApi::class)
fun BaseTestCase.openMainScreenWithExistingHotWallet(seedPhrase: String, accessCode: String = "") {
    step("Click on 'I have a wallet' button") {
        onIntroductionScreen { iHaveWalletButton.clickWithAssertion() }
    }
    step("Click on 'Import existing wallet' option") {
        onIntroductionScreen { importWalletOption.clickWithAssertion() }
    }
    // Only the cloud backup button is toggle-gated, so this one is always there — wait, never skip.
    step("Click on 'Use recovery phrase' button") {
        awaitSuccess { onIntroductionScreen { useRecoveryPhraseButton.assertIsDisplayed() } }
        onIntroductionScreen { useRecoveryPhraseButton.performClick() }
    }
    step("Click on 'Phrase text field'") {
        waitForIdle()
        onImportWalletScreen { phraseTextField.performClick() }
    }
    step("Type seed phrase in 'Phrase text field'") {
        onImportWalletScreen { phraseTextField.performTextReplacement(seedPhrase) }
    }
    step("Click on 'Import' button") {
        onImportWalletScreen {
            importButton.assertIsEnabled()
            importButton.performClick()
        }
    }
    step("Click on 'Continue' button") {
        onImportWalletScreen {
            continueButton.assertIsEnabled()
            continueButton.performClick()
        }
    }
    if (accessCode.isNotEmpty()) {
        step("Enter access code '$accessCode' (create)") {
            onHotWalletAccessCodeScreen {
                accessCodeInput.performClick()
                accessCodeInput.performTextInput(accessCode)
            }
        }
        step("Re-enter access code '$accessCode' (confirm)") {
            // Create+confirm screens share ACCESS_CODE_INPUT — gate on confirm-screen title.
            composeTestRule.waitUntilAtLeastOneExists(
                hasText(getResourceString(CoreUiR.string.access_code_confirm_title)),
                timeoutMillis = WAIT_UNTIL_TIMEOUT_LONG,
            )
            onHotWalletAccessCodeScreen {
                accessCodeInput.performClick()
                accessCodeInput.performTextInput(accessCode)
            }
        }
        step("Dismiss biometry prompt if shown") {
            // The sheet is optional, so wait for it or for the Finish screen that replaces it — deciding
            // before either has rendered leaves the sheet up. Wallet creation runs in between and on a
            // cold launch outlasts the ordinary timeout.
            composeTestRule.waitUntilAtLeastOneExists(
                matcher = hasText(getResourceString(CoreUiR.string.save_user_wallet_agreement_dont_allow))
                    or hasText(getResourceString(CommonR.string.common_finish)),
                timeoutMillis = WAIT_UNTIL_TIMEOUT_WALLET_CREATION,
            )
            onBiometryDialog {
                if (dontAllowButton.isDisplayedSafely()) {
                    dontAllowButton.performClick()
                }
            }
        }
    } else {
        step("Click on 'Skip' button") {
            onImportWalletScreen { skipButton.performClick() }
        }
        step("Click on 'Skip anyway' dialog button") {
            onDialog { skipAnywayButton.performClick() }
        }
    }
    step("Click on 'Finish' button") {
        awaitSuccess { onImportWalletScreen { finishButton.assertExists() } }
        onImportWalletScreen {
            finishButton.assertIsEnabled()
            finishButton.performClick()
        }
    }
    step("Assert 'Main' screen is displayed") {
        onMainScreen { screenContainer.assertIsDisplayed() }
    }
    step("Dismiss Market Tooltip by clicking close button") {
        onMarketsTooltipScreen { closeButton.clickWithAssertion() }
    }
}

fun BaseTestCase.synchronizeAddresses(
    balance: String? = null,
    isBalanceAvailable: Boolean = true,
    assertBalance: Boolean = true,
) {
    step("Click on 'Synchronize addresses' button") {
        onMainScreen { synchronizeAddressesButton.clickWithAssertion() }
    }

    if (!assertBalance) return

    when {
        !isBalanceAvailable -> step("Assert wallet balance = '$DASH_SIGN'") {
            onMainScreen { totalBalanceText.assertTextContains(DASH_SIGN) }
        }
        balance != null -> {
            step("Assert wallet balance != '$DASH_SIGN'") {
                awaitBalanceLoaded()
            }
            step("Assert wallet balance = '$balance'") {
                awaitAssert(timeoutMillis = WAIT_UNTIL_TIMEOUT) {
                    onMainScreen { totalBalanceText.assertTextContains(balance) }
                }
            }
        }
        else -> step("Assert wallet balance != '$DASH_SIGN'") {
            awaitBalanceLoaded()
        }
    }
}

/**
 * The balance settles only once every network status lands — 7–10+ s on CI, past the flakySafely default.
 */
private fun BaseTestCase.awaitBalanceLoaded() {
    awaitSuccess(timeoutMillis = WAIT_UNTIL_TIMEOUT) {
        onMainScreen { totalBalanceText.assert(!hasText(DASH_SIGN)) }
    }
}

fun BaseTestCase.openDeviceSettingsScreen() {
    step("Open wallet details") {
        waitForIdle()
        onMainScreenTopBar { moreButton.clickWithAssertion() }
    }
    step("Open 'Wallet settings' screen") {
        onDetailsScreen { walletNameButton.performClick() }
    }
    step("Click on 'Device settings' button") {
        onWalletSettingsScreen {
            scrollToDeviceSettings()
            deviceSettingsButton.clickWithAssertion()
        }
    }
}

fun BaseTestCase.openWalletConnectScreen() {
    step("Click 'More' button on TopBar") {
        onMainScreenTopBar { moreButton.clickWithAssertion() }
    }
    step("Click on 'Wallet Connect' button") {
        onDetailsScreen { walletConnectButton.clickWithAssertion() }
    }
}