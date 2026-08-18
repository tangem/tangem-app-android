@file:Suppress("MagicNumber")

package com.tangem.core.ui.ds2.modal

import android.content.res.Configuration
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.bottomsheets.internal.ModalBottomSheetWithBackHandling
import com.tangem.core.ui.components.bottomsheets.internal.collapse
import com.tangem.core.ui.components.bottomsheets.modal.MODAL_SHEET_MAX_HEIGHT
import com.tangem.core.ui.components.haze.hazeSourceTangem
import com.tangem.core.ui.components.sheetscaffold.TangemSheetState
import com.tangem.core.ui.components.sheetscaffold.TangemSheetValue
import com.tangem.core.ui.components.sheetscaffold.rememberSheetState
import com.tangem.core.ui.ds2.surface.TangemSurface
import com.tangem.core.ui.extensions.conditionalCompose
import com.tangem.core.ui.res.LocalBottomSheetAlwaysVisible
import com.tangem.core.ui.res.LocalHazeState
import com.tangem.core.ui.res.LocalWindowSize
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.test.BaseBottomSheetTestTags
import com.tangem.core.ui.utils.WindowInsetsZero
import dev.chrisbanes.haze.rememberHazeState

/**
 * Design-system v2 modal bottom sheet: a floating modal-material card with a drag indicator,
 * optional [title] slot and [content], shown above a dimmed scrim.
 *
 * [Figma](https://www.figma.com/design/AsnJ5CPHib4Qxw12gszjMS/%F0%9F%92%A0-DS-Components?node-id=8256-1265&m=dev)
 *
 * Behavior notes:
 * - Maximum height is 80% of the screen; taller content scrolls (see `scrollableContent`).
 * - **Default**: a floating card with 8dp margins, 36dp top / 32dp bottom corners, translucent
 *   modal material ([TangemSurface.MaterialStyle.Modal]) and a 36dp drag indicator.
 * - **Expanded** ([isExpanded] = true): an edge-to-edge sheet with opaque `bg.primary` fill,
 *   square bottom corners and a 44dp drag indicator.
 * - **Presentation** ([presentation]): with a [LocalTangemModalHost] in the window, the modal is
 *   rendered as a same-window overlay and its material truly blurs the app UI behind it; without
 *   a host (or with [TangemModal.Presentation.Dialog]) it opens in a separate dialog window with
 *   identical animations and logic, but the card renders flat and opaque — cross-window blur is
 *   impossible.
 * - [LocalHazeState] is rescoped inside the card, so material elements of [title] (e.g. round
 *   navigation buttons) blur the modal surface, not the screen behind it.
 *
 * @param config bottom sheet config: visibility, dismiss callback and the typed content model
 * @param onBack custom back handler; when `null`, back dismisses the sheet
 * @param isExpanded switches to the expanded (opaque, edge-to-edge) variant
 * @param dismissOnClickOutside when `false`, taps on the scrim and back presses don't dismiss
 * @param scrollableContent whether [content] is wrapped in a vertical scroll container
 * @param presentation where the modal lives — same-window overlay or dialog window. See
 *   [TangemModal.Presentation].
 * @param title pinned slot above the content, typically a navigation-style header
 * @param content content of the sheet below [title]
 */
@Composable
inline fun <reified T : TangemBottomSheetConfigContent> TangemModal(
    config: TangemBottomSheetConfig,
    noinline onBack: (() -> Unit)? = null,
    isExpanded: Boolean = false,
    dismissOnClickOutside: Boolean = true,
    scrollableContent: Boolean = true,
    presentation: TangemModal.Presentation = TangemModal.Presentation.Auto,
    crossinline title: @Composable BoxScope.(T) -> Unit = {},
    crossinline content: @Composable ColumnScope.(T) -> Unit,
) {
    val host = LocalTangemModalHost.current
    val overlayHost = when (presentation) {
        TangemModal.Presentation.Auto,
        TangemModal.Presentation.Overlay,
        -> host
        TangemModal.Presentation.Dialog -> null
    }
    when {
        LocalBottomSheetAlwaysVisible.current -> PreviewTangemModal<T>(
            config = config,
            isExpanded = isExpanded,
            scrollableContent = scrollableContent,
            title = title,
            content = content,
        )
        overlayHost != null -> OverlayTangemModal<T>(
            host = overlayHost,
            config = config,
            onBack = onBack,
            isExpanded = isExpanded,
            dismissOnClickOutside = dismissOnClickOutside,
            scrollableContent = scrollableContent,
            title = title,
            content = content,
        )
        else -> DefaultTangemModal<T>(
            config = config,
            onBack = onBack,
            isExpanded = isExpanded,
            dismissOnClickOutside = dismissOnClickOutside,
            scrollableContent = scrollableContent,
            title = title,
            content = content,
        )
    }
}

/** Public namespace of the modal component. */
object TangemModal {

    /**
     * Where the modal is presented.
     *
     * - [Auto] — same-window overlay when a [LocalTangemModalHost] is available, dialog window
     *   otherwise. The default.
     * - [Overlay] — same-window overlay; falls back to the dialog window when no host is present.
     * - [Dialog] — always a separate dialog window, even when a host is available. Use when the
     *   modal must stack above other real dialogs.
     */
    enum class Presentation {
        Auto,
        Overlay,
        Dialog,
    }
}

/**
 * Same-window presentation: instead of opening a dialog, the modal registers its content into
 * [host], which renders it above the window content (see [TangemModalHost]).
 */
@Composable
@PublishedApi
@Suppress("LongParameterList")
internal inline fun <reified T : TangemBottomSheetConfigContent> OverlayTangemModal(
    host: TangemModalHostState,
    config: TangemBottomSheetConfig,
    noinline onBack: (() -> Unit)?,
    isExpanded: Boolean,
    dismissOnClickOutside: Boolean,
    scrollableContent: Boolean,
    crossinline title: @Composable BoxScope.(T) -> Unit,
    crossinline content: @Composable ColumnScope.(T) -> Unit,
) {
    val transition = rememberModalTransition(config = config, dismissOnClickOutside = dismissOnClickOutside)

    if (transition.isVisible && config.content is T) {
        val modalContent by rememberUpdatedState(
            @Composable {
                OverlayModalScaffold(
                    sheetState = transition.sheetState,
                    backProgress = transition.backProgress,
                    openProgress = transition::openProgress,
                    isDismissEnabled = dismissOnClickOutside,
                    onDismissRequest = config.onDismissRequest,
                    onBack = onBack,
                ) {
                    TangemModalContainer<T>(
                        config = config,
                        isExpanded = isExpanded,
                        isMaterial = !isExpanded,
                        scrollableContent = scrollableContent,
                        openProgress = transition::openProgress,
                        anchorProgress = { transition.sheetState.openProgress() },
                        title = title,
                        content = content,
                    )
                }
            },
        )
        val key = remember { Any() }
        DisposableEffect(host, key) {
            host.show(key) { modalContent() }
            onDispose {
                if (transition.sheetState.targetValue != TangemSheetValue.Hidden) {
                    // The call site left composition while the modal is still shown (e.g. a
                    // Decompose slot dismissed its component) — the host keeps the entry alive
                    // just long enough to play the collapse animation.
                    host.retire(key) { transition.sheetState.hide() }
                } else {
                    host.dismiss(key)
                }
            }
        }
    }
}

/**
 * Dialog-window presentation: the same [OverlayModalScaffold] (scrim, capsule morph, predictive
 * back) hosted in a bare transparent full-screen dialog window. The card is rendered flat and
 * opaque — cross-window blur is impossible, so no material.
 */
@Composable
@PublishedApi
@Suppress("LongParameterList")
internal inline fun <reified T : TangemBottomSheetConfigContent> DefaultTangemModal(
    config: TangemBottomSheetConfig,
    noinline onBack: (() -> Unit)?,
    isExpanded: Boolean,
    dismissOnClickOutside: Boolean,
    scrollableContent: Boolean,
    crossinline title: @Composable BoxScope.(T) -> Unit,
    crossinline content: @Composable ColumnScope.(T) -> Unit,
) {
    val transition = rememberModalTransition(config = config, dismissOnClickOutside = dismissOnClickOutside)

    if (transition.isVisible && config.content is T) {
        Dialog(
            onDismissRequest = config.onDismissRequest,
            properties = DialogProperties(
                // Back and outside clicks are handled by the scaffold (predictive fold + scrim).
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false,
            ),
        ) {
            ModalDialogWindowEffect()
            OverlayModalScaffold(
                sheetState = transition.sheetState,
                backProgress = transition.backProgress,
                openProgress = transition::openProgress,
                isDismissEnabled = dismissOnClickOutside,
                onDismissRequest = config.onDismissRequest,
                onBack = onBack,
            ) {
                TangemModalContainer<T>(
                    config = config,
                    isExpanded = isExpanded,
                    isMaterial = false,
                    scrollableContent = scrollableContent,
                    openProgress = transition::openProgress,
                    anchorProgress = { transition.sheetState.openProgress() },
                    title = title,
                    content = content,
                )
            }
        }
    }
}

/**
 * Show/dismiss machinery shared by both presentations: the draggable sheet, the predictive-back
 * fold and the lingering visibility that keeps the modal composed while the collapse animation
 * plays after `config.isShown` turns `false`.
 */
@Stable
@PublishedApi
internal class ModalTransition(
    val sheetState: TangemSheetState,
    val backProgress: Animatable<Float, AnimationVector1D>,
    isVisible: Boolean,
) {
    var isVisible: Boolean by mutableStateOf(isVisible)

    /** Slide progress with the predictive-back fold applied. See [combinedOpenProgress]. */
    fun openProgress(): Float {
        return combinedOpenProgress(openProgress = sheetState.openProgress(), backProgress = backProgress.value)
    }
}

@Composable
@PublishedApi
internal fun rememberModalTransition(config: TangemBottomSheetConfig, dismissOnClickOutside: Boolean): ModalTransition {
    val sheetState = rememberSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { sheetValue ->
            // Ignoring transitions to hidden prevents dismiss on drag down / outside click.
            dismissOnClickOutside || sheetValue != TangemSheetValue.Hidden
        },
    )
    val transition = remember(sheetState) {
        ModalTransition(
            sheetState = sheetState,
            backProgress = Animatable(initialValue = 0f),
            isVisible = config.isShown,
        )
    }

    LaunchedEffect(key1 = config.isShown) {
        if (config.isShown) {
            // The back-gesture fold of a previous appearance must not leak into a fresh one.
            transition.backProgress.snapTo(targetValue = 0f)
            transition.isVisible = true
        } else {
            transition.sheetState.collapse { transition.isVisible = false }
        }
    }

    return transition
}

/**
 * Strips the dialog window down to a transparent, undimmed, unanimated full-screen host — the
 * scaffold draws its own scrim and animates the sheet itself.
 */
@Composable
@PublishedApi
internal fun ModalDialogWindowEffect() {
    val view = LocalView.current
    SideEffect {
        val window = (view.parent as? DialogWindowProvider)?.window ?: return@SideEffect
        window.setDimAmount(0f)
        window.setWindowAnimations(0)
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
@PublishedApi
internal inline fun <reified T : TangemBottomSheetConfigContent> PreviewTangemModal(
    config: TangemBottomSheetConfig,
    isExpanded: Boolean,
    scrollableContent: Boolean,
    crossinline title: @Composable BoxScope.(T) -> Unit,
    crossinline content: @Composable ColumnScope.(T) -> Unit,
) {
    val peekHeight = LocalWindowSize.current.height * MODAL_SHEET_MAX_HEIGHT
    ModalBottomSheetWithBackHandling(
        onDismissRequest = config.onDismissRequest,
        sheetState = rememberSheetState(
            skipPartiallyExpanded = true,
            initialValue = TangemSheetValue.Expanded,
        ),
        peekHeightDp = peekHeight,
        containerColor = Color.Transparent,
        scrimColor = TangemTheme.colors3.overlay.modal,
        contentWindowInsets = { WindowInsetsZero },
        dragHandle = null,
        onBack = null,
        content = {
            TangemModalContainer<T>(
                config = config,
                isExpanded = isExpanded,
                isMaterial = !isExpanded,
                scrollableContent = scrollableContent,
                openProgress = { OPEN_PROGRESS_SETTLED },
                anchorProgress = { OPEN_PROGRESS_SETTLED },
                title = title,
                content = content,
            )
        },
    )
}

@Composable
@PublishedApi
@Suppress("LongParameterList")
internal inline fun <reified T : TangemBottomSheetConfigContent> TangemModalContainer(
    config: TangemBottomSheetConfig,
    isExpanded: Boolean,
    isMaterial: Boolean,
    scrollableContent: Boolean,
    noinline openProgress: () -> Float,
    noinline anchorProgress: () -> Float,
    crossinline title: @Composable BoxScope.(T) -> Unit,
    crossinline content: @Composable ColumnScope.(T) -> Unit,
) {
    val model = config.content as? T ?: return

    val maxHeight = LocalWindowSize.current.height * MODAL_SHEET_MAX_HEIGHT
    val morphGeometry = remember { ModalMorphGeometry() }
    val shape = remember(morphGeometry) { ModalMorphShape(geometry = morphGeometry) }
    val opaqueColor = TangemTheme.colors3.bg.primary

    Column(
        modifier = Modifier
            .heightIn(max = maxHeight)
            .fillMaxWidth()
            .testTag(BaseBottomSheetTestTags.CONTAINER),
    ) {
        TangemSurface(
            modifier = Modifier.modalMorphLayout(
                geometry = morphGeometry,
                isExpanded = isExpanded,
                windowInsets = WindowInsets.systemBars,
                progress = openProgress,
                anchorProgress = anchorProgress,
            ),
            isMaterial = isMaterial,
            materialStyle = TangemSurface.MaterialStyle.Modal,
            color = opaqueColor,
            shape = shape,
        ) {
            // The card content is its own haze source, so glass elements of the title (nav
            // buttons) blur the modal content — not the screen behind the card. Mirroring the
            // global blurEnabled keeps their solid fallback in sync when blur is off.
            val outerHazeState = LocalHazeState.current
            val contentHazeState = rememberHazeState()
            SideEffect { contentHazeState.blurEnabled = outerHazeState.blurEnabled }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .conditionalCompose(isMaterial) {
                        drawBehind { drawRect(color = opaqueColor.copy(alpha = morphGeometry.flatten)) }
                    }
                    .conditionalCompose(isExpanded) { navigationBarsPadding() },
            ) {
                CompositionLocalProvider(LocalHazeState provides contentHazeState) {
                    DragIndicator(isExpanded = isExpanded)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        title(model)
                    }
                }
                val contentModifier = Modifier.hazeSourceTangem(state = contentHazeState)
                if (scrollableContent) {
                    Column(modifier = contentModifier.verticalScroll(rememberScrollState())) {
                        content(model)
                    }
                } else {
                    Column(modifier = contentModifier) {
                        content(model)
                    }
                }
            }
        }
    }
}

@Composable
@PublishedApi
internal fun DragIndicator(isExpanded: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(width = if (isExpanded) 44.dp else 36.dp, height = 4.dp)
                .background(color = TangemTheme.colors3.border.tertiary, shape = CircleShape),
        )
    }
}

// region preview

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Preview(showBackground = true, widthDp = 360, heightDp = 640, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TangemModalPreview() {
    TangemThemePreviewRedesign {
        Box(modifier = Modifier.background(TangemTheme.colors3.bg.secondary)) {
            CompositionLocalProvider(LocalBottomSheetAlwaysVisible provides true) {
                TangemModal<PreviewModalContent>(
                    config = TangemBottomSheetConfig(
                        isShown = true,
                        onDismissRequest = {},
                        content = PreviewModalContent,
                    ),
                    title = {
                        Text(
                            text = "Title",
                            style = TangemTheme.typography3.heading.medium,
                            color = TangemTheme.colors3.text.primary,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(vertical = 12.dp),
                        )
                    },
                    content = {
                        repeat(times = 3) { index ->
                            Text(
                                text = "Row ${index + 1}",
                                style = TangemTheme.typography3.body.medium,
                                color = TangemTheme.colors3.text.secondary,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            )
                        }
                    },
                )
            }
        }
    }
}

private object PreviewModalContent : TangemBottomSheetConfigContent

// endregion