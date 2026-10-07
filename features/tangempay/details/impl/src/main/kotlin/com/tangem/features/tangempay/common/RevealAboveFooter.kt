package com.tangem.features.tangempay.common

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/** Scrolls the focused field into view with [footerHeight] of clearance so a pinned footer never covers it. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun Modifier.revealAboveFooter(footerHeight: Dp): Modifier {
    val requester = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val footerPx = with(density) { footerHeight.toPx() }
    val ime = WindowInsets.ime
    var size by remember { mutableStateOf(IntSize.Zero) }
    var isFocused by remember { mutableStateOf(false) }
    val revealRect: (IntSize) -> Rect = { fieldSize ->
        Rect(left = 0f, top = 0f, right = fieldSize.width.toFloat(), bottom = fieldSize.height + footerPx)
    }
    val reveal: (IntSize) -> Unit = { fieldSize ->
        scope.launch { requester.bringIntoView(revealRect(fieldSize)) }
    }
    LaunchedEffect(requester, ime, density, footerPx) {
        snapshotFlow { ime.getBottom(density) }.collectLatest {
            if (isFocused && size != IntSize.Zero) requester.bringIntoView(revealRect(size))
        }
    }
    return this
        .onSizeChanged { newSize ->
            if (isFocused && newSize.height > size.height) reveal(newSize)
            size = newSize
        }
        .bringIntoViewRequester(requester)
        .onFocusEvent { focusState ->
            isFocused = focusState.isFocused
            if (focusState.isFocused && size != IntSize.Zero) reveal(size)
        }
}