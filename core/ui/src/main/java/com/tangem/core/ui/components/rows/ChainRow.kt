package com.tangem.core.ui.components.rows

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import com.tangem.core.ui.R
import com.tangem.core.ui.components.TangemSwitch
import com.tangem.core.ui.components.currency.icon.CurrencyIcon
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.components.rows.model.ChainRowUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview

/**
 * [Figma Component](https://www.figma.com/design/14ISV23YB1yVW1uNVwqrKv/Android?node-id=1608-1147&t=ewlXfWwbDnRhjw4B-4)
 * */
@Composable
fun ChainRow(model: ChainRowUM, modifier: Modifier = Modifier, action: @Composable BoxScope.() -> Unit = {}) {
    ChainRowContainer(
        modifier = modifier,
        icon = {
            CurrencyIcon(
                state = model.icon,
                shouldDisplayNetwork = true,
            )
        },
        text = {
            RowText(
                mainText = model.name,
                secondText = model.type,
                subtitle = if (model.showCustom) {
                    resourceReference(R.string.common_custom)
                } else {
                    null
                },
                isEnabled = model.enabled,
                accentMainText = true,
                accentSecondText = false,
            )
        },
        action = action,
    )
}

@Composable
inline fun ChainRowContainer(
    modifier: Modifier = Modifier,
    icon: @Composable BoxScope.() -> Unit,
    text: @Composable BoxScope.() -> Unit,
    action: @Composable BoxScope.() -> Unit,
) {
    RowContentContainer(
        modifier = modifier
            .heightIn(min = TangemTheme.dimens.size68)
            .padding(vertical = TangemTheme.dimens.spacing8)
            .padding(
                start = TangemTheme.dimens.spacing8,
                end = TangemTheme.dimens.spacing12,
            ),
        horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing12),
        icon = icon,
        text = text,
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
                TangemSwitch(onCheckedChange = {}, checked = false)
            },
        )
    }
}

private class ChainRowParameterProvider : CollectionPreviewParameterProvider<ChainRowUM>(
    collection = listOf(
        ChainRowUM(
            name = "Cardano",
            type = "ADA",
            icon = CurrencyIconState.Locked,
            showCustom = true,
        ),
        ChainRowUM(
            name = "Binance",
            type = "BNB",
            icon = CurrencyIconState.Locked,
            showCustom = false,
        ),
        ChainRowUM(
            name = "123456789010111213141516",
            type = "BNB",
            icon = CurrencyIconState.Locked,
            showCustom = true,
        ),
        ChainRowUM(
            name = "123456789010111213141516",
            type = "123456789010111213141516",
            icon = CurrencyIconState.Locked,
            showCustom = false,
        ),
    ),
)
// endregion Preview