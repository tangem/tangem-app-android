package com.tangem.features.tangempay.common

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.launch

/** Scrolls the focused field into view with [footerHeight] of clearance so a pinned footer never covers it. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun Modifier.revealAboveFooter(footerHeight: Dp): Modifier {
    val requester = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()
    val footerPx = with(LocalDensity.current) { footerHeight.toPx() }
    var size by remember { mutableStateOf(IntSize.Zero) }
    return this
        .onSizeChanged { size = it }
        .bringIntoViewRequester(requester)
        .onFocusEvent { focusState ->
            if (focusState.isFocused && size != IntSize.Zero) {
                scope.launch {
                    requester.bringIntoView(
                        Rect(left = 0f, top = 0f, right = size.width.toFloat(), bottom = size.height + footerPx),
                    )
                }
            }
        }
}