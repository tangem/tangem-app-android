package com.tangem.screens.accounts

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.test.accounts.JointAccountCreationTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode

class JointAccountConfigPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<JointAccountConfigPageObject>(semanticsProvider = semanticsProvider) {

    val nameField: KNode = child {
        hasTestTag(JointAccountCreationTestTags.CONFIG_NAME_FIELD)
    }

    val continueButton: KNode = child {
        hasTestTag(JointAccountCreationTestTags.CONFIG_CONTINUE_BUTTON)
    }
}

internal fun BaseTestCase.onJointAccountConfigScreen(function: JointAccountConfigPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)