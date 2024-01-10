package com.tangem.feature.wallet.presentation.wallet.ui.components.visa

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
import com.tangem.core.ui.components.SpacerWMax
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheet
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.wallet.impl.R
import com.tangem.feature.wallet.presentation.wallet.state2.model.BalancesAndLimitsBottomSheetConfig

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
            BlockContent(
                title = stringReference("Balance, ${config.currency}"),
                content = {
                    BlockItem(
                        title = stringReference("Total"),
                        value = config.balance.totalBalance,
                    )
                    BlockItem(
                        title = stringReference("AML Verified"),
                        value = config.balance.amlVerified,
                    )
                    BlockItem(
                        title = stringReference("Available"),
                        value = config.balance.availableBalance,
                    )
                    BlockItem(
                        title = stringReference("Blocked"),
                        value = config.balance.blockedBalance,
                    )
                    BlockItem(
                        title = stringReference("Debit"),
                        value = config.balance.debit,
                    )
                    BlockItem(
                        title = stringReference("Pending refund"),
                        value = config.balance.pending,
                    )
                },
                onInfoIconClick = config.onBalanceInfoClick,
            )
        },
        secondBlock = {
            BlockContent(
                title = stringReference("Limits, ${config.currency}"),
                description = stringReference("Available by ${config.limit.availableBy}"),
                content = {
                    BlockItem(
                        title = stringReference("In-store (otp)"),
                        value = config.limit.inStore,
                    )
                    BlockItem(
                        title = stringReference("Other (no-otp)"),
                        value = config.limit.other,
                    )
                    BlockItem(
                        title = stringReference("Single transaction"),
                        value = config.limit.singleTransaction,
                    )
                },
                onInfoIconClick = config.onBalanceInfoClick,
            )
        },
    )
}

@Composable
private inline fun BlockContent(
    title: TextReference,
    content: @Composable ColumnScope.() -> Unit,
    noinline onInfoIconClick: () -> Unit,
    modifier: Modifier = Modifier,
    description: TextReference? = null,
) {
    Column(
        modifier = modifier
            .padding(horizontal = TangemTheme.dimens.spacing16)
            .fillMaxWidth()
            .background(
                color = TangemTheme.colors.background.primary,
                shape = TangemTheme.shapes.roundedCornersXMedium,
            )
            .padding(vertical = TangemTheme.dimens.spacing8),
    ) {
        Row(
            modifier = Modifier
                .padding(
                    start = TangemTheme.dimens.spacing12,
                    end = TangemTheme.dimens.spacing4,
                )
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title.resolveReference(),
                style = TangemTheme.typography.subtitle2,
                color = TangemTheme.colors.text.tertiary,
            )
            SpacerWMax()
            if (description != null) {
                Text(
                    text = description.resolveReference(),
                    style = TangemTheme.typography.body2,
                    color = TangemTheme.colors.text.tertiary,
                )
            }
            IconButton(
                modifier = Modifier.size(TangemTheme.dimens.size32),
                onClick = onInfoIconClick,
            ) {
                Icon(
                    modifier = Modifier.size(TangemTheme.dimens.size16),
                    painter = painterResource(id = R.drawable.ic_information_24),
                    tint = TangemTheme.colors.icon.informative,
                    contentDescription = null,
                )
            }
        }
        content()
    }
}

@Composable
private fun BlockItem(title: TextReference, value: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .padding(horizontal = TangemTheme.dimens.spacing12)
            .fillMaxWidth()
            .heightIn(min = TangemTheme.dimens.size32),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title.resolveReference(),
            style = TangemTheme.typography.caption2,
            color = TangemTheme.colors.text.tertiary,
        )
        Text(
            text = value,
            style = TangemTheme.typography.caption2,
            color = TangemTheme.colors.text.primary1,
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
@Composable
private fun BalancesAndLimitsBottomSheetPreview_Light(
    @PreviewParameter(BalancesAndLimitsBottomSheetParameterProvider::class) state: BalancesAndLimitsBottomSheetConfig,
) {
    TangemTheme {
        BalancesAndLimitsContent(state)
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun BalancesAndLimitsBottomSheetPreview_Dark(
    @PreviewParameter(BalancesAndLimitsBottomSheetParameterProvider::class) state: BalancesAndLimitsBottomSheetConfig,
) {
    TangemTheme(isDark = true) {
        BalancesAndLimitsContent(state)
    }
}

private class BalancesAndLimitsBottomSheetParameterProvider :
    CollectionPreviewParameterProvider<BalancesAndLimitsBottomSheetConfig>(
        collection = listOf(
            BalancesAndLimitsBottomSheetConfig(
                currency = "USDT",
                balance = BalancesAndLimitsBottomSheetConfig.Balance(
                    totalBalance = "492.45",
                    availableBalance = "392.45",
                    blockedBalance = "36.00",
                    debit = "00.00",
                    pending = "20.99",
                    amlVerified = "356.45",
                ),
                limit = BalancesAndLimitsBottomSheetConfig.Limit(
                    availableBy = "Nov, 11",
                    inStore = "563.00",
                    other = "100.00",
                    singleTransaction = "100.00",
                ),
                onBalanceInfoClick = {},
                onLimitInfoClick = {},
            ),
        ),
    )
// endregion Preview