@file:Suppress("MagicNumber")

package com.tangem.core.ui.ds2.expandable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds2.avatar.AvatarTokens
import com.tangem.core.ui.ds2.avatar.TangemAvatar
import com.tangem.core.ui.ds2.avatar.TangemAvatarContent
import com.tangem.core.ui.ds2.avatar.lerpAvatarTokens
import com.tangem.core.ui.ds2.avatar.tokens
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.shimmers.TangemShimmer
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_arrow_collapse_20
import com.tangem.core.ui.res.generated.icons.ic_dot_12_filled
import com.tangem.core.ui.res.generated.icons.ic_grid_plus_20

/**
 * The row's avatar. Its dimensions are interpolated between the [TangemAvatar.Size.X40] and
 * [TangemAvatar.Size.X16] presets rather than scaled, so the rounded-square corner lands on the radius the
 * design system defines for each size instead of a scaled-down version of the larger one.
 */
@Composable
internal fun ExpandableAvatar(state: TangemAvatar.UiState, motion: ExpansionMotion, modifier: Modifier = Modifier) {
    TangemAvatarContent(state = state, tokens = motion.avatarTokens(), modifier = modifier)
}

private fun ExpansionMotion.avatarTokens(): AvatarTokens = lerpAvatarTokens(
    start = TangemAvatar.Size.X40.tokens(),
    stop = TangemAvatar.Size.X16.tokens(),
    fraction = expansion,
)

/**
 * Title of the row. Composed at `body.medium` and scaled down while the row moves, then swapped to a
 * real `caption.medium` once [ExpansionMotion.isSettled] — scaled glyphs stay crisp only at rest.
 */
@Composable
internal fun ExpandableTitle(title: TextReference, motion: ExpansionMotion, modifier: Modifier = Modifier) {
    val isCaption = motion.isTitleCaption
    val scale = motion.titleScale
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
 * Invisible stand-in for the title at its settled caption width. The inline run that follows the title in
 * the expanded heading is placed from this rather than from the live, scaled title, so the moment the title
 * swaps to real caption text at the end of the transition does not nudge it sideways.
 */
@Composable
internal fun ExpandableTitleAnchor(title: TextReference, modifier: Modifier = Modifier) {
    Text(
        text = title.resolveReference(),
        style = TangemTheme.typography3.caption.medium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

/** The 12dp separator dot, shared by the collapsed subtitle and the expanded heading. */
@Composable
internal fun ExpandableDot(modifier: Modifier = Modifier) {
    Icon(
        imageVector = Icons.ic_dot_12_filled,
        contentDescription = null,
        tint = TangemTheme.colors3.icon.tertiary,
        modifier = modifier.size(12.dp),
    )
}

/** Secondary caption line — the collapsed subtitle parts and the compact copies in the expanded heading. */
@Composable
internal fun ExpandableCaptionText(text: TextReference, modifier: Modifier = Modifier) {
    Text(
        text = text.resolveReference(),
        style = TangemTheme.typography3.caption.medium,
        color = TangemTheme.colors3.text.secondary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

/** Collapse affordance at the end of the expanded heading. */
@Composable
internal fun ExpandableCollapseArrow(contentDescription: String?, modifier: Modifier = Modifier) {
    Icon(
        imageVector = Icons.ic_arrow_collapse_20,
        contentDescription = contentDescription,
        tint = TangemTheme.colors3.icon.secondary,
        modifier = modifier.size(20.dp),
    )
}

/** Body of the expanded empty row — the prompt to add the first tokens. */
@Composable
internal fun ExpandableEmptyBody(
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
            TangemAvatar(
                state = TangemAvatar.UiState.Icon(imageVector = Icons.ic_grid_plus_20),
                size = TangemAvatar.Size.X40,
            )
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
internal fun ExpandableShimmerLine(lineHeight: Dp, width: Dp, modifier: Modifier = Modifier) {
    TangemShimmer(
        radius = 16.dp,
        modifier = modifier
            .width(width)
            .height(lineHeight)
            .padding(vertical = 2.dp),
    )
}