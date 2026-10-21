package com.tangem.core.ui.ds2.button

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.DsVersion
import com.tangem.core.ui.ds2.surface.TangemSurface
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.conditional
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign

/**
 * Design-system v2 button supporting an optional leading icon, label, and trailing icon.
 *
 * Version: 1.2.2
 *
 * [Figma](https://www.figma.com/design/AsnJ5CPHib4Qxw12gszjMS/%F0%9F%92%A0-DS-Components?node-id=0-1)
 *
 * Behavior notes:
 * - When [isLoading] is `true` — or when no icon and no [text] are supplied — the content fades out
 *   and a centered [com.tangem.core.ui.ds2.loader.TangemLoader] is shown in the variant's icon
 *   color. With a [loadingText] the loader is shown inline after that message instead, and the
 *   button resizes to fit it. Clicks are still routed to [onClick] unless [isEnabled] is `false`.
 * - When [text] is `null`, the button renders in icon-only mode (square footprint driven by
 *   [size]); otherwise its width grows from [TangemButton.Size]'s `minWidth` and the label
 *   truncates with an ellipsis when it can't fit. Pass `Modifier.fillMaxWidth()` (or any width
 *   modifier) on [modifier] to switch to a fixed-width layout.
 * - [iconStart], [iconEnd], and [text] may be toggled at runtime — each slot fades and expands /
 *   shrinks horizontally so the layout animates smoothly.
 * - Icon tints are always driven by [variant] (and swapped for the disabled tint when [isEnabled]
 *   is `false`); any tint set on the supplied [TangemIconUM.Icon] is ignored.
 * - Variants with a disabled alpha fade the whole button as one layer, so the label never picks up
 *   the faded fill. The focus ring is drawn outside that layer, at full opacity, whenever the button
 *   is focused — disabled buttons remain clearly highlighted for keyboard / accessibility focus.
 *
 * @param variant Visual style. See [TangemButton.Variant].
 * @param size Token-driven size preset controlling height, padding, and icon size.
 *   See [TangemButton.Size].
 * @param isLoading When `true`, hides the content and shows a centered loader.
 * @param isEnabled When `false`, the button is dimmed by the variant's disabled alpha and clicks
 *   are ignored.
 * @param isDimmed When `true`, the button takes the disabled appearance but still routes clicks to
 *   [onClick] — for an action that is unavailable yet wants to explain why when tapped. Prefer
 *   [isEnabled] `false` whenever the tap really should do nothing.
 * @param iconStart Optional leading icon.
 * @param iconEnd Optional trailing icon.
 * @param text Optional label. `null` switches the button to icon-only mode.
 * @param loadingText Optional message shown next to the loader while [isLoading] is `true`. Also
 *   announced by TalkBack in the loading state unless [contentDescription] is supplied.
 * @param contentDescription Accessibility label announced by TalkBack. Should be supplied for
 *   icon-only buttons (e.g. `"Transfers"`), for the loading state to describe the action in
 *   progress (e.g. `"Processing payment"`), and for disabled buttons to explain why they can't be
 *   activated (e.g. `"Pay is disabled, amount is not filled"`). When non-null it overrides the
 *   label text for screen readers.
 * @param interactionSource Interaction source for press/focus state. A focused state draws the
 *   variant's focus ring around the button.
 * @param onClick Invoked when the button is clicked.
 */
@Composable
fun TangemButton(
    modifier: Modifier = Modifier,
    variant: TangemButton.Variant = TangemButton.Variant.Primary,
    size: TangemButton.Size = TangemButton.Size.X10,
    isLoading: Boolean = false,
    isEnabled: Boolean = true,
    isDimmed: Boolean = false,
    iconStart: TangemIconUM? = null,
    iconEnd: TangemIconUM? = null,
    text: TextReference? = null,
    loadingText: TextReference? = null,
    contentDescription: String? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    onClick: () -> Unit,
) {
    val isLoaderInline = isLoading && loadingText != null
    val isIconOnly = text == null && !isLoaderInline
    val shouldShowLoader = isLoading || iconStart == null && iconEnd == null && text == null
    val colorTokens = variant.tokens()
    val sizeTokens = size.tokens()
    val isFocused by interactionSource.collectIsFocusedAsState()

    val hasEnabledAppearance = isEnabled && !isDimmed
    val disabledAlpha = if (hasEnabledAppearance) 1f else colorTokens.disabledAlpha
    val backgroundColor =
        if (hasEnabledAppearance) colorTokens.backgroundColor else colorTokens.disabledBackgroundColor
    val accessibilityLabel = contentDescription
        ?: if (shouldShowLoader) (loadingText ?: text)?.resolveReference() else null

    TangemSurface(
        modifier = modifier
            .semantics(mergeDescendants = true) {
                role = Role.Button
                if (!isEnabled) disabled()
                accessibilityLabel?.let { this.contentDescription = it }
            }
            .conditional(isFocused) {
                border(width = 2.dp, color = colorTokens.focusRingColor, shape = CircleShape)
            }
            .alpha(disabledAlpha),
        onClick = onClick,
        enabled = isEnabled,
        color = backgroundColor,
        border = colorTokens.defaultBorderColor?.let { BorderStroke(width = 1.dp, color = it) },
        shape = CircleShape,
        interactionSource = interactionSource,
        isMaterial = variant == TangemButton.Variant.Material,
    ) {
        TangemButtonInternal(
            isIconOnly = isIconOnly,
            isEnabled = hasEnabledAppearance,
            isLoading = shouldShowLoader,
            isLoaderInline = isLoaderInline,
            iconStart = iconStart,
            iconEnd = iconEnd,
            text = if (isLoaderInline) loadingText else text,
            colorTokens = colorTokens,
            sizeTokens = sizeTokens,
        )
    }
}

object TangemButton {

    val version = DsVersion(major = 1, minor = 2, patch = 2)

    /**
     * Visual style of the button.
     *
     * - [Brand] — brand-colored background, static-dark content.
     * - [Primary] — inverse-surface background, used for the dominant call to action.
     * - [Secondary] — opaque-surface background, used as a secondary action alongside [Primary].
     * - [SecondaryError] — [Secondary] in the error appearance (Figma `Appearance = Error`):
     *   error-subtle background with red accent content, e.g. the negative side of a paired choice.
     * - [SecondaryInfo] — [Secondary] in the info appearance (Figma `Appearance = Info`):
     *   info-subtle background with blue accent content, e.g. the affirmative side of a paired choice.
     * - [Material] — translucent haze fill (rendered by [TangemSurface] when `isMaterial = true`),
     *   used over content backgrounds.
     * - [Success] — success-colored background for positive confirmations.
     * - [Outline] — transparent background with a secondary border.
     * - [Ghost] — transparent background, no border. Lowest visual weight.
     */
    enum class Variant {
        Brand,
        Primary,
        Secondary,
        SecondaryError,
        SecondaryInfo,
        Material,
        Success,
        Outline,
        Ghost,
    }

    /**
     * Size preset. Names follow the design-system size scale (X7 = smallest, X14 = largest) and
     * map to height / padding / icon-size tokens via the internal `tokens()` extension.
     */
    enum class Size {
        X14,
        X12,
        X11,
        X10,
        X9,
        X8,
        X7,
    }
}

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun TangemButtonPreview() {
    TangemThemePreviewRedesign {
        Column(
            modifier = Modifier
                .background(TangemTheme.colors3.bg.primary)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            PreviewSection(label = "Variants (size X10)") {
                TangemButton.Variant.entries.forEach { variant ->
                    PreviewVariantRow(variant = variant)
                }
            }
            PreviewSection(label = "Sizes (Primary)") {
                PreviewSizeRow()
            }
        }
    }
}

@Composable
private fun PreviewSection(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            color = TangemTheme.colors3.text.secondary,
            style = TangemTheme.typography3.body.medium,
        )
        content()
    }
}

@Composable
private fun PreviewVariantRow(variant: TangemButton.Variant) {
    val info = remember { TangemIconUM.Icon(iconRes = R.drawable.ic_information_24) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            modifier = Modifier.widthIn(min = 72.dp),
            text = variant.name,
            color = TangemTheme.colors3.text.tertiary,
            style = TangemTheme.typography3.body.medium,
        )
        TangemButton(variant = variant, text = stringReference("Label"), onClick = {})
        TangemButton(variant = variant, text = stringReference("Icons"), iconStart = info, iconEnd = info, onClick = {})
        TangemButton(variant = variant, text = stringReference("Loading"), isLoading = true, onClick = {})
        TangemButton(
            variant = variant,
            text = stringReference("Label"),
            loadingText = stringReference("Loading"),
            isLoading = true,
            onClick = {},
        )
        TangemButton(variant = variant, text = stringReference("Disabled"), isEnabled = false, onClick = {})
        TangemButton(variant = variant, iconStart = info, onClick = {})
    }
}

@Composable
private fun PreviewSizeRow() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TangemButton.Size.entries.forEach { size ->
            TangemButton(size = size, text = stringReference(size.name), onClick = {})
        }
    }
}