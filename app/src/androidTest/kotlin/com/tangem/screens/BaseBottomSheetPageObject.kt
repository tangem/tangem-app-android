package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.R
import com.tangem.core.ui.test.BaseBottomSheetTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString

class BaseBottomSheetPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<BaseBottomSheetPageObject>(semanticsProvider = semanticsProvider) {

    val hideButton: KNode = child {
        hasText(getResourceString(R.string.token_details_hide_token))
        hasTestTag(BaseBottomSheetTestTags.ACTION_TITLE)
    }
}

internal fun BaseTestCase.onBottomSheet(function: BaseBottomSheetPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)