package com.tangem.feature.tokendetails.presentation.tokendetails.ui.components.staking

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.feature.tokendetails.presentation.tokendetails.TokenDetailsPreviewData
import com.tangem.feature.tokendetails.presentation.tokendetails.state.StakingBalance
import com.tangem.features.tokendetails.impl.R
import com.tangem.utils.Strings

@Composable
fun StakingBalanceBlock(state: StakingBalance.Content, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(TangemTheme.shapes.roundedCornersXMedium)
            .background(TangemTheme.colors.background.action)
            .padding(TangemTheme.dimens.spacing12),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing4),
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = stringResource(R.string.staking_native),
                style = TangemTheme.typography.subtitle2,
                color = TangemTheme.colors.text.tertiary,
                modifier = Modifier.padding(vertical = TangemTheme.dimens.spacing2),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing8)) {
                Text(
                    text = state.fiatAmount.resolveReference(),
                    style = TangemTheme.typography.body2,
                    color = TangemTheme.colors.text.primary1,
                )
                Text(
                    text = Strings.DOT,
                    style = TangemTheme.typography.body2,
                    color = TangemTheme.colors.text.primary1,
                )
                Text(
                    text = state.cryptoAmount.resolveReference(),
                    style = TangemTheme.typography.body2,
                    color = TangemTheme.colors.text.tertiary,
                )
            }
            Text(
                text = state.rewardAmount.resolveReference(),
                style = TangemTheme.typography.caption2,
                color = TangemTheme.colors.text.tertiary,
            )
        }
        Icon(
            painter = painterResource(id = R.drawable.ic_chevron_right_24),
            contentDescription = null,
            tint = TangemTheme.colors.icon.informative,
        )
    }
}

// region Preview
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun StakingBalanceBlock_Preview(
    @PreviewParameter(StakingBalanceBlockPreviewProvider::class) data: StakingBalance,
) {
    TangemThemePreview {
        StakingBalanceBlock(
            state = data as StakingBalance.Content,
            modifier = Modifier.padding(TangemTheme.dimens.spacing16),
        )
    }
}

private class StakingBalanceBlockPreviewProvider : PreviewParameterProvider<StakingBalance> {
    override val values: Sequence<StakingBalance>
        get() = sequenceOf(
            TokenDetailsPreviewData.tokenDetailsState_1.stakingBlocksState.stakingBalance,
            TokenDetailsPreviewData.tokenDetailsState_2.stakingBlocksState.stakingBalance,
        )
}
// endregion
