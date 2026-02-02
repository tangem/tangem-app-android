package com.tangem.core.ui.utils

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

@Composable
fun TangemSharedTransitionLayout(
    modifier: Modifier = Modifier,
    content: @Composable SharedTransitionScope.() -> Unit,
) {
    SharedTransitionLayout(modifier) {
        val sharedTransitionScope = this
        CompositionLocalProvider(
            LocalSharedTransitionScope provides sharedTransitionScope,
        ) {
            content()
        }
    }
}

@Composable
fun ProvideSharedTransitionScope(modifier: Modifier = Modifier, content: @Composable SharedTransitionScope.() -> Unit) {
    val sharedTransitionScope = LocalSharedTransitionScope.current
    Box(modifier) {
        sharedTransitionScope.content()
    }
}

private val LocalSharedTransitionScope = staticCompositionLocalOf<SharedTransitionScope> { StubSharedTransitionScope() }

private class StubSharedTransitionScope : SharedTransitionScope {
    override val isTransitionActive: Boolean = false

    override val Placeable.PlacementScope.lookaheadScopeCoordinates: LayoutCoordinates
        get() = error("No LookaheadScopeCoordinates available in StubSharedTransitionScope")

    override fun Modifier.skipToLookaheadSize(enabled: () -> Boolean): Modifier {
        return this
    }

    override fun Modifier.renderInSharedTransitionScopeOverlay(
        zIndexInOverlay: Float,
        renderInOverlay: () -> Boolean,
    ): Modifier {
        return this
    }

    override fun Modifier.sharedElement(
        sharedContentState: SharedTransitionScope.SharedContentState,
        animatedVisibilityScope: AnimatedVisibilityScope,
        boundsTransform: BoundsTransform,
        placeholderSize: SharedTransitionScope.PlaceholderSize,
        renderInOverlayDuringTransition: Boolean,
        zIndexInOverlay: Float,
        clipInOverlayDuringTransition: SharedTransitionScope.OverlayClip,
    ): Modifier {
        return this
    }

    override fun Modifier.sharedBounds(
        sharedContentState: SharedTransitionScope.SharedContentState,
        animatedVisibilityScope: AnimatedVisibilityScope,
        enter: EnterTransition,
        exit: ExitTransition,
        boundsTransform: BoundsTransform,
        resizeMode: SharedTransitionScope.ResizeMode,
        placeholderSize: SharedTransitionScope.PlaceholderSize,
        renderInOverlayDuringTransition: Boolean,
        zIndexInOverlay: Float,
        clipInOverlayDuringTransition: SharedTransitionScope.OverlayClip,
    ): Modifier {
        return this
    }

    override fun Modifier.sharedElementWithCallerManagedVisibility(
        sharedContentState: SharedTransitionScope.SharedContentState,
        visible: Boolean,
        boundsTransform: BoundsTransform,
        placeholderSize: SharedTransitionScope.PlaceholderSize,
        renderInOverlayDuringTransition: Boolean,
        zIndexInOverlay: Float,
        clipInOverlayDuringTransition: SharedTransitionScope.OverlayClip,
    ): Modifier {
        return this
    }

    override fun OverlayClip(clipShape: Shape): SharedTransitionScope.OverlayClip {
        return object : SharedTransitionScope.OverlayClip {
            override fun getClipPath(
                sharedContentState: SharedTransitionScope.SharedContentState,
                bounds: Rect,
                layoutDirection: LayoutDirection,
                density: Density,
            ): Path? {
                return Path()
            }
        }
    }

    override fun LayoutCoordinates.toLookaheadCoordinates(): LayoutCoordinates {
        return this
    }
}