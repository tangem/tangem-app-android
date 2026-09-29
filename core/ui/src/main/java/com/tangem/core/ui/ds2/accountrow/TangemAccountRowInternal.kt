@file:Suppress("MagicNumber")

package com.tangem.core.ui.ds2.accountrow

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.tangem.core.ui.components.text.applyBladeBrush
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.shimmers.TangemShimmer
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.conditionalCompose
import com.tangem.core.ui.extensions.orMaskWithStars
import com.tangem.core.ui.extensions.resolveAnnotatedReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_arrow_collapse_20
import com.tangem.core.ui.res.generated.icons.ic_cloud_exclamation_20
import com.tangem.core.ui.res.generated.icons.ic_dot_12_filled
import com.tangem.core.ui.res.generated.icons.ic_grid_plus_20
import com.tangem.utils.StringsSigns
import kotlin.math.max
import kotlin.math.roundToInt

// region Container layout

/** Slot ids of the [AccountRowContainer] custom layout. */
internal enum class AccountRowLayoutId {
    AVATAR,
    TITLE,
    TITLE_ANCHOR,
    SUBTITLE,
    BALANCE_LARGE,
    PRICE_CHANGE,
    END_BADGE,
    DOT,
    BALANCE_SMALL,
    ARROW,
    CONTENT,
}

internal val AccountRowShape = RoundedCornerShape(24.dp)

/**
 * Card container of [TangemAccountRow].
 *
 * A single custom [Layout] owns both states instead of swapping two layouts, because the avatar and
 * the title travel continuously between them: each child is placed at the point interpolated by
 * [AccountRowMotion.expansion] between its collapsed and its expanded anchor, and the card height
 * is interpolated the same way. Children that belong to one state only are still placed on every
 * frame — they are hidden by their own alpha, not by being absent, so their position keeps tracking
 * the ones that move.
 */
@Suppress("LongMethod", "CyclomaticComplexMethod", "LongParameterList")
@Composable
internal fun AccountRowContainer(
    motion: AccountRowMotion,
    interactionSource: MutableInteractionSource,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val isFocused by interactionSource.collectIsFocusedAsState()
    val isInteractive = onClick != null || onLongClick != null

    val cardModifier = modifier
        .fillMaxWidth()
        .clip(AccountRowShape)
        .background(TangemTheme.colors3.bg.secondary)
        .conditionalCompose(isFocused) {
            border(
                width = 2.dp,
                color = TangemTheme.colors3.interaction.focusRing.brand,
                shape = AccountRowShape,
            )
        }
        .conditionalCompose(isInteractive) {
            combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onLongClick = onLongClick,
                onClick = onClick ?: {},
            )
        }

    Layout(content = content, modifier = cardModifier) { measurables, constraints ->
        val progress = motion.expansion
        val padding = 16.dp.roundToPx()
        val avatarGap = 8.dp.roundToPx()
        val contentGap = 12.dp.roundToPx()
        val inlineGap = 2.dp.roundToPx()
        val avatarCollapsed = 40.dp.roundToPx()
        val avatarExpanded = 16.dp.roundToPx()
        val expandedLineHeight = 20.dp.roundToPx()
        val expandedBottomPadding = 8.dp.roundToPx()

        val width = constraints.maxWidth
        val innerWidth = max(0, width - padding * 2)
        val loose = Constraints(maxWidth = width)

        val avatar = measurables.measureById(
            id = AccountRowLayoutId.AVATAR,
            constraints = Constraints.fixed(avatarCollapsed, avatarCollapsed),
        )
        val arrow = measurables.measureById(AccountRowLayoutId.ARROW, loose)
        val dot = measurables.measureById(AccountRowLayoutId.DOT, loose)

        val endMaxWidth = (innerWidth * END_CLUSTER_MAX_WIDTH_FRACTION).toInt()
        val endConstraints = Constraints(maxWidth = endMaxWidth)
        val balanceLarge = measurables.measureById(AccountRowLayoutId.BALANCE_LARGE, endConstraints)
        val priceChange = measurables.measureById(AccountRowLayoutId.PRICE_CHANGE, endConstraints)
        val endBadge = measurables.measureById(AccountRowLayoutId.END_BADGE, endConstraints)
        val balanceSmall = measurables.measureById(AccountRowLayoutId.BALANCE_SMALL, endConstraints)

        val endWidth = maxOf(endBadge.widthOrZero(), balanceLarge.widthOrZero(), priceChange.widthOrZero())
        val startWidthCollapsed = max(
            0,
            innerWidth - avatarCollapsed - avatarGap - contentGap - endWidth,
        )
        val titleWidthExpanded = max(
            0,
            innerWidth - avatarExpanded - avatarGap - arrow.widthOrZero() - avatarGap -
                dot.widthOrZero() - balanceSmall.widthOrZero() - inlineGap * 2,
        )
        val titleScale = motion.titleScale
        val titleBudgetExpanded = (titleWidthExpanded / ACCOUNT_ROW_TITLE_EXPANDED_SCALE).roundToInt()
        val titleMaxWidth = if (motion.isTitleCaption) {
            titleWidthExpanded
        } else {
            lerp(startWidthCollapsed, titleBudgetExpanded, progress)
        }

        val title = measurables.measureById(
            id = AccountRowLayoutId.TITLE,
            constraints = Constraints(maxWidth = max(0, titleMaxWidth)),
        )
        val subtitle = measurables.measureById(
            id = AccountRowLayoutId.SUBTITLE,
            constraints = Constraints(maxWidth = startWidthCollapsed),
        )
        val titleAnchor = measurables.measureById(
            id = AccountRowLayoutId.TITLE_ANCHOR,
            constraints = Constraints(maxWidth = max(0, titleWidthExpanded)),
        )
        val slot = measurables.measureById(
            id = AccountRowLayoutId.CONTENT,
            constraints = Constraints(minWidth = width, maxWidth = width),
        )

        val startClusterHeight = title.heightOrZero() +
            if (subtitle != null) inlineGap + subtitle.height else 0
        val endClusterHeight = when {
            endBadge != null -> endBadge.height
            balanceLarge != null && priceChange != null ->
                balanceLarge.height + inlineGap + priceChange.height
            else -> max(balanceLarge.heightOrZero(), priceChange.heightOrZero())
        }
        val mainHeight = maxOf(avatarCollapsed, startClusterHeight, endClusterHeight)
        val collapsedHeight = mainHeight + padding * 2

        val lineHeight = maxOf(
            expandedLineHeight,
            arrow.heightOrZero(),
            (title.heightOrZero() * titleScale).roundToInt(),
        )
        val expandedHeadingHeight = padding + lineHeight + expandedBottomPadding
        val expandedHeight = expandedHeadingHeight + slot.heightOrZero()

        val height = lerp(collapsedHeight, expandedHeight, progress)
        val headingBottom = lerp(collapsedHeight, expandedHeadingHeight, progress)

        layout(width = width, height = max(0, height)) {
            val avatarX = padding
            val avatarY = lerp(
                padding + (mainHeight - avatarCollapsed) / 2,
                padding + (lineHeight - avatarExpanded) / 2,
                progress,
            )
            avatar?.placeRelative(x = avatarX, y = avatarY)

            val titleX = lerp(
                padding + avatarCollapsed + avatarGap,
                padding + avatarExpanded + avatarGap,
                progress,
            )
            val startClusterTop = padding + (mainHeight - startClusterHeight) / 2
            val titleCenterY = lerp(
                startClusterTop + title.heightOrZero() / 2,
                padding + lineHeight / 2,
                progress,
            )
            val titleY = titleCenterY - title.heightOrZero() / 2
            title?.placeRelative(x = titleX, y = titleY)

            subtitle?.placeRelative(
                x = titleX,
                y = titleY + title.heightOrZero() + inlineGap,
            )

            val endRight = width - padding
            val endTop = padding + (mainHeight - endClusterHeight) / 2
            endBadge?.placeRelative(x = endRight - endBadge.width, y = endTop)
            balanceLarge?.placeRelative(x = endRight - balanceLarge.width, y = endTop)
            priceChange?.placeRelative(
                x = endRight - priceChange.width,
                y = if (balanceLarge != null) endTop + balanceLarge.height + inlineGap else endTop,
            )

            val titleRight = lerp(
                padding + avatarCollapsed + avatarGap + title.widthOrZero(),
                padding + avatarExpanded + avatarGap + titleAnchor.widthOrZero(),
                progress,
            )
            val dotX = titleRight + inlineGap
            dot?.placeRelative(x = dotX, y = titleCenterY - dot.height / 2)
            balanceSmall?.placeRelative(
                x = dotX + dot.widthOrZero() + inlineGap,
                y = titleCenterY - balanceSmall.height / 2,
            )

            arrow?.placeRelative(
                x = endRight - arrow.width,
                y = lerp(
                    padding + (mainHeight - arrow.height) / 2,
                    padding + (lineHeight - arrow.height) / 2,
                    progress,
                ),
            )

            slot?.placeRelative(x = 0, y = headingBottom)
        }
    }
}

/** How much of the row's inner width the collapsed balance column may take before the title shrinks. */
private const val END_CLUSTER_MAX_WIDTH_FRACTION = 0.6

private fun List<Measurable>.measureById(id: AccountRowLayoutId, constraints: Constraints): Placeable? =
    firstOrNull { it.layoutId == id }?.measure(constraints)

private fun Placeable?.widthOrZero(): Int = this?.width ?: 0

private fun Placeable?.heightOrZero(): Int = this?.height ?: 0

// endregion

// region Slot contents

/**
 * Title of the row. Composed at `body.medium` and scaled down while the row moves, then swapped to a
 * real `caption.medium` once [AccountRowMotion.isSettled] — scaled glyphs stay crisp only at rest.
 */
@Composable
internal fun AccountRowTitle(title: TextReference, motion: AccountRowMotion, modifier: Modifier = Modifier) {
    val isCaption = motion.isSettled && motion.expansion == 1f
    val scale = if (isCaption) 1f else lerp(1f, ACCOUNT_ROW_TITLE_EXPANDED_SCALE, motion.expansion)
    Text(
        text = title.resolveReference(),
        style = if (isCaption) TangemTheme.typography3.caption.medium else TangemTheme.typography3.body.medium,
        color = TangemTheme.colors3.text.primary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
            transformOrigin = TransformOrigin(pivotFractionX = 0f, pivotFractionY = 0.5f)
        },
    )
}

/**
 * Invisible stand-in for the title at its settled caption width. The dot and the compact balance are
 * placed from this rather than from the live, scaled title, so the moment the title swaps to real
 * caption text at the end of the transition does not nudge them sideways.
 */
@Composable
internal fun AccountRowTitleAnchor(title: TextReference, modifier: Modifier = Modifier) {
    Text(
        text = title.resolveReference(),
        style = TangemTheme.typography3.caption.medium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

/** Collapsed subtitle: `2 tokens · 2 members`. The separating dot is dropped with the members count. */
@Composable
internal fun AccountRowSubtitle(
    tokensAmount: TextReference?,
    membersAmount: TextReference?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (tokensAmount != null) {
            AccountRowCaptionText(text = tokensAmount)
        }
        if (tokensAmount != null && membersAmount != null) {
            AccountRowDot()
        }
        if (membersAmount != null) {
            AccountRowCaptionText(text = membersAmount)
        }
    }
}

/** The 12dp separator dot, shared by the collapsed subtitle and the expanded heading. */
@Composable
internal fun AccountRowDot(modifier: Modifier = Modifier) {
    Icon(
        imageVector = Icons.ic_dot_12_filled,
        contentDescription = null,
        tint = TangemTheme.colors3.icon.tertiary,
        modifier = modifier.size(12.dp),
    )
}

@Composable
private fun AccountRowCaptionText(text: TextReference, modifier: Modifier = Modifier) {
    Text(
        text = text.resolveReference(),
        style = TangemTheme.typography3.caption.medium,
        color = TangemTheme.colors3.text.secondary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

/** Collapsed fiat balance, with the optional update-error cloud in front of it. */
@Composable
internal fun AccountRowLargeBalance(
    balance: TextReference,
    modifier: Modifier = Modifier,
    showUpdateWarning: Boolean = false,
    isBalanceHidden: Boolean = false,
    isFlickering: Boolean = false,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showUpdateWarning) {
            Icon(
                imageVector = Icons.ic_cloud_exclamation_20,
                contentDescription = null,
                tint = TangemTheme.colors3.icon.primary,
                modifier = Modifier.size(20.dp),
            )
        }
        Text(
            text = balance.orMaskWithStars(isBalanceHidden).resolveAnnotatedReference(),
            style = TangemTheme.typography3.body.medium.applyBladeBrush(
                isEnabled = isFlickering,
                textColor = TangemTheme.colors3.text.primary,
            ),
            color = TangemTheme.colors3.text.primary,
            maxLines = 1,
        )
    }
}

/** Expanded heading balance — the same number one step down the type scale, in secondary. */
@Composable
internal fun AccountRowSmallBalance(
    balance: TextReference,
    modifier: Modifier = Modifier,
    isBalanceHidden: Boolean = false,
    isFlickering: Boolean = false,
) {
    Text(
        text = balance.orMaskWithStars(isBalanceHidden).resolveReference(),
        style = TangemTheme.typography3.caption.medium.applyBladeBrush(
            isEnabled = isFlickering,
            textColor = TangemTheme.colors3.text.secondary,
        ),
        color = TangemTheme.colors3.text.secondary,
        maxLines = 1,
        modifier = modifier,
    )
}

/** The `—` shown instead of a balance when it could not be calculated. */
@Composable
internal fun AccountRowBalanceError(modifier: Modifier = Modifier) {
    Text(
        text = StringsSigns.DASH_SIGN,
        style = TangemTheme.typography3.body.medium,
        color = TangemTheme.colors3.text.primary,
        maxLines = 1,
        modifier = modifier,
    )
}

/** Collapse affordance at the end of the expanded heading. */
@Composable
internal fun AccountRowCollapseArrow(contentDescription: String?, modifier: Modifier = Modifier) {
    Icon(
        imageVector = Icons.ic_arrow_collapse_20,
        contentDescription = contentDescription,
        tint = TangemTheme.colors3.icon.secondary,
        modifier = modifier.size(20.dp),
    )
}

/** Body of the expanded empty account — the prompt to add the first tokens. */
@Composable
internal fun AccountRowEmptyBody(
    message: TextReference,
    addTokensLabel: TextReference,
    onAddTokensClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(TangemTheme.colors3.bg.opaque.primary),
            ) {
                Icon(
                    imageVector = Icons.ic_grid_plus_20,
                    contentDescription = null,
                    tint = TangemTheme.colors3.icon.secondary,
                    modifier = Modifier.size(20.dp),
                )
            }
            Text(
                text = message.resolveReference(),
                style = TangemTheme.typography3.caption.medium,
                color = TangemTheme.colors3.text.secondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        TangemButton(
            variant = TangemButton.Variant.Secondary,
            size = TangemButton.Size.X9,
            text = addTokensLabel,
            onClick = onAddTokensClick,
        )
    }
}

/** Fixed-width shimmer bar sized after a typography line, as the token row's loading state does it. */
@Composable
internal fun AccountRowShimmerLine(lineHeight: Dp, width: Dp, modifier: Modifier = Modifier) {
    TangemShimmer(
        radius = 16.dp,
        modifier = modifier
            .width(width)
            .height(lineHeight)
            .padding(vertical = 2.dp),
    )
}

// endregion