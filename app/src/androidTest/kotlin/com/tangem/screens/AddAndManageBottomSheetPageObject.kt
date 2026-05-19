package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString
import com.tangem.core.res.R as CoreResR

class AddAndManageBottomSheetPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<AddAndManageBottomSheetPageObject>(semanticsProvider = semanticsProvider) {

    val title: KNode = child {
        hasText(getResourceString(CoreResR.string.main_add_and_manage_tokens))
        useUnmergedTree = true
    }

    val addTokensButton: KNode = child {
        hasText(getResourceString(CoreResR.string.add_and_manage_sheet_manage_title))
        useUnmergedTree = true
    }

    val organizeTokensButton: KNode = child {
        hasText(getResourceString(CoreResR.string.add_and_manage_sheet_organize_title))
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onAddAndManageBottomSheet(function: AddAndManageBottomSheetPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)