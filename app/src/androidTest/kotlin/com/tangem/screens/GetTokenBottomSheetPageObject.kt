package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.R
import com.tangem.core.ui.test.BaseBottomSheetTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString

/**
 * "Get token" bottom sheet shown after picking a token in the Add funds flow.
 * Contains quick actions (Buy / Receive / …) and the "Go to token" button.
 */
class GetTokenBottomSheetPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<GetTokenBottomSheetPageObject>(semanticsProvider = semanticsProvider) {

    val title: KNode = child {
        hasTestTag(BaseBottomSheetTestTags.TITLE)
    }

    val closeButton: KNode = child {
        hasTestTag(BaseBottomSheetTestTags.CLOSE_BUTTON)
    }

    // The "Get token" sheet action rows use combinedClickable; the row's testTag lands on a
    // separate zero-bounds semantics node that fails assertIsDisplayed. Matching the merged node
    // by its title text yields the displayed, clickable row (performClick injects a touch at its
    // center, which the row's clickable handles).
    val buyButton: KNode = child {
        hasText(getResourceString(R.string.common_buy))
    }

    val receiveButton: KNode = child {
        hasText(getResourceString(R.string.common_receive))
    }

    val goToTokenButton: KNode = child {
        hasText(getResourceString(R.string.common_go_to_token))
    }
}

internal fun BaseTestCase.onGetTokenBottomSheet(function: GetTokenBottomSheetPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)