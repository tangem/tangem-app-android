package com.tangem.scenarios

import com.tangem.common.BaseTestCase
import com.tangem.common.R
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.screens.accounts.onAccountDetailsScreen
import com.tangem.screens.accounts.onAccountInfoEditorScreen
import com.tangem.screens.accounts.onArchivedAccountsScreen
import com.tangem.screens.onDetailsScreen
import com.tangem.screens.onDialog
import com.tangem.screens.onMainScreenTopBar
import com.tangem.screens.onWalletSettingsScreen
import io.github.kakaocup.kakao.common.utilities.getResourceString
import io.qameta.allure.kotlin.Allure.step

fun BaseTestCase.openWalletSettingsScreen() {
    step("Open 'Wallet details' screen") {
        onMainScreenTopBar { moreButton.clickWithAssertion() }
    }
    step("Open 'Wallet settings' screen") {
        onDetailsScreen { walletNameButton.clickWithAssertion() }
    }
}

fun BaseTestCase.startAccountCreation() {
    step("Click on 'Add account' button") {
        onWalletSettingsScreen { addAccountButton.clickWithAssertion() }
    }
    step("Assert 'Account info editor' screen is displayed") {
        onAccountInfoEditorScreen { screenContainer.assertIsDisplayed() }
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

fun BaseTestCase.checkUnsavedChangesCreationModal() {
    step("Assert 'Unsaved changes' alert is displayed") {
        onDialog { dialogContainer.assertIsDisplayed() }
    }
    step("Assert 'Unsaved changes' alert has proper title" ) {
        onDialog { title.assertTextContains(getResourceString(R.string.account_unsaved_dialog_title)) }
    }
    step("Assert 'Unsaved changes' alert has proper description for account creation" ) {
        onDialog {
            text.assertTextContains(getResourceString(R.string.account_unsaved_dialog_message_create))
        }
    }
    step("Assert 'Keep editing' button is displayed in alert with proper text") {
        onDialog { keepEditButton.assertIsDisplayed() }
    }
    step("Assert 'Discard' button is displayed in alert") {
        onDialog { discardButton.assertIsDisplayed() }
    }
}

fun BaseTestCase.assertUnsavedChangesEditionModal() {
    step("Assert 'Unsaved changes' alert is displayed") {
        onDialog { dialogContainer.assertIsDisplayed() }
    }
    step("Assert 'Unsaved changes' alert has proper title" ) {
        onDialog { title.assertTextContains("Unsaved changes") }
    }
    step("Assert 'Unsaved changes' alert has proper description for account creation" ) {
        onDialog {
            text.assertTextContains(getResourceString(R.string.account_unsaved_dialog_message_edit))
        }
    }
    step("Assert 'Keep editing' button is displayed in alert with proper text") {
        onDialog { keepEditButton.assertIsDisplayed() }
    }
    step("Assert 'Discard' button is displayed in alert") {
        onDialog { discardButton.assertIsDisplayed() }
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