package com.tangem.screens.tangempay

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.test.TangemPayTestTags
import com.tangem.core.ui.test.TopAppBarTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import androidx.compose.ui.test.hasTestTag as withTestTag

class TangemPayKycPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<TangemPayKycPageObject>(semanticsProvider = semanticsProvider) {

    val screenContainer: KNode = child {
        hasTestTag(TangemPayTestTags.KYC_SCREEN)
        useUnmergedTree = true
    }

    val backButton: KNode = child {
        hasTestTag(TopAppBarTestTags.CLOSE_BUTTON)
        hasAnyAncestor(withTestTag(TangemPayTestTags.KYC_SCREEN))
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onTangemPayKycScreen(function: TangemPayKycPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)