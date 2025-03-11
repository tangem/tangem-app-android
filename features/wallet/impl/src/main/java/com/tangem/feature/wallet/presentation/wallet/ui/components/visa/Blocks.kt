package com.tangem.feature.wallet.presentation.wallet.ui.components.visa

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.tangem.core.ui.components.SpacerH8
import com.tangem.core.ui.components.SpacerWMax
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme

private const val BLOCK_ITEM_NAME_WEIGHT = .45f
private const val BLOCK_ITEM_VALUE_WEIGHT = .55f

@Composable
internal inline fun BlockContent(
    title: TextReference,
    content: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
    description: @Composable RowScope.() -> Unit = {},
) {
    Column(
        modifier = modifier
            .padding(horizontal = TangemTheme.dimens.spacing16)
            .fillMaxWidth()
            .background(
                color = TangemTheme.colors.background.primary,
                shape = TangemTheme.shapes.roundedCornersXMedium,
            ),
    ) {
        Row(
            modifier = Modifier
                .padding(start = TangemTheme.dimens.spacing12)
                .fillMaxWidth()
                .heightIn(min = TangemTheme.dimens.size42),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title.resolveReference(),
                style = TangemTheme.typography.subtitle2,
                color = TangemTheme.colors.text.tertiary,
            )
            SpacerWMax()
            description()
        }
        content()
        SpacerH8()
    }
}

@Composable
internal fun BlockItem(title: TextReference, value: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = TangemTheme.dimens.size32)
            .padding(
                vertical = TangemTheme.dimens.spacing8,
                horizontal = TangemTheme.dimens.spacing12,
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            modifier = Modifier.weight(BLOCK_ITEM_NAME_WEIGHT),
            text = title.resolveReference(),
            style = TangemTheme.typography.caption2,
            color = TangemTheme.colors.text.tertiary,
            textAlign = TextAlign.Start,
        )
        Text(
            modifier = Modifier.weight(BLOCK_ITEM_VALUE_WEIGHT),
            text = value,
            style = TangemTheme.typography.caption2,
            color = TangemTheme.colors.text.primary1,
            textAlign = TextAlign.End,
        )
    }
}