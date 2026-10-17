package com.tangem.screens.accounts

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.test.accounts.JointAccountCreationTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode

class JointAccountCompositionPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<JointAccountCompositionPageObject>(semanticsProvider = semanticsProvider) {

    val continueButton: KNode = child {
        hasTestTag(JointAccountCreationTestTags.COMPOSITION_CONTINUE_BUTTON)
    }
}

internal fun BaseTestCase.onJointAccountCompositionScreen(function: JointAccountCompositionPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)