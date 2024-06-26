package com.tangem.core.ui.components.rows

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import com.tangem.core.ui.R
import com.tangem.core.ui.components.rows.model.ChainRowUM
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview

/**
 * [Figma Component](https://www.figma.com/design/14ISV23YB1yVW1uNVwqrKv/Android?node-id=1608-1147&t=ewlXfWwbDnRhjw4B-4)
 * */
@Composable
fun ChainRow(model: ChainRowUM, modifier: Modifier = Modifier, action: @Composable BoxScope.() -> Unit = {}) {
    RowContentContainer(
        modifier = modifier
            .heightIn(min = TangemTheme.dimens.size68)
            .padding(
                vertical = TangemTheme.dimens.spacing12,
                horizontal = TangemTheme.dimens.spacing14,
            ),
        icon = {
            RowIcon(
                modifier = Modifier.size(TangemTheme.dimens.size36),
                resId = model.icon.resId,
                isColored = model.icon.isColored,
                showAccentBadge = false,
            )
        },
        text = {
            RowTitleAndSubtitle(
                title = model.name,
                subtitle = model.type,
                accentTitle = true,
                accentSubtitle = false,
            )
        },
        action = action,
    )
}

// region Preview
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_ChainRow(@PreviewParameter(ChainRowParameterProvider::class) state: ChainRowUM) {
    TangemThemePreview {
        ChainRow(
            modifier = Modifier.background(TangemTheme.colors.background.primary),
            model = state,
            action = {
                IconButton(
                    modifier = Modifier.size(TangemTheme.dimens.size32),
                    onClick = { /* [REDACTED_TODO_COMMENT]*/ },
                ) {
                    Icon(
                        modifier = Modifier.size(TangemTheme.dimens.size24),
                        tint = TangemTheme.colors.icon.informative,
                        painter = painterResource(id = R.drawable.ic_chevron_24),
                        contentDescription = null,
                    )
                }
            },
        )
    }
}

private class ChainRowParameterProvider : CollectionPreviewParameterProvider<ChainRowUM>(
    collection = listOf(
        ChainRowUM(
            name = stringReference("Cardano"),
            type = stringReference("ADA"),
            icon = ChainRowUM.Icon(
                resId = R.drawable.img_cardano_22,
                isColored = true,
            ),
        ),
        ChainRowUM(
            name = stringReference("Binance"),
            type = stringReference("BNB"),
            icon = ChainRowUM.Icon(
                resId = R.drawable.ic_bsc_16,
                isColored = false,
            ),
        ),
        ChainRowUM(
            name = stringReference("123456789010111213141516"),
            type = stringReference("BNB"),
            icon = ChainRowUM.Icon(
                resId = R.drawable.ic_bsc_16,
                isColored = false,
            ),
        ),
        ChainRowUM(
            name = stringReference("123456789010111213141516"),
            type = stringReference("123456789010111213141516"),
            icon = ChainRowUM.Icon(
                resId = R.drawable.ic_bsc_16,
                isColored = false,
            ),
        ),
    ),
)
// endregion Preview