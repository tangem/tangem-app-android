package com.tangem.common.extensions

import androidx.compose.ui.test.SemanticsMatcher
import com.tangem.common.utils.LazyListItemNode
import com.tangem.core.ui.components.buttons.actions.HasBadgeKey
import com.tangem.core.ui.components.buttons.actions.IsDimmedKey
import io.github.kakaocup.compose.node.element.KNode

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