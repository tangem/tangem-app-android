package com.tangem.core.ui.ds.badge

import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.ds.badge.TangemBadgeSize.*
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.clickableSingle
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign

/**
 * Tangem badge component to display a small piece of information with optional icon.
 * [Figma](https://www.figma.com/design/RU7AIgwHtGdMfy83T5UOoR/Core-Library?node-id=8441-83535&m=dev)
 *
 * @param badgeUM       [TangemBadgeUM] containing all the badge parameters.
 * @param modifier      Modifier to be applied to the badge.
 *
[REDACTED_AUTHOR]
 */
@Composable
fun TangemBadge(badgeUM: TangemBadgeUM, modifier: Modifier = Modifier) {
    TangemBadge(
        text = badgeUM.text,
        iconRes = badgeUM.iconRes,
        size = badgeUM.size,
        shape = badgeUM.shape,
        color = badgeUM.color,
        type = badgeUM.type,
        iconPosition = badgeUM.iconPosition,
        onClick = badgeUM.onClick,
        modifier = modifier,
    )
}

/**
 * Tangem badge component to display a small piece of information with optional icon.
 * [Figma](https://www.figma.com/design/RU7AIgwHtGdMfy83T5UOoR/Core-Library?node-id=8441-83535&m=dev)
 *
 * @param text          TextReference for the badge label.
 * @param modifier      Modifier to be applied to the badge.
 * @param iconRes       Drawable resource ID for the icon to be displayed in the badge.
 * @param size          [TangemBadgeSize] defining the size of the badge.
 * @param shape         [TangemBadgeShape] defining the shape of the badge.
 * @param color         [TangemBadgeColor] defining the color scheme of the badge.
 * @param type          [TangemBadgeType] defining the style of the badge.
 * @param iconPosition  [TangemBadgeIconPosition] defining icon position of the badge.
 * @param onClick       Lambda to be invoked when the badge is clicked (optional).
 *
[REDACTED_AUTHOR]
 */
@Composable
fun TangemBadge(
    text: TextReference,
    modifier: Modifier = Modifier,
    @DrawableRes iconRes: Int? = null,
    size: TangemBadgeSize = X9,
    shape: TangemBadgeShape = TangemBadgeShape.Default,
    color: TangemBadgeColor = TangemBadgeColor.Gray,
    type: TangemBadgeType = TangemBadgeType.Solid,
    iconPosition: TangemBadgeIconPosition = TangemBadgeIconPosition.Start,
    onClick: (() -> Unit)? = null,
) {
    val iconColor = getIconColor(type = type, color = color)
    Row(
        horizontalArrangement = Arrangement.spacedBy(size.toContentPadding()),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .heightIn(min = size.toHeightDp())
            .clip(shape.toShape(size))
            .getBackgroundColor(type = type, color = color, shape = shape.toShape(size))
            .padding(size.toPaddingDp(position = iconPosition))
            .clickableSingle(enabled = onClick != null, onClick = { onClick?.invoke() }),
    ) {
        AnimatedVisibility(
            visible = iconRes != null && iconPosition == TangemBadgeIconPosition.Start,
            modifier = Modifier.size(size = size.toContentSize()),
            label = "Start Icon Visibility",
        ) {
            val wrappedIconRes = remember(this) { requireNotNull(iconRes) }
            Icon(
                painter = painterResource(id = wrappedIconRes),
                contentDescription = null,
                tint = iconColor,
            )
        }
        Text(
            text = text.resolveReference(),
            style = size.toTextStyle(),
            maxLines = 1,
            color = getTextColor(type = type, color = color),
        )

        AnimatedVisibility(
            visible = iconRes != null && iconPosition == TangemBadgeIconPosition.End,
            modifier = Modifier.size(size = size.toContentSize()),
            label = "End Icon Visibility",
        ) {
            val wrappedIconRes = remember(this) { requireNotNull(iconRes) }
            Icon(
                painter = painterResource(id = wrappedIconRes),
                contentDescription = null,
                tint = iconColor,
            )
        }
    }
}

/**
 * Tangem badge shape options.
 */
enum class TangemBadgeShape {
    Default,
    Rounded,
    ;

    @ReadOnlyComposable
    @Composable
    internal fun toShape(size: TangemBadgeSize) = RoundedCornerShape(
        when (this) {
            Rounded -> when (size) {
                X4,
                X6,
                -> TangemTheme.dimens2.x4
                X9 -> TangemTheme.dimens2.x25
            }
            Default -> when (size) {
                X4 -> TangemTheme.dimens2.x1
                X6,
                X9,
                -> 6.dp
            }
        },
    )
}

/**
 * Tangem badge size options.
 */
enum class TangemBadgeSize {
    X4,
    X6,
    X9,
    ;

    @ReadOnlyComposable
    @Composable
    internal fun toHeightDp() = when (this) {
        X4 -> TangemTheme.dimens2.x4
        X6 -> TangemTheme.dimens2.x6
        X9 -> TangemTheme.dimens2.x9
    }

    @ReadOnlyComposable
    @Composable
    internal fun toPaddingDp(position: TangemBadgeIconPosition) = when (this) {
        X4 -> when (position) {
            TangemBadgeIconPosition.Start -> PaddingValues(start = 4.dp, end = 6.dp)
            TangemBadgeIconPosition.End -> PaddingValues(start = 6.dp, end = 4.dp)
        }
        X6 -> when (position) {
            TangemBadgeIconPosition.Start -> PaddingValues(start = 8.dp, end = 12.dp)
            TangemBadgeIconPosition.End -> PaddingValues(start = 12.dp, end = 8.dp)
        }
        X9 -> when (position) {
            TangemBadgeIconPosition.Start -> PaddingValues(start = 12.dp, end = 16.dp)
            TangemBadgeIconPosition.End -> PaddingValues(start = 16.dp, end = 12.dp)
        }
    }

    @ReadOnlyComposable
    @Composable
    internal fun toContentSize() = when (this) {
        X4 -> TangemTheme.dimens2.x3
        X6,
        X9,
        -> TangemTheme.dimens2.x4
    }

    @ReadOnlyComposable
    @Composable
    internal fun toContentPadding() = when (this) {
        X4 -> TangemTheme.dimens2.x0_5
        X6,
        X9,
        -> TangemTheme.dimens2.x1
    }

    @ReadOnlyComposable
    @Composable
    internal fun toTextStyle() = when (this) {
        X4 -> TangemTheme.typography2.captionSemibold11
        X6 -> TangemTheme.typography2.captionSemibold12
        X9 -> TangemTheme.typography2.bodySemibold16
    }
}

/**
 * Position of the icon in the Tangem badge.
 */
enum class TangemBadgeIconPosition {
    Start,
    End,
}

/**
 * Tangem badge type options.
 */
enum class TangemBadgeType {
    Solid,
    Tinted,
    Outline,
}

/**
 * Tangem badge color options.
 */
enum class TangemBadgeColor {
    Blue,
    Red,
    Gray,
}

@ReadOnlyComposable
@Composable
private fun getIconColor(type: TangemBadgeType, color: TangemBadgeColor) = when (color) {
    TangemBadgeColor.Gray -> TangemTheme.colors2.markers.iconGray
    TangemBadgeColor.Blue -> when (type) {
        TangemBadgeType.Outline,
        TangemBadgeType.Tinted,
        -> TangemTheme.colors2.markers.iconBlue
        TangemBadgeType.Solid -> TangemTheme.colors2.graphic.neutral.primaryInvertedConstant
    }
    TangemBadgeColor.Red -> when (type) {
        TangemBadgeType.Outline,
        TangemBadgeType.Tinted,
        -> TangemTheme.colors2.markers.iconRed
        TangemBadgeType.Solid -> TangemTheme.colors2.graphic.neutral.primaryInvertedConstant
    }
}

@ReadOnlyComposable
@Composable
private fun getTextColor(type: TangemBadgeType, color: TangemBadgeColor) = when (color) {
    TangemBadgeColor.Gray -> TangemTheme.colors2.markers.textGray
    TangemBadgeColor.Blue -> when (type) {
        TangemBadgeType.Outline,
        TangemBadgeType.Tinted,
        -> TangemTheme.colors2.markers.textBlue
        TangemBadgeType.Solid -> TangemTheme.colors2.text.neutral.primaryInvertedConstant
    }
    TangemBadgeColor.Red -> when (type) {
        TangemBadgeType.Outline,
        TangemBadgeType.Tinted,
        -> TangemTheme.colors2.markers.textRed
        TangemBadgeType.Solid -> TangemTheme.colors2.text.neutral.primaryInvertedConstant
    }
}

@ReadOnlyComposable
@Composable
private fun Modifier.getBackgroundColor(type: TangemBadgeType, color: TangemBadgeColor, shape: Shape) = when (type) {
    TangemBadgeType.Solid -> background(
        when (color) {
            TangemBadgeColor.Gray -> TangemTheme.colors2.markers.backgroundSolidGray
            TangemBadgeColor.Blue -> TangemTheme.colors2.markers.backgroundSolidBlue
            TangemBadgeColor.Red -> TangemTheme.colors2.markers.backgroundSolidRed
        },
    )
    TangemBadgeType.Tinted -> background(
        when (color) {
            TangemBadgeColor.Gray -> TangemTheme.colors2.markers.backgroundTintedGray
            TangemBadgeColor.Blue -> TangemTheme.colors2.markers.backgroundTintedBlue
            TangemBadgeColor.Red -> TangemTheme.colors2.markers.backgroundTintedRed
        },
    )
    TangemBadgeType.Outline -> {
        border(
            color = when (color) {
                TangemBadgeColor.Gray -> TangemTheme.colors2.markers.borderGray
                TangemBadgeColor.Blue -> TangemTheme.colors2.markers.borderTintedBlue
                TangemBadgeColor.Red -> TangemTheme.colors2.markers.borderTintedRed
            },
            shape = shape,
            width = 1.dp,
        )
    }
}

// region Preview
@Composable
@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun TangemBadge_Preview(@PreviewParameter(TangemBadgePreviewProvider::class) params: TangemBadgeColor) {
    TangemThemePreviewRedesign {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .background(TangemTheme.colors2.surface.level1)
                .padding(8.dp),
        ) {
            repeat(2) { yIndex ->
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    repeat(TangemBadgeType.entries.size) { index ->
                        TangemBadge(
                            text = stringReference("Title"),
                            iconRes = R.drawable.ic_information_24,
                            type = TangemBadgeType.entries[index],
                            color = params,
                            shape = TangemBadgeShape.entries[yIndex % 2],
                            iconPosition = TangemBadgeIconPosition.entries[yIndex % 2],
                        )
                    }
                }
            }
        }
    }
}

private class TangemBadgePreviewProvider : PreviewParameterProvider<TangemBadgeColor> {
    override val values: Sequence<TangemBadgeColor>
        get() = sequenceOf(
            TangemBadgeColor.Gray,
            TangemBadgeColor.Blue,
            TangemBadgeColor.Red,
        )
}
// endregion