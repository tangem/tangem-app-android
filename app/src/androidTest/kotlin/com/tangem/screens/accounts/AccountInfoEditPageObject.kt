package com.tangem.screens.accounts

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.hasTestTag
import com.tangem.common.BaseTestCase
import com.tangem.common.utils.LazyListItemNode
import com.tangem.core.ui.test.BuyTokenFiatListTestTags
import com.tangem.core.ui.test.accounts.AccountDetailsScreenTestTags
import com.tangem.core.ui.test.SendScreenTestTags
import com.tangem.core.ui.test.TopAppBarTestTags
import com.tangem.core.ui.test.accounts.AccountInfoEditScreenTestTags
import com.tangem.core.ui.utils.LazyListItemPositionSemantics
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.compose.node.element.lazylist.KLazyListNode

class AccountInfoEditPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<AccountInfoEditPageObject>(semanticsProvider = semanticsProvider) {

    val screenContainer: KNode = child {
        hasTestTag(AccountInfoEditScreenTestTags.ACCOUNT_DETAILS_CONTAINER)
    }

    val accountCurrentIcon: KNode = child {
        hasTestTag("")
    }

    val accountNameField: KNode = child {
        hasTestTag("")
    }

    val accountColorMenu: KNode = child {
        hasTestTag("")
    }

    private val iconsList = KLazyListNode(
        semanticsProvider = semanticsProvider,
        viewBuilderAction = { hasTestTag(AccountInfoEditScreenTestTags.ICONS_LIST) },
        itemTypeBuilder = { itemType(::LazyListItemNode) },
        positionMatcher = { position ->
            SemanticsMatcher.expectValue(
                LazyListItemPositionSemantics,
                position,
            )
        },
    )

    private val colorsList = KLazyListNode(
        semanticsProvider = semanticsProvider,
        viewBuilderAction = { hasTestTag(AccountInfoEditScreenTestTags.COLORS_LIST) },
        itemTypeBuilder = { itemType(::LazyListItemNode) },
        positionMatcher = { position ->
            SemanticsMatcher.expectValue(
                LazyListItemPositionSemantics,
                position,
            )
        },
    )

    @OptIn(ExperimentalTestApi::class)
    fun pickRandomIcon() {
        val count = semanticsProvider
            .onNode(androidx.compose.ui.test.hasTestTag(AccountInfoEditScreenTestTags.ICONS_LIST))
            .fetchSemanticsNode()
            .children
            .size
        require(count > 0) { "No icon items found" }

        val randomIndex = (Math.random() * count).toInt()
        iconsList.childAt<LazyListItemNode>(randomIndex) {
            performClick()
        }
    }

    @OptIn(ExperimentalTestApi::class)
    fun pickRandomColor() {
        val count = colorsList.fetchSemanticsNodes().size
        require(count > 0) { "No color items found" }

        val randomIndex = (Math.random() * count).toInt()
        colorsList.childAt<LazyListItemNode>(randomIndex) {
            performClick()
        }
    }


}

internal fun BaseTestCase.onAccountInfoEditorScreen(function: AccountInfoEditPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)