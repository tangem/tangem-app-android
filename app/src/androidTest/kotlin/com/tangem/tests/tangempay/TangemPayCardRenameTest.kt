package com.tangem.tests.tangempay

import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.TANGEM_PAY_ELIGIBILITY_SCENARIO
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_LONG
import com.tangem.common.extensions.assertTextContainsSafe
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.utils.resetWireMockScenarioState
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.core.res.R as CoreResR
import com.tangem.scenarios.openTangemPayCardRename
import com.tangem.screens.onDialog
import com.tangem.screens.tangempay.onTangemPayCardPageScreen
import com.tangem.screens.tangempay.onTangemPayCardRenameScreen
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.kakaocup.kakao.common.utilities.getResourceString
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Test

@HiltAndroidTest
class TangemPayCardRenameTest : BaseTestCase() {

    private val renameScenario = "tangem_pay_card_rename"
    private val startedState = "Started"
    private val renameErrorState = "RenameError"
    private val eligibilityState = "PaeraCustomer"

    private val newCardName = "Renamed Card"
    private val initialCardName = "My Card"

    @AllureId("9711")
    @DisplayName("Tangem Pay: successful card rename")
    @Test
    fun successfulCardRenameTest() {
        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO)
                resetWireMockScenarioState(renameScenario)
            },
        ).run {
            step("Set WireMock scenario: '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to state: '$eligibilityState'") {
                setWireMockScenarioState(scenarioName = TANGEM_PAY_ELIGIBILITY_SCENARIO, state = eligibilityState)
            }
            step("Set WireMock scenario: '$renameScenario' to state: '$startedState'") {
                setWireMockScenarioState(scenarioName = renameScenario, state = startedState)
            }
            step("Open card rename screen") { openTangemPayCardRename() }
            step("Enter new card name '$newCardName'") {
                onTangemPayCardRenameScreen { nameField.performTextReplacement(newCardName) }
            }
            step("Click on 'Done' button") {
                onTangemPayCardRenameScreen { doneButton.clickWithAssertion() }
            }
            step("Assert card name contains '$newCardName'") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onTangemPayCardPageScreen {
                        cardNameEditButton.assertTextContainsSafe(newCardName, substring = true)
                    }
                }
            }
        }
    }

    @AllureId("9712")
    @DisplayName("Tangem Pay: invalid characters on card rename")
    @Test
    fun invalidCharactersOnCardRenameTest() {
        val blankName = "   "
        val maxLengthName = "AaBbCcDdEeFfGgHhIiJj"
        val overflowChar = "K"
        val invalidSymbolName = "abc$"
        val emojiName = "😀"
        val invalidTitle = getResourceString(CoreResR.string.tangempay_card_details_rename_card_invalid_title)
        val invalidMessage = getResourceString(CoreResR.string.tangempay_card_details_rename_card_invalid_description)

        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO)
                resetWireMockScenarioState(renameScenario)
            },
        ).run {
            step("Set WireMock scenario: '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to state: '$eligibilityState'") {
                setWireMockScenarioState(scenarioName = TANGEM_PAY_ELIGIBILITY_SCENARIO, state = eligibilityState)
            }
            step("Set WireMock scenario: '$renameScenario' to state: '$startedState'") {
                setWireMockScenarioState(scenarioName = renameScenario, state = startedState)
            }
            step("Open card rename screen") { openTangemPayCardRename() }
            step("Enter blank name") {
                onTangemPayCardRenameScreen { nameField.performTextReplacement(blankName) }
            }
            step("Assert 'Done' button is not enabled") {
                onTangemPayCardRenameScreen { doneButton.assertIsNotEnabled() }
            }
            step("Clear card name field") {
                onTangemPayCardRenameScreen { nameField.performTextClearance() }
            }
            step("Assert 'Done' button is not enabled") {
                onTangemPayCardRenameScreen { doneButton.assertIsNotEnabled() }
            }
            step("Enter a 20-character name '$maxLengthName'") {
                onTangemPayCardRenameScreen { nameField.performTextReplacement(maxLengthName) }
            }
            step("Assert 'Done' button is enabled") {
                onTangemPayCardRenameScreen { doneButton.assertIsEnabled() }
            }
            step("Try to enter a 21st character '$overflowChar'") {
                onTangemPayCardRenameScreen { nameField.performTextInput(overflowChar) }
            }
            step("Assert card name field still contains only 20 characters") {
                onTangemPayCardRenameScreen { nameField.assertTextEquals(maxLengthName) }
            }
            step("Enter name with invalid symbol '$invalidSymbolName'") {
                onTangemPayCardRenameScreen { nameField.performTextReplacement(invalidSymbolName) }
            }
            step("Click on 'Done' button") {
                onTangemPayCardRenameScreen { doneButton.clickWithAssertion() }
            }
            step("Assert 'Invalid characters' dialog title is displayed") {
                onDialog { title.assertTextContainsSafe(invalidTitle) }
            }
            step("Assert 'Invalid characters' dialog message is displayed") {
                onDialog { text.assertTextContainsSafe(invalidMessage) }
            }
            step("Click on 'OK' button") {
                onDialog { okButton.clickWithAssertion() }
            }
            step("Enter emoji name") {
                onTangemPayCardRenameScreen { nameField.performTextReplacement(emojiName) }
            }
            step("Click on 'Done' button") {
                onTangemPayCardRenameScreen { doneButton.clickWithAssertion() }
            }
            step("Assert 'Invalid characters' dialog title is displayed") {
                onDialog { title.assertTextContainsSafe(invalidTitle) }
            }
            step("Assert 'Invalid characters' dialog message is displayed") {
                onDialog { text.assertTextContainsSafe(invalidMessage) }
            }
            step("Click on 'OK' button") {
                onDialog { okButton.clickWithAssertion() }
            }
        }
    }

    @AllureId("9713")
    @DisplayName("Tangem Pay: backend error on card rename")
    @Test
    fun backendErrorOnCardRenameTest() {
        val errorTitle = getResourceString(CoreResR.string.tangem_pay_card_details_unable_to_rename_card_title)
        val errorMessage = getResourceString(CoreResR.string.tangempay_card_details_unable_to_rename_card_description)

        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO)
                resetWireMockScenarioState(renameScenario)
            },
        ).run {
            step("Set WireMock scenario: '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to state: '$eligibilityState'") {
                setWireMockScenarioState(scenarioName = TANGEM_PAY_ELIGIBILITY_SCENARIO, state = eligibilityState)
            }
            step("Set WireMock scenario: '$renameScenario' to state: '$renameErrorState'") {
                setWireMockScenarioState(scenarioName = renameScenario, state = renameErrorState)
            }
            step("Open card rename screen") { openTangemPayCardRename() }
            step("Enter new card name '$newCardName'") {
                onTangemPayCardRenameScreen { nameField.performTextReplacement(newCardName) }
            }
            step("Click on 'Done' button") {
                onTangemPayCardRenameScreen { doneButton.clickWithAssertion() }
            }
            step("Assert rename error dialog title is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onDialog { title.assertTextContainsSafe(errorTitle) }
                }
            }
            step("Assert rename error dialog message is displayed") {
                onDialog { text.assertTextContainsSafe(errorMessage) }
            }
            step("Click on 'OK' button") {
                onDialog { okButton.clickWithAssertion() }
            }
            step("Click on 'Close' button") {
                onTangemPayCardRenameScreen { closeButton.clickWithAssertion() }
            }
            step("Assert card name contains '$initialCardName'") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onTangemPayCardPageScreen {
                        cardNameEditButton.assertTextContainsSafe(initialCardName, substring = true)
                    }
                }
            }
        }
    }
}