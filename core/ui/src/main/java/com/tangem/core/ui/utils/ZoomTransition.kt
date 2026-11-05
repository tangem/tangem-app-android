package com.tangem.core.ui.utils

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.SharedTransitionScope.ResizeMode.Companion.scaleToBounds
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.round
import kotlin.math.roundToInt

private val LocalZoomHosts = staticCompositionLocalOf<List<ZoomHost>> { emptyList() }

/**
 * Hosts one child of a stack that zooms between its children, like iOS zoom transitions: the root app stack, or
 * a stack nested in a screen (the feed inside the shtorka). [sharedTransitionScope] wraps the whole stack,
 * [animatedVisibilityScope] is this child's enter/exit scope.
 *
 * Hosts nest. A [zoomTransitionSource] registers with every enclosing host, so the same view can zoom into a
 * screen of its own stack or of an outer one; a [zoomTransitionDestination] matches in the nearest host.
 */
@Composable
fun ZoomTransitionChild(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    content: @Composable () -> Unit,
) {
    val host = remember(sharedTransitionScope, animatedVisibilityScope) {
        ZoomHost(sharedTransitionScope = sharedTransitionScope, animatedVisibilityScope = animatedVisibilityScope)
    }

    CompositionLocalProvider(LocalZoomHosts provides LocalZoomHosts.current + host) {
        Box(modifier = Modifier.onGloballyPositioned(host::onPositioned)) {
            content()

            host.sources.forEach { (key, source) ->
                ZoomSourceStandIn(key = key, source = source, host = host)
            }
        }
    }
}

/**
 * Marks the view a screen zooms out of. Push the screen with
 * [com.tangem.core.decompose.navigation.pushZooming] and this [key] to zoom into it; going back zooms into this
 * view again.
 *
 * The shared bounds live on a stand-in at each enclosing [ZoomTransitionChild], not on this view: Compose takes
 * the size of shared bounds from the untransformed layout, so a view under an ancestor's scale layer (the
 * semi-open shtorka scales its content) would jump when the zoom starts. The stand-in sits at the view's bounds
 * as its host sees them and draws its recording, while the view itself hides for the duration of the zoom.
 */
@Composable
fun Modifier.zoomTransitionSource(key: String, shape: Shape): Modifier {
    val hosts = LocalZoomHosts.current
    if (hosts.isEmpty()) return this

    val layer = rememberGraphicsLayer()
    val source = remember(layer, shape) { ZoomSource(layer = layer, shape = shape) }

    DisposableEffect(hosts, key, source) {
        hosts.forEach { host -> host.sources[key] = source }
        onDispose {
            hosts.forEach { host ->
                host.sources.remove(key, source)
                source.inFlightByHost.remove(host)
            }
        }
    }

    return onGloballyPositioned(source::onPositioned)
        .drawWithContent {
            layer.record { this@drawWithContent.drawContent() }
            if (!source.isInFlight()) drawLayer(layer)
        }
}

/**
 * The screen side of [zoomTransitionSource]; a `null` [key] leaves the screen to its stack's own animation.
 * [isZooming] is false while the screen is revealed or covered by another one, which is not a zoom of its own.
 */
@Composable
fun Modifier.zoomTransitionDestination(key: String?, shape: Shape, isZooming: Boolean = true): Modifier {
    if (key == null) return this
    val host = LocalZoomHosts.current.lastOrNull() ?: return this
    val sharedContentState = host.sharedTransitionScope.rememberSharedContentState(key = key)

    // Shared bounds without a match skip their enter and exit, and the stack doesn't slide a zoomed screen, so a
    // source gone by the time of the zoom (its block hid) would cut instead. A plain fade covers that case.
    val fallbackAlpha by host.animatedVisibilityScope.transition.animateFloat(
        transitionSpec = { tween() },
        label = "zoom fallback alpha",
    ) { state -> if (state == EnterExitState.Visible) 1f else 0f }

    return graphicsLayer { alpha = if (!isZooming || sharedContentState.isMatchFound) 1f else fallbackAlpha }
        .zoomBounds(host = host, sharedContentState = sharedContentState, shape = shape)
}

@Composable
private fun ZoomSourceStandIn(key: String, source: ZoomSource, host: ZoomHost) {
    val sharedTransitionScope = host.sharedTransitionScope
    val sharedContentState = sharedTransitionScope.rememberSharedContentState(key = key)
    val childTransition = host.animatedVisibilityScope.transition
    SideEffect {
        // isMatchFound is not observable, so the source would never redraw to hide itself. The child's own
        // transition states are, and they flip on exactly the frames a push or pop starts and settles.
        source.inFlightByHost[host] = {
            childTransition.currentState != childTransition.targetState &&
                sharedTransitionScope.isTransitionActive &&
                sharedContentState.isMatchFound
        }
    }

    Box(
        modifier = Modifier
            .layout { measurable, _ ->
                val bounds = source.boundsIn(host)
                val placeable = measurable.measure(
                    Constraints.fixed(width = bounds.width.roundToInt(), height = bounds.height.roundToInt()),
                )
                layout(placeable.width, placeable.height) {
                    placeable.place(bounds.topLeft.round())
                }
            }
            .zoomBounds(host = host, sharedContentState = sharedContentState, shape = source.shape)
            .drawWithContent {
                val recordedWidth = source.layer.size.width
                if (source.inFlightByHost[host]?.invoke() == true && recordedWidth > 0) {
                    scale(scale = size.width / recordedWidth, pivot = Offset.Zero) {
                        drawLayer(source.layer)
                    }
                }
            },
    )
}

private fun Modifier.zoomBounds(
    host: ZoomHost,
    sharedContentState: SharedTransitionScope.SharedContentState,
    shape: Shape,
): Modifier = with(host.sharedTransitionScope) {
    this@zoomBounds.sharedBounds(
        sharedContentState = sharedContentState,
        animatedVisibilityScope = host.animatedVisibilityScope,
        enter = fadeIn(),
        exit = fadeOut(),
        resizeMode = scaleToBounds(contentScale = ContentScale.FillWidth, alignment = Alignment.TopCenter),
        clipInOverlayDuringTransition = OverlayClip(shape),
    )
}

@Stable
private class ZoomHost(
    val sharedTransitionScope: SharedTransitionScope,
    val animatedVisibilityScope: AnimatedVisibilityScope,
) {
    val sources = mutableStateMapOf<String, ZoomSource>()
    var coordinates: LayoutCoordinates? = null
        private set
    var boundsInRoot by mutableStateOf(Rect.Zero)
        private set

    fun onPositioned(coordinates: LayoutCoordinates) {
        this.coordinates = coordinates
        boundsInRoot = coordinates.boundsInRoot()
    }
}

@Stable
private class ZoomSource(val layer: GraphicsLayer, val shape: Shape) {
    private var coordinates: LayoutCoordinates? = null
    private var boundsInRoot by mutableStateOf(Rect.Zero)
    val inFlightByHost = mutableStateMapOf<ZoomHost, () -> Boolean>()

    fun isInFlight(): Boolean = inFlightByHost.values.any { isInFlight -> isInFlight() }

    fun onPositioned(coordinates: LayoutCoordinates) {
        this.coordinates = coordinates
        boundsInRoot = coordinates.boundsInRoot()
    }

    /**
     * Bounds in [host]'s space, with every scale and offset between the two applied. Reads the root bounds of
     * both only to relayout the caller when either moves — the coordinates themselves are not state.
     */
    fun boundsIn(host: ZoomHost): Rect {
        if (boundsInRoot == Rect.Zero || host.boundsInRoot == Rect.Zero) return Rect.Zero
        val hostCoordinates = host.coordinates?.takeIf { it.isAttached } ?: return Rect.Zero
        val sourceCoordinates = coordinates?.takeIf { it.isAttached } ?: return Rect.Zero
        return hostCoordinates.localBoundingBoxOf(sourceCoordinates, clipBounds = false)
    }
}