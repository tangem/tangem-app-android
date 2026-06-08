package com.tangem.core.ui.ds2.badge

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.ds.image.TangemIcon
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.surface.TangemSurface
import com.tangem.core.ui.extensions.ColorReference2
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign

/**
 * Design-system v2 badge: a compact pill displaying a short label with optional leading / trailing
 * icons.
 *
 * [Figma](https://www.figma.com/design/AsnJ5CPHib4Qxw12gszjMS/%F0%9F%92%A0-DS-Components?node-id=2002-213)
 *
 * Behavior notes:
 * - Shape is always a fully-rounded pill (`borderRadius.full`).
 * - Icon tints are driven by [variant] + [status]; any tint set on a supplied [TangemIconUM.Icon]
 *   is overridden. Other [TangemIconUM] subtypes (e.g. currency / image / url) pass through with
 *   their own colors intact.
 * - The badge is non-interactive by default. Pass [onClick] to make it clickable.
 *
 * @param text Badge label.
 * @param modifier Modifier applied to the badge container.
 * @param variant Visual style. See [TangemBadge.Variant].
 * @param status Status color scheme (Neutral / Info / Error / Success / Warning).
 * @param size Token-driven size preset controlling height, padding, icon size and text style.
 *   See [TangemBadge.Size].
 * @param iconStart Optional leading icon.
 * @param iconEnd Optional trailing icon.
 * @param contentDescription Accessibility label announced by TalkBack. When non-null it overrides
 *   the label text for screen readers.
 * @param onClick Optional click handler. `null` makes the badge non-interactive.
 */
@Suppress("LongParameterList")
@Composable
fun TangemBadge(
    text: TextReference,
    modifier: Modifier = Modifier,
    variant: TangemBadge.Variant = TangemBadge.Variant.Tinted,
    status: TangemBadge.Status = TangemBadge.Status.Neutral,
    size: TangemBadge.Size = TangemBadge.Size.X9,
    iconStart: TangemIconUM? = null,
    iconEnd: TangemIconUM? = null,
    contentDescription: String? = null,
    onClick: (() -> Unit)? = null,
) {
    val colorTokens = resolveColorTokens(variant = variant, status = status)
    val sizeTokens = size.tokens()

    TangemSurface(
        modifier = modifier
            .semantics(mergeDescendants = true) {
                if (onClick != null) role = Role.Button
                contentDescription?.let { this.contentDescription = it }
            }
            .heightIn(min = sizeTokens.minHeight),
        onClick = onClick,
        color = colorTokens.backgroundColor,
        border = colorTokens.borderColor?.let { BorderStroke(1.dp, it) },
        shape = RoundedCornerShape(999.dp),
    ) {
        BadgeContent(
            iconStart = iconStart,
            iconEnd = iconEnd,
            text = text,
            colorTokens = colorTokens,
            sizeTokens = sizeTokens,
        )
    }
}

@Composable
private fun BadgeContent(
    iconStart: TangemIconUM?,
    iconEnd: TangemIconUM?,
    text: TextReference,
    colorTokens: BadgeColorTokens,
    sizeTokens: BadgeSizeTokens,
) {
    Row(
        modifier = Modifier
            .heightIn(min = sizeTokens.minHeight)
            .padding(
                horizontal = sizeTokens.containerHorizontalPadding,
                vertical = sizeTokens.containerVerticalPadding,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        iconStart?.let { icon ->
            TangemIcon(
                modifier = Modifier.size(sizeTokens.iconSize),
                tangemIconUM = icon.applyTint(colorTokens.iconTint),
            )
        }
        Text(
            modifier = Modifier.padding(horizontal = sizeTokens.labelPadding),
            text = text.resolveReference(),
            color = colorTokens.textColor,
            style = sizeTokens.textStyle,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
        )
        iconEnd?.let { icon ->
            TangemIcon(
                modifier = Modifier.size(sizeTokens.iconSize),
                tangemIconUM = icon.applyTint(colorTokens.iconTint),
            )
        }
    }
}

/**
 * Forces [tint] onto [TangemIconUM.Icon] so the badge's variant always drives icon color. Other
 * icon types (currency, image, url) pass through unchanged so they keep their own visuals.
 */
private fun TangemIconUM.applyTint(tint: Color): TangemIconUM = when (this) {
    is TangemIconUM.Icon -> copy(tint = ColorReference2 { tint })
    else -> this
}

object TangemBadge {

    /**
     * Visual style of the badge.
     *
     * - [Tinted] — soft tinted fill (subtle status color or opaque neutral), no border.
     * - [Outline] — tinted fill with a matching border.
     * - [Solid] — saturated status color fill with static-dark content.
     */
    enum class Variant {
        Tinted,
        Outline,
        Solid,
    }

    /** Status color scheme of the badge. */
    enum class Status {
        Neutral,
        Info,
        Error,
        Success,
        Warning,
    }

    /**
     * Size preset. Names follow the design-system size scale: X9 is the largest (min height 36dp),
     * X4 is the smallest (min height 16dp).
     */
    enum class Size {
        X9,
        X6,
        X4,
    }
}

/** Resolved colors for a (variant, status) pair. */
private data class BadgeColorTokens(
    val backgroundColor: Color,
    val textColor: Color,
    val iconTint: Color,
    val borderColor: Color? = null,
)

/** Resolved per-size dimensions used by [TangemBadge]. */
private data class BadgeSizeTokens(
    val minHeight: Dp,
    val containerHorizontalPadding: Dp,
    val containerVerticalPadding: Dp,
    val labelPadding: Dp,
    val iconSize: Dp,
    val textStyle: TextStyle,
)

@Composable
@ReadOnlyComposable
private fun TangemBadge.Size.tokens(): BadgeSizeTokens {
    val typography = TangemTheme.typography3
    return when (this) {
        TangemBadge.Size.X9 -> BadgeSizeTokens(
            minHeight = 36.dp,
            containerHorizontalPadding = 8.dp,
            containerVerticalPadding = 8.dp,
            labelPadding = 4.dp,
            iconSize = 20.dp,
            textStyle = typography.subheading.medium,
        )
        TangemBadge.Size.X6 -> BadgeSizeTokens(
            minHeight = 24.dp,
            containerHorizontalPadding = 4.dp,
            containerVerticalPadding = 4.dp,
            labelPadding = 4.dp,
            iconSize = 16.dp,
            textStyle = typography.caption.medium,
        )
        TangemBadge.Size.X4 -> BadgeSizeTokens(
            minHeight = 16.dp,
            containerHorizontalPadding = 2.dp,
            containerVerticalPadding = 0.dp,
            labelPadding = 2.dp,
            iconSize = 12.dp,
            textStyle = typography.caption.medium,
        )
    }
}

@Suppress("LongMethod", "CyclomaticComplexMethod")
@Composable
@ReadOnlyComposable
private fun resolveColorTokens(variant: TangemBadge.Variant, status: TangemBadge.Status): BadgeColorTokens {
    val colors = TangemTheme.colors3
    return when (variant) {
        TangemBadge.Variant.Tinted -> when (status) {
            TangemBadge.Status.Neutral -> BadgeColorTokens(
                backgroundColor = colors.bg.opaque.primary,
                textColor = colors.text.secondary,
                iconTint = colors.icon.secondary,
            )
            TangemBadge.Status.Info -> BadgeColorTokens(
                backgroundColor = colors.bg.status.infoSubtle,
                textColor = colors.text.status.info,
                iconTint = colors.icon.status.info,
            )
            TangemBadge.Status.Error -> BadgeColorTokens(
                backgroundColor = colors.bg.status.errorSubtle,
                textColor = colors.text.status.error,
                iconTint = colors.icon.status.error,
            )
            TangemBadge.Status.Success -> BadgeColorTokens(
                backgroundColor = colors.bg.status.successSubtle,
                textColor = colors.text.status.success,
                iconTint = colors.icon.status.success,
            )
            TangemBadge.Status.Warning -> BadgeColorTokens(
                backgroundColor = colors.bg.status.warningSubtle,
                textColor = colors.text.status.warning,
                iconTint = colors.icon.status.warning,
            )
        }
        TangemBadge.Variant.Outline -> when (status) {
            TangemBadge.Status.Neutral -> BadgeColorTokens(
                backgroundColor = colors.bg.opaque.primary,
                textColor = colors.text.secondary,
                iconTint = colors.icon.secondary,
                borderColor = colors.border.primary,
            )
            TangemBadge.Status.Info -> BadgeColorTokens(
                backgroundColor = colors.bg.status.infoSubtle,
                textColor = colors.text.status.info,
                iconTint = colors.icon.status.info,
                borderColor = colors.border.status.infoSubtle,
            )
            TangemBadge.Status.Error -> BadgeColorTokens(
                backgroundColor = colors.bg.status.errorSubtle,
                textColor = colors.text.status.error,
                iconTint = colors.icon.status.error,
                borderColor = colors.border.status.errorSubtle,
            )
            TangemBadge.Status.Success -> BadgeColorTokens(
                backgroundColor = colors.bg.status.successSubtle,
                textColor = colors.text.status.success,
                iconTint = colors.icon.status.success,
                borderColor = colors.border.status.successSubtle,
            )
            TangemBadge.Status.Warning -> BadgeColorTokens(
                backgroundColor = colors.bg.status.warningSubtle,
                textColor = colors.text.status.warning,
                iconTint = colors.icon.status.warning,
                borderColor = colors.border.status.warningSubtle,
            )
        }
        TangemBadge.Variant.Solid -> when (status) {
            TangemBadge.Status.Neutral -> BadgeColorTokens(
                backgroundColor = colors.bg.opaque.primary,
                textColor = colors.text.primary,
                iconTint = colors.icon.primary,
            )
            TangemBadge.Status.Info -> BadgeColorTokens(
                backgroundColor = colors.bg.status.info,
                textColor = colors.text.staticDark.primary,
                iconTint = colors.icon.staticDark,
            )
            TangemBadge.Status.Error -> BadgeColorTokens(
                backgroundColor = colors.bg.status.error,
                textColor = colors.text.staticDark.primary,
                iconTint = colors.icon.staticDark,
            )
            TangemBadge.Status.Success -> BadgeColorTokens(
                backgroundColor = colors.bg.status.success,
                textColor = colors.text.staticDark.primary,
                iconTint = colors.icon.staticDark,
            )
            TangemBadge.Status.Warning -> BadgeColorTokens(
                backgroundColor = colors.bg.status.warning,
                textColor = colors.text.staticDark.primary,
                iconTint = colors.icon.staticDark,
            )
        }
    }
}

// region Previews

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun TangemBadgePreview() {
    TangemThemePreviewRedesign {
        Column(
            modifier = Modifier
                .background(TangemTheme.colors3.bg.primary)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TangemBadge.Variant.entries.forEach { variant ->
                PreviewVariantBlock(variant = variant)
            }
            PreviewSizesBlock()
        }
    }
}

@Composable
private fun PreviewVariantBlock(variant: TangemBadge.Variant) {
    val icon = remember { TangemIconUM.Icon(iconRes = R.drawable.ic_information_24) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = variant.name,
            color = TangemTheme.colors3.text.secondary,
            style = TangemTheme.typography3.body.medium,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TangemBadge.Status.entries.forEach { status ->
                TangemBadge(
                    text = stringReference(status.name),
                    variant = variant,
                    status = status,
                    iconStart = icon,
                )
            }
        }
    }
}

@Composable
private fun PreviewSizesBlock() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            modifier = Modifier.widthIn(min = 72.dp),
            text = "Sizes (Tinted / Info)",
            color = TangemTheme.colors3.text.secondary,
            style = TangemTheme.typography3.body.medium,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TangemBadge.Size.entries.forEach { size ->
                TangemBadge(
                    text = stringReference(size.name),
                    variant = TangemBadge.Variant.Tinted,
                    status = TangemBadge.Status.Info,
                    size = size,
                )
            }
        }
    }
}

// endregion