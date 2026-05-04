package com.tangem.scenarios

import com.tangem.common.BaseTestCase
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.screens.accounts.onAccountDetailsScreen
import com.tangem.screens.accounts.onArchivedAccountsScreen
import com.tangem.screens.onDetailsScreen
import com.tangem.screens.onDialog
import com.tangem.screens.onMainScreenTopBar
import com.tangem.screens.onWalletSettingsScreen
import io.qameta.allure.kotlin.Allure.step

fun BaseTestCase.openWalletSettingsScreen() {
    step("Open 'Wallet details' screen") {
        onMainScreenTopBar { moreButton.clickWithAssertion() }
    }
    step("Open 'Wallet settings' screen") {
        onDetailsScreen { walletNameButton.clickWithAssertion() }
    }
}

fun BaseTestCase.openAccountDetails(accountName: String) {
    step("Click on account: '$accountName'") {
        onWalletSettingsScreen { accountItem(accountName).clickWithAssertion() }
    }
    step("Assert 'Account details' screen is displayed") {
        onAccountDetailsScreen { screenContainer.assertIsDisplayed() }
    }
}

fun BaseTestCase.archiveAccount() {
    step("Assert 'Archive' button is displayed") {
        onAccountDetailsScreen { archiveAccountButton.assertIsDisplayed() }
    }
    step("Click on 'Archive' button") {
        onAccountDetailsScreen { archiveAccountButton.clickWithAssertion() }
    }
    step("Confirm archivation in dialog") {
        onDialog { archiveButton.clickWithAssertion() }
    }
}

fun BaseTestCase.assertArchiveConfirmationDialog() {
    step("Assert confirmation dialog is displayed") {
        onDialog { dialogContainer.assertIsDisplayed() }
    }
    step("Assert confirmation dialog has 'Cancel' button") {
        onDialog { cancelButton.assertIsDisplayed() }
    }
    step("Assert confirmation dialog has 'Archive' button") {
        onDialog { archiveButton.assertIsDisplayed() }
    }
}

fun BaseTestCase.assertErrorDialog(expectedTitle: String, expectedMessage: String) {
    step("Assert error dialog is displayed") {
        onDialog { dialogContainer.assertIsDisplayed() }
    }
    step("Assert error dialog has proper title") {
        onDialog {
            title.assertTextContains(expectedTitle)
        }
    }
    step("Assert error dialog has explanatory text") {
        onDialog {
            text.assertTextContains(expectedMessage)
        }
    }
    step("Assert error dialog has 'OK' button") {
        onDialog { okButton.assertIsDisplayed() }
    }
}

fun BaseTestCase.dismissErrorDialog() {
    step("Dismiss error dialog by clicking 'Ok' button") {
        onDialog { okButton.clickWithAssertion() }
    }
}

fun BaseTestCase.openArchivedAccountsScreen() {
    step("Click on 'Archived accounts' button") {
        onWalletSettingsScreen { openArchivedAccountsButton.clickWithAssertion() }
    }
}

fun BaseTestCase.assertArchivedAccountIsDisplayed(accountName: String) {
    step("Assert archived account with name '$accountName' is displayed") {
        onArchivedAccountsScreen {
            findArchivedAccountItemByName(accountName)
                .container.assertIsDisplayed()
        }
    }
}

fun BaseTestCase.restoreArchivedAccount(accountName: String) {
    step("Restore account with name '$accountName'") {
        onArchivedAccountsScreen {
            findArchivedAccountItemByName(accountName)
                .restoreButton.clickWithAssertion()
        }
    }
}