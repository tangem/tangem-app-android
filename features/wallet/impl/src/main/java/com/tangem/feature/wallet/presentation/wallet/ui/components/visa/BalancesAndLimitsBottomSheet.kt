package com.tangem.feature.wallet.presentation.wallet.ui.components.visa

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import com.tangem.core.ui.components.SpacerH16
import com.tangem.core.ui.components.bottomsheets.sheet.TangemBottomSheet
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.feature.wallet.impl.R
import com.tangem.feature.wallet.presentation.wallet.state.model.BalancesAndLimitsBottomSheetConfig

@Composable
internal fun BalancesAndLimitsBottomSheet(config: TangemBottomSheetConfig) {
    TangemBottomSheet(
        config = config,
        containerColor = TangemTheme.colors.background.secondary,
    ) { content: BalancesAndLimitsBottomSheetConfig ->
        BalancesAndLimitsContent(content)
    }
}

@Composable
private fun BalancesAndLimitsContent(config: BalancesAndLimitsBottomSheetConfig, modifier: Modifier = Modifier) {
    ContentContainer(
        modifier = modifier,
        title = {
            Text(
                text = "Balances & Limits",
                style = TangemTheme.typography.subtitle1,
                color = TangemTheme.colors.text.primary1,
            )
        },
        firstBlock = {
            BalancesBlock(balances = config.balance)
        },
        secondBlock = {
            LimitsBlock(limits = config.limit)
        },
    )
}

@Composable
private fun BalancesBlock(balances: BalancesAndLimitsBottomSheetConfig.Balance, modifier: Modifier = Modifier) {
    BlockContent(
        modifier = modifier,
        title = stringReference("Balance"),
        content = {
            BlockItem(
                title = stringReference("Total"),
                value = balances.totalBalance,
            )
            BlockItem(
                title = stringReference("AML Verified"),
                value = balances.amlVerified,
            )
            BlockItem(
                title = stringReference("Available"),
                value = balances.availableBalance,
            )
            BlockItem(
                title = stringReference("Blocked"),
                value = balances.blockedBalance,
            )
            BlockItem(
                title = stringReference("Debit"),
                value = balances.debit,
            )
        },
        description = {
            InfoButton(onClick = balances.onInfoClick)
        },
    )
}

@Composable
private fun LimitsBlock(limits: BalancesAndLimitsBottomSheetConfig.Limit, modifier: Modifier = Modifier) {
    BlockContent(
        modifier = modifier,
        title = stringReference("Limits"),
        content = {
            BlockItem(
                title = stringReference("Total"),
                value = limits.total,
            )
            BlockItem(
                title = stringReference("Other (no-otp)"),
                value = limits.other,
            )
            BlockItem(
                title = stringReference("Single transaction"),
                value = limits.singleTransaction,
            )
        },
        description = {
            Text(
                text = "Available till ${limits.availableBy}",
                style = TangemTheme.typography.body2,
                color = TangemTheme.colors.text.tertiary,
            )
            InfoButton(onClick = limits.onInfoClick)
        },
    )
}

@Composable
private fun InfoButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(
        modifier = modifier.size(TangemTheme.dimens.size32),
        onClick = onClick,
    ) {
        Icon(
            modifier = Modifier.size(TangemTheme.dimens.size16),
            painter = painterResource(id = R.drawable.ic_information_24),
            tint = TangemTheme.colors.icon.informative,
            contentDescription = null,
        )
    }
}

@Composable
private inline fun ContentContainer(
    title: @Composable BoxScope.() -> Unit,
    firstBlock: @Composable ColumnScope.() -> Unit,
    secondBlock: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.background(TangemTheme.colors.background.secondary),
        verticalArrangement = Arrangement.spacedBy(space = TangemTheme.dimens.spacing12),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = TangemTheme.dimens.size44),
            contentAlignment = Alignment.Center,
            content = title,
        )
        firstBlock()
        secondBlock()
        SpacerH16()
    }
}

// region Preview
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun BalancesAndLimitsBottomSheetPreview(
    @PreviewParameter(BalancesAndLimitsBottomSheetParameterProvider::class) state: BalancesAndLimitsBottomSheetConfig,
) {
    TangemThemePreview {
        BalancesAndLimitsContent(state)
    }
}

private class BalancesAndLimitsBottomSheetParameterProvider :
    CollectionPreviewParameterProvider<BalancesAndLimitsBottomSheetConfig>(
        collection = listOf(
            BalancesAndLimitsBottomSheetConfig(
                balance = BalancesAndLimitsBottomSheetConfig.Balance(
                    totalBalance = "492.45 USDT",
                    availableBalance = "392.45 USDT",
                    blockedBalance = "36.00 USDT",
                    debit = "00.00 USDT",
                    amlVerified = "356.45 USDT",
                    onInfoClick = {},
                ),
                limit = BalancesAndLimitsBottomSheetConfig.Limit(
                    availableBy = "Nov, 11 USDT",
                    total = "563.00 USDT",
                    other = "100.00 USDT",
                    singleTransaction = "100.00 USDT",
                    onInfoClick = {},
                ),
            ),
        ),
    )
// endregion Preview