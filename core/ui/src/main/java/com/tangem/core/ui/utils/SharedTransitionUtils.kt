package com.tangem.core.ui.utils

import androidx.compose.animation.*
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

/**
 * Default `true`: composables outside [ProvideSharedTransitionScope] keep previous behaviour.
 * Inside [ProvideSharedTransitionScope], becomes `true` after the wrapper has received attached
 * [LayoutCoordinates] from [Modifier.onGloballyPositioned].
 *
 * Workaround for Compose Animation: shared bounds may detach before coordinates exist inside SubcomposeLayout
 * slots (LazyColumn, Scaffold topBar, etc.).
 *
 * See [discussion](https://stackoverflow.com/questions/79466980/jetpack-compose-sharedbounds-inside-centeralignedtopappbar-crashes-on-first-scre).
 */
private val LocalSharedBoundsLayoutCoordinatesReady = compositionLocalOf { true }

/**
 * Crash-safe wrapper around [SharedTransitionScope.sharedBounds]: applies the modifier only after the enclosing
 * [ProvideSharedTransitionScope] has reported attached layout coordinates; otherwise returns the receiver unchanged.
 *
 * Outside [ProvideSharedTransitionScope] the readiness flag defaults to `true`, and the scope falls back to a stub
 * whose `sharedBounds` is a no-op, so the call is always safe.
 */
@Composable
fun Modifier.sharedBoundsSafely(
    sharedContentState: SharedTransitionScope.SharedContentState,
    animatedVisibilityScope: AnimatedVisibilityScope,
    boundsTransform: BoundsTransform,
    resizeMode: SharedTransitionScope.ResizeMode? = null,
): Modifier {
    if (!LocalSharedBoundsLayoutCoordinatesReady.current) return this
    val sharedTransitionScope = LocalSharedTransitionScope.current
    return with(sharedTransitionScope) {
        if (resizeMode != null) {
            this@sharedBoundsSafely.sharedBounds(
                sharedContentState = sharedContentState,
                animatedVisibilityScope = animatedVisibilityScope,
                boundsTransform = boundsTransform,
                resizeMode = resizeMode,
            )
        } else {
            this@sharedBoundsSafely.sharedBounds(
                sharedContentState = sharedContentState,
                animatedVisibilityScope = animatedVisibilityScope,
                boundsTransform = boundsTransform,
            )
        }
    }
}

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
    var isLayoutCoordinatesReady by remember { mutableStateOf(false) }
    Box(
        modifier.onGloballyPositioned { coordinates ->
            if (coordinates.isAttached) {
                isLayoutCoordinatesReady = true
            }
        },
    ) {
        CompositionLocalProvider(LocalSharedBoundsLayoutCoordinatesReady provides isLayoutCoordinatesReady) {
            sharedTransitionScope.content()
        }
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