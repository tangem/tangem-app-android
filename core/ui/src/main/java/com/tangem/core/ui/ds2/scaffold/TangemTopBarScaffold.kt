package com.tangem.core.ui.ds2.scaffold

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.systemBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalLayoutDirection
import com.tangem.core.ui.components.haze.hazeSourceTangem
import com.tangem.core.ui.res.TangemTheme

private enum class ScaffoldSlot { TopBar, Content, Overlay }

/**
 * Design-system v2 scaffold: [content] scrolls edge-to-edge under an overlaid, blurring [topBar].
 *
 * [topBar]'s height is measured (not fixed) and handed to [content] as [contentPadding], which [content]
 * must apply so it rests in the safe area yet still scrolls under the bar and system bars.
 *
 * @param modifier applied to the scaffold root.
 * @param containerColor background behind [content], and what the blur samples.
 * @param topBar bar overlaid on [content]; blurs the content beneath it (usually a top navigation).
 * @param overlay optional chrome drawn above [content] but below [topBar], outside the blur source —
 *   e.g. a band of pinned tabs or a custom fade shared by the bar and the band. Kept out of the haze
 *   source so blur effects never sample their own overlay back as a ghost. Receives the same padding
 *   as [content].
 * @param content screen body behind [topBar]; must consume [contentPadding].
 */
@Composable
fun TangemTopBarScaffold(
    modifier: Modifier = Modifier,
    containerColor: Color = TangemTheme.colors3.bg.primary,
    topBar: @Composable () -> Unit,
    overlay: (@Composable BoxScope.(contentPadding: PaddingValues) -> Unit)? = null,
    content: @Composable BoxScope.(contentPadding: PaddingValues) -> Unit,
) {
    val systemBars = WindowInsets.systemBars.asPaddingValues()
    val layoutDirection = LocalLayoutDirection.current

    SubcomposeLayout(modifier = modifier) { constraints ->
        val topBarPlaceables = subcompose(ScaffoldSlot.TopBar) {
            Box(Modifier.fillMaxWidth()) { topBar() }
        }.map { it.measure(constraints.copy(minWidth = 0, minHeight = 0)) }
        val topBarHeight = topBarPlaceables.maxOfOrNull { it.height } ?: 0

        val contentPadding = PaddingValues(
            top = topBarHeight.toDp(),
            bottom = systemBars.calculateBottomPadding(),
            start = systemBars.calculateStartPadding(layoutDirection),
            end = systemBars.calculateEndPadding(layoutDirection),
        )

        val contentPlaceables = subcompose(ScaffoldSlot.Content) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .hazeSourceTangem()
                    .background(containerColor),
            ) {
                content(contentPadding)
            }
        }.map { it.measure(constraints) }

        val overlayPlaceables = if (overlay == null) {
            emptyList()
        } else {
            subcompose(ScaffoldSlot.Overlay) {
                Box(modifier = Modifier.fillMaxSize()) {
                    overlay(contentPadding)
                }
            }.map { it.measure(constraints) }
        }

        layout(constraints.maxWidth, constraints.maxHeight) {
            contentPlaceables.forEach { it.place(x = 0, y = 0) }
            overlayPlaceables.forEach { it.place(x = 0, y = 0) }
            topBarPlaceables.forEach { it.place(x = 0, y = 0) }
        }
    }
}