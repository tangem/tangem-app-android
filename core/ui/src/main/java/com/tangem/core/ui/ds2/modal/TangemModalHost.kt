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
import androidx.compose.ui.layout.Layout
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

    /** Modals registered for warm-up only: composed and measured by [TangemModalHost], never shown. */
    internal val prewarmEntries = mutableStateListOf<Entry>()

    /** Whether at least one modal is currently on screen; warm-up entries are not on screen. */
    val hasVisibleModal: Boolean
        get() = entries.isNotEmpty()

    @Immutable
    internal class Entry(val key: Any, val content: @Composable () -> Unit) {
        var isRetiring: Boolean = false
    }

    @PublishedApi
    internal fun show(key: Any, content: @Composable () -> Unit) {
        entries.upsert(key = key, content = content)
    }

    @PublishedApi
    internal fun dismiss(key: Any) {
        entries.removeAll { it.key == key }
    }

    /**
     * Registers [content] to be composed and measured without ever being shown, so that the first
     * real appearance of a modal no longer pays for it.
     *
     * The first modal opened in a fresh process starts moving a few hundred milliseconds after the
     * tap, while every later one — even a different modal with different content — starts within
     * ~100ms: the price is the one-off warm-up of the shared machinery (classes and their JIT, the
     * layout and shape nodes, the surface material, the intrinsic height probe of the content), not
     * the content of the particular sheet. Paying it while the viewer is still reading the screen
     * makes their first tap behave like their second.
     *
     * A prewarmed entry is invisible and inert — [TangemModalHost] measures it but never places it,
     * and it carries neither scrim nor back handler. What it does *not* suppress is composition
     * itself: effects, animations and image loads inside the content do run. Hence opt-in per call
     * site (see the `prewarm` parameter of [TangemModal]).
     */
    @PublishedApi
    internal fun prewarm(key: Any, content: @Composable () -> Unit) {
        prewarmEntries.upsert(key = key, content = content)
    }

    /** Drops the warm-up entry of [key] — it is either being shown for real now, or is gone. */
    @PublishedApi
    internal fun stopPrewarm(key: Any) {
        prewarmEntries.removeAll { it.key == key }
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

    private fun MutableList<Entry>.upsert(key: Any, content: @Composable () -> Unit) {
        val entry = Entry(key = key, content = content)
        val index = indexOfFirst { it.key == key }
        if (index >= 0) set(index, entry) else add(entry)
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
 * Modals registered for warm-up only (see [TangemModalHostState.prewarm]) are rendered in a
 * separate layer that is measured but never placed, so they cost composition and measurement and
 * nothing else. With none of those registered the host tree is exactly what it was before.
 *
 * @param state the registry of shown modals
 * @param hazeState haze state whose source is the content behind the modals; provided as
 *   [LocalHazeState] to modal content
 * @param modifier modifier for the host container; typically fills the window content
 */
@Composable
fun TangemModalHost(state: TangemModalHostState, hazeState: HazeState, modifier: Modifier = Modifier) {
    if (state.entries.isEmpty() && state.prewarmEntries.isEmpty()) return
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
            if (state.prewarmEntries.isNotEmpty()) {
                ModalPrewarmLayer(entries = state.prewarmEntries)
            }
        }
    }
}

/**
 * Renders prewarmed modals measured but never placed.
 *
 * Composition and measurement are the expensive halves of a first modal appearance — class loading
 * and JIT of the sheet machinery, the intrinsic height probe, the text layout of the content — and
 * both of them happen here. Skipping the placement is what keeps the warm-up invisible: an unplaced
 * placeable is not drawn, not hit-tested and not reachable by semantics, so neither the screen nor
 * the viewer's taps ever meet it. The layer reports a zero size, so it cannot influence the host's
 * own layout either, and the modals are measured against the host's constraints — the same ones the
 * real appearance will get.
 */
@Composable
private fun ModalPrewarmLayer(entries: List<TangemModalHostState.Entry>) {
    Layout(
        content = {
            entries.forEach { entry ->
                key(entry.key) { entry.content() }
            }
        },
        measurePolicy = { measurables, constraints ->
            measurables.forEach { it.measure(constraints) }
            layout(width = 0, height = 0) {}
        },
    )
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
                // Committed: keep the previewed offset — the dismissal continues from it.
                onBack != null -> onBack()
                isDismissEnabled -> onDismissRequest()
                // else: back is blocked, the gesture previewed nothing — nothing to restore.
            }
        } catch (expected: CancellationException) {
            // Cancelled: slide the card back into place.
            scope.launch { backProgress.animateTo(targetValue = 0f) }
        }
    }

    // Anchors appear after the first layout pass; only then the sheet can animate in.
    if (sheetState.hasExpandedState) {
        LaunchedEffect(sheetState) { sheetState.show() }
    }

    // Dragging the sheet to hidden dismisses the modal — but only once it has settled there, after
    // the finger is lifted. The draggable's currentValue flips to Hidden halfway through the drag,
    // and dismissing on that takes the modal away from under the finger mid-gesture.
    // "Was visible" is read off the target, not the settled value, so a sheet swiped away before
    // its show animation finished still reports its dismissal instead of staying parked offscreen.
    LaunchedEffect(sheetState) {
        var wasVisible = false
        snapshotFlow { sheetState.targetValue to sheetState.settledValue }.collect { (target, settled) ->
            when {
                target != TangemSheetValue.Hidden -> wasVisible = true
                wasVisible && settled == TangemSheetValue.Hidden -> onDismissRequest()
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                // The sheet must be laid out to produce its drag anchors, but until those exist it
                // has no offset to be placed at — stay invisible for that one pass instead of
                // flashing a card parked at the top of the window.
                .graphicsLayer { alpha = if (sheetState.hasExpandedState) 1f else 0f },
            contentAlignment = Alignment.TopCenter,
        ) {
            CustomBottomSheet(
                state = sheetState,
                peekHeight = 0.dp,
                modifier = Modifier
                    .semantics { dialog() }
                    // The predictive-back gesture previews the dismissal by sliding the sheet down
                    // a share of its height; the sheet's own offset is untouched, so committing
                    // continues the slide from here and cancelling brings it back.
                    .graphicsLayer {
                        translationY = backProgress.value * MODAL_BACK_PREVIEW_FRACTION * size.height
                    },
            ) {
                content()
            }
        }
    }
}