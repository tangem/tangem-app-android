package com.tangem.tests.accounts

import androidx.compose.ui.test.longClick
import com.tangem.common.BaseTestCase
import com.tangem.common.R
import com.tangem.common.extensions.clickAndWaitFor
import com.tangem.common.extensions.clickOnSystemButton
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.utils.DerivationPathHelper
import com.tangem.common.utils.resetWireMockScenarioState
import com.tangem.common.utils.setClipboardText
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.scenarios.*
import com.tangem.screens.*
import com.tangem.screens.accounts.onAccountInfoEditorScreen
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.kakaocup.kakao.common.utilities.getResourceString
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Assert.assertTrue
import org.junit.Test

@HiltAndroidTest
class AccountCreationTest : BaseTestCase() {

    private val userTokensScenario = "user_tokens_api"

    @Test
    @AllureId("5504")
    @DisplayName("Accounts: account creation network error handling")
    fun accountCreationErrorTest() {
        val accountName = "Account 2"
        val userAccountsGetErrorState = "AccountsGetError"
        val userAccountsPutErrorState = "AccountsPutError"
        val userAccountsBeforeCreationState = "AccountReadyToCreate"

        setupHooks(
            additionalBeforeSection = {
                setWireMockScenarioState(userTokensScenario, userAccountsGetErrorState)
            },
            additionalAfterSection = {
                resetWireMockScenarioState(userTokensScenario)
            },
        ).run {

            step("Open 'Main Screen'") { openMainScreen() }
            step("Synchronize addresses") { synchronizeAddresses() }
            step("Open wallet settings") { openWalletSettingsScreen() }
            step("Start account creation") { startAccountCreation() }

            step("Enter account name: '$accountName'") {
                onAccountInfoEditorScreen {
                    accountNameField.performClick()
                    accountNameField.performTextInput(accountName)
                }
            }
            step("Click 'Add account' button (GET accounts is blocked)") {
                onAccountInfoEditorScreen {
                    saveAccountButton.clickAndWaitFor(
                        rule = composeTestRule,
                        expectedCondition = {
                            onDialog { dialogContainer.assertIsDisplayed() }
                        },
                    )
                }
            }
            step("Assert error dialog details") {
                assertErrorDialog(
                    expectedTitle = getResourceString(R.string.common_something_went_wrong),
                    expectedMessage = getResourceString(com.tangem.core.ui.R.string.account_generic_error_dialog_message),
                )
            }
            step("Dismiss error dialog") { dismissErrorDialog() }
            step("Assert still on account creation screen") {
                onAccountInfoEditorScreen { screenContainer.assertIsDisplayed() }
            }

            step("Unblock GET accounts, block PUT accounts") {
                setWireMockScenarioState(userTokensScenario, userAccountsPutErrorState)
            }
            step("Click 'Add account' button again (PUT accounts is blocked)") {
                onAccountInfoEditorScreen {
                    saveAccountButton.clickAndWaitFor(
                        rule = composeTestRule,
                        expectedCondition = {
                            onDialog { dialogContainer.assertIsDisplayed() }
                        },
                    )
                }
            }
            step("Assert still on account creation screen") {
                assertErrorDialog(
                    expectedTitle = getResourceString(R.string.common_something_went_wrong),
                    expectedMessage = getResourceString(R.string.account_generic_error_dialog_message),
                )
            }

            step("Unblock both 'accounts' requests") {
                setWireMockScenarioState(userTokensScenario, userAccountsBeforeCreationState)
            }

            step("Dismiss error dialog") { dismissErrorDialog() }
            step("Assert still on account creation screen") {
                onAccountInfoEditorScreen { screenContainer.assertIsDisplayed() }
            }

            step("Click 'Add account' button again (both requests unblocked)") {
                onAccountInfoEditorScreen {
                    saveAccountButton.clickAndWaitFor(
                        rule = composeTestRule,
                        expectedCondition = {
                            onManageTokensScreen { topAppBarTitle.assertIsDisplayed() }
                        },
                    )
                }
            }
            step("Assert 'Manage Tokens' title is displayed") {
                onManageTokensScreen { topAppBarTitle.assertIsDisplayed() }
            }
            step("Close 'Manage Tokens' screen") {
                onManageTokensScreen { topAppBarBackButton.clickWithAssertion() }
            }
            step("Assert 'Wallet settings' screen is displayed") {
                onWalletSettingsScreen { addAccountButton.assertIsDisplayed() }
            }
            step("Assert new account '$accountName' appears in accounts list") {
                onWalletSettingsScreen { accountItem(accountName).assertIsDisplayed() }
            }
        }
    }

    @Test
    @AllureId("5507")
    @DisplayName("Accounts: name field verifications")
    fun accountsCreationNameFieldValidationTest() {
        val accountName = "TestAccount12"
        val longName = "A".repeat(21)
        val emptyPlaceholderValue = "New account"
        val editedName = "Edited"
        val context = device.context
        val pasteButtonName = "Paste"

        setupHooks().run {
            step("Set clipboard text '$longName'") {
                setClipboardText(context,longName)
            }
            step("Open 'Main Screen'") { openMainScreen() }
            step("Synchronize addresses") { synchronizeAddresses() }
            step("Open 'Wallet settings' screen") { openWalletSettingsScreen() }
            step("Click on 'Add account' button") {
                onWalletSettingsScreen { addAccountButton.clickWithAssertion() }
            }
            step("Assert 'Edit account details' dialog screen appears") {
                onAccountInfoEditorScreen { screenContainer.assertIsDisplayed() }
            }

            step("Enter account name manually: '$accountName'") {
                onAccountInfoEditorScreen {
                    accountNameField.performClick()
                    accountNameField.performTextInput(accountName)
                }
            }
            step("Assert name input is stable (keyboard doesn't flicker)") {
                onAccountInfoEditorScreen {
                    accountNameField.assertTextContains(accountName)
                }
            }
            step("Assert 'Add account' button is enabled") {
                onAccountInfoEditorScreen {
                    saveAccountButton.assertIsEnabled()
                }
            }

            step("Clear the 'Edit name' field") {
                onAccountInfoEditorScreen {
                    accountNameField.performTextClearance()
                }
            }
            step("Assert 'Add account' button becomes inactive when field is empty") {
                onAccountInfoEditorScreen {
                    saveAccountButton.assertIsNotEnabled()
                }
            }
            step("Paste name from clipboard: '$accountName'") {
                onAccountInfoEditorScreen {
                    accountNameField.performTextReplacement(accountName)
                }
            }
            step("Assert pasted text is displayed in 'Account name' field") {
                onAccountInfoEditorScreen {
                    accountNameField.assertTextContains(accountName)
                }
            }
            step("Assert 'Add account' button is enabled") {
                onAccountInfoEditorScreen {
                    saveAccountButton.assertIsEnabled()
                }
            }
            step("Edit the entered name (clear and retype)") {
                onAccountInfoEditorScreen {
                    accountNameField.performTextReplacement(editedName)
                }
            }
            step("Assert edited name in 'Account name' field is displayed") {
                onAccountInfoEditorScreen {
                    accountNameField.assertTextContains(editedName)
                }
            }
            step("Delete all text and leave 'Account name' field empty") {
                onAccountInfoEditorScreen {
                    accountNameField.performTextClearance()
                }
            }
            step("Assert 'Add account' button is inactive") {
                onAccountInfoEditorScreen {
                    saveAccountButton.assertIsNotEnabled()
                }
            }
            step("Type name with more than 20 symbols") {
                onAccountInfoEditorScreen {
                    accountNameField.performTextReplacement(longName)
                }
            }
            step("Assert text over 20 symbols was not pasted and placeholder remains empty") {
                onAccountInfoEditorScreen {
                    accountNameField.assertTextContains(emptyPlaceholderValue, substring = true)
                }
            }
            step("Assert 'Add account' button is inactive") {
                onAccountInfoEditorScreen {
                    saveAccountButton.assertIsNotEnabled()
                }
            }
            step("Clear text field") {
                onAccountInfoEditorScreen { accountNameField.performTextClearance() }
            }
            step("Paste text longer than 20 characters to 'Account name' field") {
                onAccountInfoEditorScreen {
                    accountNameField.performTouchInput { longClick(durationMillis = 2_000L) }
                }
            }
            step("Click on system 'Paste' button to paste clipboard text") {
                clickOnSystemButton(pasteButtonName)
            }
            step("Assert text over 20 symbols was not pasted and placeholder remains empty") {
                onAccountInfoEditorScreen {
                    accountNameField.assertTextContains(emptyPlaceholderValue, substring = true)
                }
            }
            step("Assert 'Add account' button is inactive") {
                onAccountInfoEditorScreen {
                    saveAccountButton.assertIsNotEnabled()
                }
            }
        }
    }

    @Test
    @AllureId("5505")
    @DisplayName(
        "Accounts: check unsaved changes notification " +
            "after attempt to close edited account creation form"
    )
    fun accountsCreationUnsavedChangesForNameFieldNotificationTest() {
        val accountName = "Hikarik Test"

        setupHooks().run {
            step("Open 'Main Screen'") { openMainScreen() }
            step("Synchronize addresses") { synchronizeAddresses() }
            step("Open 'Wallet settings' screen") { openWalletSettingsScreen() }
            step("Click on 'Add account' button") {
                onWalletSettingsScreen { addAccountButton.clickWithAssertion() }
            }
            step("Assert edit account details dialog screen appears") {
                onAccountInfoEditorScreen { screenContainer.assertIsDisplayed() }
            }

            step("Enter account name manually: '$accountName'") {
                onAccountInfoEditorScreen {
                    accountNameField.performClick()
                    accountNameField.performTextInput(accountName)
                }
            }
            step("Tap 'Cross' button to attempt closing the screen") {
                onAccountInfoEditorScreen {
                    crossButton.clickWithAssertion()
                }
            }
            step("Verify 'Unsaved changes' screen parts") {
                checkUnsavedChangesCreationModal()
            }

            step("Tap 'Keep Editing' button to stay on screen") {
                onDialog { keepEditButton.clickWithAssertion() }
            }
            step("Assert app still on 'Create account' screen") {
                onAccountInfoEditorScreen { screenContainer.assertIsDisplayed() }
            }
            step("Assert previously entered data is preserved") {
                onAccountInfoEditorScreen {
                    accountNameField.assertTextContains(accountName)
                }
            }

            step("Tap 'Cross' button to attempt closing the screen") {
                onAccountInfoEditorScreen { crossButton.clickWithAssertion() }
            }
            step("Assert 'Unsaved changes' alert is displayed again") {
                onDialog { dialogContainer.assertIsDisplayed() }
            }
            step("Tap 'Discard' button to discard and close") {
                onDialog { discardButton.clickWithAssertion() }
            }
            step("Assert 'Create account' screen is closed and 'Wallet settings' displayed again") {
                onWalletSettingsScreen {
                    screenContainer.assertIsDisplayed()
                }
            }
            step("Verify no new account has appeared in the list") {
                onWalletSettingsScreen {
                    accountItem(accountName).assertDoesNotExist()
                }
            }
        }
    }

    @Test
    @AllureId("5502")
    @DisplayName("Accounts: account creation, accounts mode and per-account token derivation")
    fun accountCreationAndDerivationTest() {
        val createdAccountName = "Account 2"
        val accountReadyState = "AccountReadyToCreateDerivation"
        val accountIndex = "1"
        val btcTokenName = "Bitcoin"
        val ethTokenName = "Ethereum"

        setupHooks(
            additionalBeforeSection = {
                setWireMockScenarioState(userTokensScenario, accountReadyState)
            },
            additionalAfterSection = {
                resetWireMockScenarioState(userTokensScenario)
            },
        ).run {
            step("Open 'Main Screen'") { openMainScreen() }
            step("Synchronize addresses") { synchronizeAddresses() }
            step("Open wallet settings") { openWalletSettingsScreen() }
            step("Start account creation") { startAccountCreation() }

            step("Enter account name: '$createdAccountName'") {
                onAccountInfoEditorScreen {
                    accountNameField.performClick()
                    accountNameField.performTextInput(createdAccountName)
                }
            }
            step("Assert account creation screen with derivation hint is displayed") {
                onAccountInfoEditorScreen { screenContainer.assertIsDisplayed() }
            }

            step("Click 'Add account' and wait for 'Manage Tokens'") {
                onAccountInfoEditorScreen {
                    saveAccountButton.clickAndWaitFor(
                        rule = composeTestRule,
                        expectedCondition = {
                            onManageTokensScreen { topAppBarTitle.assertIsDisplayed() }
                        },
                    )
                }
            }

            step("Close 'Manage Tokens' screen") {
                onManageTokensScreen { topAppBarBackButton.clickWithAssertion() }
            }
            step("Assert 'Wallet settings' screen is displayed") {
                onWalletSettingsScreen { addAccountButton.assertIsDisplayed() }
            }
            step("Assert new account '$createdAccountName' appears (last) in accounts list") {
                onWalletSettingsScreen { accountItem(createdAccountName).assertIsDisplayed() }
            }
            step("Navigate back to wallet details") {
                onWalletSettingsScreen { topAppBarBackButton.clickWithAssertion() }
            }
            step("Navigate back to main screen") {
                onDetailsScreen { topAppBarBackButton.clickWithAssertion() }
            }

            step("Assert accounts mode is on main: account '$createdAccountName' section is visible") {
                onMainScreen { findAccountSectionByName(createdAccountName).assertIsDisplayed() }
            }

            step("Assert per-account token derivation paths from the domain account model") {
                val account = awaitCryptoPortfolioAccount(derivationIndex = accountIndex.toInt())

                val btcPaths = account.derivationPathsForToken(btcTokenName)
                assertTrue(
                    "Expected a $btcTokenName derivation with 3rd node = $accountIndex' (account index). Paths: $btcPaths",
                    btcPaths.any { DerivationPathHelper.nodeAt(it, index1Based = 3) == "$accountIndex'" },
                )

                val ethPaths = account.derivationPathsForToken(ethTokenName)
                assertTrue(
                    "Expected an $ethTokenName derivation with 5th node = $accountIndex (account index). Paths: $ethPaths",
                    ethPaths.any { DerivationPathHelper.nodeAt(it, index1Based = 5) == accountIndex },
                )
            }
        }
    }

    @Test
    @AllureId("8746")
    @DisplayName("Accounts: empty account placeholder and 'Add tokens' entry to manage tokens")
    fun emptyAccountPlaceholderTest() {
        val createdAccountName = "Account 2"
        val accountReadyState = "AccountReadyToCreateEmpty"

        setupHooks(
            additionalBeforeSection = {
                setWireMockScenarioState(userTokensScenario, accountReadyState)
            },
            additionalAfterSection = {
                resetWireMockScenarioState(userTokensScenario)
            },
        ).run {
            step("Open 'Main Screen'") { openMainScreen() }
            step("Synchronize addresses") { synchronizeAddresses() }
            step("Open wallet settings") { openWalletSettingsScreen() }
            step("Start account creation") { startAccountCreation() }

            step("Enter account name: '$createdAccountName'") {
                onAccountInfoEditorScreen {
                    accountNameField.performClick()
                    accountNameField.performTextInput(createdAccountName)
                }
            }
            step("Click on 'Add account' and wait for 'Manage Tokens'") {
                onAccountInfoEditorScreen {
                    saveAccountButton.clickAndWaitFor(
                        rule = composeTestRule,
                        expectedCondition = {
                            onManageTokensScreen { topAppBarTitle.assertIsDisplayed() }
                        },
                    )
                }
            }
            step("Close 'Manage Tokens' without adding any token") {
                onManageTokensScreen { topAppBarBackButton.clickWithAssertion() }
            }
            step("Assert 'Wallet settings' screen is displayed") {
                onWalletSettingsScreen { addAccountButton.assertIsDisplayed() }
            }
            step("Assert new empty account '$createdAccountName' appears in accounts list") {
                onWalletSettingsScreen { accountItem(createdAccountName).assertIsDisplayed() }
            }
            step("Navigate back to wallet details") {
                onWalletSettingsScreen { topAppBarBackButton.clickWithAssertion() }
            }
            step("Navigate back to main screen") {
                onDetailsScreen { topAppBarBackButton.clickWithAssertion() }
            }
            step("Expand empty account '$createdAccountName' section") {
                onMainScreen {
                    scrollToAccountSection(createdAccountName)
                    findAccountSectionByName(createdAccountName).clickAndWaitFor(
                        rule = composeTestRule,
                        expectedCondition = {
                            onMainScreen { emptyAccountTokensPlaceholder.assertIsDisplayed() }
                        },
                    )
                }
            }
            step("Assert empty tokens placeholder is displayed") {
                onMainScreen { emptyAccountTokensPlaceholder.assertIsDisplayed() }
            }
            step("Assert 'Add tokens' button is displayed under the placeholder") {
                onMainScreen { emptyAccountAddTokensButton.assertIsDisplayed() }
            }
            step("Click on 'Add tokens' button") {
                onMainScreen {
                    emptyAccountAddTokensButton.clickAndWaitFor(
                        rule = composeTestRule,
                        expectedCondition = {
                            onManageTokensScreen { topAppBarTitle.assertIsDisplayed() }
                        },
                    )
                }
            }
            step("Assert 'Manage Tokens' screen is opened for the account") {
                onManageTokensScreen { topAppBarTitle.assertIsDisplayed() }
            }
        }
    }
}