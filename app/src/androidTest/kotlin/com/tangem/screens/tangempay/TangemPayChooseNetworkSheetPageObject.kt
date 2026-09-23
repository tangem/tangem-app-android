package com.tangem.screens.tangempay

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.res.R as CoreResR
import com.tangem.core.ui.test.ChooseNetworkBottomSheetTestTags
import com.tangem.core.ui.test.TangemPayTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString
import androidx.compose.ui.test.hasTestTag as withTestTag
import androidx.compose.ui.test.hasText as withText

/**
 * Pay-specific 'Choose network' bottom sheet reached from 'Add funds' → 'From another wallet or
 * exchange'. It reuses the shared network-row tags but splits the rows into its own Fast/Other
 * sections, so a row is addressed through the section it belongs to.
 */
class TangemPayChooseNetworkSheetPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<TangemPayChooseNetworkSheetPageObject>(semanticsProvider = semanticsProvider) {

    val title: KNode = child {
        hasText(getResourceString(CoreResR.string.common_choose_network))
        useUnmergedTree = true
    }

    val fastOptionsTitle: KNode = child {
        hasText(getResourceString(CoreResR.string.tangempay_choose_network_fast_way))
        useUnmergedTree = true
    }

    val otherOptionsTitle: KNode = child {
        hasText(getResourceString(CoreResR.string.tangempay_choose_network_other_ways))
        useUnmergedTree = true
    }

    fun networkItem(name: String): KNode = child {
        hasTestTag(ChooseNetworkBottomSheetTestTags.NETWORK_ITEM)
        hasAnyDescendant(withText(name))
        useUnmergedTree = true
    }

    fun fastOptionNetwork(name: String): KNode = sectionNetwork(
        sectionTag = TangemPayTestTags.CHOOSE_NETWORK_FAST_SECTION,
        name = name,
    )

    fun otherOptionNetwork(name: String): KNode = sectionNetwork(
        sectionTag = TangemPayTestTags.CHOOSE_NETWORK_OTHER_SECTION,
        name = name,
    )

    /** The row's second line — the token label when idle, the progress or error text otherwise. */
    fun networkSubtitle(name: String): KNode = child {
        hasTestTag(ChooseNetworkBottomSheetTestTags.NETWORK_SUBTITLE)
        hasAnySibling(withText(name))
        useUnmergedTree = true
    }

    private fun sectionNetwork(sectionTag: String, name: String): KNode = child {
        hasTestTag(ChooseNetworkBottomSheetTestTags.NETWORK_ITEM)
        hasAnyAncestor(withTestTag(sectionTag))
        hasAnyDescendant(withText(name))
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onTangemPayChooseNetworkSheet(
    function: TangemPayChooseNetworkSheetPageObject.() -> Unit,
) = onComposeScreen(composeTestRule, function)