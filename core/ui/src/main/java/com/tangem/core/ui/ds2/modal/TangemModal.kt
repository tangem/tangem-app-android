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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.bottomsheets.internal.ModalBottomSheetWithBackHandling
import com.tangem.core.ui.components.bottomsheets.internal.collapse
import com.tangem.core.ui.components.haze.hazeSourceTangem
import com.tangem.core.ui.components.sheetscaffold.TangemSheetState
import com.tangem.core.ui.components.sheetscaffold.TangemSheetValue
import com.tangem.core.ui.components.sheetscaffold.rememberSheetState
import com.tangem.core.ui.ds2.surface.TangemSurface
import com.tangem.core.ui.extensions.conditionalCompose
import com.tangem.core.ui.res.LocalBottomSheetAlwaysVisible
import com.tangem.core.ui.res.LocalHazeState
import com.tangem.core.ui.res.LocalIsInDarkTheme
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.TangemThemeRedesign
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
 * - The card is as tall as its content, growing at most up to the status bar; taller content
 *   scrolls (see `scrollableContent`).
 * - **Default**: a floating card with 8dp margins, 36dp top / 32dp bottom corners, translucent
 *   modal material ([TangemSurface.MaterialStyle.Modal]) and a 36dp drag indicator.
 * - **Expanded**: as the content approaches the full height, the margins and bottom corners blend
 *   away, the glass fades to opaque `bg.primary`, the navigation-bar clearance moves inside the
 *   card and the drag indicator widens to 44dp — the card lands edge-to-edge. There is no flag:
 *   the state is derived from the measured content height. A modal that opens with full-height
 *   content opens already expanded; crossing the threshold while shown (content growing or
 *   shrinking) animates the morph, iOS-sheet style.
 * - **Focus**: a field gaining focus anywhere in the card expands it to the full height with the
 *   same animation (and back when focus clears), regardless of the content height — the iOS
 *   keyboard-detent behavior. The content is IME-padded, so the focused field stays above the
 *   keyboard.
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
 * @param dismissOnClickOutside when `false`, taps on the scrim and back presses don't dismiss
 * @param scrollableContent whether [content] is wrapped in a vertical scroll container
 * @param presentation where the modal lives — same-window overlay or dialog window. See
 *   [TangemModal.Presentation].
 * @param isMaterialEnabled when `false`, the overlay card renders flat and opaque instead of glass —
 *   for screens whose background the material would sample into a washed-out sheen (e.g. video)
 * @param forceDarkTheme renders the whole modal — scrim, card and slots — in the dark palette whatever the app
 *   theme is. The overlay presentation composes the modal inside the host, in a different branch of the tree, so
 *   a palette forced around the call site never reaches it.
 * @param containerColor overrides the card fill; `null` keeps the component's own surface colour. Read at the
 *   call site, so pass a colour from the palette the modal renders in.
 * @param showDragIndicator whether the drag pill is drawn above [title]. Its space stays either way, so the title
 *   sits at the same height with or without it.
 * @param prewarm when `true`, the sheet is composed and measured while hidden — invisibly, off to
 *   the side of the host — so that its first appearance starts moving as fast as its second. Only
 *   worth it where the first tap is the one that matters (an onboarding screen) and where the call
 *   site is composed well before that tap; see [TangemModalHostState.prewarm] for the price it
 *   pays and when. Ignored in the dialog presentation (nothing is registered in a host there,
 *   and a warm-up dialog would be a real window) and in previews.
 * @param title pinned slot above the content, typically a navigation-style header
 * @param content content of the sheet below [title]
 */
@Composable
inline fun <reified T : TangemBottomSheetConfigContent> TangemModal(
    config: TangemBottomSheetConfig,
    noinline onBack: (() -> Unit)? = null,
    dismissOnClickOutside: Boolean = true,
    scrollableContent: Boolean = true,
    presentation: TangemModal.Presentation = TangemModal.Presentation.Auto,
    isMaterialEnabled: Boolean = true,
    forceDarkTheme: Boolean = false,
    containerColor: Color? = null,
    showDragIndicator: Boolean = true,
    prewarm: Boolean = false,
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
            containerColor = containerColor,
            showDragIndicator = showDragIndicator,
            scrollableContent = scrollableContent,
            title = title,
            content = content,
        )
        overlayHost != null -> OverlayTangemModal<T>(
            host = overlayHost,
            isMaterialEnabled = isMaterialEnabled,
            forceDarkTheme = forceDarkTheme,
            containerColor = containerColor,
            showDragIndicator = showDragIndicator,
            config = config,
            onBack = onBack,
            dismissOnClickOutside = dismissOnClickOutside,
            scrollableContent = scrollableContent,
            prewarm = prewarm,
            title = title,
            content = content,
        )
        else -> DefaultTangemModal<T>(
            config = config,
            forceDarkTheme = forceDarkTheme,
            containerColor = containerColor,
            showDragIndicator = showDragIndicator,
            onBack = onBack,
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
    isMaterialEnabled: Boolean,
    forceDarkTheme: Boolean,
    containerColor: Color?,
    showDragIndicator: Boolean,
    config: TangemBottomSheetConfig,
    noinline onBack: (() -> Unit)?,
    dismissOnClickOutside: Boolean,
    scrollableContent: Boolean,
    prewarm: Boolean,
    crossinline title: @Composable BoxScope.(T) -> Unit,
    crossinline content: @Composable ColumnScope.(T) -> Unit,
) {
    val transition = rememberModalTransition(
        config = config,
        dismissOnClickOutside = dismissOnClickOutside,
    )
    // One key for both registries: the shown entry and the warm-up entry of a call site are the
    // same modal in two states, and switching between them must not leave the other one behind.
    val key = remember { Any() }

    if (transition.isVisible && config.content is T) {
        val modalContent by rememberUpdatedState(
            @Composable {
                // The palette wraps the scaffold, not just the slots: the scrim and the card read it too.
                ModalPalette(forceDarkTheme = forceDarkTheme) {
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
                            isMaterial = isMaterialEnabled,
                            containerColor = containerColor,
                            showDragIndicator = showDragIndicator,
                            scrollableContent = scrollableContent,
                            title = title,
                            content = content,
                        )
                    }
                }
            },
        )
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
    } else if (prewarm && !config.isShown) {
        // Read off the config, not the (lagging) transition: on the frame between the tap and the
        // transition catching up, the card must not be composed for the warm-up layer just to be
        // thrown away and composed again for real.
        PrewarmTangemModal<T>(
            host = host,
            key = key,
            config = config,
            isMaterial = isMaterialEnabled,
            forceDarkTheme = forceDarkTheme,
            containerColor = containerColor,
            showDragIndicator = showDragIndicator,
            scrollableContent = scrollableContent,
            title = title,
            content = content,
        )
    }
}

/**
 * Registers the card of a hidden modal in [host] as a warm-up entry (see
 * [TangemModalHostState.prewarm]) for as long as the modal stays hidden.
 *
 * Only the card is warmed, not the scaffold around it: a scrim would darken the screen and a
 * predictive-back handler would swallow the back gesture of the screen underneath, and neither
 * belongs to a modal nobody is looking at.
 *
 * The card is composed exactly as a real appearance composes it — the sheet only slides it in, the
 * card itself does not change size — so the measurement being warmed is the one it will need.
 */
@Composable
@PublishedApi
@Suppress("LongParameterList")
internal inline fun <reified T : TangemBottomSheetConfigContent> PrewarmTangemModal(
    host: TangemModalHostState,
    key: Any,
    config: TangemBottomSheetConfig,
    isMaterial: Boolean,
    forceDarkTheme: Boolean,
    containerColor: Color?,
    showDragIndicator: Boolean,
    scrollableContent: Boolean,
    crossinline title: @Composable BoxScope.(T) -> Unit,
    crossinline content: @Composable ColumnScope.(T) -> Unit,
) {
    val prewarmContent by rememberUpdatedState(
        @Composable {
            // The warm-up measures the card the viewer will get, so it is warmed in the same palette.
            ModalPalette(forceDarkTheme = forceDarkTheme) {
                // Until the call site has a model of its own — a screen whose sheet content is only
                // built by the tap that shows it — the shared chrome is warmed on its own. That is
                // where the bulk of a first appearance goes anyway: the same warm-up serves any modal.
                if (config.content is T) {
                    TangemModalContainer<T>(
                        config = config,
                        isMaterial = isMaterial,
                        containerColor = containerColor,
                        showDragIndicator = showDragIndicator,
                        scrollableContent = scrollableContent,
                        title = title,
                        content = content,
                    )
                } else {
                    ModalPrewarmChrome(
                        isMaterial = isMaterial,
                        showDragIndicator = showDragIndicator,
                        scrollableContent = scrollableContent,
                    )
                }
            }
        },
    )
    DisposableEffect(host, key) {
        host.prewarm(key) { prewarmContent() }
        onDispose { host.stopPrewarm(key) }
    }
}

/**
 * The palette a modal renders in. The overlay presentation hands its content to [TangemModalHost], which composes
 * it in its own branch of the tree, so composition locals provided around the call site do not reach it.
 *
 * Re-entering the theme would also hand the card a haze state of its own, and the card's material samples the
 * host's — so the one in scope is carried through the wrap.
 */
@Composable
@PublishedApi
internal fun ModalPalette(forceDarkTheme: Boolean, content: @Composable () -> Unit) {
    if (forceDarkTheme) {
        val hazeState = LocalHazeState.current
        CompositionLocalProvider(LocalIsInDarkTheme provides true) {
            TangemThemeRedesign {
                CompositionLocalProvider(LocalHazeState provides hazeState, content = content)
            }
        }
    } else {
        content()
    }
}

/** The card of a contentless modal: everything of a modal appearance except the caller's content. */
@Composable
@PublishedApi
internal fun ModalPrewarmChrome(isMaterial: Boolean, showDragIndicator: Boolean, scrollableContent: Boolean) {
    TangemModalContainer<TangemBottomSheetConfigContent.Empty>(
        config = TangemBottomSheetConfig.Empty,
        isMaterial = isMaterial,
        containerColor = null,
        showDragIndicator = showDragIndicator,
        scrollableContent = scrollableContent,
        title = {},
        content = {},
    )
}

/**
 * Dialog-window presentation: the same [OverlayModalScaffold] (scrim, slide, predictive back)
 * hosted in a bare transparent full-screen dialog window. The card is rendered flat and opaque —
 * cross-window blur is impossible, so no material.
 */
@Composable
@PublishedApi
@Suppress("LongParameterList")
internal inline fun <reified T : TangemBottomSheetConfigContent> DefaultTangemModal(
    config: TangemBottomSheetConfig,
    forceDarkTheme: Boolean,
    containerColor: Color?,
    showDragIndicator: Boolean,
    noinline onBack: (() -> Unit)?,
    dismissOnClickOutside: Boolean,
    scrollableContent: Boolean,
    crossinline title: @Composable BoxScope.(T) -> Unit,
    crossinline content: @Composable ColumnScope.(T) -> Unit,
) {
    val transition = rememberModalTransition(
        config = config,
        dismissOnClickOutside = dismissOnClickOutside,
    )

    if (transition.isVisible && config.content is T) {
        Dialog(
            onDismissRequest = config.onDismissRequest,
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false,
            ),
        ) {
            ModalDialogWindowEffect()
            ModalPalette(forceDarkTheme = forceDarkTheme) {
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
                        isMaterial = false,
                        containerColor = containerColor,
                        showDragIndicator = showDragIndicator,
                        scrollableContent = scrollableContent,
                        title = title,
                        content = content,
                    )
                }
            }
        }
    }
}

/**
 * Show/dismiss machinery shared by both presentations: the draggable sheet, the predictive-back
 * preview and the lingering visibility that keeps the modal composed while the collapse animation
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

    /** Slide progress with the predictive-back preview applied. See [combinedOpenProgress]. */
    fun openProgress(): Float {
        return combinedOpenProgress(openProgress = sheetState.openProgress(), backProgress = backProgress.value)
    }
}

@Composable
@PublishedApi
internal fun rememberModalTransition(
    config: TangemBottomSheetConfig,
    dismissOnClickOutside: Boolean,
): ModalTransition {
    val sheetState = rememberSheetState(
        skipPartiallyExpanded = true,
        initialValue = TangemSheetValue.Hidden,
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
            // The back-gesture preview of a previous appearance must not leak into a fresh one.
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
@Suppress("LongParameterList")
internal inline fun <reified T : TangemBottomSheetConfigContent> PreviewTangemModal(
    config: TangemBottomSheetConfig,
    containerColor: Color?,
    showDragIndicator: Boolean,
    scrollableContent: Boolean,
    crossinline title: @Composable BoxScope.(T) -> Unit,
    crossinline content: @Composable ColumnScope.(T) -> Unit,
) {
    ModalBottomSheetWithBackHandling(
        onDismissRequest = config.onDismissRequest,
        sheetState = rememberSheetState(
            skipPartiallyExpanded = true,
            initialValue = TangemSheetValue.Expanded,
        ),
        peekHeightDp = 0.dp,
        containerColor = Color.Transparent,
        scrimColor = TangemTheme.colors3.overlay.modal,
        contentWindowInsets = { WindowInsetsZero },
        dragHandle = null,
        onBack = null,
        content = {
            TangemModalContainer<T>(
                config = config,
                isMaterial = true,
                containerColor = containerColor,
                showDragIndicator = showDragIndicator,
                scrollableContent = scrollableContent,
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
    isMaterial: Boolean,
    containerColor: Color?,
    showDragIndicator: Boolean,
    scrollableContent: Boolean,
    crossinline title: @Composable BoxScope.(T) -> Unit,
    crossinline content: @Composable ColumnScope.(T) -> Unit,
) {
    val model = config.content as? T ?: return

    val morphGeometry = remember { ModalMorphGeometry() }
    ModalMorphAnimationEffect(geometry = morphGeometry)
    val shape = remember(morphGeometry) { ModalMorphShape(geometry = morphGeometry) }
    val opaqueColor = containerColor ?: TangemTheme.colors3.bg.primary

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(BaseBottomSheetTestTags.CONTAINER),
    ) {
        TangemSurface(
            modifier = Modifier.modalCardLayout(
                geometry = morphGeometry,
                windowInsets = WindowInsets.systemBars,
            ),
            isMaterial = isMaterial,
            materialStyle = TangemSurface.MaterialStyle.Modal,
            isMaterialBorderEnabled = false,
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
                    .conditionalCompose(isMaterial) {
                        drawBehind { drawRect(color = opaqueColor.copy(alpha = morphGeometry.flatten)) }
                    }
                    .modalCardFill(geometry = morphGeometry)
                    // A focused field anywhere in the card expands it to the full height, iOS
                    // keyboard-detent style; hasFocus aggregates the whole subtree.
                    .onFocusEvent { state -> morphGeometry.isFocused = state.hasFocus },
            ) {
                CompositionLocalProvider(LocalHazeState provides contentHazeState) {
                    DragIndicator(geometry = morphGeometry, isShown = showDragIndicator)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        title(model)
                    }
                }
                // imePadding sits outside the scroll container, so the scroll viewport ends above
                // the keyboard and a focused field is brought into the visible part of the card.
                val contentModifier = Modifier
                    .hazeSourceTangem(state = contentHazeState)
                    .imePadding()
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

/**
 * Drag pill above the title, widening from 36dp to 44dp as the card morphs edge-to-edge. When not [isShown] it
 * keeps its space and draws nothing.
 */
@Composable
@PublishedApi
internal fun DragIndicator(geometry: ModalMorphGeometry, isShown: Boolean, modifier: Modifier = Modifier) {
    val color = TangemTheme.colors3.border.tertiary
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 8.dp)
            .height(4.dp)
            .conditionalCompose(isShown) { drawPill(geometry = geometry, color = color) },
    )
}

private fun Modifier.drawPill(geometry: ModalMorphGeometry, color: Color): Modifier = drawBehind {
    val width = lerp(36.dp.toPx(), 44.dp.toPx(), geometry.flatten)
    drawRoundRect(
        color = color,
        topLeft = Offset(x = (size.width - width) / 2f, y = 0f),
        size = Size(width = width, height = size.height),
        cornerRadius = CornerRadius(size.height / 2f),
    )
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