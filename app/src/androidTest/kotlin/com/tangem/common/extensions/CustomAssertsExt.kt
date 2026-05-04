package com.tangem.common.extensions

import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import com.tangem.common.utils.LazyListItemNode
import com.tangem.core.ui.components.buttons.actions.HasBadgeKey
import com.tangem.core.ui.components.buttons.actions.IsDimmedKey
import io.github.kakaocup.compose.node.element.KNode
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

fun assertElementDoesNotExist(
    elementProvider: () -> KNode,
    elementDescription: String,
) {
    try {
        elementProvider().assertExists()
        throw AssertionError("$elementDescription should not exist but was found")
    } catch (e: AssertionError) {
        val isNotFoundError = e.message?.let { message ->
            message.contains("No node found") ||
                message.contains("scrollable container") ||
                message.contains("There are no existing nodes") ||
                message.contains("There are no existing nodes for that selector")
        } ?: false
        if (isNotFoundError) {
            return
        } else {
            throw e
        }
    }
}

fun Any.assertIsDimmed(expectedValue: Boolean = true) {
    val matcher = SemanticsMatcher.expectValue(IsDimmedKey, expectedValue)
    when (this) {
        is KNode, is LazyListItemNode -> this.assert(matcher)
        else -> throw IllegalArgumentException("Unsupported type: ${this::class}")
    }
}

fun Any.assertHasBadge(expectedValue: Boolean = true) {
    val matcher = SemanticsMatcher.expectValue(HasBadgeKey, expectedValue)
    when (this) {
        is KNode, is LazyListItemNode -> this.assert(matcher)
        else -> throw IllegalArgumentException("Unsupported type: ${this::class}")
    }
}

fun List<SemanticsNode>.assertSortedByVolumeDescending() {
    val volumes = this.mapNotNull { parseVolume(it) }
    assertFalse("Trading volumes list should not be empty", volumes.isEmpty())
    assertTrue(
        "Exchanges list should be sorted by volume in descending order",
        volumes == volumes.sortedDescending(),
    )
}

fun List<SemanticsNode>.assertExchangeTypesAreCexOrDex() {
    assertFalse("Exchange types list should not be empty", isEmpty())
    forEach { node ->
        val text = extractText(node)
        assertTrue(
            "Exchange type should be 'CEX' or 'DEX', but found: $text",
            text == "CEX" || text == "DEX",
        )
    }
}

fun List<SemanticsNode>.assertTrustScoresValid() {
    val validScores = setOf("Risky", "Caution", "Trusted")
    assertFalse("Trust scores list should not be empty", isEmpty())
    forEach { node ->
        val text = extractText(node)
        assertTrue(
            "Trust score should be one of $validScores, but found: $text",
            text in validScores,
        )
    }
}

/**
 * Extracts the text content from a [KNode]'s semantics.
 * Useful when the displayed value is dynamic and you need to capture it for later assertions.
 */
fun KNode.extractText(): String {
    val node = delegate.interaction.semanticsNodeInteraction.fetchSemanticsNode(
        "Failed to extract text from KNode",
    )
    return extractText(node) ?: error("Node does not contain SemanticsProperties.Text")
}

/**
 * Extracts the text value from a semantic node's config.
 */
private fun extractText(node: SemanticsNode): String? {
    if (SemanticsProperties.Text in node.config) {
        return node.config[SemanticsProperties.Text].firstOrNull()?.text
    }
    return null
}

private fun parseVolume(node: SemanticsNode): Double? {
    val text = extractText(node) ?: return null
    return text.replace("[^0-9.]".toRegex(), "").toDoubleOrNull()
}