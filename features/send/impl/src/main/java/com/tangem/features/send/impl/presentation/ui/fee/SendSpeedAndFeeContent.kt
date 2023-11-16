package com.tangem.features.send.impl.presentation.ui.fee

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.send.impl.presentation.state.SendStates
import com.tangem.features.send.impl.presentation.state.fee.FeeSelectorState
import com.tangem.features.send.impl.presentation.state.fee.FeeType
import com.tangem.features.send.impl.presentation.viewmodel.SendClickIntents

private const val FEE_SELECTOR_KEY = "FEE_SELECTOR_KEY"
private const val FEE_CUSTOM_KEY = "FEE_CUSTOM_KEY"

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun SendSpeedAndFeeContent(state: SendStates.FeeState?, clickIntents: SendClickIntents) {
    if (state == null) return
    val feeSendState = state.feeSelectorState.collectAsStateWithLifecycle()
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(TangemTheme.colors.background.tertiary)
            .padding(
                horizontal = TangemTheme.dimens.spacing16,
            ),
        verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing12),
    ) {
        item(
            key = FEE_SELECTOR_KEY,
        ) {
            SendSpeedSelector(
                state = feeSendState,
                clickIntents = clickIntents,
            )
        }
        if (feeSendState.value is FeeSelectorState.Content) {
            item(
                key = FEE_CUSTOM_KEY,
            ) {
                AnimatedVisibility(
                    visible = feeSendState.value is FeeSelectorState.Content,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(TangemTheme.colors.background.tertiary),
                ) {
                    val fee = feeSendState.value as FeeSelectorState.Content
                    val customValues = fee.customValues.collectAsStateWithLifecycle()
                    SendCustomFeeEthereum(
                        customValues = customValues,
                        selectedFee = fee.selectedFee,
                        symbol = state.cryptoCurrencyStatus.currency.symbol,
                        modifier = Modifier
                            .animateItemPlacement(),
                    )
                }
            }
        }
        item {
            val topPadding = (feeSendState.value as? FeeSelectorState.Content)?.let { state ->
                if (state.selectedFee != FeeType.CUSTOM) {
                    TangemTheme.dimens.spacing8
                } else {
                    TangemTheme.dimens.spacing0
                }
            } ?: TangemTheme.dimens.spacing0

            SendSpeedSubtract(
                receivingAmount = state.receivedAmount,
                isSubtract = state.isSubtract,
                onSelectClick = clickIntents::onSubtractSelect,
                modifier = Modifier
                    .animateItemPlacement()
                    .padding(
                        top = topPadding,
                        bottom = TangemTheme.dimens.spacing12,
                    ),
            )
        }
    }
}
