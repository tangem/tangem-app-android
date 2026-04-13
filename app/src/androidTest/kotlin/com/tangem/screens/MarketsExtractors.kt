package com.tangem.screens

import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties

/**
 * Helpers for extracting plain-text values from Compose semantic nodes used in Markets tests.
 */
internal object MarketsExtractors {

    /**
     * Returns the first non-blank text either on the node itself or in any of its descendants.
     * Useful for compound nodes (e.g. PriceChangeInPercent) where the textual value lives in a
     * child Text composable, not the wrapper.
     */
    fun extractTextRecursively(node: SemanticsNode): String? {
        val own = if (SemanticsProperties.Text in node.config) {
            node.config[SemanticsProperties.Text].firstOrNull()?.text
        } else {
            null
        }
        if (!own.isNullOrBlank()) return own
        node.children.forEach { child ->
            val nested = extractTextRecursively(child)
            if (!nested.isNullOrBlank()) return nested
        }
        return null
    }
}