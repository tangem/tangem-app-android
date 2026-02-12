package com.tangem.common.extensions

import androidx.compose.ui.test.hasText
import io.github.kakaocup.compose.node.element.KNode

fun KNode.clickWithAssertion() {
    assertIsDisplayed()
    performClick()
}

fun KNode.assertTextContainsSafe(
    text: String,
    substring: Boolean = false,
    ignoreCase: Boolean = false,
) {
    assert(
        hasText(text = text, substring = substring, ignoreCase = ignoreCase)
    )
}

