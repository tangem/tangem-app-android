package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.R
import com.tangem.core.ui.test.BaseBottomSheetTestTags
import com.tangem.core.ui.test.BaseButtonTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString

class AddTokenBottomSheetPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<AddTokenBottomSheetPageObject>(
        semanticsProvider = semanticsProvider,
        viewBuilderAction = { hasTestTag(BaseBottomSheetTestTags.CONTAINER) },
    ) {

    val title: KNode = child {
        hasTestTag(BaseBottomSheetTestTags.TITLE)
        hasText(getResourceString(R.string.common_add_token))
    }

    val closeButton: KNode = child {
        hasTestTag(BaseBottomSheetTestTags.CLOSE_BUTTON)
        useUnmergedTree = true
    }

    val addButton: KNode = child {
        hasTestTag(BaseButtonTestTags.TEXT)
        hasText(getResourceString(R.string.common_add))
        useUnmergedTree = true
    }

    val laterButton: KNode = child {
        hasTestTag(BaseButtonTestTags.TEXT)
        hasText(getResourceString(R.string.common_later))
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onAddTokenBottomSheet(function: AddTokenBottomSheetPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)