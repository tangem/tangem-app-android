package com.tangem.core.ui.components.rows

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.isNullOrEmpty
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme

@Composable
internal inline fun RowContentContainer(
    icon: @Composable BoxScope.() -> Unit,
    text: @Composable BoxScope.() -> Unit,
    action: @Composable BoxScope.() -> Unit,
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(TangemTheme.dimens.spacing8),
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = horizontalArrangement,
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
                .requiredWidthIn(max = TangemTheme.dimens.size80)
                .heightIn(min = TangemTheme.dimens.size24),
            contentAlignment = Alignment.CenterEnd,
            content = action,
        )
    }
}

@Composable
internal fun RowText(
    mainText: String,
    secondText: String,
    accentMainText: Boolean,
    accentSecondText: Boolean,
    modifier: Modifier = Modifier,
    subtitle: TextReference? = null,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing2),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing4),
        ) {
            Text(
                modifier = Modifier.weight(weight = 10f, fill = false),
                text = mainText,
                style = TangemTheme.typography.subtitle2,
                color = if (accentMainText) TangemTheme.colors.text.primary1 else TangemTheme.colors.text.secondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Text(
                modifier = Modifier.weight(weight = 4f, fill = false),
                text = secondText,
                style = TangemTheme.typography.body2,
                color = if (accentSecondText) TangemTheme.colors.text.accent else TangemTheme.colors.text.secondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (!subtitle.isNullOrEmpty()) {
            Text(
                text = subtitle.resolveReference(),
                style = TangemTheme.typography.caption2,
                color = TangemTheme.colors.text.tertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}