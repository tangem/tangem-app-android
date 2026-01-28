package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.test.BaseButtonTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString
import com.tangem.features.onboarding.v2.impl.R as OnboardingImplR

class CreateWalletStartPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<CreateWalletStartPageObject>(semanticsProvider = semanticsProvider) {

    val scanCardOrRingButton: KNode = child {
        hasTestTag(BaseButtonTestTags.TEXT)
        hasText(getResourceString(OnboardingImplR.string.welcome_unlock_card))
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onCreateWalletStartScreen(function: CreateWalletStartPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)