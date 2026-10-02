@file:Suppress("MagicNumber")

package com.tangem.core.ui.ds2.expandablerow

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds2.badge.TangemBadge
import com.tangem.core.ui.ds2.expandable.ExpandableTitle
import com.tangem.core.ui.ds2.expandable.ExpansionMotion
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme

/**
 * Collapsed title line — the title itself plus the optional badge beside it.
 *
 * The badge is dropped from the composition once it has faded out rather than merely hidden, so the
 * expanded title is measured against the full heading width instead of a width the invisible badge
 * still reserves.
 */
@Composable
internal fun ExpandableRowTitleLine(
    title: TextReference,
    motion: ExpansionMotion,
    modifier: Modifier = Modifier,
    badge: TextReference? = null,
    badgeAlpha: Float = 1f,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ExpandableTitle(title = title, motion = motion)
        if (badge != null && badgeAlpha > 0f) {
            TangemBadge(
                text = badge,
                variant = TangemBadge.Variant.Tinted,
                status = TangemBadge.Status.Neutral,
                size = TangemBadge.Size.X4,
                modifier = Modifier.graphicsLayer { alpha = badgeAlpha },
            )
        }
    }
}

/** Collapsed primary value at the end of the row. */
@Composable
internal fun ExpandableRowValue(value: TextReference, modifier: Modifier = Modifier) {
    Text(
        text = value.resolveReference(),
        style = TangemTheme.typography3.body.medium,
        color = TangemTheme.colors3.text.primary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

/** The same value one step down the type scale, for the compact run in the expanded heading. */
@Composable
internal fun ExpandableRowCompactValue(value: TextReference, modifier: Modifier = Modifier) {
    Text(
        text = value.resolveReference(),
        style = TangemTheme.typography3.caption.medium,
        color = TangemTheme.colors3.text.primary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}