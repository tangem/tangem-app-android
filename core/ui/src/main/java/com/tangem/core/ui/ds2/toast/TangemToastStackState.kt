package com.tangem.core.ui.ds2.toast

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.AccessibilityManager
import com.tangem.core.ui.message.TangemToastMessage
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull

/**
 * State of the toasts shown by [TangemToastStack]s. Unlike a snackbar queue, toasts don't wait for each other:
 * a new one appears on top of the visible ones and pushes them down.
 *
 * The app-wide instance is provided as `LocalToastStackState` and fed by `TangemToastMessage`s sent through the
 * UI message sender; create your own only for a stack that lives outside that pipeline (e.g. the Storybook).
 *
 * Several stacks may share one state — the app root and every bottom-sheet window host one. A toast is drawn by the
 * stack that was registered last when it was shown, and moves to the deepest remaining stack if that one leaves.
 */
@Stable
class TangemToastStackState {

    internal val entries = mutableStateListOf<ToastEntry>()

    private var nextId = 0L

    private var hostDepthCounter = 0

    private var accessibilityManager: AccessibilityManager? = null

    /**
     * Shows [message] and suspends until the toast is dismissed — by its timeout, the close button, the action
     * button, or by being pushed out of the stack by newer toasts. [TangemToastMessage.onDismissRequest] is
     * invoked for every dismissal except the action button, which invokes [TangemToastMessage.action] instead.
     *
     * Cancelling the caller dismisses the toast without invoking any callback.
     */
    suspend fun show(message: TangemToastMessage) {
        val entry = ToastEntry(id = nextId++, message = message, hostDepth = hostDepthCounter)
        entries.add(0, entry)
        dismissOverflow()

        val reason = try {
            withTimeoutOrNull(timeMillis = 350L) { entry.dismissal.await() }
                ?: withTimeoutOrNull(holdMillis(entry)) { entry.dismissal.await() }
                ?: DismissReason.Timeout
        } finally {
            entry.isVisible = false
            if (hostDepthCounter == 0) entries.remove(entry)
        }

        if (reason != DismissReason.Action) message.onDismissRequest()
    }

    internal fun close(entry: ToastEntry) {
        entry.dismissal.complete(DismissReason.Closed)
    }

    internal fun performAction(entry: ToastEntry) {
        if (entry.dismissal.complete(DismissReason.Action)) entry.message.action?.invoke()
    }

    internal fun onExitFinished(entry: ToastEntry) {
        entries.remove(entry)
    }

    internal fun registerHost(accessibilityManager: AccessibilityManager?): Int {
        this.accessibilityManager = accessibilityManager
        hostDepthCounter++
        return hostDepthCounter
    }

    internal fun unregisterHost(depth: Int) {
        if (depth != hostDepthCounter) return

        hostDepthCounter--
        entries.forEach { entry ->
            if (entry.hostDepth > hostDepthCounter) entry.hostDepth = hostDepthCounter
        }
        if (hostDepthCounter == 0) entries.removeAll { !it.isVisible }
    }

    private fun dismissOverflow() {
        entries
            .filter { it.isVisible && !it.dismissal.isCompleted }
            .drop(MAX_VISIBLE_TOASTS)
            .forEach { it.dismissal.complete(DismissReason.Overflow) }
    }

    private fun holdMillis(entry: ToastEntry): Long {
        val message = entry.message
        val duration = if (entry.isMultiLine) TangemToast.Duration.Long else message.duration
        val original = duration.toMillis()

        return accessibilityManager?.calculateRecommendedTimeoutMillis(
            originalTimeoutMillis = original,
            containsIcons = message.icon != null || message.appearance != TangemToast.Appearance.Neutral,
            containsText = true,
            containsControls = message.action != null || message.isClosable,
        ) ?: original
    }

    private companion object {
        const val MAX_VISIBLE_TOASTS = 3
    }
}

@Stable
internal class ToastEntry(val id: Long, val message: TangemToastMessage, hostDepth: Int) {

    var hostDepth by mutableIntStateOf(hostDepth)

    var isVisible by mutableStateOf(true)

    var height by mutableIntStateOf(0)

    var isMultiLine = false

    val dismissal = CompletableDeferred<DismissReason>()
}

internal enum class DismissReason { Timeout, Closed, Action, Overflow }