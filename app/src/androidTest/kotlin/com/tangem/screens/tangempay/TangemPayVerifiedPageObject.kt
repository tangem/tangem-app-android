package com.tangem.screens.tangempay

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.res.R as CoreResR
import com.tangem.core.ui.test.StatusScreenTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString

class TangemPayVerifiedPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<TangemPayVerifiedPageObject>(semanticsProvider = semanticsProvider) {

    val title: KNode = child {
        hasTestTag(StatusScreenTestTags.TITLE)
        hasText(getResourceString(CoreResR.string.tangempay_verified_title))
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onTangemPayVerifiedScreen(function: TangemPayVerifiedPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)