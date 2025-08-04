package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.R
import com.tangem.core.ui.test.PopUpMenuTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString

class PopUpMenuPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<PopUpMenuPageObject>(semanticsProvider = semanticsProvider) {

    val popUpContainer: KNode = child {
        hasTestTag(PopUpMenuTestTags.CONTAINER)
    }

    val hideTokenButton: KNode = child {
        hasTestTag(PopUpMenuTestTags.BUTTON)
        hasText(getResourceString(R.string.token_details_hide_token))
    }
}

internal fun BaseTestCase.onPopUpMenu(function: PopUpMenuPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)