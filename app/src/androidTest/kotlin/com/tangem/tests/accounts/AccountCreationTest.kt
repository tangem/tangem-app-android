package com.tangem.tests.accounts

import com.tangem.common.BaseTestCase
import com.tangem.common.R
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.utils.resetWireMockScenarioState
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.scenarios.*
import com.tangem.screens.accounts.onAccountInfoEditorScreen
import com.tangem.screens.onDialog
import com.tangem.screens.onManageTokensScreen
import com.tangem.screens.onWalletSettingsScreen
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.kakaocup.kakao.common.utilities.getResourceString
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Test

@HiltAndroidTest
class AccountCreationTest : BaseTestCase() {

    private val userTokensScenario = "user_tokens_api"

    @Test
    @AllureId("5504")
    @DisplayName("Accounts: account creation network error handling")
    fun accountCreationErrorTest() {
        val accountName = "New Account"
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
            // --- Phase 0: Initial setup and reaching screens ---
            step("Open 'Main Screen'") { openMainScreen() }
            step("Synchronize addresses") { synchronizeAddresses() }
            step("Open wallet settings") { openWalletSettingsScreen() }
            step("Start account creation") { startAccountCreation() }

            // --- Phase 1: GET accounts fails ---
            step("Enter account name: '$accountName'") {
                onAccountInfoEditorScreen {
                    accountNameField.performClick()
                    accountNameField.performTextInput(accountName)
                }
            }
            step("Click 'Add account' button (GET accounts is blocked)") {
                onAccountInfoEditorScreen { saveAccountButton.clickWithAssertion() }
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

            // --- Phase 2: GET succeeds, PUT accounts fails ---
            step("Unblock GET accounts, block PUT accounts") {
                setWireMockScenarioState(userTokensScenario, userAccountsPutErrorState)
            }
            step("Click 'Add account' button again (PUT accounts is blocked)") {
                onAccountInfoEditorScreen { saveAccountButton.clickWithAssertion() }
            }
            step("Assert still on account creation screen") {
                assertErrorDialog(
                    expectedTitle = getResourceString(R.string.common_something_went_wrong),
                    expectedMessage = getResourceString(R.string.account_generic_error_dialog_message),
                )
            }
            step("Dismiss error dialog") { dismissErrorDialog() }
            step("Assert still on account creation screen") {
                onAccountInfoEditorScreen { screenContainer.assertIsDisplayed() }
            }

            // --- Phase 3: Both requests succeed ---
            step("Unblock both 'accounts' requests") {
                setWireMockScenarioState(userTokensScenario, userAccountsBeforeCreationState)
            }
            step("Click 'Add account' button again (both requests unblocked)") {
                onAccountInfoEditorScreen { saveAccountButton.clickWithAssertion() }
            }
            step("Close 'Manage Tokens' screen") {
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

        setupHooks().run {
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
            step("Assert 'Add account' button becomes active") {
                onAccountInfoEditorScreen {
                    saveAccountButton.assertIsEnabled()
                }
            }

            step("Clear the 'Edit name' field") {
                onAccountInfoEditorScreen {
                    accountNameField.performTextReplacement("")
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
            step("Assert 'Add account' button becomes active after paste") {
                onAccountInfoEditorScreen {
                    saveAccountButton.assertIsDisplayed()
                }
            }
            step("Edit the entered name (clear and retype)") {
                onAccountInfoEditorScreen {
                    accountNameField.performTextReplacement("Edited")
                }
            }
            step("Assert edited name in 'Account name' field is displayed") {
                onAccountInfoEditorScreen {
                    accountNameField.assertTextContains("Edited")
                }
            }
            step("Delete all text and leave 'Account name' field empty") {
                onAccountInfoEditorScreen {
                    accountNameField.performTextReplacement("")
                }
            }
            step("Assert 'Add account' button is inactive") {
                onAccountInfoEditorScreen {
                    saveAccountButton.assertIsNotEnabled()
                }
            }

            step("Clear field and paste text longer than 20 characters to 'Account name' field") {
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
                assertUnsavedChangesCreationModal()
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

}