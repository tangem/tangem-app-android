package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.res.R as CoreResR
import com.tangem.core.ui.test.BaseButtonTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString

class ForgetWalletPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<ForgetWalletPageObject>(semanticsProvider = semanticsProvider) {

    val acknowledgeCheckbox: KNode = child {
        hasText(getResourceString(CoreResR.string.hw_remove_wallet_warning_access))
        hasClickAction()
    }

    val forgetWalletButton: KNode = child {
        hasTestTag(BaseButtonTestTags.BUTTON)
        hasText(getResourceString(CoreResR.string.hw_remove_wallet_action_forget_title))
    }
}

internal fun BaseTestCase.onForgetWalletScreen(function: ForgetWalletPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)