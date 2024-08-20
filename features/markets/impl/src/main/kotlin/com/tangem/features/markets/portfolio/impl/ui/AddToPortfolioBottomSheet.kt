package com.tangem.features.markets.portfolio.impl.ui

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.util.fastForEachIndexed
import com.tangem.common.ui.userwallet.UserWalletItem
import com.tangem.core.ui.components.PrimaryButtonIconEnd
import com.tangem.core.ui.components.SpacerW12
import com.tangem.core.ui.components.SpacerW6
import com.tangem.core.ui.components.TangemSwitch
import com.tangem.core.ui.components.block.information.InformationBlock
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheet
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.currency.icon.CoinIcon
import com.tangem.core.ui.components.rows.ArrowRow
import com.tangem.core.ui.components.rows.BlockchainRow
import com.tangem.core.ui.components.rows.model.BlockchainRowUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.markets.impl.R
import com.tangem.features.markets.portfolio.impl.ui.preview.PreviewAddToPortfolioBSContentProvider
import com.tangem.features.markets.portfolio.impl.ui.state.AddToPortfolioBSContentUM
import com.tangem.features.markets.portfolio.impl.ui.state.SelectNetworkUM

@Composable
internal fun AddToPortfolioBottomSheet(config: TangemBottomSheetConfig) {
    TangemBottomSheet<AddToPortfolioBSContentUM>(
        config = config,
        containerColor = TangemTheme.colors.background.tertiary,
        titleText = resourceReference(R.string.markets_add_to_portfolio_button),
    ) {
        Content(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = TangemTheme.dimens.spacing16),
            state = config.content as AddToPortfolioBSContentUM,
        )
    }
}

@Composable
private fun Content(state: AddToPortfolioBSContentUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = TangemTheme.dimens.spacing16),
        verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing12),
    ) {
        UserWalletItem(state.selectedWallet)

        NetworkSelection(
            modifier = Modifier.fillMaxWidth(),
            state = state.selectNetworkUM,
        )

        AnimatedVisibility(
            visible = state.isScanCardNotificationVisible,
            modifier = Modifier.fillMaxWidth(),
        ) {
            ScanWalletWarning(modifier = Modifier.fillMaxWidth())
        }

        PrimaryButtonIconEnd(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.common_continue),
            iconResId = R.drawable.ic_tangem_24,
            onClick = {},
        )
    }
}

@Suppress("LongMethod")
@Composable
private fun NetworkSelection(state: SelectNetworkUM, modifier: Modifier = Modifier) {
    InformationBlock(
        modifier = modifier,
        title = {
            Text(
                text = stringResource(R.string.markets_select_network),
                style = TangemTheme.typography.subtitle2,
                color = TangemTheme.colors.text.tertiary,
            )
        },
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState()),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = TangemTheme.dimens.spacing14),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CoinIcon(
                    modifier = Modifier.size(TangemTheme.dimens.size36),
                    url = state.iconUrl,
                    alpha = 1f,
                    colorFilter = null,
                    fallbackResId = R.drawable.ic_custom_token_44,
                )
                SpacerW12()
                Text(
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .alignByBaseline(),
                    text = state.tokenName,
                    style = TangemTheme.typography.body1,
                    color = TangemTheme.colors.text.primary1,
                )
                SpacerW6()
                Text(
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .alignByBaseline(),
                    text = state.tokenCurrencySymbol,
                    style = TangemTheme.typography.body1,
                    color = TangemTheme.colors.text.tertiary,
                )
            }

            state.networks.fastForEachIndexed { index, network ->
                ArrowRow(
                    isLastItem = index == state.networks.lastIndex,
                    content = {
                        BlockchainRow(
                            modifier = Modifier.padding(
                                end = TangemTheme.dimens.spacing4,
                            ),
                            model = with(network) {
                                BlockchainRowUM(
                                    name = name,
                                    type = type,
                                    iconResId = iconResId,
                                    isMainNetwork = isMainNetwork,
                                    isSelected = isSelected,
                                )
                            },
                            action = {
                                TangemSwitch(
                                    checked = network.isSelected,
                                    onCheckedChange = {
                                        state.onNetworkSwitchClick(network, it)
                                    },
                                )
                            },
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun ScanWalletWarning(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(
                color = TangemTheme.colors.button.disabled,
                shape = TangemTheme.shapes.roundedCornersXMedium,
            )
            .padding(TangemTheme.dimens.spacing12),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing10),
    ) {
        Icon(
            modifier = Modifier.requiredSize(TangemTheme.dimens.size20),
            imageVector = ImageVector.vectorResource(R.drawable.ic_tangem_24),
            contentDescription = null,
        )
        Text(
            text = stringResource(R.string.markets_generate_addresses_notification),
            style = TangemTheme.typography.caption2,
            color = TangemTheme.colors.text.tertiary,
        )
    }
}

@Composable
@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun Preview(
    @PreviewParameter(PreviewAddToPortfolioBSContentProvider::class) content: AddToPortfolioBSContentUM,
) {
    TangemThemePreview {
        AddToPortfolioBottomSheet(
            config = TangemBottomSheetConfig(
                isShow = true,
                content = content,
                onDismissRequest = {},
            ),
        )
    }
}

@Composable
@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun PreviewContent(
    @PreviewParameter(PreviewAddToPortfolioBSContentProvider::class) content: AddToPortfolioBSContentUM,
) {
    TangemThemePreview {
        Content(
            modifier = Modifier
                .background(TangemTheme.colors.background.tertiary)
                .fillMaxWidth(),
            state = content,
        )
    }
}

@Composable
@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun PreviewContentRtl(
    @PreviewParameter(PreviewAddToPortfolioBSContentProvider::class) content: AddToPortfolioBSContentUM,
) {
    TangemThemePreview(rtl = true) {
        Content(
            modifier = Modifier
                .background(TangemTheme.colors.background.tertiary)
                .fillMaxWidth(),
            state = content,
        )
    }
}