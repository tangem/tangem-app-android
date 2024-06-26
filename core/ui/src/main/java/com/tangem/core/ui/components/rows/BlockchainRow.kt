package com.tangem.core.ui.components.rows

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import com.tangem.core.ui.R
import com.tangem.core.ui.components.TangemSwitch
import com.tangem.core.ui.components.rows.model.BlockchainRowUM
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview

/**
 * [Figma Component](https://www.figma.com/design/14ISV23YB1yVW1uNVwqrKv/Android?node-id=2737-2800&t=ewlXfWwbDnRhjw4B-4)
 * */
@Composable
fun BlockchainRow(model: BlockchainRowUM, action: @Composable BoxScope.() -> Unit, modifier: Modifier = Modifier) {
    RowContentContainer(
        modifier = modifier
            .heightIn(min = TangemTheme.dimens.size52)
            .padding(
                vertical = TangemTheme.dimens.spacing16,
                horizontal = TangemTheme.dimens.spacing8,
            ),
        icon = {
            RowIcon(
                modifier = Modifier.size(TangemTheme.dimens.size22),
                resId = model.icon.resId,
                isColored = model.icon.isColored,
                showAccentBadge = model.isAccented,
            )
        },
        text = {
            RowTitleAndSubtitle(
                title = model.name,
                subtitle = model.type,
                accentTitle = model.usePrimaryTextColor,
                accentSubtitle = model.isAccented,
            )
        },
        action = action,
    )
}

// region Preview
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_BlockchainRow(@PreviewParameter(BlockchainRowParameterProvider::class) state: BlockchainRowUM) {
    TangemThemePreview {
        ArrowRow(
            modifier = Modifier.background(TangemTheme.colors.background.primary),
            isLastItem = false,
            content = {
                BlockchainRow(
                    model = state,
                    action = {
                        TangemSwitch(onCheckedChange = { /* [REDACTED_TODO_COMMENT]*/ }, checked = true)
                    },
                )
            },
        )
    }
}

private class BlockchainRowParameterProvider : CollectionPreviewParameterProvider<BlockchainRowUM>(
    collection = listOf(
        BlockchainRowUM(
            name = stringReference("BNB BEACON CHAIN"),
            type = stringReference("BEP20"),
            icon = BlockchainRowUM.Icon(R.drawable.ic_bsc_16, isColored = false),
            isAccented = true,
            usePrimaryTextColor = false,
        ),
        BlockchainRowUM(
            name = stringReference("1234567890111213141516171819"),
            type = stringReference("BEP20"),
            icon = BlockchainRowUM.Icon(R.drawable.ic_bsc_16, isColored = false),
            isAccented = false,
            usePrimaryTextColor = true,
        ),
        BlockchainRowUM(
            name = stringReference("BNB BEACON CHAIN"),
            type = stringReference("1234567890111213141516171819"),
            icon = BlockchainRowUM.Icon(R.drawable.img_bsc_22, isColored = true),
            isAccented = false,
            usePrimaryTextColor = false,
        ),
    ),
)
// endregion Preview