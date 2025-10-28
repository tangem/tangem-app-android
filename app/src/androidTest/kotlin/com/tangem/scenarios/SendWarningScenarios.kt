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

fun BaseTestCase.openSendScreen(tokenName: String) {
    step("Set WireMock scenario: '$USER_TOKENS_API_SCENARIO' to state: '$tokenName'") {
        setWireMockScenarioState(scenarioName = USER_TOKENS_API_SCENARIO, state = tokenName)
    }
    step("Set WireMock scenario: '$QUOTES_API_SCENARIO' to state: '$tokenName'") {
        setWireMockScenarioState(scenarioName = QUOTES_API_SCENARIO, state = tokenName)
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
    titleResId: Int,
    messageResId: Int,
    amount: String,
    isDisplayed: Boolean = true,
    sendButtonIsDisabled: Boolean = isDisplayed,
) {
    val assertDisplay = if (isDisplayed) "displayed" else "not displayed"

    step("Assert 'Send confirm screen' is displayed") {
        onSendConfirmScreen {
            title.assertIsDisplayed()
        }
    }
    step("Assert warning title is $assertDisplay") {
        onSendConfirmScreen {
            warningTitle(titleResId).assertVisibility(isDisplayed)
        }
    }
    step("Assert warning icon is $assertDisplay") {
        onSendConfirmScreen {
            sendWarningIcon(messageResId, amount).assertVisibility(isDisplayed)
        }
    }
    step("Assert warning message is $assertDisplay") {
        onSendConfirmScreen {
            sendWarningMessage(messageResId, amount).assertVisibility(isDisplayed)
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