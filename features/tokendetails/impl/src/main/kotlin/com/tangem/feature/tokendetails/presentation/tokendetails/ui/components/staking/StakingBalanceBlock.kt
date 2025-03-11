package com.tangem.feature.tokendetails.presentation.tokendetails.ui.components.staking

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.orMaskWithStars
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.feature.tokendetails.presentation.tokendetails.TokenDetailsPreviewData.stakingBalanceBlock
import com.tangem.feature.tokendetails.presentation.tokendetails.state.StakingBlockUM
import com.tangem.features.tokendetails.impl.R
import com.tangem.utils.StringsSigns

@Composable
internal fun StakingBalanceBlock(
    state: StakingBlockUM.Staked,
    isBalanceHidden: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing4),
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = stringResourceSafe(R.string.staking_native),
                style = TangemTheme.typography.subtitle2,
                color = TangemTheme.colors.text.tertiary,
                modifier = Modifier.padding(vertical = TangemTheme.dimens.spacing2),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing8)) {
                Text(
                    text = state.fiatValue.orMaskWithStars(isBalanceHidden).resolveReference(),
                    style = TangemTheme.typography.body2,
                    color = TangemTheme.colors.text.primary1,
                )
                Text(
                    text = StringsSigns.DOT,
                    style = TangemTheme.typography.body2,
                    color = TangemTheme.colors.text.primary1,
                )
                Text(
                    text = state.cryptoValue.orMaskWithStars(isBalanceHidden).resolveReference(),
                    style = TangemTheme.typography.body2,
                    color = TangemTheme.colors.text.tertiary,
                )
            }
            if (state.rewardValue != TextReference.EMPTY) {
                Text(
                    text = state.rewardValue.orMaskWithStars(isBalanceHidden).resolveReference(),
                    style = TangemTheme.typography.caption2,
                    color = TangemTheme.colors.text.tertiary,
                )
            }
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
    @PreviewParameter(StakingBalanceBlockPreviewProvider::class) data: StakingBlockUM.Staked,
) {
    TangemThemePreview {
        StakingBalanceBlock(
            state = data,
            isBalanceHidden = false,
            modifier = Modifier.padding(TangemTheme.dimens.spacing16),
        )
    }
}

private class StakingBalanceBlockPreviewProvider : PreviewParameterProvider<StakingBlockUM.Staked> {
    override val values: Sequence<StakingBlockUM.Staked>
        get() = sequenceOf(stakingBalanceBlock)
}
// endregion