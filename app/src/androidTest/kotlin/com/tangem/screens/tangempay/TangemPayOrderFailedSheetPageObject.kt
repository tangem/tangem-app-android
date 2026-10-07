package com.tangem.screens.tangempay

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.res.R as CoreResR
import com.tangem.core.ui.test.WarningBottomSheetTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString

class TangemPayOrderFailedSheetPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<TangemPayOrderFailedSheetPageObject>(semanticsProvider = semanticsProvider) {

    val icon: KNode = child {
        hasTestTag(WarningBottomSheetTestTags.ICON)
        useUnmergedTree = true
    }

    val title: KNode = child {
        hasTestTag(WarningBottomSheetTestTags.TITLE)
        hasText(getResourceString(CoreResR.string.common_something_went_wrong))
        useUnmergedTree = true
    }

    val message: KNode = child {
        hasTestTag(WarningBottomSheetTestTags.MESSAGE)
        hasText(getResourceString(CoreResR.string.common_try_again_later))
        useUnmergedTree = true
    }

    val closeButton: KNode = child {
        hasTestTag(WarningBottomSheetTestTags.BUTTON_SECONDARY)
        hasText(getResourceString(CoreResR.string.common_close))
    }

    val retryButton: KNode = child {
        hasTestTag(WarningBottomSheetTestTags.BUTTON_PRIMARY)
        hasText(getResourceString(CoreResR.string.common_retry))
    }
}

internal fun BaseTestCase.onTangemPayOrderFailedSheet(function: TangemPayOrderFailedSheetPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)