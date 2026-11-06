package com.tangem.core.ui.ds2.draggable

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.positionChangeIgnoreConsumed
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.seconds

/**
 * Creates a [TangemDraggableState] that reorders the items of a vertical `LazyColumn` driven by
 * [listState]. Pair it with the `LazyItemScope.TangemDraggable` overload on every reorderable item.
 *
 * @param listState The same state passed to the `LazyColumn`. Reverse layout is not supported.
 * @param onMove Called while dragging, each time the dragged item passes others. `from` and `to` are
 *   `LazyColumn` indices and describe a **move** (`add(to, removeAt(from))`), not a swap; `to` may be
 *   several items away. The caller must apply it to the list the `LazyColumn` renders — until the new
 *   order is laid out, further moves wait.
 * @param onDragEnd Called the moment the finger is lifted, before the item settles into its slot.
 *   Persist the final order here.
 */
@Suppress("MagicNumber")
@Composable
fun rememberTangemDraggableState(
    listState: LazyListState,
    onMove: (from: Int, to: Int) -> Unit,
    onDragEnd: () -> Unit = {},
): TangemDraggableState {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val currentOnMove by rememberUpdatedState(onMove)
    val currentOnDragEnd by rememberUpdatedState(onDragEnd)

    return remember(listState, scope, density) {
        TangemDraggableState(
            listState = listState,
            scope = scope,
            onMove = { from, to -> currentOnMove(from, to) },
            onDragEnd = { currentOnDragEnd() },
            scrollZonePx = with(density) { 48.dp.toPx() },
            maxScrollSpeedPx = with(density) { 800.dp.toPx() },
        )
    }
}

/** Drag-to-reorder state of a vertical `LazyColumn`, created by [rememberTangemDraggableState]. */
@Stable
class TangemDraggableState internal constructor(
    private val listState: LazyListState,
    private val scope: CoroutineScope,
    private val onMove: (from: Int, to: Int) -> Unit,
    private val onDragEnd: () -> Unit,
    private val scrollZonePx: Float,
    private val maxScrollSpeedPx: Float,
) {

    var draggingKey: Any? by mutableStateOf(null)
        private set

    internal val dropTargets = HashSet<Any>()

    private var dragStartTop = 0f
    private var dragDelta by mutableFloatStateOf(0f)
    private var grabY = 0f
    private val draggedTop: Float get() = dragStartTop + dragDelta

    private var settlingKey: Any? by mutableStateOf(null)
    private var settleFromTop = 0f
    private var settleProgress by mutableFloatStateOf(1f)

    private var isMoveInFlight = false
    private var moveJob: Job? = null
    private var autoScrollJob: Job? = null
    private var settleJob: Job? = null

    private val LazyListItemInfo.center: Float get() = offset + size / 2f

    internal fun isLifted(key: Any): Boolean = key == draggingKey || key == settlingKey

    internal fun translationOf(key: Any): Float {
        val slotTop = itemInfo(key)?.offset ?: return 0f
        return when (key) {
            draggingKey -> draggedTop - slotTop
            settlingKey -> (settleFromTop - slotTop) * (1f - settleProgress)
            else -> 0f
        }
    }

    internal fun onDragStart(key: Any, grabY: Float): Boolean {
        if (draggingKey != null) return false
        val item = itemInfo(key) ?: return false

        dragStartTop = item.offset + translationOf(key)
        if (key == settlingKey) {
            settleJob?.cancel()
            settlingKey = null
        }
        dragDelta = 0f
        this.grabY = grabY
        draggingKey = key
        return true
    }

    internal fun onDrag(deltaY: Float) {
        if (draggingKey == null) return
        dragDelta += deltaY
        moveIfPassed()
        startAutoScrollIfNeeded()
    }

    internal fun onDragStop() {
        val key = draggingKey ?: return
        moveJob?.cancel()
        autoScrollJob?.cancel()
        isMoveInFlight = false

        settleJob?.cancel()
        settleFromTop = draggedTop
        settleProgress = 0f
        settlingKey = key
        draggingKey = null
        onDragEnd()

        settleJob = scope.launch {
            animate(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
            ) { value, _ -> settleProgress = value }
            if (settlingKey == key) settlingKey = null
        }
    }

    // A target is passed once the dragged item's leading edge crosses its centre. After the move the
    // way back needs the opposite edge to cross the same centre, so items of different heights don't
    // ping-pong.
    private fun moveIfPassed() {
        if (isMoveInFlight) return
        val dragged = itemInfo(draggingKey) ?: return
        val top = draggedTop
        val bottom = top + dragged.size
        val items = listState.layoutInfo.visibleItemsInfo

        val target = items.lastOrNull { it.index > dragged.index && it.key in dropTargets && bottom > it.center }
            ?: items.firstOrNull { it.index < dragged.index && it.key in dropTargets && top < it.center }
            ?: return

        move(from = dragged.index, to = target.index)
    }

    private fun move(from: Int, to: Int) {
        val firstVisibleIndex = listState.firstVisibleItemIndex
        val firstVisibleOffset = listState.firstVisibleItemScrollOffset
        // The list keeps its first visible item in place, so moving that item would scroll the list
        // along with it. A scroll request only lives until the next remeasure, which may come before
        // the caller's new order does, hence it is repeated until the move lands.
        val isAnchorNeeded = from == firstVisibleIndex || to == firstVisibleIndex

        isMoveInFlight = true
        onMove(from, to)
        moveJob = scope.launch(start = CoroutineStart.UNDISPATCHED) {
            withTimeoutOrNull(1.seconds) {
                while (itemInfo(draggingKey)?.index == from) {
                    if (isAnchorNeeded) listState.requestScrollToItem(firstVisibleIndex, firstVisibleOffset)
                    withFrameNanos { }
                }
            }
            isMoveInFlight = false
            moveIfPassed()
        }
    }

    @Suppress("MagicNumber")
    private fun startAutoScrollIfNeeded() {
        if (autoScrollJob?.isActive == true || autoScrollSpeed() == 0f) return

        autoScrollJob = scope.launch {
            var lastFrameNanos = withFrameNanos { it }
            var isScrolling = true
            while (isScrolling) {
                val frameNanos = withFrameNanos { it }
                val elapsedSeconds = (frameNanos - lastFrameNanos) / 1_000_000_000f
                lastFrameNanos = frameNanos
                isScrolling = isMoveInFlight || autoScrollStep(elapsedSeconds)
            }
        }
    }

    /** Returns `false` once there is nothing left to scroll. */
    private suspend fun autoScrollStep(elapsedSeconds: Float): Boolean {
        val speed = autoScrollSpeed()
        val slot = itemInfo(draggingKey)
        if (speed == 0f || slot == null) return false

        val consumed = try {
            listState.scrollBy(keepSlotVisible(speed * elapsedSeconds, slot))
        } catch (ignored: CancellationException) {
            // Thrown when a user scroll still holds the list's mutator; give up this step.
            currentCoroutineContext().ensureActive()
            0f
        }
        if (consumed == 0f) return false
        moveIfPassed()
        return true
    }

    private fun autoScrollSpeed(): Float {
        if (draggingKey == null) return 0f
        val layoutInfo = listState.layoutInfo
        val fingerY = draggedTop + grabY
        val contentEnd = layoutInfo.viewportEndOffset - layoutInfo.afterContentPadding
        val depth = when {
            fingerY < scrollZonePx -> fingerY - scrollZonePx
            fingerY > contentEnd - scrollZonePx -> fingerY - (contentEnd - scrollZonePx)
            else -> return 0f
        }
        return (depth / scrollZonePx).coerceIn(-1f, 1f) * maxScrollSpeedPx
    }

    // The dragged item's layout slot must stay in the viewport, otherwise the list disposes it and the
    // drag gesture with it.
    private fun keepSlotVisible(scroll: Float, slot: LazyListItemInfo): Float {
        val layoutInfo = listState.layoutInfo
        return scroll.coerceIn(
            minimumValue = -(layoutInfo.viewportEndOffset - slot.offset - 1f),
            maximumValue = slot.offset + slot.size - layoutInfo.viewportStartOffset - 1f,
        )
    }

    private fun itemInfo(key: Any?): LazyListItemInfo? {
        if (key == null) return null
        return listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == key }
    }
}

/**
 * Long press starts the drag; the dragged pointer is then consumed on the initial pass, so a
 * clickable child neither clicks on release nor lets the list scroll by finger.
 */
internal suspend fun PointerInputScope.detectTangemDrag(
    state: TangemDraggableState,
    key: Any,
    onPressChange: (Boolean) -> Unit,
    onDragStart: () -> Unit,
) = awaitEachGesture {
    val down = awaitFirstDown(requireUnconsumed = false)
    onPressChange(true)
    try {
        val longPress = awaitLongPressOrCancellation(down.id) ?: return@awaitEachGesture
        if (!state.onDragStart(key = key, grabY = longPress.position.y)) return@awaitEachGesture
        onDragStart()
        try {
            while (true) {
                val change = awaitPointerEvent(PointerEventPass.Initial).changes
                    .firstOrNull { it.id == longPress.id }
                    ?: break
                change.consume()
                if (change.changedToUpIgnoreConsumed()) break
                state.onDrag(change.positionChangeIgnoreConsumed().y)
            }
        } finally {
            state.onDragStop()
        }
    } finally {
        onPressChange(false)
    }
}