package com.tangem.scenarios

import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.QUOTES_API_SCENARIO
import com.tangem.common.constants.TestConstants.USER_TOKENS_API_SCENARIO
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.screens.onMainScreen
import com.tangem.screens.onSendConfirmScreen
import com.tangem.screens.onTokenDetailsScreen
import io.github.kakaocup.compose.node.element.KNode
import io.qameta.allure.kotlin.Allure.step

fun BaseTestCase.openSendScreen(tokenName: String, mockState: String = "") {
    val scenarioState = mockState.ifEmpty { tokenName }
    step("Set WireMock scenario: '$USER_TOKENS_API_SCENARIO' to state: '$scenarioState'") {
        setWireMockScenarioState(scenarioName = USER_TOKENS_API_SCENARIO, state = scenarioState)
    }
    step("Set WireMock scenario: '$QUOTES_API_SCENARIO' to state: '$scenarioState'") {
        setWireMockScenarioState(scenarioName = QUOTES_API_SCENARIO, state = scenarioState)
    }
    step("Open 'Main Screen'") {
        openMainScreen()
    }
    step("Synchronize addresses") {
        synchronizeAddresses()
    }
    step("Click on token with name: '$tokenName'") {
        onMainScreen { tokenWithTitleAndAddress(tokenName).clickWithAssertion() }
    }
    step("Click on 'Send' button") {
        onTokenDetailsScreen { sendButton().performClick() }
    }
}

fun BaseTestCase.checkSendWarning(
    title: String,
    message: String,
    isDisplayed: Boolean = true,
    sendButtonIsDisabled: Boolean = isDisplayed,
) {
    val assertDisplay = if (isDisplayed) "displayed" else "not displayed"

    step("Assert 'Send confirm screen' is displayed") {
        onSendConfirmScreen {
            appBarTitle.assertIsDisplayed()
        }
    }
    step("Assert warning title is $assertDisplay") {
        onSendConfirmScreen {
            warningTitle(title).assertVisibility(isDisplayed)
        }
    }
    step("Assert warning icon is $assertDisplay") {
        onSendConfirmScreen {
            sendWarningIcon(message).assertVisibility(isDisplayed)
        }
    }
    step("Assert warning message is $assertDisplay") {
        onSendConfirmScreen {
            sendWarningMessage(message).assertVisibility(isDisplayed)
        }
    }
    if (sendButtonIsDisabled)
        step("Assert 'Send' button is disabled") {
            onSendConfirmScreen {
                sendButton.assertIsNotEnabled()
            }
        }
    else
        step("Assert 'Send' button is enabled") {
            onSendConfirmScreen {
                sendButton.assertIsEnabled()
            }
        }
}

private fun KNode.assertVisibility(shouldBeDisplayed: Boolean) {
    if (shouldBeDisplayed) {
        assertIsDisplayed()
    } else {
        assertIsNotDisplayed()
    }
}