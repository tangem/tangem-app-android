package com.tangem.scenarios

import androidx.compose.ui.test.hasText
import com.tangem.common.BaseTestCase
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.domain.models.scan.ProductType
import com.tangem.screens.*
import com.tangem.tap.domain.sdk.mocks.MockContent
import com.tangem.tap.domain.sdk.mocks.MockProvider
import com.tangem.utils.StringsSigns.DASH_SIGN
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
    step("Click on 'Accept' button") {
        onDisclaimerScreen { acceptButton.clickWithAssertion() }
    }
    step("Click on 'Get started' button") {
        onStoriesScreen { getStartedButton.clickWithAssertion() }
    }
    step("Click on 'Scan card or ring' button") {
        onCreateWalletStartScreen { scanCardOrRingButton.clickWithAssertion() }
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

fun BaseTestCase.openMainScreenWithExistingHotWallet(seedPhrase: String) {
    step("Click on 'Accept' button") {
        onDisclaimerScreen { acceptButton.clickWithAssertion() }
    }
    step("Click on 'Get started' button") {
        onStoriesScreen { getStartedButton.clickWithAssertion() }
    }
    step("Click on 'Start with Mobile Wallet' button") {
        onCreateWalletStartScreen { startWithMobileWalletButton.performClick() }
    }
    step("Click on 'Import existing wallet' button") {
        onCreateMobileWalletScreen { importExistingWalletButton.performClick() }
    }
    step("Click on 'Phrase text field'") {
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
    step("Click on 'Skip' button") {
        onImportWalletScreen { skipButton.performClick() }
    }
    step("Click on 'Skip anyway' dialog button") {
        onDialog { skipAnywayButton.performClick() }
    }
    step("Click on 'Finish' button") {
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
    isBalanceAvailable: Boolean = true
) {
    step("Click on 'Synchronize addresses' button") {
        onMainScreen { synchronizeAddressesButton.clickWithAssertion() }
    }

    when {
        !isBalanceAvailable -> step("Assert wallet balance = '$DASH_SIGN'") {
            onMainScreen { totalBalanceText.assertTextContains(DASH_SIGN) }
        }
        balance != null -> {
            step("Assert wallet balance != '$DASH_SIGN'") {
                onMainScreen { totalBalanceText.assert(!hasText(DASH_SIGN)) }
            }
            step("Assert wallet balance = '$balance'") {
                onMainScreen { totalBalanceText.assertTextContains(balance) }
            }
        }
        else -> step("Assert wallet balance != '$DASH_SIGN'") {
            onMainScreen { totalBalanceText.assert(!hasText(DASH_SIGN)) }
        }
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
        onWalletSettingsScreen { deviceSettingsButton.clickWithAssertion() }
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