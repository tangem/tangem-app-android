package com.tangem.screens.tangempay

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.res.R as CoreResR
import com.tangem.core.ui.test.TangemPayTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString

class TangemPayChangePinPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<TangemPayChangePinPageObject>(semanticsProvider = semanticsProvider) {

    // TangemTopBar takes no per-title testTag, so the screen title is matched by text.
    val title: KNode = child {
        hasText(getResourceString(CoreResR.string.visa_onboarding_pin_code_title))
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

    val closeButton: KNode = child {
        hasTestTag(TangemPayTestTags.PIN_CLOSE_BUTTON)
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