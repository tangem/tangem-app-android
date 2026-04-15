package com.tangem.tests.accounts

import com.tangem.common.BaseTestCase
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.utils.resetWireMockScenarioState
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.core.ui.R
import com.tangem.scenarios.openMainScreen
import com.tangem.scenarios.synchronizeAddresses
import com.tangem.screens.accounts.onAccountDetailsScreen
import com.tangem.screens.accounts.onArchivedAccountsScreen
import com.tangem.screens.onDetailsScreen
import com.tangem.screens.onDialog
import com.tangem.screens.onMainScreenTopBar
import com.tangem.screens.onWalletSettingsScreen
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.kakaocup.kakao.common.utilities.getResourceString
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Test

@HiltAndroidTest
class AccountArchivationsTest : BaseTestCase() {

    private val apiScenario = "user_tokens_api"
    private val referralScenario = "referral_api"

    // WireMock scenarios:
    // 5979 — default mock (no scenario switch needed), main account has no archive button
    // 5974 — user_tokens_api: "TwoAccountsArchivable"
    // 5976 — user_tokens_api: "TwoAccountsWithArchivedAccounts" → "ReadyToRestore"
    // 5981 — user_tokens_api: "TwoAccountsArchivable" + referral_api: "Participating"
    // 6844 — user_tokens_api: "TwoAccountsArchivable" → "AccountsPutError"
    // 5980 — user_tokens_api: "OneAccountWithArchivedCustomToken" → "ReadyToRestoreCustomToken"
    // 7962 — user_tokens_api: "TwoAccountsWithArchivedAccounts" → "AccountsPutError"

    // +
    @Test
    @AllureId("5979")
    @DisplayName("Accounts: Verify main account archivation button not available")
    fun mainAccountArchivationAttemptTest() {

        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(apiScenario)
            }
        ).run {
            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Open wallet details") {
                onMainScreenTopBar { moreButton.clickWithAssertion() }
            }
            step("Open 'Wallet settings' screen") {
                onDetailsScreen { walletNameButton.clickWithAssertion() }
            }
            step("Click on the main account") {
                onWalletSettingsScreen { accountItem("Main account").clickWithAssertion() }
            }
            Thread.sleep(5000)
            step("Assert account details screen is displayed") {
                onAccountDetailsScreen { screenContainer.assertIsDisplayed() }
            }
            step("Assert archive button is NOT displayed") {
                onAccountDetailsScreen { archiveAccountButton.assertDoesNotExist() }
            }
        }
    }

    @Test
    @AllureId("5974")
    @DisplayName("Accounts: archive a non-main account")
    fun archiveSuccessfullyAccountTest() {
        val accountToArchive = "Account 2"

        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(apiScenario)
            }
        ).run {
            step("Set WireMock scenario to 'TwoAccountsArchivable'") {
                setWireMockScenarioState(apiScenario, "TwoAccountsArchivable")
            }
            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Open wallet details") {
                onMainScreenTopBar { moreButton.clickWithAssertion() }
            }
            step("Open 'Wallet settings' screen") {
                onDetailsScreen { walletNameButton.clickWithAssertion() }
            }
            step("Click on account: '$accountToArchive'") {
                onWalletSettingsScreen { accountItem(accountToArchive).clickWithAssertion() }
            }

            step("Verify that account details screen is displayed") {
                onAccountDetailsScreen { screenContainer.assertIsDisplayed() }
            }
            step("Assert archive button is displayed") {
                onAccountDetailsScreen { archiveAccountButton.assertIsDisplayed() }
            }
            step("Click on archive button") {
                onAccountDetailsScreen { archiveAccountButton.clickWithAssertion() }
            }

            step("Assert confirmation menu is displayed with archive and cancel actions") {
                onDialog {
                    dialogContainer.assertIsDisplayed()
                    archiveButton.assertIsDisplayed()
                    cancelButton.assertIsDisplayed()
                }
            }
            step("Click 'Archive' in confirmation menu") {
                onDialog { archiveButton.clickWithAssertion() }
            }
            step("Verify app returned to wallet settings screen") {
                onWalletSettingsScreen { addAccountButton.assertIsDisplayed() }
            }
            step("Verify archived account '$accountToArchive' is no longer listed in active accounts") {
                onWalletSettingsScreen {
                    accountItem(accountToArchive).assertDoesNotExist()
                }
            }
        }
    }

    @Test
    @AllureId("6844")
    @DisplayName("Accounts: archive account error UI")
    fun archiveAccountErrorTest() {
        val accountToArchiveName = "Account 2"

        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(apiScenario)
            }
        ).run {
            step("Set WireMock scenario to 'TwoAccountsArchivable'") {
                setWireMockScenarioState(apiScenario, "TwoAccountsArchivable")
            }
            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Open wallet details") {
                onMainScreenTopBar { moreButton.clickWithAssertion() }
            }
            step("Open 'Wallet settings' screen") {
                onDetailsScreen { walletNameButton.clickWithAssertion() }
            }
            step("Click on account: '$accountToArchiveName'") {
                onWalletSettingsScreen { accountItem(accountToArchiveName).clickWithAssertion() }
            }
            step("Verify that account details edit screen is displayed") {
                onAccountDetailsScreen { screenContainer.assertIsDisplayed() }
            }
            step("Verify that archive button IS displayed") {
                onAccountDetailsScreen { archiveAccountButton.assertIsDisplayed() }
            }

            step("Forcing application network error scenario for account data updating request") {
                setWireMockScenarioState(apiScenario, "AccountsPutError")
            }

            step("Click on archive button") {
                onAccountDetailsScreen { archiveAccountButton.clickWithAssertion() }
            }

            step("Confirm archive in dialog") {
                onDialog { archiveButton.clickWithAssertion() }
            }
            step("Assert error dialog is displayed") {
                onDialog { dialogContainer.assertIsDisplayed() }
            }
            step("Assert error dialog has proper title") {
                onDialog { title.assertIsDisplayed() }
                onDialog { title.assertTextContains("Something went wrong") }
            }

            step("Assert error dialog has explanatory text") {
                onDialog { text.assertIsDisplayed() }
                onDialog { text.assertTextContains(getResourceString(R.string.account_generic_error_dialog_message)) }
            }

            step("Assert error dialog has OK button") {
                onDialog { okButton.assertIsDisplayed() }
            }
        }
    }

    @Test
    @AllureId("5981")
    @DisplayName(
        "Accounts: archive account participated in referral program." +
            "Expected that account is not possible to archive though."
    )
    fun archiveAccountParticipatedInReferralTest() {
        // WireMock: user_tokens_api → "TwoAccountsArchivable" + referral_api → "Participating"
        // Expected: error alert mentioning "referral program", archive button remains visible
        val accountToArchiveName = "Account 2"

        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(apiScenario)
                resetWireMockScenarioState(referralScenario)
            }
        ).run {

            step("Set WireMock scenario to 'TwoAccountsArchivable'") {
                setWireMockScenarioState(apiScenario, "TwoAccountsArchivable")
            }

            step("Set referral WireMock scenario to 'Participating'") {
                setWireMockScenarioState(referralScenario, "Participating")
            }


            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Open wallet details") {
                onMainScreenTopBar { moreButton.clickWithAssertion() }
            }
            step("Open 'Wallet settings' screen") {
                onDetailsScreen { walletNameButton.clickWithAssertion() }
            }

            Thread.sleep(10_00_0)

            step("Click on account: '$accountToArchiveName'") {
                onWalletSettingsScreen { accountItem(accountToArchiveName).clickWithAssertion() }
            }

            // TODO verify that request sent to /referral/{:walletId}

            step("Verify that account details edit screen is displayed") {
                onAccountDetailsScreen { screenContainer.assertIsDisplayed() }
            }
            step("Verify that archive button IS displayed") {
                onAccountDetailsScreen { archiveAccountButton.assertIsDisplayed() }
            }

            step("Click on archive button") {
                onAccountDetailsScreen { archiveAccountButton.clickWithAssertion() }
            }

            step("Confirm archive in dialog") {
                onDialog { archiveButton.clickWithAssertion() }
            }

            step("Assert error alert mentions referral program") {
                onDialog {
                    dialogContainer.assertIsDisplayed()
                    title.assertIsDisplayed()
                    text.assertTextContains("referral program")
                }
            }
            step("Assert archive button is still visible after error") {
                onAccountDetailsScreen { archiveAccountButton.assertIsDisplayed() }
            }

            // step("Assert error dialog is displayed") {
            //     onDialog { dialogContainer.assertIsDisplayed() }
            // }
            // step("Assert error dialog has proper title") {
            //     onDialog { title.assertIsDisplayed() }
            //     onDialog { title.assertTextContains("Something went wrong") }
            // }

            step("Assert error dialog has explanatory text") {
                onDialog { text.assertIsDisplayed() }
                onDialog { text.assertTextContains(getResourceString(R.string.account_generic_error_dialog_message)) }
            }

            step("Assert error dialog has OK button") {
                onDialog { okButton.assertIsDisplayed() }
            }

        }
    }

    @Test
    @AllureId("5976")
    @DisplayName("Accounts: restore an archived account")
    fun restoreArchivedAccountTest() {
        // WireMock: user_tokens_api → "TwoAccountsWithArchivedAccounts", then switch to "ReadyToRestore"
        val archivedAccountName = "Account 3"

        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(apiScenario)
            }
        ).run {

            step("Set WireMock scenario to 'TwoAccountsWithArchivedAccounts'") {
                setWireMockScenarioState(apiScenario, "TwoAccountsWithArchivedAccounts")
            }
            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Open wallet details") {
                onMainScreenTopBar { moreButton.clickWithAssertion() }
            }
            step("Open 'Wallet settings' screen") {
                onDetailsScreen { walletNameButton.clickWithAssertion() }
            }

            Thread.sleep(15000)

            step("Click on 'Archived accounts' button") {
                onWalletSettingsScreen { openArchivedAccountsButton.clickWithAssertion() }
            }
            step("Assert archived account '$archivedAccountName' is displayed") {
                onArchivedAccountsScreen { findArchivedAccountItemByName(archivedAccountName).assertIsDisplayed() }
            }

            step("Switch WireMock to 'ReadyToRestore'") {
                setWireMockScenarioState(apiScenario, "ReadyToRestore")
            }
            step("Click restore button for '$archivedAccountName'") {
                onArchivedAccountsScreen { restoreAccountByName(archivedAccountName).clickWithAssertion() }
            }

            step("Assert wallet settings screen is displayed") {
                onWalletSettingsScreen { addAccountButton.assertIsDisplayed() }
            }
            step("Assert restored account '$archivedAccountName' appears in active accounts list") {
                onWalletSettingsScreen { accountItem(archivedAccountName).assertIsDisplayed() }
            }

            step("Click on 'Archived accounts' button again") {
                onWalletSettingsScreen { openArchivedAccountsButton.clickWithAssertion() }
            }
            step("Assert '$archivedAccountName' is no longer in archived list") {
                onArchivedAccountsScreen { findArchivedAccountItemByName(archivedAccountName).assertDoesNotExist() }
            }

        }
    }

    @Test
    @AllureId("5980")
    @DisplayName("Accounts: restore archived account with custom token transfer")
    fun restoreArchivedAccountWithCustomTokenTransferTest() {
        val archivedAccountName = "Account 3"

        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(apiScenario)
            }
        ).run {
            step("Set WireMock scenario to 'OneAccountWithArchivedCustomToken'") {
                setWireMockScenarioState(apiScenario, "OneAccountWithArchivedCustomToken")
            }
            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Open wallet details") {
                onMainScreenTopBar { moreButton.clickWithAssertion() }
            }
            step("Open 'Wallet settings' screen") {
                onDetailsScreen { walletNameButton.clickWithAssertion() }
            }

            step("Click on 'Archived accounts' button") {
                onWalletSettingsScreen { openArchivedAccountsButton.clickWithAssertion() }
            }
            step("Assert archived account '$archivedAccountName' is displayed") {
                onArchivedAccountsScreen { findArchivedAccountItemByName(archivedAccountName).assertIsDisplayed() }
            }

            step("Switch WireMock to 'ReadyToRestoreCustomToken'") {
                setWireMockScenarioState(apiScenario, "ReadyToRestoreCustomToken")
            }
            step("Click restore button for '$archivedAccountName'") {
                onArchivedAccountsScreen { restoreAccountByName(archivedAccountName).clickWithAssertion() }
            }

            step("Assert wallet settings screen is displayed") {
                onWalletSettingsScreen { addAccountButton.assertIsDisplayed() }
            }
            step("Assert restored account appears in active accounts list") {
                onWalletSettingsScreen { accountItem(archivedAccountName).assertIsDisplayed() }
            }

            step("Click on 'Archived accounts' button again") {
                onWalletSettingsScreen { openArchivedAccountsButton.clickWithAssertion() }
            }
            step("Assert '$archivedAccountName' is no longer in archived list") {
                onArchivedAccountsScreen { findArchivedAccountItemByName(archivedAccountName).assertDoesNotExist() }
            }
        }
    }

    @Test
    @AllureId("7962")
    @DisplayName("Accounts: restore archived account error")
    fun restoreArchivedAccountErrorTest() {
        val archivedAccountName = "Account 3"

        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(apiScenario)
            }
        ).run {
            step("Set WireMock scenario to 'TwoAccountsWithArchivedAccounts'") {
                setWireMockScenarioState(apiScenario, "TwoAccountsWithArchivedAccounts")
            }
            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Open wallet details") {
                onMainScreenTopBar { moreButton.clickWithAssertion() }
            }
            step("Open 'Wallet settings' screen") {
                onDetailsScreen { walletNameButton.clickWithAssertion() }
            }

            step("Click on 'Archived accounts' button") {
                onWalletSettingsScreen { openArchivedAccountsButton.clickWithAssertion() }
            }
            step("Assert archived account '$archivedAccountName' is displayed") {
                onArchivedAccountsScreen { findArchivedAccountItemByName(archivedAccountName).assertIsDisplayed() }
            }

            step("Switch WireMock to 'AccountsPutError' to simulate restore failure") {
                setWireMockScenarioState(apiScenario, "AccountsPutError")
            }
            step("Click restore button for '$archivedAccountName'") {
                onArchivedAccountsScreen { restoreAccountByName(archivedAccountName).clickWithAssertion() }
            }
            step("Assert error dialog is displayed") {
                onDialog { dialogContainer.assertIsDisplayed() }
            }
            step("Assert error dialog has proper title") {
                onDialog { title.assertIsDisplayed() }
            }
            step("Assert error dialog has explanatory text") {
                onDialog { text.assertIsDisplayed() }
            }
            step("Assert error dialog has OK button") {
                onDialog { okButton.assertIsDisplayed() }
            }
            step("Dismiss error dialog") {
                onDialog { okButton.clickWithAssertion() }
            }
            step("Assert archived account '$archivedAccountName' is still in archived list") {
                onArchivedAccountsScreen { findArchivedAccountItemByName(archivedAccountName).assertIsDisplayed() }
            }
        }
    }

    fun exceedingAmountOfAccountsByAccountRestore() {
    }

    fun accountArchivationUIElements() {
    }
}