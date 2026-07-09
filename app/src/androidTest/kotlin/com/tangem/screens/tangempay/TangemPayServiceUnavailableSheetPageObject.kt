package com.tangem.screens.tangempay

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.test.WarningBottomSheetTestTags
import com.tangem.core.res.R as CoreResR
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString

class TangemPayServiceUnavailableSheetPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<TangemPayServiceUnavailableSheetPageObject>(semanticsProvider = semanticsProvider) {

    val title: KNode = child {
        hasTestTag(WarningBottomSheetTestTags.TITLE)
        hasText(getResourceString(CoreResR.string.tangempay_service_unavailable_title))
        useUnmergedTree = true
    }

    val description: KNode = child {
        hasTestTag(WarningBottomSheetTestTags.MESSAGE)
        hasText(getResourceString(CoreResR.string.tangempay_service_unavailable_description))
        useUnmergedTree = true
    }

    // Merged tree: BUTTON_PRIMARY tags the button, its label is a child — only the merged node has both.
    val gotItButton: KNode = child {
        hasTestTag(WarningBottomSheetTestTags.BUTTON_PRIMARY)
        hasText(getResourceString(CoreResR.string.common_got_it))
    }
}

internal fun BaseTestCase.onTangemPayServiceUnavailableSheet(
    function: TangemPayServiceUnavailableSheetPageObject.() -> Unit,
) = onComposeScreen(composeTestRule, function)