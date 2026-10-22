package com.tangem.screens.tangempay

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.res.R as CoreResR
import com.tangem.core.ui.test.StatusScreenTestTags
import com.tangem.core.ui.test.TopNavigationTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString

class TangemPayRegionUnavailablePageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<TangemPayRegionUnavailablePageObject>(semanticsProvider = semanticsProvider) {

    val title: KNode = child {
        hasTestTag(StatusScreenTestTags.TITLE)
        hasText(getResourceString(CoreResR.string.tangempay_region_unavailable_title))
        useUnmergedTree = true
    }

    val description: KNode = child {
        hasTestTag(StatusScreenTestTags.DESCRIPTION)
        hasText(getResourceString(CoreResR.string.tangempay_region_unavailable_description))
        useUnmergedTree = true
    }

    val joinWaitlistButton: KNode = child {
        hasText(getResourceString(CoreResR.string.tangempay_region_unavailable_primary_button))
        hasClickAction()
    }

    val selectAnotherRegionButton: KNode = child {
        hasText(getResourceString(CoreResR.string.tangempay_region_unavailable_secondary_button))
        hasClickAction()
    }

    val closeButton: KNode = child {
        hasTestTag(TopNavigationTestTags.CLOSE_BUTTON)
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onTangemPayRegionUnavailableScreen(
    function: TangemPayRegionUnavailablePageObject.() -> Unit,
) = onComposeScreen(composeTestRule, function)