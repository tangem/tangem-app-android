package com.tangem.scenarios

import com.tangem.common.BaseTestCase
import com.tangem.common.R
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_VERY_LONG
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.domain.models.account.Account
import com.tangem.screens.accounts.onAccountDetailsScreen
import com.tangem.screens.accounts.onAccountInfoEditorScreen
import com.tangem.screens.accounts.onArchivedAccountsScreen
import com.tangem.screens.onDetailsScreen
import com.tangem.screens.onDialog
import com.tangem.screens.onMainScreenTopBar
import com.tangem.screens.onWalletSettingsScreen
import com.tangem.utils.logging.TangemLogger
import io.github.kakaocup.kakao.common.utilities.getResourceString
import io.qameta.allure.kotlin.Allure.step
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout

private const val ACCOUNT_POLL_INTERVAL_MS = 500L

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
    step("Assert 'Unsaved changes' alert has proper title") {
        onDialog { title.assertTextContains(getResourceString(R.string.account_unsaved_dialog_title)) }
    }
    step("Assert 'Unsaved changes' alert has proper description for account creation") {
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
    step("Assert 'Unsaved changes' alert has proper title") {
        onDialog { title.assertTextContains(getResourceString(R.string.account_unsaved_dialog_title)) }
    }
    step("Assert 'Unsaved changes' alert has proper description for account creation") {
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

/**
 * Polls [singleAccountListSupplier] for the selected wallet until a [Account.CryptoPortfolio] with the given
 * [derivationIndex] appears with a non-empty token list, then returns it.
 *
 * Per-account token derivation paths live in the domain account model
 * ([Account.CryptoPortfolio.cryptoCurrencies] → [com.tangem.domain.models.network.Network.derivationPath]),
 * not in the tester-menu "Addresses info" (which reads from the account-agnostic wallet managers store and
 * only ever shows main/base derivations). Reading the model directly is the reliable source for asserting
 * per-account derivations.
 */
fun BaseTestCase.awaitCryptoPortfolioAccount(derivationIndex: Int): Account.CryptoPortfolio {
    val walletId = getSelectedWalletSyncUseCase().getOrNull()?.walletId
        ?: error("No selected wallet found")

    var account: Account.CryptoPortfolio? = null
    runBlocking {
        withTimeout(WAIT_UNTIL_TIMEOUT_VERY_LONG) {
            while (true) {
                val candidate = singleAccountListSupplier.getSyncOrNull(walletId)
                    ?.accounts
                    ?.filterIsInstance<Account.CryptoPortfolio>()
                    ?.firstOrNull { it.derivationIndex.value == derivationIndex }

                if (candidate != null && candidate.cryptoCurrencies.isNotEmpty()) {
                    TangemLogger.i(
                        "Account with derivation index $derivationIndex resolved: " +
                            "${candidate.cryptoCurrencies.size} token(s)",
                    )
                    account = candidate
                    return@withTimeout
                }

                delay(ACCOUNT_POLL_INTERVAL_MS)
            }
        }
    }

    return requireNotNull(account) {
        "Account with derivation index $derivationIndex was not found for wallet $walletId"
    }
}

/**
 * Returns all derivation paths of tokens whose name equals [tokenName] (case-insensitive) within this account.
 */
fun Account.CryptoPortfolio.derivationPathsForToken(tokenName: String): List<String> = cryptoCurrencies
    .filter { it.name.equals(tokenName, ignoreCase = true) }
    .mapNotNull { it.network.derivationPath.value }