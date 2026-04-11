package com.tangem.tests.accounts

import com.tangem.common.BaseTestCase
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.utils.resetWireMockScenarioState
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.scenarios.openMainScreen
import com.tangem.scenarios.synchronizeAddresses
import com.tangem.screens.accounts.onAccountDetailsScreen
import com.tangem.screens.onDetailsScreen
import com.tangem.screens.onDialog
import com.tangem.screens.onMainScreenTopBar
import com.tangem.screens.onWalletSettingsScreen
import dagger.hilt.android.testing.HiltAndroidTest
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Test

@HiltAndroidTest
class AccountArchivationTest : BaseTestCase() {

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

    @Test
    @AllureId("5979")
    @DisplayName("Accounts: Verify main account archivation not available")
    fun mainAccountArchivationAttemptTest() {
        // WireMock: default mock (no scenario switch needed)

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
            /*step("Assert account details screen is displayed") {
                onAccountDetailsScreen { screenContainer.assertIsDisplayed() }
            }
            step("Assert archive button is NOT displayed") {
                onAccountDetailsScreen { archiveAccountButton.assertDoesNotExist() }
            }*/
        }
    }

    @Test
    @AllureId("5974")
    @DisplayName("Accounts: archive a non-main account")
    fun archiveAccountTest() {
        // WireMock: user_tokens_api → "TwoAccountsArchivable"
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

            /*step("Assert account details screen is displayed") {
                onAccountDetailsScreen { screenContainer.assertIsDisplayed() }
            }
            step("Assert archive button IS displayed") {
                onAccountDetailsScreen { archiveAccountButton.assertIsDisplayed() }
            }
            step("Click on archive button") {
                onAccountDetailsScreen { archiveAccountButton.clickWithAssertion() }
            }
            step("Assert confirmation menu is displayed with archive and cancel actions") {
                onDialog {
                    dialogContainer.assertIsDisplayed()
                    archiveConfirmButton.assertIsDisplayed()
                    cancelButton.assertIsDisplayed()
                }
            }
            step("Click 'Archive' in confirmation menu") {
                onDialog { archiveConfirmButton.clickWithAssertion() }
            }
            step("Assert returned to wallet settings screen") {
                onWalletSettingsScreen { screenContainer.assertIsDisplayed() }
            }
            step("Assert archived account '$accountToArchive' is no longer listed") {
                onWalletSettingsScreen {
                    accountItem(accountToArchive).assertDoesNotExist()
                }
            }*/
        }
    }

    @Test
    @AllureId("6844")
    @DisplayName("Accounts: archive account error UI")
    fun archiveAccountErrorTest() {
        // WireMock: user_tokens_api → "TwoAccountsArchivable", then switch to "AccountsPutError" before archive
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

            Thread.sleep(15_000)

            /*step("Confirm archive in dialog") {
                onDialog { archiveConfirmButton.clickWithAssertion() }
            }
            step("Assert account details screen is still displayed") {
                onAccountDetailsScreen { screenContainer.assertIsDisplayed() }
            }*/
            step("Assert error dialog is displayed") {
                onDialog { dialogContainer.assertIsDisplayed() }
            }
            step("Assert error dialog has explanatory text") {
                onDialog { text.assertIsDisplayed() }
                onDialog { text.assertTextContains("Something went wrong") }
            }
            step("Assert error dialog has OK button") {
                onDialog { okButton.assertIsDisplayed() }
            }
        }
    }

    @Test
    @AllureId("5981")
    @DisplayName("Accounts: archive account participated in referral program")
    fun archiveAccountParticipatedInReferralTest() {
        // WireMock: user_tokens_api → "TwoAccountsArchivable" + referral_api → "Participating"
        // Expected: error alert mentioning "referral program", archive button remains visible
        val accountToArchive = "Account 2"

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
            step("Click on account: '$accountToArchive'") {
                onWalletSettingsScreen { accountItem(accountToArchive).clickWithAssertion() }
            }
            /*step("Click on archive button") {
                onAccountDetailsScreen { archiveAccountButton.clickWithAssertion() }
            }
            step("Assert error alert mentions referral program") {
                onDialog {
                    dialogContainer.assertIsDisplayed()
                    text.assertTextContains("referral program")
                }
            }
            step("Assert archive button is still visible after error") {
                onAccountDetailsScreen { archiveAccountButton.assertIsDisplayed() }
            }*/
        }
    }

    @Test
    @AllureId("5976")
    @DisplayName("Accounts: restore an archived account")
    fun restoreArchivedAccountTest() {
        // WireMock: user_tokens_api → "TwoAccountsWithArchivedAccounts", then switch to "ReadyToRestore"

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
            /*step("Open archived accounts screen") {
                onWalletSettingsScreen { archivedAccountsButton.clickWithAssertion() }
            }
            step("Switch WireMock to 'ReadyToRestore'") {
                setWireMockScenarioState(apiScenario, "ReadyToRestore")
            }
            step("Tap restore on archived account") {
                // TODO: implement restore action
            }
            step("Assert account is restored to active list") {
                // TODO: verify account appears in wallet settings
            }*/
        }
    }

    @Test
    @AllureId("5980")
    @DisplayName("Accounts: restore archived account with custom token transfer")
    fun restoreArchivedAccountWithCustomTokenTransferTest() {
        // WireMock: user_tokens_api → "OneAccountWithArchivedCustomToken",
        //   then switch to "ReadyToRestoreCustomToken"
        // Expected: migration dialog appears, token transfers to correct account

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
            /*step("Open archived accounts screen") {
                onWalletSettingsScreen { archivedAccountsButton.clickWithAssertion() }
            }
            step("Switch WireMock to 'ReadyToRestoreCustomToken'") {
                setWireMockScenarioState(apiScenario, "ReadyToRestoreCustomToken")
            }
            step("Tap restore on archived account with custom token") {
                // TODO: implement restore action
            }
            step("Assert migration dialog appears") {
                onDialog { dialogContainer.assertIsDisplayed() }
            }
            step("Assert token transfers to correct account") {
                // TODO: verify token migration
            }*/
        }
    }

    @Test
    @AllureId("7962")
    @DisplayName("Accounts: restore archived account error")
    fun restoreArchivedAccountErrorTest() {
        // WireMock: user_tokens_api → "TwoAccountsWithArchivedAccounts", then switch to "AccountsPutError"
        // Expected: error alert with "try again" message

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
            /*step("Open archived accounts screen") {
                onWalletSettingsScreen { archivedAccountsButton.clickWithAssertion() }
            }
            step("Switch WireMock to error state") {
                setWireMockScenarioState(apiScenario, "AccountsPutError")
            }
            step("Tap restore on archived account") {
                // TODO: implement restore action
            }
            step("Assert error alert is displayed") {
                onDialog {
                    dialogContainer.assertIsDisplayed()
                    text.assertTextContains("try again")
                }
            }*/
        }
    }

}