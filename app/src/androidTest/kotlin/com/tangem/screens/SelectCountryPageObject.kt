package com.tangem.screens

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.common.utils.LazyListItemNode
import com.tangem.core.ui.test.BaseSearchBarTestTags
import com.tangem.core.ui.test.SelectCountryBottomSheetTestTags
import com.tangem.core.ui.utils.LazyListItemPositionSemantics
import com.tangem.features.onramp.impl.R
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.compose.node.element.lazylist.KLazyListNode
import io.github.kakaocup.kakao.common.utilities.getResourceString
import androidx.compose.ui.test.hasTestTag as withTestTag
import androidx.compose.ui.test.hasText as withText

class SelectCountryPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<SelectCountryPageObject>(semanticsProvider = semanticsProvider) {


    private val lazyList = KLazyListNode(
        semanticsProvider = semanticsProvider,
        viewBuilderAction = { hasTestTag(SelectCountryBottomSheetTestTags.LAZY_LIST) },
        itemTypeBuilder = { itemType(::LazyListItemNode) },
        positionMatcher = { position ->
            SemanticsMatcher.expectValue(
                LazyListItemPositionSemantics,
                position
            )
        }
    )

    val searchBar: KNode = child {
        hasTestTag(BaseSearchBarTestTags.SEARCH_BAR)
        useUnmergedTree = true
    }

    fun countryWithNameAndIcon(name: String): KNode {
        return lazyList.child<KNode> {
            hasText(name)
            hasAnySibling(withTestTag(SelectCountryBottomSheetTestTags.COUNTRY_ICON))
            useUnmergedTree = true
        }
    }

    fun unavailableCountryWithNameAndIcon(name: String): KNode {
        return lazyList.child<KNode> {
            hasText(name)
            hasAnySibling(withText(getResourceString(R.string.onramp_country_unavailable)))
            hasAnySibling(withTestTag(SelectCountryBottomSheetTestTags.UNAVAILABLE_COUNTRY_ICON))
            useUnmergedTree = true
        }
    }
}

internal fun BaseTestCase.onSelectCountryBottomSheet(function: SelectCountryPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)