package com.tangem.tests.accounts

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.REFERRAL_API_SCENARIO
import com.tangem.common.constants.TestConstants.USER_TOKENS_API_SCENARIO
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.utils.resetWireMockScenarioState
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.core.ui.R
import com.tangem.scenarios.*
import com.tangem.screens.accounts.onAccountDetailsScreen
import com.tangem.screens.accounts.onArchivedAccountsScreen
import com.tangem.screens.onDetailsScreen
import com.tangem.screens.onDialog
import com.tangem.screens.onMainScreen
import com.tangem.screens.onWalletSettingsScreen
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.kakaocup.kakao.common.utilities.getResourceString
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Test

@HiltAndroidTest
class AccountArchivationsTest : BaseTestCase() {

    private val userTokensScenario = USER_TOKENS_API_SCENARIO
    private val referralScenario = REFERRAL_API_SCENARIO

    @Test
    @AllureId("5979")
    @DisplayName("Accounts: Verify main account archivation button not available")
    fun mainAccountArchivationAttemptTest() {
        val mainAccountName = "Main account"

        setupHooks().run {
            step("Open 'Main Screen'") { openMainScreen() }
            step("Synchronize addresses") { synchronizeAddresses() }
            step("Open wallet settings") { openWalletSettingsScreen() }
            step("Open wallet account with name $mainAccountName") { openAccountDetails(mainAccountName) }
            step("Assert archive button is NOT displayed") {
                onAccountDetailsScreen { archiveAccountButton.assertDoesNotExist() }
            }
        }
    }

    @Test
    @AllureId("5974")
    @DisplayName("Accounts: archive a non-main account")
    fun archiveSuccessfullyAccountTest() {
        val accountToArchiveName = "Account 2"
        val userAccountsState = "TwoAccountsArchivable"

        setupHooks(
            additionalBeforeSection = {
                setWireMockScenarioState(userTokensScenario, userAccountsState)
            },
            additionalAfterSection = {
                resetWireMockScenarioState(userTokensScenario)
            },
        ).run {
            step("Open 'Main Screen'") { openMainScreen() }
            step("Synchronize addresses") { synchronizeAddresses() }
            step("Open wallet settings") { openWalletSettingsScreen() }
            step("Open wallet account with name $accountToArchiveName") {
                openAccountDetails(accountToArchiveName)
            }

            step("Assert 'Archive' button is displayed") {
                onAccountDetailsScreen { archiveAccountButton.assertIsDisplayed() }
            }
            step("Click on 'Archive' button") {
                onAccountDetailsScreen { archiveAccountButton.clickWithAssertion() }
            }
            step("Verify 'Archive confirmation' dialog appeared with all elements") {
                assertArchiveConfirmationDialog()
            }
            step("Click 'Archive' in confirmation menu") {
                onDialog { archiveButton.clickWithAssertion() }
            }

            step("Verify app returned to 'Wallet settings' screen") {
                onWalletSettingsScreen { addAccountButton.assertIsDisplayed() }
            }
            step("Verify archived account '$accountToArchiveName' is no longer listed") {
                onWalletSettingsScreen {
                    accountItem(accountToArchiveName).assertDoesNotExist()
                }
            }
        }
    }

    @Test
    @AllureId("6844")
    @DisplayName("Accounts: account archivation error on UI")
    fun archiveAccountErrorTest() {
        val accountToArchiveName = "Account 2"
        val userAccountsState = "TwoAccountsArchivable"
        val userAccountsErrorState = "AccountsPutError"

        setupHooks(
            additionalBeforeSection = {
                setWireMockScenarioState(userTokensScenario, userAccountsState)
            },
            additionalAfterSection = {
                resetWireMockScenarioState(userTokensScenario)
            },
        ).run {
            step("Open 'Main Screen'") { openMainScreen() }
            step("Synchronize addresses") { synchronizeAddresses() }
            step("Open wallet settings") { openWalletSettingsScreen() }
            step("Open wallet account with name $accountToArchiveName") {
                openAccountDetails(accountToArchiveName)
            }

            step("Forcing network error scenario") {
                setWireMockScenarioState(userTokensScenario, userAccountsErrorState)
            }
            step("Attempt to archive the account") { archiveAccount() }
            step("Assert error dialog details") {
                assertErrorDialog(
                    expectedTitle = getResourceString(R.string.common_something_went_wrong),
                    expectedMessage = getResourceString(R.string.account_generic_error_dialog_message),
                )
            }
        }
    }

    @Test
    @AllureId("5981")
    @DisplayName("Accounts: archive account with referral program error")
    fun archiveAccountReferralErrorTest() {
        val accountToArchiveName = "Account 2"
        val userAccountsState = "TwoAccountsArchivableAndReferral"
        val referralActiveState = "Participating"

        setupHooks(
            additionalBeforeSection = {
                setWireMockScenarioState(userTokensScenario, userAccountsState)
                setWireMockScenarioState(referralScenario, referralActiveState)
            },
            additionalAfterSection = {
                resetWireMockScenarioState(userTokensScenario)
                resetWireMockScenarioState(referralScenario)
            },
        ).run {
            step("Open 'Main Screen'") { openMainScreen() }
            step("Synchronize addresses") { synchronizeAddresses() }
            step("Open wallet settings") { openWalletSettingsScreen() }
            step("Open wallet account with name $accountToArchiveName") {
                openAccountDetails(accountToArchiveName)
            }
            step("Attempt to archive the account") { archiveAccount() }

            step("Assert error dialog details") {
                assertErrorDialog(
                    expectedTitle = getResourceString(R.string.account_could_not_archive_referral_program_title),
                    expectedMessage = getResourceString(R.string.account_could_not_archive_referral_program_message),
                )
            }
            step("Dismiss error dialog") { dismissErrorDialog() }
            step("Assert 'Archive' button is still visible after error") {
                onAccountDetailsScreen { archiveAccountButton.assertIsDisplayed() }
            }
        }
    }

    @Test
    @AllureId("5976")
    @DisplayName("Accounts: restore a simple archived account")
    fun restoreSimpleArchivedAccountTest() {
        val archivedAccountName = "Account 3"
        val userAccountsInitialState = "TwoAccountsWithArchivedAccounts"
        val userAccountsAfterArchivationState = "ReadyToRestore"

        setupHooks(
            additionalBeforeSection = {
                setWireMockScenarioState(userTokensScenario, userAccountsInitialState)
            },
            additionalAfterSection = {
                resetWireMockScenarioState(userTokensScenario)
            },
        ).run {
            step("Open 'Main Screen'") { openMainScreen() }
            step("Synchronize addresses") { synchronizeAddresses() }
            step("Open wallet settings") { openWalletSettingsScreen() }
            step("Open 'Archived accounts' screen") { openArchivedAccountsScreen() }
            step("Verify archived wallet account with name '$archivedAccountName' is present") {
                assertArchivedAccountIsDisplayed(archivedAccountName)
            }

            step("Switch WireMock to '$userAccountsAfterArchivationState' users scenario state") {
                setWireMockScenarioState(userTokensScenario, userAccountsAfterArchivationState)
            }
            step("Restore account with name '$archivedAccountName'") {
                restoreArchivedAccount(archivedAccountName)
            }

            step("Assert 'Wallet settings' screen is displayed") {
                onWalletSettingsScreen { addAccountButton.assertIsDisplayed() }
            }
            step("Assert restored account '$archivedAccountName' appears in 'Active accounts' list") {
                onWalletSettingsScreen { accountItem(archivedAccountName).assertIsDisplayed() }
            }
        }
    }

    @Test
    @AllureId("5980")
    @DisplayName("Accounts: restore archived account with custom token transfer")
    fun restoreArchivedAccountWithCustomTokensTest() {
        val mainAccountName = "Main account"
        val archivedAccountName = "Account 2"
        val customTokenName = "Ethereum"
        val expectedArchivedTokensInfo = "1 token"
        val userAccountsInitialState = "OneAccountWithArchivedCustomToken"
        val userAccountsReadyToRestoreState = "ReadyToRestoreCustomToken"

        setupHooks(
            additionalBeforeSection = {
                setWireMockScenarioState(userTokensScenario, userAccountsInitialState)
            },
            additionalAfterSection = {
                resetWireMockScenarioState(userTokensScenario)
            },
        ).run {
            step("Open 'Main Screen'") { openMainScreen() }
            step("Synchronize addresses") { synchronizeAddresses() }
            step("Open wallet settings") { openWalletSettingsScreen() }
            step("Open 'Archived accounts' screen") { openArchivedAccountsScreen() }

            step("Verify archived account '$archivedAccountName' shows '$expectedArchivedTokensInfo'") {
                onArchivedAccountsScreen {
                    val row = findArchivedAccountItemByName(archivedAccountName)
                    row.container.assertIsDisplayed()
                    row.subtitle.assertTextContains(expectedArchivedTokensInfo, substring = true)
                }
            }
            step("Switch WireMock to '$userAccountsReadyToRestoreState'") {
                setWireMockScenarioState(userTokensScenario, userAccountsReadyToRestoreState)
            }
            step("Click restore button for '$archivedAccountName'") {
                onArchivedAccountsScreen {
                    findArchivedAccountItemByName(archivedAccountName)
                        .restoreButton.clickWithAssertion()
                }
            }

            step("Assert custom token migration dialog is displayed") {
                onDialog { dialogContainer.assertIsDisplayed() }
            }
            step("Assert dialog text mentions main account '$mainAccountName'") {
                onDialog { text.assertTextContains(mainAccountName, substring = true) }
            }
            step("Assert dialog text mentions restoring account '$archivedAccountName'") {
                onDialog { text.assertTextContains(archivedAccountName, substring = true) }
            }
            step("Confirm migration in dialog") {
                onDialog { gotItButton.clickWithAssertion() }
            }

            step("Assert 'Wallet settings' screen is displayed") {
                onWalletSettingsScreen { addAccountButton.assertIsDisplayed() }
            }
            step("Assert restored account '$archivedAccountName' is in active accounts list") {
                onWalletSettingsScreen { accountItem(archivedAccountName).assertIsDisplayed() }
            }
            step("Navigate back to wallet details") {
                onWalletSettingsScreen { topAppBarBackButton.clickWithAssertion() }
            }
            step("Navigate back to main screen") {
                onDetailsScreen { topAppBarBackButton.clickWithAssertion() }
            }

            step("Assert main account '$mainAccountName' is visible on main screen") {
                onMainScreen { findAccountSectionByName(mainAccountName).assertIsDisplayed() }
            }
            step("Assert restored account '$archivedAccountName' is visible on main screen") {
                onMainScreen { findAccountSectionByName(archivedAccountName).assertIsDisplayed() }
            }

            step("Expand main account '$mainAccountName'") {
                onMainScreen { findAccountSectionByName(mainAccountName).clickWithAssertion() }
            }
            step("Assert '$customTokenName' is NOT displayed under main account") {
                onMainScreen { findTokenInAnyAccountByName(customTokenName).assertDoesNotExist() }
            }
            step("Expand main account '$mainAccountName'") {
                onMainScreen { findAccountSectionByName(mainAccountName).clickWithAssertion() }
            }
            step("Assert '$customTokenName' is NOT displayed under main account") {
                onMainScreen {
                    findTokenInAnyAccountByName(customTokenName).assertDoesNotExist()
                }
            }

            step("Expand restored account '$archivedAccountName'") {
                onMainScreen { findAccountSectionByName(archivedAccountName).clickWithAssertion() }
            }
            step("Assert '$customTokenName' IS displayed under restored account") {
                onMainScreen {
                    findTokenInAnyAccountByName(customTokenName).assertIsDisplayed()
                }
            }
        }
    }

    @Test
    @AllureId("7962")
    @DisplayName("Accounts: restore archived account error")
    fun restoreArchivedAccountErrorTest() {
        val archivedAccountName = "Account 3"
        val userAccountsInitialState = "TwoAccountsWithArchivedAccounts"
        val userAccountsRestorationErrorState = "AccountsPutError"

        setupHooks(
            additionalBeforeSection = {
                setWireMockScenarioState(userTokensScenario, userAccountsInitialState)
            },
            additionalAfterSection = {
                resetWireMockScenarioState(userTokensScenario)
            },
        ).run {
            step("Open 'Main Screen'") { openMainScreen() }
            step("Synchronize addresses") { synchronizeAddresses() }
            step("Open wallet settings") { openWalletSettingsScreen() }
            step("Open 'Archived accounts' screen") { openArchivedAccountsScreen() }
            step("Verify archived wallet account with name '$archivedAccountName' is present") {
                assertArchivedAccountIsDisplayed(archivedAccountName)
            }

            step("Switch WireMock to '$userAccountsRestorationErrorState' user" +
                "accounts scenario state to simulate restoration failure") {
                setWireMockScenarioState(userTokensScenario, userAccountsRestorationErrorState)
            }
            step("Attempt restore account with name '$archivedAccountName'") {
                restoreArchivedAccount(archivedAccountName)
            }

            step("Assert error dialog details") {
                assertErrorDialog(
                    expectedTitle = getResourceString(R.string.common_something_went_wrong),
                    expectedMessage = getResourceString(R.string.account_generic_error_dialog_message),
                )
            }
            step("Dismiss error dialog") { dismissErrorDialog() }

            step("Assert archived account '$archivedAccountName' is still in archived list") {
                onArchivedAccountsScreen {
                    findArchivedAccountItemByName(archivedAccountName)
                        .container.assertIsDisplayed()
                }
            }
        }
    }

}
