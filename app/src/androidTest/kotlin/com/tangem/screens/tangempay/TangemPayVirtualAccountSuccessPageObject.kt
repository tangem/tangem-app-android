package com.tangem.screens.tangempay

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.test.StatusScreenTestTags
import com.tangem.core.ui.test.TangemPayTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode

class TangemPayVirtualAccountSuccessPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<TangemPayVirtualAccountSuccessPageObject>(semanticsProvider = semanticsProvider) {

    val title: KNode = child {
        hasTestTag(StatusScreenTestTags.TITLE)
        useUnmergedTree = true
    }

    val closeButton: KNode = child {
        hasTestTag(TangemPayTestTags.VA_DEPOSIT_SUCCESS_CLOSE_BUTTON)
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onTangemPayVirtualAccountSuccessScreen(
    function: TangemPayVirtualAccountSuccessPageObject.() -> Unit,
) = onComposeScreen(composeTestRule, function)