package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.R
import com.tangem.core.ui.test.DialogTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString

class DialogPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<DialogPageObject>(semanticsProvider = semanticsProvider) {

    val dialogContainer: KNode = child {
        hasTestTag(DialogTestTags.DIALOG_CONTAINER)
    }

    val cancelButton: KNode = child {
        hasTestTag(DialogTestTags.BUTTON)
        hasText(getResourceString(R.string.common_cancel))
    }

    val hideButton: KNode = child {
        hasTestTag(DialogTestTags.BUTTON)
        hasText(getResourceString(R.string.token_details_hide_alert_hide))
    }
}

internal fun BaseTestCase.onDialog(function: DialogPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)