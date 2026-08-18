package com.tangem.core.ui.ds2.modal

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.dialog
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.haze.hazeSourceTangem
import com.tangem.core.ui.components.sheetscaffold.CustomBottomSheet
import com.tangem.core.ui.components.sheetscaffold.TangemSheetState
import com.tangem.core.ui.components.sheetscaffold.TangemSheetValue
import com.tangem.core.ui.res.LocalHazeState
import com.tangem.core.ui.res.TangemTheme
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.*
import kotlin.time.Duration.Companion.seconds

/**
 * Registry of modals rendered by a [TangemModalHost]. Modals shown via [TangemModal] register
 * their content here (when a host is provided) instead of opening a separate dialog window, so
 * they live in the same window as the app UI and their material can blur it.
 *
 * The host's composition outlives the modal call sites: when a call site leaves composition while
 * its modal is still shown (e.g. a Decompose slot dismisses the component), the entry is
 * [retire]d — kept rendered just long enough to play the collapse animation — instead of
 * vanishing instantly.
 *
 * Create via [rememberTangemModalHostState], expose via [LocalTangemModalHost] and render via
 * [TangemModalHost] at the root of the window content (above everything the modals should cover).
 */
@Stable
class TangemModalHostState internal constructor(private val scope: CoroutineScope) {

    internal val entries = mutableStateListOf<Entry>()

    /** Whether at least one modal is currently registered. */
    val hasVisibleModal: Boolean
        get() = entries.isNotEmpty()

    @Immutable
    internal class Entry(val key: Any, val content: @Composable () -> Unit) {
        var isRetiring: Boolean = false
    }

    @PublishedApi
    internal fun show(key: Any, content: @Composable () -> Unit) {
        val entry = Entry(key = key, content = content)
        val index = entries.indexOfFirst { it.key == key }
        if (index >= 0) entries[index] = entry else entries += entry
    }

    @PublishedApi
    internal fun dismiss(key: Any) {
        entries.removeAll { it.key == key }
    }

    /**
     * Keeps the entry rendered while [exit] plays its closing animation, then removes it. Used
     * when the modal's call site leaves composition while the modal is still shown. Bounded by a
     * timeout so a stuck animation can't leak the entry.
     */
    @PublishedApi
    internal fun retire(key: Any, exit: suspend () -> Unit) {
        val entry = entries.firstOrNull { it.key == key } ?: return
        if (entry.isRetiring) return
        entry.isRetiring = true
        scope.launch {
            try {
                withTimeout(1.seconds) { exit() }
            } catch (expected: TimeoutCancellationException) {
                // The exit animation is best-effort; a stuck one must not leak the entry.
            } finally {
                dismiss(key)
            }
        }
    }
}

/**
 * Host of same-window modals. `null` means there is no host in the current window and [TangemModal]
 * falls back to its dialog-window presentation.
 */
val LocalTangemModalHost = staticCompositionLocalOf<TangemModalHostState?> { null }

/** Create and remember a [TangemModalHostState]. */
@Composable
fun rememberTangemModalHostState(): TangemModalHostState {
    val scope = rememberCoroutineScope()
    return remember { TangemModalHostState(scope = scope) }
}

/**
 * Renders the modals registered in [state], in registration order (the last shown modal is on
 * top). Place it above the content the modals should cover, and mark that content as the haze
 * source of [hazeState] (`Modifier.hazeSourceTangem(hazeState)`) so the modal material blurs it.
 *
 * @param state the registry of shown modals
 * @param hazeState haze state whose source is the content behind the modals; provided as
 *   [LocalHazeState] to modal content
 * @param modifier modifier for the host container; typically fills the window content
 */
@Composable
fun TangemModalHost(state: TangemModalHostState, hazeState: HazeState, modifier: Modifier = Modifier) {
    if (state.entries.isEmpty()) return
    CompositionLocalProvider(LocalHazeState provides hazeState) {
        Box(modifier = modifier) {
            state.entries.forEachIndexed { index, entry ->
                key(entry.key) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            // Each modal is a source layer above the covered content (layer 0),
                            // so the next modal's material samples this one too.
                            .hazeSourceTangem(state = hazeState, zIndex = (index + 1).toFloat(), key = entry.key),
                    ) {
                        entry.content()
                    }
                }
            }
        }
    }
}

/**
 * Same-window presentation scaffold of one modal: scrim, predictive back handling, show/dismiss
 * animations and the draggable sheet. The equivalent of the dialog window in the overlay world.
 */
@Composable
@PublishedApi
@Suppress("LongParameterList")
internal fun OverlayModalScaffold(
    sheetState: TangemSheetState,
    backProgress: Animatable<Float, AnimationVector1D>,
    openProgress: () -> Float,
    isDismissEnabled: Boolean,
    onDismissRequest: () -> Unit,
    onBack: (() -> Unit)?,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scope = rememberCoroutineScope()
    val isBackDismissEnabled = onBack != null || isDismissEnabled
    PredictiveBackHandler { progressEvents ->
        try {
            progressEvents.collect { event ->
                if (isBackDismissEnabled) backProgress.snapTo(event.progress)
            }
            when {
                // Committed: keep the folded state — the dismissal continues from it.
                onBack != null -> onBack()
                isDismissEnabled -> onDismissRequest()
                // else: back is blocked, the gesture previewed nothing — nothing to restore.
            }
        } catch (expected: CancellationException) {
            // Cancelled: unfold the card back into place.
            scope.launch { backProgress.animateTo(targetValue = 0f) }
        }
    }

    // Anchors appear after the first layout pass; only then the sheet can animate in.
    if (sheetState.hasExpandedState) {
        LaunchedEffect(sheetState) { sheetState.show() }
    }

    // Dragging the sheet to hidden dismisses the modal — but only after it has been shown,
    // since the sheet starts in the hidden state.
    LaunchedEffect(sheetState) {
        var wasVisible = false
        snapshotFlow { sheetState.currentValue }.collect { value ->
            if (value != TangemSheetValue.Hidden) {
                wasVisible = true
            } else if (wasVisible) {
                onDismissRequest()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = openProgress() }
                .background(TangemTheme.colors3.overlay.modal)
                .pointerInput(isDismissEnabled) {
                    detectTapGestures {
                        if (isDismissEnabled) onDismissRequest()
                    }
                },
        )
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            CustomBottomSheet(
                state = sheetState,
                peekHeight = 0.dp,
                modifier = Modifier.semantics { dialog() },
            ) {
                content()
            }
        }
    }
}