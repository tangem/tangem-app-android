package com.tangem.common.extensions

import androidx.compose.ui.test.SemanticsMatcher
import com.tangem.core.ui.utils.LazyListItemPositionSemantics
import io.github.kakaocup.compose.node.builder.ViewBuilder

fun ViewBuilder.hasLazyListItemPosition(position: Int) = apply {
    addSemanticsMatcher(SemanticsMatcher.expectValue(LazyListItemPositionSemantics, position))
}