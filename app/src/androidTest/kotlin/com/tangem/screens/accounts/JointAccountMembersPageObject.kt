package com.tangem.screens.accounts

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.test.accounts.JointAccountCreationTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode

class JointAccountMembersPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<JointAccountMembersPageObject>(semanticsProvider = semanticsProvider) {

    val closeButton: KNode = child {
        hasTestTag(JointAccountCreationTestTags.MEMBERS_CLOSE_BUTTON)
    }
}

internal fun BaseTestCase.onJointAccountMembersScreen(function: JointAccountMembersPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)