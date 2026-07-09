package com.tangem.features.promobanners.impl.campaigns.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.SpacerH8
import com.tangem.core.ui.components.SpacerW4
import com.tangem.core.ui.components.account.AccountCharIcon
import com.tangem.core.ui.components.account.AccountIconSize
import com.tangem.core.ui.components.account.AccountResIcon
import com.tangem.core.ui.components.account.PaymentAccountIcon
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.components.token.TokenItem
import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.promobanners.impl.campaigns.entity.SelectedAccountUM

@Composable
internal fun PromoCampaignTokenItem(
    selectedToken: TokenItemState,
    selectedAccount: SelectedAccountUM?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        if (selectedAccount != null) {
            Row(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                when (val icon = selectedAccount.iconState) {
                    is CurrencyIconState.PaymentAccount -> PaymentAccountIcon(size = icon.size)
                    is CurrencyIconState.CryptoPortfolio.Icon -> AccountResIcon(
                        resId = icon.resId,
                        color = icon.color,
                        size = AccountIconSize.ExtraSmall,
                    )
                    is CurrencyIconState.CryptoPortfolio.Letter -> AccountCharIcon(
                        char = icon.char.resolveReference().first(),
                        color = icon.color,
                        size = AccountIconSize.ExtraSmall,
                    )
                    is CurrencyIconState.CoinIcon,
                    is CurrencyIconState.CustomTokenIcon,
                    is CurrencyIconState.Empty,
                    is CurrencyIconState.FiatIcon,
                    CurrencyIconState.Loading,
                    CurrencyIconState.Locked,
                    is CurrencyIconState.TokenIcon,
                    -> Unit
                }

                SpacerW4()

                Text(
                    modifier = Modifier
                        .padding(vertical = 2.dp)
                        .alignByBaseline(),
                    text = selectedAccount.name.resolveReference(),
                    color = TangemTheme.colors.text.primary1,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    style = TangemTheme.typography.caption1,
                )
            }

            SpacerH8()
        }

        TokenItem(
            state = selectedToken,
            isBalanceHidden = false,
            itemPaddingValues = PaddingValues(horizontal = 16.dp),
        )
    }
}

// region Preview
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_PromoCampaignTokenItem_WithAccount() {
    TangemThemePreviewRedesign {
        PromoCampaignTokenItem(
            modifier = Modifier
                .background(TangemTheme.colors3.bg.primary)
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(TangemTheme.colors.background.primary),
            selectedToken = CampaignPreviewData.tokenItem,
            selectedAccount = CampaignPreviewData.selectedAccount,
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_PromoCampaignTokenItem_NoAccount() {
    TangemThemePreviewRedesign {
        PromoCampaignTokenItem(
            modifier = Modifier
                .background(TangemTheme.colors3.bg.primary)
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(TangemTheme.colors.background.primary),
            selectedToken = CampaignPreviewData.tokenItem,
            selectedAccount = null,
        )
    }
}
// endregion