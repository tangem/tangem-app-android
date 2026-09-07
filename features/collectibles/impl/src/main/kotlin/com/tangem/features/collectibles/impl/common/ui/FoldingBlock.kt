package com.tangem.features.collectibles.impl.common.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier

private const val FOLD_MILLIS = 200

@Composable
internal fun FoldingBlock(isFolding: Boolean, content: @Composable ColumnScope.() -> Unit) {
    var isFolded by remember(isFolding) { mutableStateOf(false) }

    if (isFolding) {
        LaunchedEffect(Unit) {
            withFrameNanos {}
            isFolded = true
        }
    }

    // Only while folding: in the main list the same blocks change height on their own — the filler follows the
    // pack count — and animating that would drag the scroll position along with it.
    val modifier = if (isFolding) {
        Modifier.animateContentSize(animationSpec = tween(FOLD_MILLIS))
    } else {
        Modifier
    }

    Column(modifier = modifier) {
        if (!isFolded) content()
    }
}