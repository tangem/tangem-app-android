package com.tangem.feature.wallet.presentation.wallet.ui.components.visa

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import com.tangem.core.ui.components.RectangleShimmer
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.feature.wallet.impl.R
import com.tangem.feature.wallet.presentation.wallet.state.model.BalancesAndLimitsBlockState

private const val BALANCES_AND_LIMITS_BLOCK_KEY = "BalancesAndLimitsBlock"

internal fun LazyListScope.balancesAndLimitsBlock(state: BalancesAndLimitsBlockState, modifier: Modifier = Modifier) {
    item(key = BALANCES_AND_LIMITS_BLOCK_KEY, contentType = BALANCES_AND_LIMITS_BLOCK_KEY) {
        BalancesAndLimitsBlock(state, modifier)
    }
}

@Composable
private fun BalancesAndLimitsBlock(state: BalancesAndLimitsBlockState, modifier: Modifier = Modifier) {
    val onClick: () -> Unit = remember(state) {
        { (state as? BalancesAndLimitsBlockState.Content)?.onClick?.invoke() }
    }
    val isEnabled: Boolean = remember(state) {
        state is BalancesAndLimitsBlockState.Content && state.isEnabled
    }

    ContentContainer(
        modifier = modifier.fillMaxWidth(),
        enabled = isEnabled,
        onClick = onClick,
        title = {
            Text(
                text = "Balances & Limits",
                style = TangemTheme.typography.subtitle2,
                color = TangemTheme.colors.text.tertiary,
            )
        },
        content = {
            Content(state = state)
        },
        endIcon = {
            Icon(
                modifier = Modifier.size(TangemTheme.dimens.size24),
                painter = painterResource(id = R.drawable.ic_chevron_right_24),
                tint = TangemTheme.colors.icon.informative,
                contentDescription = null,
            )
        },
    )
}

@Composable
private inline fun ContentContainer(
    enabled: Boolean,
    noinline onClick: () -> Unit,
    crossinline title: @Composable () -> Unit,
    crossinline content: @Composable () -> Unit,
    crossinline endIcon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(TangemTheme.dimens.radius16),
        colors = CardDefaults.cardColors(
            containerColor = TangemTheme.colors.background.primary,
            contentColor = TangemTheme.colors.text.primary1,
            disabledContainerColor = TangemTheme.colors.background.primary,
            disabledContentColor = TangemTheme.colors.text.primary1,
        ),
        onClick = onClick,
        enabled = enabled,
    ) {
        Row(
            modifier = Modifier
                .padding(all = TangemTheme.dimens.spacing12)
                .fillMaxWidth()
                .heightIn(min = TangemTheme.dimens.size48),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing6),
                horizontalAlignment = Alignment.Start,
            ) {
                title()
                content()
            }
            endIcon()
        }
    }
}

@Composable
private fun Content(state: BalancesAndLimitsBlockState, modifier: Modifier = Modifier) {
    AnimatedContent(
        modifier = modifier,
        targetState = state,
        label = "Update the balances and limits block",
    ) { blockState ->
        when (blockState) {
            is BalancesAndLimitsBlockState.Content -> with(blockState) {
                AvailableLimit(
                    availableBalance = availableBalance,
                    limitDays = limitDays,
                )
            }
            is BalancesAndLimitsBlockState.Error -> {
                Text(
                    text = "–",
                    style = TangemTheme.typography.subtitle2,
                    color = TangemTheme.colors.text.primary1,
                )
            }
            is BalancesAndLimitsBlockState.Loading -> {
                RectangleShimmer(
                    modifier = Modifier
                        .width(TangemTheme.dimens.size200)
                        .height(TangemTheme.dimens.size20),
                )
            }
        }
    }
}

@Composable
private fun AvailableLimit(availableBalance: String, limitDays: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing8),
    ) {
        Text(
            text = availableBalance,
            style = TangemTheme.typography.body2,
            color = TangemTheme.colors.text.primary1,
        )
        Text(
            text = "available for $limitDays day(s)",
            style = TangemTheme.typography.body2,
            color = TangemTheme.colors.text.tertiary,
        )
    }
}

// region Preview
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun BalancesAndLimitsBlockPreview(
    @PreviewParameter(BalancesAndLimitsBlockParameterProvider::class) state: BalancesAndLimitsBlockState,
) {
    TangemThemePreview {
        BalancesAndLimitsBlock(state)
    }
}

private class BalancesAndLimitsBlockParameterProvider : CollectionPreviewParameterProvider<BalancesAndLimitsBlockState>(
    collection = listOf(
        BalancesAndLimitsBlockState.Loading,
        BalancesAndLimitsBlockState.Error,
        BalancesAndLimitsBlockState.Content(
            availableBalance = "400.00 USDT",
            limitDays = 7,
            isEnabled = true,
            onClick = {},
        ),
    ),
)
// endregion Preview