package com.tangem.screens.accounts

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.test.accounts.JointAccountCreationTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode

class JointAccountDisplayNamePageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<JointAccountDisplayNamePageObject>(semanticsProvider = semanticsProvider) {

    val nameField: KNode = child {
        hasTestTag(JointAccountCreationTestTags.DISPLAY_NAME_FIELD)
    }

    val createAccountButton: KNode = child {
        hasTestTag(JointAccountCreationTestTags.DISPLAY_NAME_CREATE_BUTTON)
    }
}

internal fun BaseTestCase.onJointAccountDisplayNameScreen(function: JointAccountDisplayNamePageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)