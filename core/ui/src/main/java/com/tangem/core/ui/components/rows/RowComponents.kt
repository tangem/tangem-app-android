package com.tangem.core.ui.components.rows

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme

@Composable
internal inline fun RowContentContainer(
    icon: @Composable BoxScope.() -> Unit,
    text: @Composable BoxScope.() -> Unit,
    action: @Composable BoxScope.() -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing8, Alignment.Start),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            content = icon,
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = TangemTheme.dimens.size22),
            contentAlignment = Alignment.CenterStart,
            content = text,
        )
        Box(
            modifier = Modifier
                .requiredWidthIn(max = TangemTheme.dimens.size48)
                .heightIn(min = TangemTheme.dimens.size24),
            contentAlignment = Alignment.CenterEnd,
            content = action,
        )
    }
}

@Composable
internal fun RowTitleAndSubtitle(
    title: TextReference,
    subtitle: TextReference,
    accentTitle: Boolean,
    accentSubtitle: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing4),
    ) {
        Text(
            modifier = Modifier.weight(weight = 10f, fill = false),
            text = title.resolveReference(),
            style = TangemTheme.typography.subtitle2,
            color = if (accentTitle) TangemTheme.colors.text.primary1 else TangemTheme.colors.text.secondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Text(
            modifier = Modifier.weight(weight = 4f, fill = false),
            text = subtitle.resolveReference(),
            style = TangemTheme.typography.body2,
            color = if (accentSubtitle) TangemTheme.colors.text.accent else TangemTheme.colors.text.secondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun RowIcon(
    @DrawableRes resId: Int,
    isColored: Boolean,
    showAccentBadge: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        if (isColored) {
            Image(
                modifier = Modifier.matchParentSize(),
                painter = painterResource(id = resId),
                contentDescription = null,
            )
        } else {
            Icon(
                modifier = Modifier
                    .background(
                        color = TangemTheme.colors.button.secondary,
                        shape = CircleShape,
                    )
                    .matchParentSize(),
                painter = painterResource(id = resId),
                tint = TangemTheme.colors.icon.informative,
                contentDescription = null,
            )
        }

        if (showAccentBadge) {
            Badge(modifier = Modifier.align(Alignment.TopEnd))
        }
    }
}

@Composable
private fun Badge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(TangemTheme.dimens.size6)
            .background(
                color = TangemTheme.colors.background.primary,
                shape = CircleShape,
            ),
    ) {
        Box(
            modifier = Modifier
                .padding(all = TangemTheme.dimens.spacing1)
                .matchParentSize()
                .background(
                    color = TangemTheme.colors.icon.accent,
                    shape = CircleShape,
                ),
        )
    }
}