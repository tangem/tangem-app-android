@file:Suppress("MagicNumber")

package com.tangem.core.ui.ds2.tokenrow

import androidx.compose.animation.Animatable
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.RippleConfiguration
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.text.applyBladeBrush
import com.tangem.core.ui.ds2.badge.TangemBadge
import com.tangem.core.ui.ds2.loader.TangemLoader
import com.tangem.core.ui.ds2.loader.TangemLoaderSize
import com.tangem.core.ui.ds2.shimmers.TangemShimmer
import com.tangem.core.ui.ds2.tokenicon.TangemTokenIcon
import com.tangem.core.ui.ds2.util.TangemPriceChange
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.conditionalCompose
import com.tangem.core.ui.extensions.orMaskWithStars
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_cloud_exclamation_20
import com.tangem.core.ui.res.generated.icons.ic_warning_20
import kotlin.math.max

/** Content dimming applied to the leading texts of unavailable rows (Figma `opacity/disabled`). */
internal const val TOKEN_ROW_DISABLED_ALPHA = 0.4f

// region Container layout

/** Slot ids of the [TokenRowContainer] custom layout. */
internal enum class TokenRowLayoutId {
    HEAD, START_TOP, END_TOP, START_BOTTOM, END_BOTTOM, TAIL, EXTRA_BOTTOM
}

/**
 * Token-row container
 *
 * Policy:
 * - HEAD and TAIL are measured first and take their intrinsic width.
 * - END slots take the free space left after guaranteeing the START side its minimum width
 *   (30% of the row for the top line, 32% for the bottom line).
 * - START slots fill the remaining width, never shrinking below that minimum.
 * - A line with no counterpart on the other axis is vertically centered in the main area.
 * - EXTRA_BOTTOM is placed full-width below the main content with an 8dp gap.
 */
@Suppress("LongMethod")
@Composable
internal fun TokenRowContainer(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val isInteractive = onClick != null || onLongClick != null

    val density = LocalDensity.current
    val verticalPadding = with(density) { 4.dp.roundToPx() }
    val extraContentPadding = with(density) { 8.dp.roundToPx() }
    val contentPadding = with(density) { 12.dp.roundToPx() }

    val rowModifier = modifier
        .conditionalCompose(isFocused) {
            border(
                width = 2.dp,
                color = TangemTheme.colors3.interaction.focusRing.brand,
                shape = RoundedCornerShape(4.dp),
            )
        }
        .conditionalCompose(isInteractive) {
            combinedClickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                role = Role.Button,
                onLongClick = onLongClick,
                onClick = onClick ?: {},
            )
        }

    WithTokenRowRipple(enabled = isInteractive) {
        Layout(
            content = content,
            modifier = rowModifier,
        ) { measurables, constraints ->
            val layoutWidth = max(0, constraints.maxWidth - contentPadding * 2)

            val startTopMinWidth = (layoutWidth * TITLE_MIN_WIDTH_COEFFICIENT).toInt()
            val startBottomMinWidth = (layoutWidth * PRICE_MIN_WIDTH_COEFFICIENT).toInt()

            val headPlaceable = measurables.measure(
                layoutId = TokenRowLayoutId.HEAD,
                constraints = constraints.copy(minWidth = 0),
            )
            val tailPlaceable = measurables.measure(
                layoutId = TokenRowLayoutId.TAIL,
                constraints = constraints.copy(minWidth = 0),
            )

            val availableWidthForBody = layoutWidth - headPlaceable.widthOrZero() - tailPlaceable.widthOrZero()

            // End slots take the whole free space but always leave the start side its minimum width.
            val endTopPlaceable = measurables.measure(
                layoutId = TokenRowLayoutId.END_TOP,
                constraints = constraints.copy(
                    minWidth = 0,
                    maxWidth = max(0, availableWidthForBody - startTopMinWidth),
                ),
            )
            val endBottomPlaceable = measurables.measure(
                layoutId = TokenRowLayoutId.END_BOTTOM,
                constraints = constraints.copy(
                    minWidth = 0,
                    maxWidth = max(0, availableWidthForBody - startBottomMinWidth),
                ),
            )

            // Start slots fill the remaining width but never less than their minimum.
            val startTopPlaceable = measurables.measure(
                layoutId = TokenRowLayoutId.START_TOP,
                constraints = constraints.copy(
                    minWidth = 0,
                    maxWidth = max(
                        a = startTopMinWidth,
                        b = availableWidthForBody - endTopPlaceable.widthOrZero(),
                    ),
                ),
            )
            val startBottomPlaceable = measurables.measure(
                layoutId = TokenRowLayoutId.START_BOTTOM,
                constraints = constraints.copy(
                    minWidth = 0,
                    maxWidth = max(
                        a = startBottomMinWidth,
                        b = availableWidthForBody - endBottomPlaceable.widthOrZero(),
                    ),
                ),
            )

            val extraBottomPlaceable = measurables.measure(
                layoutId = TokenRowLayoutId.EXTRA_BOTTOM,
                constraints = constraints,
            )

            val mainLayoutHeight = maxOf(
                headPlaceable.heightOrZero(),
                tailPlaceable.heightOrZero(),
                startTopPlaceable.heightOrZero() + startBottomPlaceable.heightOrZero() + verticalPadding,
                endTopPlaceable.heightOrZero() + endBottomPlaceable.heightOrZero() + verticalPadding,
            )

            val mainContentBottomPadding = if (extraBottomPlaceable != null) {
                extraBottomPlaceable.heightOrZero() + contentPadding
            } else {
                contentPadding
            }

            val layoutHeight = mainLayoutHeight + contentPadding + mainContentBottomPadding

            layout(width = constraints.maxWidth, height = layoutHeight) {
                headPlaceable?.placeRelative(
                    x = contentPadding,
                    y = contentPadding + (mainLayoutHeight - headPlaceable.height).div(other = 2),
                )

                startTopPlaceable?.placeRelative(
                    x = contentPadding + headPlaceable.widthOrZero(),
                    y = contentPadding + if (startBottomPlaceable == null) {
                        (mainLayoutHeight - startTopPlaceable.height).div(2)
                    } else {
                        0
                    },
                )

                startBottomPlaceable?.placeRelative(
                    x = contentPadding + headPlaceable.widthOrZero(),
                    y = contentPadding + if (startTopPlaceable == null) {
                        (mainLayoutHeight - startBottomPlaceable.height).div(2)
                    } else {
                        startTopPlaceable.heightOrZero() + verticalPadding
                    },
                )

                endTopPlaceable?.placeRelative(
                    x = layoutWidth - endTopPlaceable.widthOrZero() - tailPlaceable.widthOrZero() + contentPadding,
                    y = contentPadding + if (endBottomPlaceable == null) {
                        (mainLayoutHeight - endTopPlaceable.height).div(2)
                    } else {
                        0
                    },
                )

                endBottomPlaceable?.placeRelative(
                    x = layoutWidth - endBottomPlaceable.widthOrZero() - tailPlaceable.widthOrZero() + contentPadding,
                    y = contentPadding + if (endTopPlaceable == null) {
                        (mainLayoutHeight - endBottomPlaceable.height).div(2)
                    } else {
                        endTopPlaceable.heightOrZero() + verticalPadding
                    },
                )

                tailPlaceable?.placeRelative(
                    x = layoutWidth - tailPlaceable.width + contentPadding,
                    y = contentPadding + (mainLayoutHeight - tailPlaceable.height).div(other = 2),
                )

                extraBottomPlaceable?.placeRelative(
                    x = 0,
                    y = contentPadding + mainLayoutHeight + extraContentPadding,
                )
            }
        }
    }
}

private const val TITLE_MIN_WIDTH_COEFFICIENT = 0.3
private const val PRICE_MIN_WIDTH_COEFFICIENT = 0.32

private fun List<Measurable>.measure(layoutId: TokenRowLayoutId, constraints: Constraints): Placeable? {
    return firstOrNull { it.layoutId == layoutId }?.measure(constraints)
}

private fun Placeable?.widthOrZero(): Int = this?.width ?: 0

private fun Placeable?.heightOrZero(): Int = this?.height ?: 0

@Composable
private fun WithTokenRowRipple(enabled: Boolean, content: @Composable () -> Unit) {
    if (enabled) {
        CompositionLocalProvider(LocalRippleConfiguration provides tokenRowRipple(), content = content)
    } else {
        content()
    }
}

@Composable
@ReadOnlyComposable
private fun tokenRowRipple(): RippleConfiguration = RippleConfiguration(
    color = TangemTheme.colors3.interaction.press.default,
    rippleAlpha = RippleAlpha(
        draggedAlpha = 0f,
        focusedAlpha = 0f,
        hoveredAlpha = 0.05f,
        pressedAlpha = 0.1f,
    ),
)

// endregion

// region Slot contents

/** Head slot: 40dp token icon with the 12dp gap to the content, like the DS2 row. */
@Composable
internal fun TokenRowHeadIcon(icon: TangemTokenIcon.UiState) {
    TangemTokenIcon(
        state = icon,
        size = TangemTokenIcon.Size.X40,
        modifier = Modifier
            .layoutId(layoutId = TokenRowLayoutId.HEAD)
            .padding(end = 12.dp),
    )
}

/** Market subtitle line: rank badge (e.g. `2`) and capitalization (e.g. `1.196T`). */
@Composable
internal fun TokenRowMarketSubtitleContent(
    position: TextReference?,
    capitalization: TextReference?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (position != null) {
            TangemBadge(
                text = position,
                variant = TangemBadge.Variant.Tinted,
                status = TangemBadge.Status.Neutral,
                size = TangemBadge.Size.X4,
            )
        }
        if (capitalization != null) {
            Text(
                text = capitalization.resolveReference(),
                style = TangemTheme.typography3.caption.medium,
                color = TangemTheme.colors3.text.secondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Market price line. When [updateDirection] is set, the text flashes in the direction color and
 * fades back to primary every time [price] changes — ports the live-update blink of the legacy
 * `TokenPriceText`. The first composition never blinks.
 */
@Composable
internal fun TokenRowMarketPriceContent(
    price: TextReference,
    modifier: Modifier = Modifier,
    updateDirection: TangemPriceChange.Direction? = null,
) {
    val generalColor = TangemTheme.colors3.text.primary
    val growColor = TangemTheme.colors3.text.accent.blue
    val fallColor = TangemTheme.colors3.text.status.error

    val color = remember(generalColor) { Animatable(generalColor) }
    var isFirstEmissionSkipped by remember { mutableStateOf(false) }

    LaunchedEffect(price) {
        if (!isFirstEmissionSkipped) {
            isFirstEmissionSkipped = true
            return@LaunchedEffect
        }
        val blinkColor = when (updateDirection) {
            TangemPriceChange.Direction.Up -> growColor
            TangemPriceChange.Direction.Down -> fallColor
            TangemPriceChange.Direction.Neutral, null -> return@LaunchedEffect
        }
        color.animateTo(blinkColor, snap())
        color.animateTo(generalColor, tween(durationMillis = PRICE_BLINK_FADE_DURATION_MILLIS))
    }

    Text(
        text = price.resolveReference(),
        style = TangemTheme.typography3.body.medium,
        color = color.value,
        maxLines = 1,
        overflow = TextOverflow.Visible,
        modifier = modifier,
    )
}

private const val PRICE_BLINK_FADE_DURATION_MILLIS = 500

/** Title line: token name, optional pending-transaction loader, ticker (baseline-aligned), badge. */
@Composable
internal fun TokenRowTitleContent(
    title: TextReference,
    modifier: Modifier = Modifier,
    ticker: TextReference? = null,
    badge: TangemTokenRow.Badge? = null,
    hasPending: Boolean = false,
    isDimmed: Boolean = false,
) {
    Row(
        modifier = modifier.alpha(if (isDimmed) TOKEN_ROW_DISABLED_ALPHA else 1f),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title.resolveReference(),
            style = TangemTheme.typography3.body.medium,
            color = TangemTheme.colors3.text.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(weight = 1f, fill = false)
                .alignByBaseline(),
        )
        if (hasPending) {
            TangemLoader(
                size = TangemLoaderSize.X16,
                color = TangemTheme.colors3.icon.tertiary,
            )
        }
        if (ticker != null) {
            Text(
                text = ticker.resolveReference(),
                style = TangemTheme.typography3.caption.medium,
                color = TangemTheme.colors3.text.secondary,
                maxLines = 1,
                modifier = Modifier.alignByBaseline(),
            )
        }
        if (badge != null) {
            TangemBadge(
                text = badge.text,
                variant = badge.variant,
                status = badge.status,
                size = TangemBadge.Size.X4,
            )
        }
    }
}

/** Subtitle line: quote (e.g. `$1.00`) and the [TangemPriceChange] indicator. */
@Composable
internal fun TokenRowSubtitleContent(
    quote: TextReference?,
    priceChange: TangemPriceChange.State?,
    modifier: Modifier = Modifier,
    isDimmed: Boolean = false,
    isFlickering: Boolean = false,
) {
    Row(
        modifier = modifier.alpha(if (isDimmed) TOKEN_ROW_DISABLED_ALPHA else 1f),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (quote != null) {
            Text(
                text = quote.resolveReference(),
                style = TangemTheme.typography3.caption.medium.applyBladeBrush(
                    isEnabled = isFlickering,
                    textColor = TangemTheme.colors3.text.secondary,
                ),
                color = TangemTheme.colors3.text.secondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (priceChange != null) {
            TangemPriceChange(state = priceChange, isFlickering = isFlickering)
        }
    }
}

/** Fiat balance with optional contract-error and update-error icons in front of it. */
@Composable
internal fun TokenRowBalanceContent(
    fiatBalance: TextReference,
    modifier: Modifier = Modifier,
    showContractWarning: Boolean = false,
    showUpdateWarning: Boolean = false,
    isFlickering: Boolean = false,
    isBalanceHidden: Boolean = false,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showContractWarning) {
            Icon(
                imageVector = Icons.ic_warning_20,
                contentDescription = null,
                tint = TangemTheme.colors3.icon.status.warning,
                modifier = Modifier.size(20.dp),
            )
        }
        if (showUpdateWarning) {
            Icon(
                imageVector = Icons.ic_cloud_exclamation_20,
                contentDescription = null,
                tint = TangemTheme.colors3.icon.primary,
                modifier = Modifier.size(20.dp),
            )
        }
        Text(
            text = fiatBalance.orMaskWithStars(isBalanceHidden).resolveReference(),
            style = TangemTheme.typography3.body.medium.applyBladeBrush(
                isEnabled = isFlickering,
                textColor = TangemTheme.colors3.text.primary,
            ),
            color = TangemTheme.colors3.text.primary,
            maxLines = 1,
        )
    }
}

/** Plain caption line used for secondary amounts (crypto balance, Organize fiat balance). */
@Composable
internal fun TokenRowCaptionText(
    text: TextReference,
    modifier: Modifier = Modifier,
    isFlickering: Boolean = false,
    isBalanceHidden: Boolean = false,
) {
    Text(
        text = text.orMaskWithStars(isBalanceHidden).resolveReference(),
        style = TangemTheme.typography3.caption.medium.applyBladeBrush(
            isEnabled = isFlickering,
            textColor = TangemTheme.colors3.text.secondary,
        ),
        color = TangemTheme.colors3.text.secondary,
        maxLines = 1,
        modifier = modifier,
    )
}

/** Fixed-width shimmer bar sized after a typography line, like the [TangemShimmer] text overload. */
@Composable
internal fun TokenRowShimmerLine(style: TextStyle, width: Dp, modifier: Modifier = Modifier) {
    val lineHeight = with(LocalDensity.current) { style.lineHeight.toDp() }
    TangemShimmer(
        radius = 16.dp,
        modifier = modifier
            .width(width)
            .height(lineHeight)
            .padding(vertical = 2.dp),
    )
}

// endregion