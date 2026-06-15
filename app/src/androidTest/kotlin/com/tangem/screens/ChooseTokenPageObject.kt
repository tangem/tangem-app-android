package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.test.BaseBottomSheetTestTags
import com.tangem.core.ui.test.BaseSearchBarTestTags
import com.tangem.core.ui.test.BuyTokenScreenTestTags
import com.tangem.core.ui.test.TokenElementsTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString
import androidx.compose.ui.test.hasTestTag as withTestTag
import androidx.compose.ui.test.hasText as withText
import com.tangem.core.res.R as CoreResR

/**
 * Token chooser bottom sheet opened from the main-screen "Add funds" button.
 *
 * After the onramp redesign this is a [BaseBottomSheetTestTags.CONTAINER] bottom sheet
 * (centered title + close icon), not a full screen with a top app bar.
 */
class ChooseTokenPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<ChooseTokenPageObject>(
        semanticsProvider = semanticsProvider,
        viewBuilderAction = { hasTestTag(BaseBottomSheetTestTags.CONTAINER) },
    ) {

    val topAppBarTitle: KNode = child {
        hasText(getResourceString(CoreResR.string.common_add_funds))
        useUnmergedTree = true
    }

    val searchBar: KNode = child {
        hasTestTag(BaseSearchBarTestTags.SEARCH_BAR)
        useUnmergedTree = true
    }

    fun tokenWithTitle(tokenTitle: String): KNode = child {
        hasTestTag(BuyTokenScreenTestTags.LAZY_LIST_ITEM)
        hasAnyDescendant(withTestTag(TokenElementsTestTags.TOKEN_TITLE))
        hasAnyDescendant(withText(tokenTitle))
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onChooseTokenScreen(function: ChooseTokenPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)