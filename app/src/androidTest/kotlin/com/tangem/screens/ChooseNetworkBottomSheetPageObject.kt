package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.R
import com.tangem.core.ui.test.BaseBottomSheetTestTags
import com.tangem.core.ui.test.ChooseNetworkBottomSheetTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString
import androidx.compose.ui.test.hasText as withText

class ChooseNetworkBottomSheetPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<ChooseNetworkBottomSheetPageObject>(semanticsProvider = semanticsProvider) {

    val title: KNode = child {
        hasTestTag(BaseBottomSheetTestTags.TITLE)
        hasText(getResourceString(R.string.common_choose_network))
        useUnmergedTree = true
    }

    fun networkItem(title: String, subtitle: String): KNode = child {
        hasTestTag(ChooseNetworkBottomSheetTestTags.NETWORK_ITEM)
        hasAnyDescendant(withText(title))
        hasAnyDescendant(withText(subtitle))
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onChooseNetworkBottomSheet(function: ChooseNetworkBottomSheetPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)