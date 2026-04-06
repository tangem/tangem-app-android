package com.tangem.common.extensions

import android.os.SystemClock
import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
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

fun KNode.isDisplayedSafely(): Boolean {
    return try {
        assertIsDisplayed()
        true
    } catch (_: AssertionError) {
        false
    }
}

fun KNode.assertVisibility(shouldBeDisplayed: Boolean) {
    if (shouldBeDisplayed) {
        assertIsDisplayed()
    } else {
        assertIsNotDisplayed()
    }
}

fun KNode.clickAndWaitFor(
    rule: ComposeTestRule,
    timeoutMs: Long = 5_000,
    maxRetries: Int = 3,
    expectedCondition: () -> Unit,
) {
    for (attempt in 1..maxRetries) {
        performClick()
        rule.waitForIdle()

        try {
            rule.waitUntil(timeoutMs) {
                runCatching { expectedCondition() }.isSuccess
            }
            return
        } catch (_: ComposeTimeoutException) {
        }
    }

    throw AssertionError("Condition not met after $maxRetries click attempts")
}

fun KNode.performTextInputInChunks(
    text: String,
    chunkSize: Int = 2,
    delayBetweenChunksMs: Long = 100
) {
    val chunks = text.chunked(chunkSize)
    chunks.forEachIndexed { index, chunk ->
        performTextInput(chunk)
        if (index < chunks.lastIndex) {
            SystemClock.sleep(delayBetweenChunksMs)
        }
    }
}
