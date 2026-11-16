package com.tangem.core.ui.ds2.button

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.vector.ImageVector
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_chevron_left_20
import com.tangem.core.ui.res.generated.icons.ic_cross_20
import com.tangem.core.ui.test.TopNavigationTestTags

@Composable
@NonRestartableComposable
fun TangemButton.Back(modifier: Modifier = Modifier, onClick: () -> Unit) {
    TangemButton(
        modifier = modifier.testTag(TopNavigationTestTags.BACK_BUTTON),
        variant = TangemButton.Variant.Material,
        size = TangemButton.Size.X11,
        iconStart = TangemIconUM.Icon(Icons.ic_chevron_left_20),
        onClick = onClick,
    )
}

@Composable
@NonRestartableComposable
fun TangemButton.Close(modifier: Modifier = Modifier, isEnabled: Boolean = true, onClick: () -> Unit) {
    TangemButton(
        modifier = modifier.testTag(TopNavigationTestTags.CLOSE_BUTTON),
        variant = TangemButton.Variant.Material,
        size = TangemButton.Size.X11,
        iconStart = TangemIconUM.Icon(Icons.ic_cross_20),
        isEnabled = isEnabled,
        onClick = onClick,
    )
}

@Composable
@NonRestartableComposable
fun TangemButton.GroupEntry(
    iconUM: TangemIconUM,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    onClick: () -> Unit,
) {
    TangemButton(
        modifier = modifier,
        variant = TangemButton.Variant.Ghost,
        size = TangemButton.Size.X9,
        iconStart = iconUM,
        isEnabled = isEnabled,
        onClick = onClick,
    )
}

@Composable
@NonRestartableComposable
fun TangemButton.GroupEntry(
    imageVector: ImageVector,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    onClick: () -> Unit,
) {
    GroupEntry(iconUM = TangemIconUM.Icon(imageVector), modifier = modifier, isEnabled = isEnabled, onClick = onClick)
}

/**
 * Hold-to-confirm preset of [TangemButton] (Figma `State = Hold`): the action fires only after
 * the user keeps the button pressed until a press-colored overlay fills it from the start edge.
 *
 * [Figma](https://www.figma.com/design/AsnJ5CPHib4Qxw12gszjMS/%F0%9F%92%A0-DS-Components?node-id=12845-873)
 *
 * Behavior notes:
 * - Touch-down plays a 200 ms press pulse over the whole button; there is no Material ripple.
 * - The overlay then grows linearly over 2 s while the pointer stays down, with haptic ticks that
 *   speed up as it fills. On completion a confirm haptic plays, [onConfirm] is invoked once and the
 *   overlay fades out in 50 ms.
 * - Releasing early — or a parent scroll taking the pointer — plays a reject haptic, retracts the
 *   overlay (240 ms, ease out), shakes the button sideways by 8dp and crossfades the label to [hint]
 *   for about a second, the width following the text. A plain tap therefore confirms nothing but
 *   explains what to do. Icon-only buttons shake without the hint.
 * - While [isLoading] is `true` or [isEnabled] is `false` the hold is ignored, and a hold in
 *   progress is cancelled if either flips mid-gesture. Callers usually set [isLoading] from
 *   [onConfirm] to show the action running.
 * - Layout, loader, icon tinting, disabled appearance and focus ring match [TangemButton].
 *
 * Accessibility: TalkBack users can't perform the hold, so the confirm action is exposed as the
 * button's click action — a double tap confirms directly. Supply [contentDescription] when the
 * label alone doesn't make clear the action is irreversible (e.g. `"Send 0.5 BTC"`).
 *
 * @param variant Visual style; only the variants with a Figma `Hold` state are available. See
 *   [TangemButton.HoldVariant].
 * @param size Token-driven size preset. See [TangemButton.Size].
 * @param isLoading When `true`, shows the loader instead of the content and ignores holds.
 * @param isEnabled When `false`, the button takes the disabled appearance and holds are ignored.
 * @param iconStart Optional leading icon.
 * @param iconEnd Optional trailing icon.
 * @param text Optional label. `null` switches the button to icon-only mode.
 * @param loadingText Optional message shown next to the loader while [isLoading] is `true`.
 * @param hint Text shown in place of [text] after an early release. Defaults to "Tap and hold".
 * @param contentDescription Accessibility label announced by TalkBack; overrides the label text.
 * @param interactionSource Interaction source for focus state; a focused button draws the focus ring.
 * @param onConfirm Invoked once each time a hold completes, or when an accessibility service
 *   activates the button.
 */
@Suppress("LongParameterList")
@Composable
fun TangemButton.HoldToConfirm(
    modifier: Modifier = Modifier,
    variant: TangemButton.HoldVariant = TangemButton.HoldVariant.Primary,
    size: TangemButton.Size = TangemButton.Size.X10,
    isLoading: Boolean = false,
    isEnabled: Boolean = true,
    iconStart: TangemIconUM? = null,
    iconEnd: TangemIconUM? = null,
    text: TextReference? = null,
    loadingText: TextReference? = null,
    hint: TextReference = resourceReference(R.string.common_tap_and_hold_hint),
    contentDescription: String? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    onConfirm: () -> Unit,
) {
    val holdState = rememberHoldToConfirmState()
    val isHoldable = isEnabled && !isLoading

    TangemButtonContainer(
        modifier = modifier.then(
            rememberHoldToConfirmGesture(isEnabled = isHoldable, state = holdState, onConfirm = onConfirm),
        ),
        variant = variant.toVariant(),
        size = size,
        isLoading = isLoading,
        isEnabled = isEnabled,
        isDimmed = false,
        iconStart = iconStart,
        iconEnd = iconEnd,
        text = text,
        loadingText = loadingText,
        contentDescription = contentDescription,
        interactionSource = interactionSource,
        hold = HoldToConfirmSpec(
            state = holdState,
            overlayColor = variant.holdOverlayColor(),
            hint = hint,
            isActionEnabled = isHoldable,
            onConfirm = onConfirm,
        ),
    )
}