package com.tangem.screens.tangempay

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.test.TangemPayTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode

class TangemPayChangePinPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<TangemPayChangePinPageObject>(semanticsProvider = semanticsProvider) {

    val title: KNode = child {
        hasTestTag(TangemPayTestTags.PIN_SCREEN_TITLE)
        useUnmergedTree = true
    }

    val description: KNode = child {
        hasTestTag(TangemPayTestTags.PIN_SCREEN_DESCRIPTION)
        useUnmergedTree = true
    }

    val inputField: KNode = child {
        hasTestTag(TangemPayTestTags.PIN_INPUT_FIELD)
        useUnmergedTree = true
    }

    val submitButton: KNode = child {
        hasTestTag(TangemPayTestTags.PIN_SUBMIT_BUTTON)
        useUnmergedTree = true
    }

    val errorMessage: KNode = child {
        hasTestTag(TangemPayTestTags.PIN_ERROR_MESSAGE)
        useUnmergedTree = true
    }

    val successTitle: KNode = child {
        hasTestTag(TangemPayTestTags.PIN_SUCCESS_TITLE)
        useUnmergedTree = true
    }

    val successDescription: KNode = child {
        hasTestTag(TangemPayTestTags.PIN_SUCCESS_DESCRIPTION)
        useUnmergedTree = true
    }

    val doneButton: KNode = child {
        hasTestTag(TangemPayTestTags.PIN_DONE_BUTTON)
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onTangemPayChangePinScreen(function: TangemPayChangePinPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)