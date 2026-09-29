package com.tangem.screens.accounts

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.test.accounts.JointAccountCreationTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode

class JointAccountPromoPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<JointAccountPromoPageObject>(semanticsProvider = semanticsProvider) {

    val continueButton: KNode = child {
        hasTestTag(JointAccountCreationTestTags.PROMO_CONTINUE_BUTTON)
    }
}

internal fun BaseTestCase.onJointAccountPromoScreen(function: JointAccountPromoPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)