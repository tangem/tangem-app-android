package com.tangem.feature.swap.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.components.CardWithIcon
import com.tangem.core.ui.components.ClickableWarningCard
import com.tangem.core.ui.components.Keyboard
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.components.PrimaryButtonIconRight
import com.tangem.core.ui.components.RefreshableWaringCard
import com.tangem.core.ui.components.SimpleOkDialog
import com.tangem.core.ui.components.SmallInfoCard
import com.tangem.core.ui.components.SmallInfoCardWithWarning
import com.tangem.core.ui.components.SpacerH16
import com.tangem.core.ui.components.keyboardAsState
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.swap.models.FeeState
import com.tangem.feature.swap.models.SwapButton
import com.tangem.feature.swap.models.SwapCardData
import com.tangem.feature.swap.models.SwapPermissionState
import com.tangem.feature.swap.models.SwapStateHolder
import com.tangem.feature.swap.models.SwapWarning
import com.tangem.feature.swap.models.TransactionCardType
import com.tangem.feature.swap.presentation.R

@Suppress("LongMethod")
@Composable
internal fun SwapScreenContent(state: SwapStateHolder, onPermissionWarningClick: () -> Unit) {
    val keyboard by keyboardAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = TangemTheme.colors.background.secondary),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = TangemTheme.dimens.spacing16,
                    end = TangemTheme.dimens.spacing16,
                    top = TangemTheme.dimens.spacing16,
                    bottom = TangemTheme.dimens.spacing32,
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing16),
        ) {
            MainInfo(state)

            FeeItem(feeState = state.fee, currency = state.networkCurrency)

            if (state.warnings.isNotEmpty()) {
                SwapWarnings(
                    warnings = state.warnings,
                    onApproveWarningClick = onPermissionWarningClick,
                )
            }

            if (state.permissionState is SwapPermissionState.InProgress) {
                CardWithIcon(
                    title = stringResource(id = R.string.swapping_pending_transaction_title),
                    description = stringResource(id = R.string.swapping_pending_transaction_subtitle),
                    icon = {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(TangemTheme.dimens.size16),
                            color = TangemTheme.colors.icon.primary1,
                            strokeWidth = TangemTheme.dimens.size2,
                        )
                    },
                )
            }

            if (state.warnings.any { it is SwapWarning.InsufficientFunds }) {
                PrimaryButton(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(id = R.string.swapping_insufficient_funds),
                    enabled = false,
                    showProgress = state.swapButton.loading,
                    onClick = state.swapButton.onClick,
                )
            } else {
                PrimaryButtonIconRight(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(id = R.string.swapping_swap),
                    icon = painterResource(id = R.drawable.ic_tangem_24),
                    enabled = state.swapButton.enabled,
                    showProgress = state.swapButton.loading,
                    onClick = state.swapButton.onClick,
                )
            }
        }

        AnimatedVisibility(
            visible = keyboard is Keyboard.Opened,
            modifier = Modifier
                .align(Alignment.BottomCenter),
        ) {
            Text(
                text = stringResource(id = R.string.send_max_amount_label),
                style = TangemTheme.typography.button,
                color = TangemTheme.colors.text.primary1,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TangemTheme.colors.button.secondary)
                    .clickable { state.onMaxAmountSelected }
                    .padding(
                        horizontal = TangemTheme.dimens.spacing14,
                        vertical = TangemTheme.dimens.spacing16,
                    ),
                textAlign = TextAlign.Start,
            )
        }

        if (state.alert != null) {
            SimpleOkDialog(
                message = state.alert.message ?: stringResource(id = R.string.swapping_generic_error),
                onDismissDialog = state.alert.onClick,
            )
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun MainInfo(state: SwapStateHolder) {
    Box(
        modifier = Modifier
            .fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Column {
            TransactionCard(
                type = state.sendCardData.type,
                balance = state.sendCardData.balance,
                amount = state.sendCardData.amount,
                amountEquivalent = state.sendCardData.amountEquivalent,
                tokenIconUrl = state.sendCardData.tokenIconUrl,
                tokenCurrency = state.sendCardData.tokenCurrency,
                networkIconRes = state.sendCardData.networkIconRes,
                onChangeTokenClick = if (state.sendCardData.canSelectAnotherToken) state.onSelectTokenClick else null,
            )
            SpacerH16()
            TransactionCard(
                type = state.receiveCardData.type,
                balance = state.receiveCardData.balance,
                amount = state.receiveCardData.amount,
                amountEquivalent = state.receiveCardData.amountEquivalent,
                tokenIconUrl = state.receiveCardData.tokenIconUrl,
                tokenCurrency = state.receiveCardData.tokenCurrency,
                networkIconRes = state.receiveCardData.networkIconRes,
                onChangeTokenClick = if (state.receiveCardData.canSelectAnotherToken) {
                    state.onSelectTokenClick
                } else {
                    null
                },
            )
        }
        Card(
            elevation = TangemTheme.dimens.elevation3,
            shape = CircleShape,
            backgroundColor = TangemTheme.colors.background.plain,
            contentColor = TangemTheme.colors.text.primary1,
            modifier = Modifier
                .size(TangemTheme.dimens.size48),
            onClick = state.onChangeCardsClicked,
        ) {
            Icon(
                painter = painterResource(id = com.tangem.core.ui.R.drawable.ic_exchange_vertical_24),
                contentDescription = null,
                tint = TangemTheme.colors.text.primary1,
                modifier = Modifier
                    .padding(TangemTheme.dimens.spacing12),
            )
        }
    }
}

@Composable
private fun FeeItem(feeState: FeeState, currency: String) {
    val titleString = stringResource(id = R.string.send_fee_label)
    when (feeState) {
        is FeeState.Loaded -> {
            SmallInfoCard(startText = titleString, endText = feeState.fee, isLoading = false)
        }
        FeeState.Loading -> {
            SmallInfoCard(startText = titleString, endText = "", isLoading = true)
        }
        is FeeState.NotEnoughFundsWarning -> {
            SmallInfoCardWithWarning(
                startText = titleString,
                endText = feeState.fee,
                warningText = stringResource(id = R.string.token_details_send_blocked_fee_format, currency),
            )
        }
        is FeeState.Empty -> {}
    }
}

@Composable
private fun SwapWarnings(
    warnings: List<SwapWarning>,
    onApproveWarningClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .background(color = TangemTheme.colors.background.secondary)
            .fillMaxWidth(),
    ) {
        warnings.forEach { warning ->
            when (warning) {
                // SwapWarning.HighPriceImpact -> {
                //     WarningCard(title = stringResource(id = R.string.), description = stringResource(id = R.string.))
                // }
                is SwapWarning.PermissionNeeded -> {
                    ClickableWarningCard(
                        title = stringResource(id = R.string.swapping_permission_header),
                        description = stringResource(
                            id = R.string.swapping_permission_subheader,
                            warning.tokenCurrency,
                        ),
                        icon = {
                            Image(
                                painter = painterResource(id = com.tangem.core.ui.R.drawable.ic_locked_24),
                                contentDescription = null,
                                modifier = Modifier.size(TangemTheme.dimens.size20),
                            )
                        },
                        onClick = onApproveWarningClick,
                    )
                }
                is SwapWarning.GenericWarning -> {
                    RefreshableWaringCard(
                        title = stringResource(id = R.string.common_warning),
                        description = warning.message ?: stringResource(id = R.string.swapping_generic_error),
                        onClick = warning.onClick,
                    )
                }
                else -> {}
                // is SwapWarning.RateExpired -> {
                //     RefreshableWaringCard(
                //         title = stringResource(id = R.string.),
                //         description = stringResource(id = R.string.),
                //         onClick = warning.onClick,
                //     )
                // }
            }
        }
    }
}

// region preview

private val sendCard = SwapCardData(
    type = TransactionCardType.SendCard {},
    amount = "1 000 000",
    amountEquivalent = "1 000 000",
    tokenIconUrl = "",
    tokenCurrency = "DAI",
    networkIconRes = R.drawable.img_polygon_22,
    canSelectAnotherToken = false,
    balance = "123",
)

private val receiveCard = SwapCardData(
    type = TransactionCardType.ReceiveCard(),
    amount = "1 000 000",
    amountEquivalent = "1 000 000",
    tokenIconUrl = "",
    tokenCurrency = "DAI",
    networkIconRes = R.drawable.img_polygon_22,
    canSelectAnotherToken = true,
    balance = "33333",
)

private val state = SwapStateHolder(
    sendCardData = sendCard,
    receiveCardData = receiveCard,
    fee = FeeState.Loaded(fee = "0.155 MATIC (0.14 $)"),
    warnings = listOf(SwapWarning.PermissionNeeded("DAI")),
    networkCurrency = "MATIC",
    swapButton = SwapButton(enabled = true, loading = false, onClick = {}),
    onRefresh = {}, onBackClicked = {}, onChangeCardsClicked = {},
    permissionState = SwapPermissionState.InProgress,
    // alert = SwapWarning.GenericWarning("There was an error. Please try again.") {},
)

@Preview
@Composable
private fun SwapScreenContentPreview() {
    TangemTheme(isDark = false) {
        SwapScreenContent(state = state) {}
    }
}

// endregion preview
