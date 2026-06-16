package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.test.HotWalletAccessCodeTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode

class HotWalletAccessCodePageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<HotWalletAccessCodePageObject>(semanticsProvider = semanticsProvider) {

    val accessCodeInput: KNode = child {
        hasTestTag(HotWalletAccessCodeTestTags.ACCESS_CODE_INPUT)
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onHotWalletAccessCodeScreen(function: HotWalletAccessCodePageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)