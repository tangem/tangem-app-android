package com.tangem.features.markets.portfolio.add.impl.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.common.ui.account.AccountIcon
import com.tangem.common.ui.account.AccountIconPreviewData
import com.tangem.common.ui.account.toUM
import com.tangem.core.ui.components.PrimaryButtonIconEnd
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.SpacerH16
import com.tangem.core.ui.components.SpacerW12
import com.tangem.core.ui.components.account.AccountIconSize
import com.tangem.core.ui.components.buttons.common.TangemButtonIconPosition
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.components.token.TokenItem
import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.core.ui.components.token.state.TokenItemState.FiatAmountState
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemColorPalette
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.test.WalletConnectBottomSheetTestTags
import com.tangem.domain.models.account.AccountName
import com.tangem.features.markets.impl.R
import com.tangem.features.markets.portfolio.add.impl.ui.state.AddTokenUM
import java.util.UUID

@Composable
internal fun AddTokenContent(state: AddTokenUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        TokenItem(
            modifier = Modifier
                .clip(RoundedCornerShape(TangemTheme.dimens.radius14))
                .background(color = TangemTheme.colors.background.action),
            state = state.tokenToAdd,
            isBalanceHidden = false,
        )

        SpacerH(TangemTheme.dimens.spacing14)
        Column(
            modifier = Modifier.background(
                color = TangemTheme.colors.background.action,
                shape = RoundedCornerShape(TangemTheme.dimens.radius14),
            ),
        ) {
            PortfolioRow(state.portfolio)
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 12.dp),
                thickness = TangemTheme.dimens.size0_5,
                color = TangemTheme.colors.stroke.primary,
            )
            NetworkRow(state.network)
        }

        SpacerH16()

        AddButton(
            modifier = modifier.fillMaxWidth(),
            state = state.button,
        )
    }
}

@Composable
private fun PortfolioRow(state: AddTokenUM.Portfolio, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clickable(enabled = state.editable, onClick = state.onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val leftText = if (state.isAccountMode) R.string.account_details_title else R.string.wc_common_wallet
        Text(
            modifier = Modifier.weight(1f),
            text = stringResourceSafe(leftText),
            style = TangemTheme.typography.body1,
            color = TangemTheme.colors.text.primary1,
        )
        SpacerW12()
        if (state.accountIconUM != null) {
            AccountIcon(
                name = state.name,
                icon = state.accountIconUM,
                size = AccountIconSize.Small,
            )
        }
        Text(
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp),
            text = state.name.resolveReference(),
            style = TangemTheme.typography.body1,
            color = TangemTheme.colors.text.primary1,
        )
        if (state.editable) {
            Icon(
                modifier = Modifier
                    .size(width = 18.dp, height = 24.dp)
                    .testTag(WalletConnectBottomSheetTestTags.NETWORKS_SELECTOR_ICON),
                painter = painterResource(id = R.drawable.ic_select_18_24),
                contentDescription = null,
                tint = TangemTheme.colors.icon.informative,
            )
        }
    }
}

@Composable
private fun NetworkRow(state: AddTokenUM.Network, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clickable(enabled = state.editable, onClick = state.onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = stringResourceSafe(R.string.wc_common_networks),
            style = TangemTheme.typography.body1,
            color = TangemTheme.colors.text.primary1,
        )
        SpacerW12()
        Icon(
            modifier = Modifier.size(24.dp),
            tint = Color.Unspecified,
            imageVector = ImageVector.vectorResource(id = state.icon),
            contentDescription = null,
        )
        Text(
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp),
            text = state.name.resolveReference(),
            style = TangemTheme.typography.body1,
            color = TangemTheme.colors.text.primary1,
        )
        if (state.editable) {
            Icon(
                modifier = Modifier
                    .size(width = 18.dp, height = 24.dp)
                    .testTag(WalletConnectBottomSheetTestTags.NETWORKS_SELECTOR_ICON),
                painter = painterResource(id = R.drawable.ic_select_18_24),
                contentDescription = null,
                tint = TangemTheme.colors.icon.informative,
            )
        }
    }
}

@Composable
private fun AddButton(state: AddTokenUM.Button, modifier: Modifier = Modifier) {
    val endIcon = if (state.isEnabled && state.isTangemIconVisible) {
        TangemButtonIconPosition.End(R.drawable.ic_tangem_24)
    } else {
        TangemButtonIconPosition.None
    }
    PrimaryButtonIconEnd(
        modifier = modifier,
        text = state.text.resolveReference(),
        iconResId = endIcon.iconResId,
        onClick = state.onConfirmClick,
        enabled = state.isEnabled,
        showProgress = state.showProgress,
    )
}

@Preview(widthDp = 360, showBackground = true)
@Preview(widthDp = 360, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview(@PreviewParameter(PreviewProvider::class) state: AddTokenUM) {
    TangemThemePreview {
        AddTokenContent(
            state = state,
        )
    }
}

private class PreviewProvider : PreviewParameterProvider<AddTokenUM> {
    private val tokenState
        get() = TokenItemState.Content(
            id = UUID.randomUUID().toString(),
            iconState = CurrencyIconState.TokenIcon(
                url = null,
                topBadgeIconResId = R.drawable.img_eth_22,
                fallbackTint = TangemColorPalette.Black,
                fallbackBackground = TangemColorPalette.Meadow,
                isGrayscale = false,
                shouldShowCustomBadge = false,
            ),
            titleState = TokenItemState.TitleState.Content(
                text = stringReference(value = "Tether"),
            ),
            fiatAmountState = FiatAmountState.Content(text = ""),
            subtitle2State = TokenItemState.Subtitle2State.TextContent(text = ""),
            subtitleState = TokenItemState.SubtitleState.TextContent(value = stringReference("USDT")),
            onItemClick = {},
            onItemLongClick = {},
        )

    val networkUM
        get() = AddTokenUM.Network(
            icon = R.drawable.img_eth_22,
            name = stringReference("Ethereum"),
            editable = true,
            onClick = {},
        )

    val button
        get() = AddTokenUM.Button(
            isEnabled = false,
            showProgress = false,
            isTangemIconVisible = false,
            text = resourceReference(R.string.common_add),
            onConfirmClick = { },
        )

    val account
        get() = AddTokenUM.Portfolio(
            accountIconUM = AccountIconPreviewData.randomAccountIcon(),
            name = AccountName.DefaultMain.toUM().value,
            editable = true,
            onClick = {},
        )
    val wallet
        get() = AddTokenUM.Portfolio(
            accountIconUM = null,
            name = stringReference("Wallet"),
            editable = true,
            onClick = {},
        )

    override val values: Sequence<AddTokenUM>
        get() = sequenceOf(
            AddTokenUM(
                tokenToAdd = tokenState,
                network = networkUM,
                portfolio = account,
                button = button,
            ),
            AddTokenUM(
                tokenToAdd = tokenState,
                network = networkUM,
                portfolio = wallet,
                button = button,
            ),
            AddTokenUM(
                tokenToAdd = tokenState,
                network = networkUM.copy(editable = false),
                portfolio = wallet.copy(editable = false),
                button = button.copy(
                    isEnabled = true,
                    isTangemIconVisible = true,
                ),
            ),
        )
}