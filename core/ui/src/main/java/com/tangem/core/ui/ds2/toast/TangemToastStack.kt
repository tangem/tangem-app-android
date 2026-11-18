@file:Suppress("MagicNumber")

package com.tangem.core.ui.ds2.toast

import android.content.res.Configuration
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.message.TangemToastMessage
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import kotlinx.coroutines.launch

/**
 * Design-system v2 (DS3) **Toast Stack** — the host that shows the toasts of a [TangemToastStackState] with the
 * show, push and auto-dismiss animation. The app root, every bottom-sheet window and every dialog-window modal
 * already host one for the app-wide state, so features show toasts by sending a [TangemToastMessage], not by
 * placing a stack.
 *
 * Ships as part of [TangemToast] and shares its version.
 *
 * [Figma](https://www.figma.com/design/AsnJ5CPHib4Qxw12gszjMS/%F0%9F%92%A0-DS-Components?node-id=14277-1044)
 *
 * Behavior notes:
 * - A toast grows from the status bar's centre into its slot, then stays for 2 s ([TangemToast.Duration.Short])
 *   or 3 s ([TangemToast.Duration.Long], or any label that wraps) and shrinks into its own centre.
 * - A new toast appears on top and pushes the visible ones down by its height. A dismissed toast leaves its slot
 *   empty until it is gone; toasts below it then move up.
 * - At most three toasts are visible: a fourth dismisses the oldest.
 * - The stack takes no touches outside its toasts, so place it over the content in a full-size container.
 *
 * @param state Toasts to show. Several stacks may share one state — see [TangemToastStackState].
 * @param modifier Modifier applied to the stack. It fills the available width.
 * @param placement Where the first toast sits — under the status bar and the top navigation, or at the top of a
 *   sheet.
 */
@Composable
fun TangemToastStack(
    state: TangemToastStackState,
    modifier: Modifier = Modifier,
    placement: TangemToastStack.Placement = TangemToastStack.Placement.Screen,
) {
    val accessibilityManager = LocalAccessibilityManager.current
    val depth = remember(state) { state.registerHost(accessibilityManager) }
    DisposableEffect(state) {
        onDispose { state.unregisterHost(depth) }
    }

    val density = LocalDensity.current
    val statusBarHeight = WindowInsets.statusBars.getTop(density)
    val spacing = with(density) { 8.dp.roundToPx() }
    var topInWindow by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(
                when (placement) {
                    TangemToastStack.Placement.Screen -> Modifier
                        .statusBarsPadding()
                        .padding(top = 44.dp)
                    TangemToastStack.Placement.Sheet -> Modifier
                },
            )
            .padding(start = 16.dp, top = 24.dp, end = 16.dp)
            .onPlaced { topInWindow = it.positionInWindow().y },
        contentAlignment = Alignment.TopCenter,
    ) {
        var slotTop = 0
        state.entries.forEach { entry ->
            if (entry.hostDepth != depth) return@forEach

            key(entry.id) {
                val top by animateIntAsState(
                    targetValue = slotTop,
                    animationSpec = tween(durationMillis = 350, easing = ToastShowEasing),
                    label = "toastSlotTop",
                )
                AnimatedToast(
                    entry = entry,
                    state = state,
                    startCenterY = { statusBarHeight / 2f - topInWindow },
                    modifier = Modifier.offset { IntOffset(x = 0, y = top) },
                )
            }
            slotTop += entry.height + spacing
        }
    }
}

/** Public API surface of [TangemToastStack]. */
object TangemToastStack {

    /** Where the first toast of the stack sits. */
    enum class Placement {
        /** Below the status bar and the top navigation of a full-screen window. */
        Screen,

        /** At the top of a bottom-sheet window, which has no status bar or top navigation above it. */
        Sheet,
    }
}

@Preview(name = "Light", showBackground = true, heightDp = 400)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, heightDp = 400)
@Composable
private fun TangemToastStackPreview() {
    TangemThemePreviewRedesign {
        val state = remember { TangemToastStackState() }
        val scope = rememberCoroutineScope()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(TangemTheme.colors3.bg.primary),
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TangemButton(text = stringReference("Short"), size = TangemButton.Size.X9) {
                        scope.launch { state.show(TangemToastMessage(label = stringReference("Toast label"))) }
                    }
                    TangemButton(text = stringReference("Multi-line"), size = TangemButton.Size.X9) {
                        scope.launch {
                            state.show(
                                TangemToastMessage(
                                    label = stringReference("Balance is currently very very very very hidden"),
                                    appearance = TangemToast.Appearance.Info,
                                ),
                            )
                        }
                    }
                }
            }
            TangemToastStack(state = state, placement = TangemToastStack.Placement.Sheet)
        }
    }
}