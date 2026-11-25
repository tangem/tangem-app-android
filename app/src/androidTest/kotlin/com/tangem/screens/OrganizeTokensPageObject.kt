package com.tangem.screens

import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.performTouchInput
import com.tangem.common.BaseTestCase
import com.tangem.common.extensions.displayedTextsInVisualOrder
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
import androidx.compose.ui.test.hasAnyAncestor as withAnyAncestor
import androidx.compose.ui.test.hasTestTag as withTestTag
import androidx.compose.ui.test.hasText as withText

class OrganizeTokensPageObject(private val semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<OrganizeTokensPageObject>(semanticsProvider = semanticsProvider) {

    // region TopBar
    val title: KNode = child {
        hasText(getResourceString(R.string.organize_tokens_title))
        useUnmergedTree = true
    }

    val organizeMenuButton: KNode = child {
        hasTestTag(OrganizeTokensScreenTestTags.MENU_BUTTON)
        useUnmergedTree = true
    }

    val groupButton: KNode = child {
        hasText(getResourceString(R.string.organize_tokens_group))
        useUnmergedTree = true
    }

    val sortByBalanceButton: KNode = child {
        hasText(getResourceString(R.string.organize_tokens_sort_by_balance))
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

    // The row appends the currency symbol to the name ("Bitcoin BTC"), so an exact match never hits.
    fun tokenWithTitle(tokenTitle: String): KNode {
        return lazyList.child {
            hasTestTag(OrganizeTokensScreenTestTags.TOKEN_LIST_ITEM)
            hasAnyDescendant(withText(text = tokenTitle, substring = true))
            useUnmergedTree = true
        }
    }

    fun tokenNetworkGroupTitle(tokenNetwork: String): KNode {
        return lazyList.child {
            hasTestTag(OrganizeTokensScreenTestTags.GROUP_TITLE_ITEM)
            hasAnyDescendant(withText(text = tokenNetwork, substring = true))
            useUnmergedTree = true
        }
    }

    // The redesigned rows carry LazyListItemPosition on the draggable wrapper, one level above the
    // tagged TOKEN_LIST_ITEM node, so match the item by tag and title and read the position off its
    // ancestor instead of expecting both on one node.
    fun tokenWithTitleAndPosition(tokenTitle: String, index: Int): KNode {
        return lazyList.child {
            hasTestTag(OrganizeTokensScreenTestTags.TOKEN_LIST_ITEM)
            hasAnyAncestor(SemanticsMatcher.expectValue(LazyListItemPositionSemantics, index))
            hasAnyDescendant(withText(text = tokenTitle, substring = true))
            useUnmergedTree = true
        }
    }

    // Read TOKEN_TITLE nodes, not the outer TOKEN_LIST_ITEM: the item container here doesn't merge its
    // descendants, so the nested title Text never surfaces on the item node's semantics.
    fun getDisplayedTokenTitles(): List<String> =
        semanticsProvider.onAllNodes(
            withTestTag(TokenElementsTestTags.TOKEN_TITLE) and
                withAnyAncestor(withTestTag(OrganizeTokensScreenTestTags.TOKENS_LAZY_LIST)),
            useUnmergedTree = true,
        ).displayedTextsInVisualOrder()

    // Reorder a row by a long press on it followed by a vertical drag onto the destination row, then
    // lift. The redesigned list has no drag handle: the whole row carries the reorder gesture, which
    // only engages after the long-press timeout, so hold in place before moving or it reads as a scroll.
    // Only valid within one network group (isValidDropTarget), so source and destination must share a group.
    fun dragToken(source: String, destination: String) {
        val sourceItem = semanticsProvider.onNode(tokenItemMatcher(source), useUnmergedTree = true)
        val sourceNode = sourceItem.fetchSemanticsNode()
        val destinationNode = semanticsProvider
            .onNode(tokenItemMatcher(destination), useUnmergedTree = true)
            .fetchSemanticsNode()

        // performTouchInput coordinates are local to the source node; express both endpoints there.
        val origin = sourceNode.positionInRoot
        val start = sourceNode.boundsInRoot.center - origin
        val end = destinationNode.boundsInRoot.center - origin

        sourceItem.performTouchInput {
            down(start)
            advanceEventTime(LONG_PRESS_HOLD_MS)
            repeat(times = DRAG_STEPS) { step ->
                moveTo(lerp(start = start, stop = end, fraction = (step + 1).toFloat() / DRAG_STEPS))
                advanceEventTime(DRAG_STEP_MS)
            }
            moveTo(end)
            advanceEventTime(DRAG_STEP_MS)
            up()
        }
    }

    private fun tokenItemMatcher(tokenTitle: String): SemanticsMatcher =
        withTestTag(OrganizeTokensScreenTestTags.TOKEN_LIST_ITEM) and hasAnyDescendant(withText(tokenTitle))

    private companion object {
        const val LONG_PRESS_HOLD_MS = 800L
        const val DRAG_STEPS = 16
        const val DRAG_STEP_MS = 24L
    }
}

internal fun BaseTestCase.onOrganizeTokensScreen(function: OrganizeTokensPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)