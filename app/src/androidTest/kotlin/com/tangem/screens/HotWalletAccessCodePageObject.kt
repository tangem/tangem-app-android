package com.tangem.screens

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.test.HotWalletAccessCodeTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import androidx.compose.ui.test.hasAnyAncestor as withAnyAncestor
import androidx.compose.ui.test.hasTestTag as withTestTag
import androidx.compose.ui.test.isDialog as withDialog

class HotWalletAccessCodePageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<HotWalletAccessCodePageObject>(semanticsProvider = semanticsProvider) {

    val accessCodeInput: KNode = child {
        hasTestTag(HotWalletAccessCodeTestTags.ACCESS_CODE_INPUT)
        useUnmergedTree = true
    }

    val unlockDialogAccessCodeInput: KNode = child {
        addSemanticsMatcher(unlockDialogAccessCodeInputMatcher)
        useUnmergedTree = true
    }
}

// The access code screen of the wallet import stays composed under the unlock dialog with the same tag.
val unlockDialogAccessCodeInputMatcher: SemanticsMatcher =
    withTestTag(HotWalletAccessCodeTestTags.ACCESS_CODE_INPUT) and withAnyAncestor(withDialog())

internal fun BaseTestCase.onHotWalletAccessCodeScreen(function: HotWalletAccessCodePageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)