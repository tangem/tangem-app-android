package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.wallet.R
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString

class SecurityModePageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<SecurityModePageObject>(semanticsProvider = semanticsProvider) {

    // Description only appears on the Security Mode screen — unambiguous "screen opened" signal.
    val longTapOptionDescription: KNode = child {
        hasText(getResourceString(R.string.details_manage_security_long_tap_description))
        useUnmergedTree = true
    }

    val saveChangesButton: KNode = child {
        hasText(getResourceString(R.string.common_save_changes))
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onSecurityModeScreen(function: SecurityModePageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)