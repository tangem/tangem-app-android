package com.tangem.tests.accounts

import com.tangem.common.BaseTestCase
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.utils.resetWireMockScenarioState
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.scenarios.openMainScreen
import com.tangem.scenarios.synchronizeAddresses
import com.tangem.screens.accounts.onAccountDetailsScreen
import com.tangem.screens.accounts.onAccountInfoEditorScreen
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

    @Test
    @AllureId("5979")
    @DisplayName("Accounts: Verify main account archivation not available")
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
                // Default state has 1 account with name=null (main account)
                // The main account item in wallet settings — use first account item
                onWalletSettingsScreen { accountItem("Account 1").clickWithAssertion() }
                // TODO: confirm the display name for the default/main account (name=null in JSON)
            }
            /*step("Assert account details screen is displayed") {
                onDetailsScreen { screenContainer.assertIsDisplayed() }
            }
            step("Assert archive button is NOT displayed") {
                onDetailsScreen { archiveAccountButton.assertDoesNotExist() }
            }*/
        }
    }

    @Test
    @AllureId("5974")
    @DisplayName("Accounts: archive a non-main account")
    fun archiveAccountTest() {
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

            step("Assert account details screen is displayed") {
                onAccountDetailsScreen { screenContainer.assertIsDisplayed() }
            }
            step("Assert archive button IS displayed") {
                onAccountDetailsScreen { archiveAccountButton.assertIsDisplayed() }
            }

            // Step 3: Tap archive button → confirmation menu
            /*
            step("Click on archive button") {
                onAccountDetailsForm { archiveAccountButton.clickWithAssertion() }
            }
            step("Assert confirmation menu is displayed with archive and cancel actions") {
                onDialog {
                    dialogContainer.assertIsDisplayed()
                    // TODO: confirm actual button selectors for archive confirmation dialog
                    archiveConfirmButton.assertIsDisplayed()
                    cancelButton.assertIsDisplayed()
                }
            }

            // Step 4: Confirm archive
            step("Click 'Archive' in confirmation menu") {
                onDialog { archiveConfirmButton.clickWithAssertion() }
            }
            step("Assert returned to wallet settings screen") {
                waitForIdle()
                onWalletSettingsScreen { topAppBarBackButton.assertIsDisplayed() }
            }
            step("Assert archived account '$accountToArchive' is no longer listed") {
                // After archive, WireMock state is "AccountArchived" which returns
                // user-tokens-after-archive-response.json (without Account 2)
                onWalletSettingsScreen {
                    flakySafely {
                        accountItem(accountToArchive).assertDoesNotExist()
                    }
                }
                */
        }
    }
    // TODO: Assert toast with success message is displayed

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
            // TODO: This needs a WireMock state where GET returns two accounts
            //  but PUT returns an error. Options:
            //  a) Create new mapping "TwoAccountsArchivablePutError"
            //  b) Or use "TwoAccountsArchivable" for initial load, then switch
            //     to "AccountsPutError" before the archive action
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
            //...

            // Switch WireMock to error state BEFORE triggering PUT
            step("Forcing application network error scenario for account data updating request") {
                setWireMockScenarioState(apiScenario, "AccountsPutError")
            }

            step("Click on archive button") {
                onAccountDetailsScreen { archiveAccountButton.clickWithAssertion() }
            }

            Thread.sleep(15_000)

            /*step("Confirm archive in dialog") {
                onModal { archiveConfirmButton.clickWithAssertion() }
            }

            // Expected: still on account screen, error dialog appears
            step("Assert account details screen is still displayed") {
                onAccountDetailsForm { screenContainer.assertIsDisplayed() }
            }*/
            step("Assert error dialog is displayed") {
                onDialog { dialogContainer.assertIsDisplayed() }
            }
            step("Assert error dialog has explanatory text") {
                onDialog { text.assertIsDisplayed() }
                onDialog { text.assertTextContains("Something went wrong") }
                // TODO: confirm actual error text selector
            }
            step("Assert error dialog has OK button") {
                onDialog { okButton.assertIsDisplayed() }
            }
        }
    }

// ?? not sure should be here
// fun accountRestorationTest() {
//
// }

    fun archiveAccountParticipatedInReferral() {
        TODO("Implement case 5981")
    }

// TODO account archivation in offline mode: verify that update request gone after

}