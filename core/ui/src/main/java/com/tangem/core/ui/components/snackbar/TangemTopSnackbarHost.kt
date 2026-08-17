@file:Suppress("MagicNumber")
package com.tangem.core.ui.components.snackbar

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.AccessibilityManager
import androidx.compose.ui.platform.LocalAccessibilityManager
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.core.ui.res.TangemTheme
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * Snackbar host that displays a [TangemTopSnackbar] sliding and scaling in from the top of the screen.
 * Typical placement: at the top of the screen's root [Box], above the main content.
 *
 * ```
 * Box(Modifier.fillMaxSize()) {
 *     MainContent()
 *     TangemTopSnackbarHost(
 *         hostState = topSnackbarHostState,
 *         modifier = Modifier
 *             .align(Alignment.TopCenter)
 *             .statusBarsPadding()
 *             .padding(top = TangemTheme.dimens.spacing8),
 *     )
 * }
 * ```
 *
 * @param hostState state that controls which snackbar is shown
 * @param modifier  modifier applied to the host container
 */
@Composable
fun TangemTopSnackbarHost(hostState: TangemTopSnackbarHostState, modifier: Modifier = Modifier) {
    val accessibilityManager = LocalAccessibilityManager.current
    val myDepth = remember(hostState) { hostState.registerHost(accessibilityManager) }
    DisposableEffect(hostState) {
        onDispose { hostState.unregisterHost(myDepth) }
    }

    // Only the host that owns the currently shown snackbar displays it.
    val currentSnackbar = if (hostState.isSnackbarHost(myDepth)) hostState.currentSnackbar else null

    ScaleFromTopWithFade(
        current = currentSnackbar,
        modifier = modifier,
    ) { snackbar ->
        TangemTopSnackbar(
            snackbarMessage = snackbar,
            modifier = Modifier.padding(horizontal = TangemTheme.dimens.spacing16),
        )
    }
}

private fun SnackbarMessage.Duration.toMillis(hasAction: Boolean, accessibilityManager: AccessibilityManager?): Long {
    val original =
        when (this) {
            SnackbarMessage.Duration.Indefinite -> Long.MAX_VALUE
            SnackbarMessage.Duration.Long -> 10000L
            SnackbarMessage.Duration.Short -> 4000L
        }
    if (accessibilityManager == null) {
        return original
    }
    return accessibilityManager.calculateRecommendedTimeoutMillis(
        originalTimeoutMillis = original,
        containsIcons = true,
        containsText = true,
        containsControls = hasAction,
    )
}

@Stable
class TangemTopSnackbarHostState {

    private val mutex = Mutex()

    var currentSnackbar by mutableStateOf<SnackbarMessage?>(null)
        private set

    // Depth of the host that displays [currentSnackbar]. It is captured once, when the snackbar is shown, so a host
    // registered later (e.g. a bottom sheet opened on top) does not steal an already displayed snackbar.
    // Composed as observable state so hosts recompose when the ownership changes.
    private var snackbarHostDepth by mutableIntStateOf(0)

    private var hostDepthCounter = 0

    private var accessibilityManager: AccessibilityManager? = null

    internal fun registerHost(accessibilityManager: AccessibilityManager?): Int {
        this.accessibilityManager = accessibilityManager
        hostDepthCounter++
        return hostDepthCounter
    }

    internal fun unregisterHost(depth: Int) {
        if (depth == hostDepthCounter) {
            hostDepthCounter--
            // The host that displayed the snackbar is gone, pass the snackbar to the deepest one left.
            if (snackbarHostDepth > hostDepthCounter) {
                snackbarHostDepth = hostDepthCounter
            }
        }
    }

    internal fun isSnackbarHost(depth: Int): Boolean = depth == snackbarHostDepth

    suspend fun showSnackbar(
        message: String,
        actionLabel: String? = null,
        withDismissAction: Boolean = false,
        duration: SnackbarMessage.Duration =
            if (actionLabel == null) SnackbarMessage.Duration.Short else SnackbarMessage.Duration.Indefinite,
    ) {
        showSnackbar(
            SnackbarMessage(
                message = stringReference(message),
                actionLabel = actionLabel?.let { stringReference(it) },
                duration = duration,
                onDismissRequest = {},
                action = if (withDismissAction) {
                    null
                } else {
                    {}
                },
            ),
        )
    }

    suspend fun showSnackbar(message: SnackbarMessage) {
        mutex.withLock {
            try {
                snackbarHostDepth = hostDepthCounter

                // The timeout is counted here and not in the host, so that changing the displaying host does not
                // prolong the lifetime of the snackbar.
                val timeout = message.duration.toMillis(
                    hasAction = message.action != null,
                    accessibilityManager = accessibilityManager,
                )
                val isTimedOut = withTimeoutOrNull(timeout) {
                    suspendCancellableCoroutine { continuation ->
                        currentSnackbar = message.withDismiss(
                            onDismiss = { if (continuation.isActive) continuation.resume(Unit) },
                        )
                    }
                } == null

                // Dismissal by an action or by a request is reported by the message itself, timeout is not.
                if (isTimedOut) message.onDismissRequest()
            } finally {
                currentSnackbar = null
            }
        }
    }
}

private fun SnackbarMessage.withDismiss(onDismiss: () -> Unit): SnackbarMessage {
    return copy(
        onDismissRequest = {
            onDismissRequest()
            onDismiss()
        },
        action = if (action != null) {
            {
                action.invoke()
                onDismiss()
            }
        } else {
            null
        },
    )
}

// Adapted from Material3's FadeInFadeOutWithScale, with scale pivot anchored at the top center.
@Suppress("UnsafeCallOnNullableType")
@Composable
private fun ScaleFromTopWithFade(
    current: SnackbarMessage?,
    modifier: Modifier = Modifier,
    content: @Composable (SnackbarMessage) -> Unit,
) {
    val state = remember { ScaleFromTopState<SnackbarMessage?>() }
    if (current != state.current) {
        state.current = current
        val keys = state.items.map { it.key }.toMutableList()
        if (!keys.contains(current)) keys.add(current)
        state.items.clear()
        keys.filterNotNull().mapTo(state.items) { key ->
            ScaleFromTopItem(key) { children ->
                val isVisible = key == current
                val opacity = animatedOpacity(
                    animation = tween(durationMillis = 200),
                    visible = isVisible,
                    onAnimationFinish = {
                        if (key != state.current) {
                            state.items.removeAll { it.key == key }
                            state.scope?.invalidate()
                        }
                    },
                )
                val scale = animatedScale(
                    enterAnimation = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium,
                    ),
                    exitAnimation = tween(durationMillis = 180),
                    visible = isVisible,
                )
                Box(
                    Modifier.graphicsLayer(
                        scaleX = scale.value,
                        scaleY = scale.value,
                        alpha = opacity.value,
                        transformOrigin = TransformOrigin(pivotFractionX = 0.5f, pivotFractionY = 0f),
                    ),
                ) {
                    children()
                }
            }
        }
    }
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        state.scope = currentRecomposeScope
        state.items.forEach { (item, transition) -> key(item) { transition { content(item!!) } } }
    }
}

@Suppress("DoubleMutabilityForCollection")
private class ScaleFromTopState<T> {
    var current: Any? = Any()
    var items = mutableListOf<ScaleFromTopItem<T>>()
    var scope: RecomposeScope? = null
}

private data class ScaleFromTopItem<T>(
    val key: T,
    val transition: @Composable (content: @Composable () -> Unit) -> Unit,
)

@Composable
private fun animatedOpacity(
    animation: AnimationSpec<Float>,
    visible: Boolean,
    onAnimationFinish: () -> Unit = {},
): State<Float> {
    val alpha = remember { Animatable(if (visible) 0f else 1f) }
    LaunchedEffect(visible) {
        alpha.animateTo(if (visible) 1f else 0f, animationSpec = animation)
        onAnimationFinish()
    }
    return alpha.asState()
}

@Composable
private fun animatedScale(
    enterAnimation: AnimationSpec<Float>,
    exitAnimation: AnimationSpec<Float>,
    visible: Boolean,
): State<Float> {
    val scale = remember { Animatable(if (visible) 0.85f else 1f) }
    LaunchedEffect(visible) {
        scale.animateTo(
            targetValue = if (visible) 1f else 0.85f,
            animationSpec = if (visible) enterAnimation else exitAnimation,
        )
    }
    return scale.asState()
}