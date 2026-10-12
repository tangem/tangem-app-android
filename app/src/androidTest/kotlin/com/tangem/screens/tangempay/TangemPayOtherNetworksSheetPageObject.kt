package com.tangem.screens.tangempay

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.res.R as CoreResR
import com.tangem.core.ui.test.BaseButtonTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString
import androidx.compose.ui.test.hasText as withText

/** Informational sheet shown instead of a deposit address when a DISABLED network is picked. */
class TangemPayOtherNetworksSheetPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<TangemPayOtherNetworksSheetPageObject>(semanticsProvider = semanticsProvider) {

    val header: KNode = child {
        hasText(getResourceString(CoreResR.string.tangempay_other_networks_header))
        useUnmergedTree = true
    }

    val title: KNode = child {
        hasText(getResourceString(CoreResR.string.tangempay_other_networks_title))
        useUnmergedTree = true
    }

    val subtitle: KNode = child {
        hasText(getResourceString(CoreResR.string.tangempay_other_networks_subtitle))
        useUnmergedTree = true
    }

    val closeButton: KNode = child {
        hasTestTag(BaseButtonTestTags.BUTTON)
        hasAnyDescendant(withText(getResourceString(CoreResR.string.common_close)))
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onTangemPayOtherNetworksSheet(
    function: TangemPayOtherNetworksSheetPageObject.() -> Unit,
) = onComposeScreen(composeTestRule, function)