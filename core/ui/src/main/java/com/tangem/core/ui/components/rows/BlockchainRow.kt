package com.tangem.core.ui.components.rows

import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import com.tangem.core.ui.R
import com.tangem.core.ui.components.TangemSwitch
import com.tangem.core.ui.components.rows.model.BlockchainRowUM
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview

private const val DISABLED_ICON_ALPHA = 0.4f

/**
 * [Figma Component](https://www.figma.com/design/14ISV23YB1yVW1uNVwqrKv/Android?node-id=2737-2800&t=ewlXfWwbDnRhjw4B-4)
 * */
@Composable
fun BlockchainRow(model: BlockchainRowUM, modifier: Modifier = Modifier, action: @Composable BoxScope.() -> Unit) {
    RowContentContainer(
        modifier = modifier
            .heightIn(min = TangemTheme.dimens.size52)
            .padding(
                top = TangemTheme.dimens.spacing8,
                bottom = TangemTheme.dimens.spacing8,
                start = TangemTheme.dimens.spacing8,
            ),
        icon = {
            RowIcon(
                resId = model.iconResId,
                isColored = model.isSelected && model.isEnabled,
                showAccentBadge = model.isMainNetwork,
                isEnabled = model.isEnabled,
            )
        },
        text = {
            RowText(
                mainText = model.name,
                secondText = model.type,
                accentMainText = model.isSelected,
                accentSecondText = model.isMainNetwork,
                isEnabled = model.isEnabled,
            )
        },
        action = action,
    )
}

@Composable
private fun RowIcon(
    @DrawableRes resId: Int,
    isColored: Boolean,
    showAccentBadge: Boolean,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
) {
    Box(modifier = modifier.size(TangemTheme.dimens.size24)) {
        if (isColored) {
            Image(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(TangemTheme.dimens.size22),
                painter = painterResource(id = resId),
                contentDescription = null,
            )
        } else {
            Icon(
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(
                        color = TangemTheme.colors.button.secondary,
                        shape = CircleShape,
                    )
                    .alpha(if (isEnabled) 1f else DISABLED_ICON_ALPHA)
                    .size(TangemTheme.dimens.size22),
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
            .size(TangemTheme.dimens.size8)
            .background(
                color = TangemTheme.colors.background.primary,
                shape = CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(TangemTheme.dimens.size5)
                .background(
                    color = TangemTheme.colors.icon.accent,
                    shape = CircleShape,
                ),
        )
    }
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
                        TangemSwitch(onCheckedChange = { }, checked = true, enabled = state.isEnabled)
                    },
                )
            },
        )
    }
}

private class BlockchainRowParameterProvider : CollectionPreviewParameterProvider<BlockchainRowUM>(
    collection = listOf(
        BlockchainRowUM(
            id = "0",
            name = "BNB BEACON CHAIN",
            type = "BEP20",
            iconResId = R.drawable.img_bsc_22,
            isMainNetwork = true,
            isSelected = true,
        ),
        BlockchainRowUM(
            id = "1",
            name = "1234567890111213141516171819",
            type = "BEP20",
            iconResId = R.drawable.ic_bsc_16,
            isMainNetwork = true,
            isSelected = false,
        ),
        BlockchainRowUM(
            id = "2",
            name = "BNB BEACON CHAIN",
            type = "1234567890111213141516171819",
            iconResId = R.drawable.ic_bsc_16,
            isMainNetwork = false,
            isSelected = false,
        ),
        BlockchainRowUM(
            id = "2",
            name = "BNB BEACON CHAIN",
            type = "1234567890111213141516171819",
            iconResId = R.drawable.ic_bsc_16,
            isMainNetwork = false,
            isSelected = false,
            isEnabled = false,
        ),
    ),
)
// endregion Preview