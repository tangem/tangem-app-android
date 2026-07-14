package com.tangem.screens

import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.performTouchInput
import com.tangem.common.BaseTestCase
import com.tangem.common.extensions.displayedTextsInVisualOrder
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
import androidx.compose.ui.test.hasAnyAncestor as withAnyAncestor
import androidx.compose.ui.test.hasAnyChild as withAnyChild
import androidx.compose.ui.test.hasAnySibling as withAnySibling
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
            hasAnyDescendant(withText(tokenNetwork))
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

    /**
     * Reads displayed token titles from the 'Organize tokens' screen in visual order
     * (top-to-bottom, then left-to-right). Mirrors [MainScreenPageObject.getDisplayedTokenTitles]
     * so the two lists can be compared directly.
     *
     * Titles are read from the [TokenElementsTestTags.TOKEN_TITLE] nodes rather than the outer
     * TOKEN_LIST_ITEM: the item container here does not merge its descendants, so the title Text
     * (nested inside the title Row) never surfaces on the item node's semantics.
     */
    fun getDisplayedTokenTitles(): List<String> =
        semanticsProvider.onAllNodes(
            withTestTag(TokenElementsTestTags.TOKEN_TITLE) and
                withAnyAncestor(withTestTag(OrganizeTokensScreenTestTags.TOKENS_LAZY_LIST)),
            useUnmergedTree = true,
        ).displayedTextsInVisualOrder()

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

    /**
     * Drag-and-drops the [source] token onto the [destination] token's slot to reorder the list,
     * the Android analog of iOS `press(forDuration:thenDragTo:)`.
     *
     * The list is a reorderable LazyColumn (sh.calvin.reorderable) driven from each row's drag
     * handle ([OrganizeTokensScreenTestTags.DRAGGABLE_IMAGE]). We synthesize the gesture manually:
     * press down on the source handle, hold briefly, then move in small steps to the destination
     * row's centre and lift — a single `swipe` won't engage the reorder detector.
     *
     * NOTE: reordering is only permitted **within the same network group** (`isValidDropTarget`),
     * so both tokens must belong to the same group for the drop to take effect.
     */
    fun dragToken(source: String, destination: String) {
        val sourceHandle = semanticsProvider.onNode(dragHandleMatcher(source), useUnmergedTree = true)
        val sourceNode = sourceHandle.fetchSemanticsNode()
        val destinationNode = semanticsProvider
            .onNode(tokenItemMatcher(destination), useUnmergedTree = true)
            .fetchSemanticsNode()

        // performTouchInput coordinates are local to the source node; express both endpoints there.
        val origin = sourceNode.positionInRoot
        val start = sourceNode.boundsInRoot.center - origin
        val end = destinationNode.boundsInRoot.center - origin

        sourceHandle.performTouchInput {
            down(start)
            advanceEventTime(DRAG_HOLD_MS)
            repeat(times = DRAG_STEPS) { step ->
                moveTo(lerp(start = start, stop = end, fraction = (step + 1).toFloat() / DRAG_STEPS))
                advanceEventTime(DRAG_STEP_MS)
            }
            up()
        }
    }

    // The drag handle sits under an untagged wrapper inside TOKEN_NON_FIAT_BLOCK, so anchor on the
    // enclosing TOKEN_LIST_ITEM (one handle + one title per item) instead of the direct parent.
    private fun dragHandleMatcher(tokenTitle: String): SemanticsMatcher =
        withTestTag(OrganizeTokensScreenTestTags.DRAGGABLE_IMAGE) and
            withAnyAncestor(tokenItemMatcher(tokenTitle))

    private fun tokenItemMatcher(tokenTitle: String): SemanticsMatcher =
        withTestTag(OrganizeTokensScreenTestTags.TOKEN_LIST_ITEM) and hasAnyDescendant(withText(tokenTitle))

    private companion object {
        const val DRAG_HOLD_MS = 200L
        const val DRAG_STEPS = 16
        const val DRAG_STEP_MS = 16L
    }
}

internal fun BaseTestCase.onOrganizeTokensScreen(function: OrganizeTokensPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)