package com.tangem.common.utils

import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import io.github.kakaocup.compose.node.element.lazylist.KLazyListItemNode

/**
 * Represents an item node in a LazyList for Compose UI testing.
 * Allows test actions (clicks, scrolls) and assertions on individual list items.
 */
class LazyListItemNode(
    semanticsNode: SemanticsNode,
    semanticsProvider: SemanticsNodeInteractionsProvider,
) : KLazyListItemNode<LazyListItemNode>(semanticsNode, semanticsProvider)