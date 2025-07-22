package com.tangem.common.extensions

import io.github.kakaocup.compose.node.element.KNode

fun KNode.clickWithAssertion() {
    assertIsDisplayed()
    performClick()
}