package com.tangem.features.markets.portfolio.impl.ui

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.util.fastForEachIndexed
import com.tangem.common.ui.userwallet.UserWalletItem
import com.tangem.core.ui.components.*
import com.tangem.core.ui.components.block.TangemBlockCardColors
import com.tangem.core.ui.components.block.information.InformationBlock
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheet
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.buttons.common.TangemButton
import com.tangem.core.ui.components.buttons.common.TangemButtonIconPosition
import com.tangem.core.ui.components.buttons.common.TangemButtonSize
import com.tangem.core.ui.components.buttons.common.TangemButtonsDefaults
import com.tangem.core.ui.components.currency.icon.CoinIcon
import com.tangem.core.ui.components.rows.ArrowRow
import com.tangem.core.ui.components.rows.BlockchainRow
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.markets.impl.R
import com.tangem.features.markets.portfolio.impl.ui.preview.PreviewAddToPortfolioBSContentProvider
import com.tangem.features.markets.portfolio.impl.ui.state.AddToPortfolioBSContentUM
import com.tangem.features.markets.portfolio.impl.ui.state.SelectNetworkUM
import kotlinx.coroutines.delay

@Composable
internal fun AddToPortfolioBottomSheet(config: TangemBottomSheetConfig) {
    TangemBottomSheet<AddToPortfolioBSContentUM>(
        config = config,
        containerColor = TangemTheme.colors.background.tertiary,
        addBottomInsets = false,
        titleText = resourceReference(R.string.markets_add_to_portfolio_button),
    ) {
        Content(
            modifier = Modifier.fillMaxWidth(),
            state = it,
        )

        WalletSelectorBottomSheet(it.walletSelectorConfig)
    }
}

@Composable
private fun Content(state: AddToPortfolioBSContentUM, modifier: Modifier = Modifier) {
    var continueButtonAreaHeight by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val scrollState = rememberScrollState()

    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .verticalScroll(state = scrollState)
                .padding(horizontal = TangemTheme.dimens.spacing16),
        ) {
            UserWalletItem(
                state = state.selectedWallet,
                blockColors = TangemBlockCardColors.copy(
                    containerColor = TangemTheme.colors.background.action,
                    disabledContainerColor = TangemTheme.colors.background.action,
                ),
            )

            SpacerH12()

            NetworkSelection(
                modifier = Modifier.fillMaxWidth(),
                state = state.selectNetworkUM,
            )

            SpacerH12()

            AnimatedVisibility(
                visible = state.isScanCardNotificationVisible,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column {
                    ScanWalletWarning(modifier = Modifier.fillMaxWidth())
                    SpacerH12()
                }

                // Scroll to the bottom when the notification appears and the scroll is at the bottom
                LaunchedEffect(Unit) {
                    if (scrollState.canScrollForward.not()) {
                        delay(timeMillis = 500)
                        scrollState.animateScrollTo(scrollState.maxValue)
                    }
                }
            }

            SpacerH(with(density) { continueButtonAreaHeight.toDp() })
        }

        AnimatedVisibility(
            visible = scrollState.canScrollForward,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            BottomFade(Modifier.align(Alignment.BottomCenter))
        }

        ContinueButton(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .onGloballyPositioned {
                    continueButtonAreaHeight = it.size.height
                },
            enabled = state.continueButtonEnabled,
            onClick = state.onContinueButtonClick,
        )
    }
}

@Composable
private fun ContinueButton(enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    TangemButton(
        enabled = enabled,
        modifier = modifier
            .padding(
                start = TangemTheme.dimens.spacing16,
                end = TangemTheme.dimens.spacing16,
                bottom = TangemTheme.dimens.spacing16,
            )
            .navigationBarsPadding()
            .fillMaxWidth(),
        text = stringResource(R.string.common_continue),
        icon = if (enabled) {
            TangemButtonIconPosition.End(R.drawable.ic_tangem_24)
        } else {
            TangemButtonIconPosition.None
        },
        showProgress = false,
        size = TangemButtonSize.Default,
        colors = TangemButtonsDefaults.primaryButtonColors,
        onClick = onClick,
        animateContentChange = true,
    )
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
        Column {
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
                            model = network,
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
            tint = TangemTheme.colors.icon.primary1,
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

// For on device testing
@Composable
@Preview
private fun PreviewContentTestOnDevice(
    @PreviewParameter(PreviewAddToPortfolioBSContentProvider::class) content: AddToPortfolioBSContentUM,
) {
    TangemThemePreview(
        alwaysShowBottomSheets = false,
    ) {
        var isShow by remember { mutableStateOf(false) }

        var contentState by remember {
            mutableStateOf(content)
        }

        LaunchedEffect(Unit) {
            contentState = content.copy(
                onContinueButtonClick = {
                    contentState = contentState.copy(
                        isScanCardNotificationVisible = !contentState.isScanCardNotificationVisible,
                    )
                },
                continueButtonEnabled = true,
                selectedWallet = content.selectedWallet.copy(
                    onClick = {
                        contentState = contentState.copy(
                            continueButtonEnabled = !contentState.continueButtonEnabled,
                        )
                    },
                ),
            )
        }

        AddToPortfolioBottomSheet(
            config = TangemBottomSheetConfig(
                isShow = isShow,
                content = contentState,
                onDismissRequest = { isShow = false },
            ),
        )

        Button(
            onClick = { isShow = !isShow },
        ) {
            Text(text = "Toggle")
        }
    }
}
