package com.tangem.common.extensions

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