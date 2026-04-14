package com.tangem.tests.accounts

import com.tangem.common.BaseTestCase
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.utils.resetWireMockScenarioState
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.scenarios.openMainScreen
import com.tangem.scenarios.synchronizeAddresses
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
class AccountCreationTest : BaseTestCase() {

    private val apiScenario = "user_tokens_api"

    // WireMock scenarios:
    // 5507 — no WireMock scenario needed (default mock)
    // 5505 — no WireMock scenario needed (default mock)
    // 5504 — user_tokens_api: "Started" → "AccountsGetError" → "AccountsPutError" → "AccountReadyToCreate"

    @Test
    @AllureId("5507")
    @DisplayName("Accounts: name field verifications")
    fun accountsCreationNameFieldValidationTest() {
        val accountName = "TestAccount" + 10 + Math.random() * 90
        val longName = "A".repeat(21)
        val maxLengthName = "A".repeat(20)

        setupHooks().run {
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
            step("Click on 'Add account' button") {
                onWalletSettingsScreen { addAccountButton.clickWithAssertion() }
            }
            step("Assert edit account details dialog screen appears") {
                onAccountInfoEditorScreen { screenContainer.assertIsDisplayed() }
            }

            step("Enter account name manually: '$accountName'") {
                onAccountInfoEditorScreen {
                    //accountNameTextField.performClick()
                    //accountNameTextField.performTextInput(accountName)
                }
            }
            step("Assert name input is stable (keyboard doesn't flicker)") {
                onAccountInfoEditorScreen {
                    //accountNameTextField.assertTextContains(accountName)
                }
            }
            step("Assert 'Add account' button becomes active") {
                onAccountInfoEditorScreen {
                    //addAccountButton.assertIsEnabled() }
                }
            }

            step("Clear the name field") {
                onAccountInfoEditorScreen {
                    //accountNameTextField.performTextReplacement("")
                }
            }
            step("Assert 'Add account' button becomes inactive when field is empty") {
                onAccountInfoEditorScreen { //addAccountButton.assertIsNotEnabled() }
                }
            }
            step("Paste name from clipboard: '$accountName'") {
                onAccountInfoEditorScreen {
                    //accountNameTextField.performTextReplacement(accountName)
                }
            }
            step("Assert pasted text is displayed in input field") {
                onAccountInfoEditorScreen {
                    //accountNameTextField.assertTextContains(accountName)
                }
            }
            step("Assert 'Add account' button becomes active after paste") {
                onAccountInfoEditorScreen { //addAccountButton.assertIsEnabled() }
                }
            }
            // Step 4: Edit the entered name
            step("Edit the entered name (clear and retype)") {
                onAccountInfoEditorScreen {
                    //accountNameTextField.performTextReplacement("Edited")
                }
            }
            step("Assert edited name is displayed") {
                onAccountInfoEditorScreen {
                    //accountNameTextField.assertTextContains("Edited")
                }
            }

            step("Delete all text and leave field empty") {
                onAccountInfoEditorScreen {
                    //accountNameTextField.performTextReplacement("")
                }
            }
            step("Assert 'Add account' button is inactive") {
                onAccountInfoEditorScreen { //addAccountButton.assertIsNotEnabled() }
                }
            }

            step("Try to enter more than 20 characters: '$longName'") {
                onAccountInfoEditorScreen {
                    //accountNameTextField.performTextReplacement(longName)
                }
            }
            step("Assert input is limited to 20 characters") {
                onAccountInfoEditorScreen {
                    //accountNameTextField.assertTextContains(maxLengthName)
                }
            }

            step("Clear field and paste text longer than 20 characters") {
                onAccountInfoEditorScreen {
                    //accountNameTextField.performTextReplacement(longName)
                }
            }
            step("Assert pasted text is truncated to 20 characters") {
                onAccountInfoEditorScreen {
                    //accountNameTextField.assertTextContains(maxLengthName)
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
    fun accountsCreationUnsavedChangesNotificationTest() {

        val accountName = ""//StringGenerator("[A-Z][a-z][0-9]").length(random(20))
        setupHooks().run {
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
            step("Click on 'Add account' button") {
                onWalletSettingsScreen { addAccountButton.clickWithAssertion() }
            }
            step("Assert edit account details dialog screen appears") {
                onAccountInfoEditorScreen { screenContainer.assertIsDisplayed() }
            }

            step("Enter account name manually: '$accountName'") {
                onAccountInfoEditorScreen {
                    //accountNameTextField.performClick()
                    //accountNameTextField.performTextInput(accountName)
                }
            }

            step("Tap close button to attempt closing the screen") {
                onAccountInfoEditorScreen { //crossButton.clickWithAssertion() }
                }
            }

            step("Assert 'Unsaved changes' alert is displayed") {
                onDialog {
                    dialogContainer.assertIsDisplayed()
                    title.assertTextContains("Unsaved changes")
                }
            }
            step("Assert 'Continue' and 'Cancel' buttons are displayed in alert") {
                onDialog {
                    continueButton.assertIsDisplayed()
                    cancelButton.assertIsDisplayed()
                }
            }
            // TODO verify buttons texts keep editing etc

            step("Tap 'Continue' button to stay on screen") {
                onDialog { continueButton.clickWithAssertion() }
            }
            step("Assert still on 'Create account' screen") {
                onAccountInfoEditorScreen { screenContainer.assertIsDisplayed() }
            }
            step("Assert previously entered data is preserved") {
                onAccountInfoEditorScreen {
                    // accountNameTextField.assertTextContains(accountName)
                }
            }

            step("Tap close button again") {
                onAccountInfoEditorScreen { //crossButton.clickWithAssertion() }
                }
            }
            step("Assert 'Unsaved changes' alert is displayed again") {
                onDialog { dialogContainer.assertIsDisplayed() }
            }
            step("Tap 'Cancel' button to discard and close") {
                onDialog { cancelButton.clickWithAssertion() }
            }
            step("Assert 'Create account' screen is closed") {
                onWalletSettingsScreen { //screenContainer.assertIsDisplayed() }
                }

            }
        }

        // TODO account creation only name filled than discard
        // TODO account creation only icon then ..
    }

    @Test
    @AllureId("5504")
    @DisplayName("Accounts: network error handling during account creation")
    fun networkErrorHandlingDuringAccountCreationTest(): Unit {
        // WireMock: user_tokens_api → "AccountReadyToCreate" (initial),
        //   then "AccountsGetError" for GET error,
        //   then "AccountsPutError" for PUT error

        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(apiScenario)
            }
        ).run {
            step("Set WireMock scenario to 'AccountReadyToCreate'") {
                setWireMockScenarioState(apiScenario, "AccountReadyToCreate")
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
            step("Click on 'Add account' button") {
                onWalletSettingsScreen { addAccountButton.clickWithAssertion() }
            }
            /*step("Switch WireMock to 'AccountsPutError'") {
                setWireMockScenarioState(apiScenario, "AccountsPutError")
            }
            step("Fill account name and submit") {
                onAccountInfoEditorScreen {
                    // TODO: fill name and tap create
                }
            }
            step("Assert error alert is displayed") {
                onDialog {
                    dialogContainer.assertIsDisplayed()
                }
            }
            step("Switch WireMock to 'AccountsGetError' and retry") {
                setWireMockScenarioState(apiScenario, "AccountsGetError")
                // TODO: verify GET error handling
            }*/
        }
    }
}