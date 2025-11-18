package com.tangem.scenarios

import com.tangem.common.BaseTestCase
import com.tangem.screens.onOnboardingScreen
import io.qameta.allure.kotlin.Allure.step

fun BaseTestCase.checkBackupScreen() {
    step("Assert 'Creating a backup' top bar title is displayed") {
        onOnboardingScreen { creatingBackupTobBarTitle.assertIsDisplayed() }
    }
    step("Assert 'Prepare your scan or ring' title is displayed") {
        onOnboardingScreen { prepareYouCardOrRingTitle.assertIsDisplayed() }
    }
    step("Assert 'Start backup' text is displayed") {
        onOnboardingScreen { startBackupText.assertIsDisplayed() }
    }
    step("Assert 'Scan card' button is displayed") {
        onOnboardingScreen { scanCardButton.assertIsDisplayed() }
    }
    step("Assert 'Skip for later' button is not displayed") {
        onOnboardingScreen { skipForLaterButton.assertIsNotDisplayed() }
    }
}

fun BaseTestCase.checkCreateWalletScreenForWalletNoWallets() {
    step("Assert 'Create wallet' tob bar title is displayed") {
        onOnboardingScreen { createWalletTopBarTitle.assertIsDisplayed() }
    }
    step("Assert top bar 'Back' button is displayed") {
        onOnboardingScreen { topBarBackButton.assertIsDisplayed() }
    }
    step("Assert top bar 'More' button is displayed") {
        onOnboardingScreen { topBarMoreButton.assertIsDisplayed() }
    }
    step("Assert 'Create wallet' title is displayed") {
        onOnboardingScreen { createWalletTitle.assertIsDisplayed() }
    }
    step("Assert 'Create wallet' text is displayed") {
        onOnboardingScreen { createWalletText.assertIsDisplayed() }
    }
    step("Assert 'Create wallet' button is displayed") {
        onOnboardingScreen { createWalletButton.assertIsDisplayed() }
    }
}

fun BaseTestCase.openAndCheckBackupScreenForWalletNoWallets() {
    step("Click on 'Create wallet' button") {
        onOnboardingScreen { createWalletButton.performClick() }
    }
    step("Assert 'Getting started' top bar title is displayed") {
        onOnboardingScreen { gettingStartedTobBarTitle.assertIsDisplayed() }
    }
    step("Assert 'Backup wallet' title is displayed") {
        onOnboardingScreen { backupWalletTitle.assertIsDisplayed() }
    }
    step("Assert 'Backup wallet' text is displayed") {
        onOnboardingScreen { backupWalletText.assertIsDisplayed() }
    }
    step("Assert 'Backup now' button is displayed") {
        onOnboardingScreen { backupWalletButton.assertIsDisplayed() }
    }
    step("Assert 'Skip for later' button is not displayed") {
        onOnboardingScreen { skipForLaterButton.assertIsNotDisplayed() }
    }
}

fun BaseTestCase.checkCreateWalletScreenForWallet2NoWallets() {
    step("Assert 'Create a wallet' top bar title is displayed") {
        onOnboardingScreen { createWalletTopBarTitle.assertIsDisplayed() }
    }
    step("Assert top bar 'Back' button is displayed") {
        onOnboardingScreen { topBarBackButton.assertIsDisplayed() }
    }
    step("Assert top bar 'More' button is displayed") {
        onOnboardingScreen { topBarMoreButton.assertIsDisplayed() }
    }
    step("Assert 'Generate keys privately' title is displayed") {
        onOnboardingScreen { generateKeysPrivatelyTitle.assertIsDisplayed() }
    }
    step("Assert 'Generate keys privately' text is displayed") {
        onOnboardingScreen { generateKeysPrivatelyText.assertIsDisplayed() }
    }
    step("Assert 'Create wallet' button is displayed") {
        onOnboardingScreen { createWalletButton.assertIsDisplayed() }
    }
    step("Assert 'Other options' button is displayed") {
        onOnboardingScreen { otherOptionsButton.assertIsDisplayed() }
    }
}
fun BaseTestCase.openAndCheckBackupScreenForWallet2NoWallets() {
    step("Click on 'Create wallet' button") {
        onOnboardingScreen { createWalletButton.performClick() }
    }
    step("Assert 'Creating a backup' top bar title is displayed") {
        onOnboardingScreen { creatingBackupTobBarTitle.assertIsDisplayed() }
    }
    step("Assert 'No backup devices' title is displayed") {
        onOnboardingScreen { noBackupDevicesTitle.assertIsDisplayed() }
    }
    step("Assert 'No backup devices' text is displayed") {
        onOnboardingScreen { noBackupDevicesText.assertIsDisplayed() }
    }
    step("Assert 'Add card or ring' button is displayed") {
        onOnboardingScreen { addCardOrRingButton.assertIsDisplayed() }
    }
    step("Assert 'Finalize backup' button is displayed") {
        onOnboardingScreen { finalizeBackupButton.assertIsDisplayed() }
    }
    step("Assert 'Skip for later' button is not displayed") {
        onOnboardingScreen { skipForLaterButton.assertIsNotDisplayed() }
    }
}