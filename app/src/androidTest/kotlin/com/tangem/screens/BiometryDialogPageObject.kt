package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.hasText as withText
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.R as CoreUiR
import com.tangem.core.ui.test.BaseButtonTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString

class BiometryDialogPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<BiometryDialogPageObject>(semanticsProvider = semanticsProvider) {

    val dontAllowButton: KNode = child {
        hasTestTag(BaseButtonTestTags.BUTTON)
        hasAnyDescendant(withText(getResourceString(CoreUiR.string.save_user_wallet_agreement_dont_allow)))
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onBiometryDialog(function: BiometryDialogPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)