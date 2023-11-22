package com.tangem.feature.swap.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.constraintlayout.compose.ConstraintLayout
import com.tangem.common.Strings.STARS
import com.tangem.core.ui.components.*
import com.tangem.core.ui.components.appbar.AppBarWithBackButton
import com.tangem.core.ui.components.notifications.Notification
import com.tangem.core.ui.components.notifications.NotificationConfig
import com.tangem.core.ui.components.states.Item
import com.tangem.core.ui.components.states.SelectableItemsState
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.getActiveIconRes
import com.tangem.core.ui.extensions.getActiveIconResByCoinId
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.swap.domain.models.ui.FeeType
import com.tangem.feature.swap.domain.models.ui.TxFee
import com.tangem.feature.swap.models.*
import com.tangem.feature.swap.models.states.ProviderState
import com.tangem.feature.swap.presentation.R
import kotlinx.collections.immutable.toImmutableList
import java.math.BigDecimal

@Suppress("LongMethod")
@Composable
internal fun SwapScreenContent(state: SwapStateHolder, onPermissionWarningClick: () -> Unit) {
    val keyboard by keyboardAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = TangemTheme.colors.background.secondary),
    ) {
        Image(
            modifier = Modifier
                .size(width = TangemTheme.dimens.size164, height = TangemTheme.dimens.size80)
                .align(Alignment.BottomCenter)
                .padding(bottom = TangemTheme.dimens.spacing28),
            painter = painterResource(id = R.drawable.ill_one_inch_powered),
            contentDescription = null,
            contentScale = ContentScale.Fit,
        )

        Column {
            AppBarWithBackButton(
                text = stringResource(R.string.common_swap),
                onBackClick = state.onBackClicked,
                iconRes = R.drawable.ic_close_24,
            )
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

                ProviderItemBlock(state = state.providerState)

                FeeItem(feeState = state.fee, currency = state.networkCurrency)

                if (state.warnings.isNotEmpty()) SwapWarnings(warnings = state.warnings)

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
                MainButton(state = state, onPermissionWarningClick = onPermissionWarningClick)
            }
        }

        AnimatedVisibility(
            visible = keyboard is Keyboard.Opened,
            modifier = Modifier
                .imePadding()
                .align(Alignment.BottomCenter),
        ) {
            Text(
                text = stringResource(id = R.string.send_max_amount_label),
                style = TangemTheme.typography.button,
                color = TangemTheme.colors.text.primary1,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TangemTheme.colors.button.secondary)
                    .clickable { state.onMaxAmountSelected?.invoke() }
                    .padding(
                        horizontal = TangemTheme.dimens.spacing14,
                        vertical = TangemTheme.dimens.spacing16,
                    ),
                textAlign = TextAlign.Start,
            )
        }

        if (state.alert != null) {
            val message = if (state.alert.type == GenericWarningType.NETWORK) {
                stringResource(id = R.string.disclaimer_error_loading)
            } else {
                state.alert.message ?: stringResource(id = R.string.swapping_generic_error)
            }
            SimpleOkDialog(
                message = message,
                onDismissDialog = state.alert.onClick,
            )
        }
    }
}

@Composable
private fun MainInfo(state: SwapStateHolder) {
    ConstraintLayout(
        modifier = Modifier.fillMaxWidth(),
    ) {
        val networkIconRes = remember {
            getActiveIconRes(state.blockchainId)
        }
        val (topCard, bottomCard, button) = createRefs()
        val priceImpactWarning = state.warnings.filterIsInstance<SwapWarning.HighPriceImpact>().firstOrNull()
        TransactionCardData(
            priceImpactWarning = priceImpactWarning,
            networkIconRes = networkIconRes,
            swapCardState = state.sendCardData,
            modifier = Modifier.constrainAs(topCard) {
                top.linkTo(parent.top)
            },
            onSelectTokenClick = state.onSelectTokenClick,
        )
        val marginCard = TangemTheme.dimens.spacing16
        TransactionCardData(
            priceImpactWarning = priceImpactWarning,
            networkIconRes = networkIconRes,
            swapCardState = state.receiveCardData,
            modifier = Modifier.constrainAs(bottomCard) {
                top.linkTo(topCard.bottom, margin = marginCard)
            },
            onSelectTokenClick = state.onSelectTokenClick,
        )
        val marginButton = TangemTheme.dimens.spacing32
        SwapButton(
            state,
            modifier = Modifier.constrainAs(button) {
                bottom.linkTo(topCard.bottom, margin = -marginButton)
                start.linkTo(topCard.start)
                end.linkTo(topCard.end)
            },
        )
    }
}

@Composable
private fun TransactionCardData(
    priceImpactWarning: SwapWarning.HighPriceImpact?,
    networkIconRes: Int?,
    swapCardState: SwapCardState,
    onSelectTokenClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    when (swapCardState) {
        is SwapCardState.Empty -> {
            TransactionCardEmpty(
                type = swapCardState.type,
                amountEquivalent = swapCardState.amountEquivalent,
                textFieldValue = swapCardState.amountTextFieldValue,
                onChangeTokenClick = if (swapCardState.canSelectAnotherToken) onSelectTokenClick else null,
                modifier = modifier,
            )
        }
        is SwapCardState.SwapCardData -> {
            TransactionCard(
                type = swapCardState.type,
                balance = if (swapCardState.isBalanceHidden) {
                    STARS
                } else {
                    swapCardState.balance
                },
                textFieldValue = swapCardState.amountTextFieldValue,
                amountEquivalent = swapCardState.amountEquivalent,
                tokenIconUrl = swapCardState.tokenIconUrl ?: "",
                tokenCurrency = swapCardState.tokenCurrency,
                priceImpact = priceImpactWarning,
                networkIconRes = if (swapCardState.isNotNativeToken) networkIconRes else null,
                iconPlaceholder = swapCardState.coinId?.let {
                    getActiveIconResByCoinId(it)
                },
                onChangeTokenClick = if (swapCardState.canSelectAnotherToken) onSelectTokenClick else null,
                modifier = modifier,
            )
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun SwapButton(state: SwapStateHolder, modifier: Modifier = Modifier) {
    Card(
        elevation = TangemTheme.dimens.elevation3,
        shape = CircleShape,
        backgroundColor = TangemTheme.colors.background.plain,
        contentColor = TangemTheme.colors.text.primary1,
        modifier = modifier.size(TangemTheme.dimens.size48),
        onClick = state.onChangeCardsClicked,
        enabled = !state.updateInProgress,
    ) {
        if (state.updateInProgress) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(TangemTheme.dimens.size16)
                    .padding(TangemTheme.dimens.spacing14),
                color = TangemTheme.colors.icon.primary1,
                strokeWidth = TangemTheme.dimens.size2,
            )
        } else {
            Icon(
                painter = painterResource(id = R.drawable.ic_exchange_vertical_24),
                contentDescription = null,
                tint = TangemTheme.colors.text.primary1,
                modifier = Modifier.padding(TangemTheme.dimens.spacing12),
            )
        }
    }
}

@Composable
private fun FeeItem(feeState: FeeState, currency: String) {
    val titleString = stringResource(id = R.string.send_network_fee_title)
    val disclaimer = stringResource(id = R.string.swapping_tangem_fee_disclaimer, "${feeState.tangemFee}%")
    when (feeState) {
        is FeeState.Loaded -> {
            if (feeState.state != null) {
                SelectableInfoCard(
                    state = feeState.state,
                    disclaimer = disclaimer,
                    onSelect = feeState.onSelectItem,
                )
            }
        }
        FeeState.Loading -> {
            SmallInfoCardWithDisclaimer(
                startText = titleString,
                endText = "",
                disclaimer = disclaimer,
                isLoading = true,
            )
        }
        is FeeState.NotEnoughFundsWarning -> {
            if (feeState.state != null) {
                SelectableInfoCardWithWarning(
                    state = feeState.state,
                    warningText = stringResource(
                        id = R.string.swapping_not_enough_funds_for_fee,
                        currency,
                        currency,
                    ),
                    disclaimer = disclaimer,
                    onSelect = feeState.onSelectItem,
                )
            }
        }
        is FeeState.Empty -> {
            // show nothing
            // SmallInfoCard(startText = titleString, endText = "")
        }
    }
}

@Composable
private fun SwapWarnings(warnings: List<SwapWarning>) {
    Column(
        modifier = Modifier
            .background(color = TangemTheme.colors.background.secondary)
            .fillMaxWidth(),
    ) {
        warnings.forEach { warning ->
            when (warning) {
                is SwapWarning.HighPriceImpact -> {
                    Notification(
                        config = warning.notificationConfig,
                    )
                }
                is SwapWarning.PermissionNeeded -> {
                    Notification(
                        config = warning.notificationConfig,
                    )
                }
                is SwapWarning.GenericWarning -> {
                    val message = warning.message?.let {
                        if (warning.shouldWrapMessage) {
                            String.format(stringResource(id = R.string.swapping_error_wrapper), it)
                        } else {
                            it
                        }
                    } ?: stringResource(id = R.string.swapping_generic_error)
                    RefreshableWaringCard(
                        title = stringResource(id = R.string.common_warning),
                        description = message,
                        onClick = warning.onClick,
                    )
                }
                is SwapWarning.NoAvailableTokensToSwap -> {
                    Notification(
                        config = warning.notificationConfig,
                    )
                }
                else -> {}
            }
            SpacerH8()
        }
    }
}

@Composable
private fun MainButton(state: SwapStateHolder, onPermissionWarningClick: () -> Unit) {
    // order is important
    when {
        state.warnings.any { it is SwapWarning.InsufficientFunds } -> {
            PrimaryButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(id = R.string.swapping_insufficient_funds),
                enabled = false,
                showProgress = state.swapButton.loading,
                onClick = state.swapButton.onClick,
            )
        }
        state.warnings.any { it is SwapWarning.PermissionNeeded } -> {
            PrimaryButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(id = R.string.swapping_give_permission),
                enabled = true,
                showProgress = state.swapButton.loading,
                onClick = onPermissionWarningClick,
            )
        }
        else -> {
            PrimaryButtonIconEnd(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(id = R.string.common_swap),
                iconResId = R.drawable.ic_tangem_24,
                enabled = state.swapButton.enabled,
                showProgress = state.swapButton.loading,
                onClick = state.swapButton.onClick,
            )
        }
    }
}

// region preview

private val sendCard = SwapCardState.SwapCardData(
    type = TransactionCardType.SendCard({}) {},
    amountTextFieldValue = TextFieldValue(),
    amountEquivalent = "1 000 000",
    tokenIconUrl = "",
    tokenCurrency = "DAI",
    isNotNativeToken = true,
    canSelectAnotherToken = false,
    balance = "123",
    coinId = "",
    isBalanceHidden = false,
)

private val receiveCard = SwapCardState.SwapCardData(
    type = TransactionCardType.ReceiveCard(),
    amountTextFieldValue = TextFieldValue(),
    amountEquivalent = "1 000 000",
    tokenIconUrl = "",
    tokenCurrency = "DAI",
    isNotNativeToken = true,
    canSelectAnotherToken = true,
    balance = "33333",
    coinId = "",
    isBalanceHidden = false,
)

val stateSelectable = SelectableItemsState(
    selectedItem = Item(
        0,
        TextReference.Str("Balance"),
        TextReference.Str("0.4405434 BTC"),
        true,
        TxFee(
            feeValue = BigDecimal.ZERO,
            gasLimit = 0,
            feeFiatFormatted = "",
            feeCryptoFormatted = "",
            feeType = FeeType.NORMAL,
        ),
    ),
    items = listOf(
        Item(
            0,
            TextReference.Str("Normal"),
            TextReference.Str("0.4405434 BTC"),
            true,
            TxFee(
                feeValue = BigDecimal.ZERO,
                gasLimit = 0,
                feeFiatFormatted = "",
                feeCryptoFormatted = "",
                feeType = FeeType.NORMAL,
            ),
        ),
        Item(
            1,
            TextReference.Str("Priority"),
            TextReference.Str("0.46 BTC"),
            false,
            TxFee(
                feeValue = BigDecimal.ZERO,
                gasLimit = 0,
                feeFiatFormatted = "",
                feeCryptoFormatted = "",
                feeType = FeeType.NORMAL,
            ),
        ),
    ).toImmutableList(),
)

private val state = SwapStateHolder(
    networkId = "ethereum",
    sendCardData = sendCard,
    receiveCardData = receiveCard,
    fee = FeeState.Loaded(
        tangemFee = 0.0,
        state = stateSelectable,
        onSelectItem = {},
    ),
    warnings = listOf(
        SwapWarning.PermissionNeeded(
            notificationConfig = NotificationConfig(
                title = stringReference("Give Premission"),
                subtitle = stringReference("To continue swapping you need to give permission to Tangem"),
                iconResId = R.drawable.ic_locked_24,
            ),
        ),
        SwapWarning.NoAvailableTokensToSwap(
            notificationConfig = NotificationConfig(
                title = stringReference("No tokens"),
                subtitle = stringReference("Swap tokens not available"),
                iconResId = R.drawable.ic_alert_24,
            ),
        ),
    ),
    networkCurrency = "MATIC",
    swapButton = SwapButton(enabled = true, loading = false, onClick = {}),
    onRefresh = {},
    onBackClicked = {},
    onChangeCardsClicked = {},
    permissionState = SwapPermissionState.InProgress,
    blockchainId = "POLYGON",
    providerState = ProviderState.Loading(),
)

@Preview
@Composable
private fun SwapScreenContentPreview() {
    TangemTheme(isDark = false) {
        SwapScreenContent(state = state) {}
    }
}

// endregion preview
