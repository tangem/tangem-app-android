package com.tangem.core.ui.ds2.footer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.haze.hazeSourceTangem
import com.tangem.core.ui.components.rememberIsKeyboardVisible
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.animation.TangemAnimationSpec
import com.tangem.core.ui.ds2.animation.TangemTransition
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.button.TangemButtonGroup
import com.tangem.core.ui.ds2.fade.TangemFade
import com.tangem.core.ui.extensions.copy
import com.tangem.core.ui.extensions.rememberLastNonNull
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_arrow_left_20
import com.tangem.core.ui.res.generated.icons.ic_arrow_right_20

/**
 * Design-system v2 footer: screen-level actions pinned to the
 * bottom edge over a fade that separates them from the content scrolling underneath.
 *
 * [Figma](https://www.figma.com/design/AsnJ5CPHib4Qxw12gszjMS/%F0%9F%92%A0-DS-Components?node-id=9062-4588)
 *
 * ```
 * TangemFooter(extraTop = { Text(hint) }) {
 *     TangemButtonGroup {
 *         TangemButton(variant = TangemButton.Variant.Secondary, text = cancel, onClick = onCancel)
 *         TangemButton(variant = TangemButton.Variant.Primary, text = confirm, onClick = onConfirm)
 *     }
 * }
 * ```
 *
 * @param modifier Modifier applied to the footer's root. The footer hugs its content vertically and
 *   fills the width it is given.
 * @param windowInsets Bottom inset applied under the content — the home-indicator / navigation-bar
 *   safe area. Inside a modal, whose container already consumes the system inset, pass
 *   [TangemFooter.ModalWindowInsets]; pass `WindowInsets(0)` to opt out entirely.
 * @param contentPadding Inner padding of the content column, inside [windowInsets]. Defaults to
 *   [TangemFooter.DefaultContentPadding] (vertical 8, horizontal 16). The vertical values are
 *   replaced by 12dp while the keyboard is open.
 * @param fadeEnabled Whether the fade behind the footer is shown. The fade draws 16dp beyond the
 *   footer's top edge so it starts dissolving above the actions. Alpha is animated on change, and
 *   the blur is dropped once it reaches zero so a disabled fade costs nothing.
 * @param fadeVariant Fade style behind the footer. [TangemFade.Variant.Hard] — the design default —
 *   cuts the content off with an opaque block under the actions; [TangemFade.Variant.Soft] is a
 *   gradient-only wash for footers that should stay see-through.
 * @param blurBackground Whether the fade blurs the content scrolling below it. Place the footer as a
 *   sibling of the haze-source content (the way a scaffold overlays its bars), not inside it —
 *   nested in the source, the footer's material content has no area left to sample.
 * @param extraTop Optional slot above [content] — typically a centered hint about the action.
 * @param content Actions block pinned to the bottom.
 */
@Suppress("LongParameterList")
@Composable
fun TangemFooter(
    modifier: Modifier = Modifier,
    windowInsets: WindowInsets = WindowInsets.navigationBars,
    contentPadding: PaddingValues = TangemFooter.DefaultContentPadding,
    fadeEnabled: Boolean = true,
    fadeVariant: TangemFade.Variant = TangemFade.Variant.Hard,
    blurBackground: Boolean = true,
    extraTop: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val isKeyboardVisible by rememberIsKeyboardVisible()

    val fadeAlpha = animateFloatAsState(
        targetValue = if (fadeEnabled) 1f else 0f,
        animationSpec = TangemAnimationSpec.Alpha,
        label = "TangemFooter.fadeAlpha",
    )
    val isBlurActive by remember(blurBackground) {
        derivedStateOf { blurBackground && fadeAlpha.value > 0f }
    }

    Box(modifier) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .exceedTop(amount = 16.dp)
                .hazeSourceTangem(zIndex = 1f),
        ) {
            TangemFade(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = fadeAlpha.value },
                position = TangemFade.Position.Bottom,
                variant = fadeVariant,
                blur = isBlurActive,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(if (isKeyboardVisible) WindowInsets(0) else windowInsets)
                .padding(
                    if (isKeyboardVisible) {
                        contentPadding.copy(top = 12.dp, bottom = 12.dp)
                    } else {
                        contentPadding
                    },
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(space = 12.dp),
        ) {
            val displayedExtraTop = rememberLastNonNull(extraTop)
            AnimatedVisibility(
                visible = extraTop != null,
                enter = TangemTransition.SlotEnterVertically,
                exit = TangemTransition.SlotExitVertically,
            ) {
                displayedExtraTop?.invoke()
            }

            content()
        }
    }
}

/**
 * [TangemFooter] hosting a start and an end action pushed to the opposite content edges — the
 * icon-button pair from the design (`Button Start` / `Button End`, size `X11`).
 *
 * @param modifier Modifier applied to the footer's root.
 * @param windowInsets See [TangemFooter].
 * @param contentPadding See [TangemFooter].
 * @param fadeEnabled See [TangemFooter].
 * @param fadeVariant See [TangemFooter].
 * @param blurBackground See [TangemFooter].
 * @param extraTop See [TangemFooter].
 * @param buttonStart Leading action, aligned to the start edge.
 * @param buttonEnd Trailing action, aligned to the end edge.
 */
@Suppress("LongParameterList")
@Composable
fun TangemFooter(
    modifier: Modifier = Modifier,
    windowInsets: WindowInsets = WindowInsets.navigationBars,
    contentPadding: PaddingValues = TangemFooter.DefaultContentPadding,
    fadeEnabled: Boolean = true,
    fadeVariant: TangemFade.Variant = TangemFade.Variant.Hard,
    blurBackground: Boolean = true,
    extraTop: (@Composable () -> Unit)? = null,
    buttonStart: @Composable () -> Unit,
    buttonEnd: @Composable () -> Unit,
) {
    TangemFooter(
        modifier = modifier,
        windowInsets = windowInsets,
        contentPadding = contentPadding,
        fadeEnabled = fadeEnabled,
        fadeVariant = fadeVariant,
        blurBackground = blurBackground,
        extraTop = extraTop,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            buttonStart()
            buttonEnd()
        }
    }
}

private fun Modifier.exceedTop(amount: Dp): Modifier = layout { measurable, constraints ->
    val extraPx = amount.roundToPx()
    val placeable = measurable.measure(
        constraints.copy(
            minHeight = constraints.minHeight + extraPx,
            maxHeight = if (constraints.hasBoundedHeight) constraints.maxHeight + extraPx else constraints.maxHeight,
        ),
    )
    layout(placeable.width, placeable.height - extraPx) {
        placeable.place(x = 0, y = -extraPx)
    }
}

object TangemFooter {

    /** Default inner padding of the content column: vertical 8, horizontal 16. */
    @Suppress("MagicNumber")
    val DefaultContentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 8.dp)

    /**
     * Bottom inset for a footer inside a modal: the fixed home-indicator padding from the design,
     * used instead of the real safe-area inset because the modal container already consumes it.
     */
    @Suppress("MagicNumber")
    val ModalWindowInsets: WindowInsets = WindowInsets(bottom = 24.dp)
}

// region Previews

@Preview(name = "Light", showBackground = true, widthDp = 402)
@Preview(
    name = "Dark",
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    widthDp = 402,
)
@Composable
private fun TangemFooterPreview() {
    TangemThemePreviewRedesign {
        Column(
            modifier = Modifier
                .background(TangemTheme.colors3.bg.primary)
                .padding(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(space = 24.dp),
        ) {
            PreviewSection(label = "Button group + extra top") {
                TangemFooter(
                    windowInsets = WindowInsets(0),
                    extraTop = { PreviewHint() },
                ) {
                    TangemButtonGroup {
                        TangemButton(
                            variant = TangemButton.Variant.Secondary,
                            size = TangemButton.Size.X12,
                            text = stringReference(value = "Label"),
                            onClick = {},
                        )
                        TangemButton(
                            variant = TangemButton.Variant.Primary,
                            size = TangemButton.Size.X12,
                            text = stringReference(value = "Label"),
                            onClick = {},
                        )
                    }
                }
            }
            PreviewSection(label = "Start / end buttons") {
                TangemFooter(
                    windowInsets = WindowInsets(0),
                    extraTop = { PreviewHint() },
                    buttonStart = {
                        TangemButton(
                            variant = TangemButton.Variant.Material,
                            size = TangemButton.Size.X11,
                            iconStart = TangemIconUM.Icon(Icons.ic_arrow_left_20),
                            onClick = {},
                        )
                    },
                    buttonEnd = {
                        TangemButton(
                            variant = TangemButton.Variant.Primary,
                            size = TangemButton.Size.X11,
                            iconStart = TangemIconUM.Icon(Icons.ic_arrow_right_20),
                            onClick = {},
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun PreviewHint() {
    Text(
        text = "Within 3 business days you will receive an email from logistic company",
        color = TangemTheme.colors3.text.secondary,
        style = TangemTheme.typography3.caption.medium,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun PreviewSection(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
        Text(
            modifier = Modifier.padding(horizontal = 16.dp),
            text = label,
            color = TangemTheme.colors3.text.secondary,
            style = TangemTheme.typography3.body.medium,
        )
        content()
    }
}

// endregion