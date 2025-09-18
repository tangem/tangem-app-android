package com.tangem.screens

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.common.extensions.hasLazyListItemPosition
import com.tangem.common.utils.LazyListItemNode
import com.tangem.core.ui.test.OrganizeTokensScreenTestTags
import com.tangem.core.ui.test.TokenElementsTestTags
import com.tangem.core.ui.utils.LazyListItemPositionSemantics
import com.tangem.feature.wallet.impl.R
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.compose.node.element.lazylist.KLazyListNode
import io.github.kakaocup.kakao.common.utilities.getResourceString
import androidx.compose.ui.test.hasAnyChild as withAnyChild
import androidx.compose.ui.test.hasAnySibling as withAnySibling
import androidx.compose.ui.test.hasTestTag as withTestTag
import androidx.compose.ui.test.hasText as withText

class OrganizeTokensPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<OrganizeTokensPageObject>(semanticsProvider = semanticsProvider) {

    // region TopBar
    val title: KNode = child {
        hasText(getResourceString(R.string.organize_tokens_title))
        useUnmergedTree = true
    }

    private val topBarGroupButton: KNode = child {
        hasTestTag(OrganizeTokensScreenTestTags.GROUP_BUTTON)
        useUnmergedTree = true
    }

    val groupButton: KNode = topBarGroupButton.child {
        hasText(getResourceString(R.string.organize_tokens_group))
        useUnmergedTree = true
    }

    val ungroupButton: KNode = topBarGroupButton.child {
        hasText(getResourceString(R.string.organize_tokens_ungroup))
        useUnmergedTree = true
    }

    val sortByBalanceButton: KNode = child {
        hasTestTag(OrganizeTokensScreenTestTags.SORT_BY_BALANCE_BUTTON)
        useUnmergedTree = true
    }
    // endregion TopBar

    private val lazyList = KLazyListNode(
        semanticsProvider = semanticsProvider,
        viewBuilderAction = { hasTestTag(OrganizeTokensScreenTestTags.TOKENS_LAZY_LIST) },
        itemTypeBuilder = {
            itemType(::LazyListItemNode)
        },
        positionMatcher = { position ->
            SemanticsMatcher.expectValue(
                LazyListItemPositionSemantics,
                position
            )
        }
    )

    val applyButton: KNode = child {
        hasTestTag(OrganizeTokensScreenTestTags.APPLY_BUTTON)
        useUnmergedTree = true
    }

    val cancelButton: KNode = child {
        hasTestTag(OrganizeTokensScreenTestTags.CANCEL_BUTTON)
        useUnmergedTree = true
    }

    fun tokenWithTitle(tokenTitle: String): KNode {
        return lazyList.child {
            hasTestTag(OrganizeTokensScreenTestTags.TOKEN_LIST_ITEM)
            hasAnyDescendant(withText(tokenTitle))
            useUnmergedTree = true
        }
    }

    fun tokenNetworkGroupTitle(tokenNetwork: String): KNode {
        return lazyList.child {
            hasTestTag(OrganizeTokensScreenTestTags.GROUP_TITLE_ITEM)
            hasAnyChild(withText(tokenNetwork))
            useUnmergedTree = true
        }
    }

    fun tokenWithTitleAndPosition(tokenTitle: String, index: Int): KNode {
        return lazyList.child {
            hasLazyListItemPosition(index)
            hasTestTag(OrganizeTokensScreenTestTags.TOKEN_LIST_ITEM)
            hasAnyDescendant(withText(tokenTitle))
            useUnmergedTree = true
        }
    }

    fun tokenDraggableButton(tokenTitle: String): KNode {
        return lazyList.child {
            hasTestTag(OrganizeTokensScreenTestTags.DRAGGABLE_IMAGE)
            useUnmergedTree = true
            hasParent(
                withTestTag(TokenElementsTestTags.TOKEN_NON_FIAT_BLOCK)
                    .and(
                        withAnySibling(
                            withTestTag(TokenElementsTestTags.TOKEN_TITLE)
                                .and(withAnyChild(withText(tokenTitle)))
                        )
                    )
            )
        }
    }
}

internal fun BaseTestCase.onOrganizeTokensScreen(function: OrganizeTokensPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)