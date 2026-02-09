package com.tangem.scenarios

import androidx.compose.ui.test.hasText
import com.tangem.common.BaseTestCase
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.domain.models.scan.ProductType
import com.tangem.screens.*
import com.tangem.screens.AlreadyUsedWalletDialogPageObject.thisIsMyWalletButton
import com.tangem.tap.domain.sdk.mocks.MockContent
import com.tangem.tap.domain.sdk.mocks.MockProvider
import com.tangem.utils.StringsSigns.DASH_SIGN
import io.qameta.allure.kotlin.Allure.step

fun BaseTestCase.scanCard(
    productType: ProductType? = null,
    mockContent: MockContent? = null,
    alreadyActivatedDialogIsShown: Boolean = false,
    isTwinsCard: Boolean = false,
) {
    if (productType != null) {
        MockProvider.setMocks(productType)
    }
    if (mockContent != null) {
        MockProvider.setMocks(mockContent)
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
    if (alreadyActivatedDialogIsShown) {
        step("Click on 'This is my wallet' button") {
            waitForIdle()
            AlreadyUsedWalletDialogPageObject { thisIsMyWalletButton.click() }
        }
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
    alreadyActivatedDialogIsShown: Boolean = false,
    isTwinsCard: Boolean = false,
) {
    step("Scan card") {
        scanCard(
            productType = productType,
            mockContent = mockContent,
            alreadyActivatedDialogIsShown = alreadyActivatedDialogIsShown,
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
        onTopBar { moreButton.clickWithAssertion() }
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
        onTopBar { moreButton.clickWithAssertion() }
    }
    step("Click on 'Wallet Connect' button") {
        onDetailsScreen { walletConnectButton.clickWithAssertion() }
    }
}