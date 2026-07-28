package com.tangem.common.extensions

import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsNodeInteractionCollection
import androidx.compose.ui.test.assertIsDisplayed

/** Returns the first non-blank text found in this node or its subtree (unmerged tree). */
fun SemanticsNode.firstTextOrNull(): String? {
    config.getOrNull(SemanticsProperties.Text)
        ?.firstOrNull()?.text?.takeIf { it.isNotBlank() }
        ?.let { return it }
    children.forEach { child -> child.firstTextOrNull()?.let { return it } }
    return null
}

/**
 * Reads the first text of every currently displayed node in this collection, in visual order
 * (top-to-bottom, then left-to-right). Non-displayed nodes and nodes without text are skipped.
 */
fun SemanticsNodeInteractionCollection.displayedTextsInVisualOrder(): List<String> {
    val count = fetchSemanticsNodes().size
    return (0 until count)
        .mapNotNull { index ->
            val interaction = get(index)
            if (runCatching { interaction.assertIsDisplayed() }.isFailure) return@mapNotNull null
            val node = interaction.fetchSemanticsNode()
            val text = node.firstTextOrNull() ?: return@mapNotNull null
            node.boundsInRoot to text
        }
        .sortedWith(compareBy({ it.first.top }, { it.first.left }))
        .map { it.second }
}