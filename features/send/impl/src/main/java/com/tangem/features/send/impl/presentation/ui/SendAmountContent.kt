package com.tangem.features.send.impl.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.tangem.core.ui.components.SecondaryButton
import com.tangem.core.ui.components.buttons.common.TangemButtonSize
import com.tangem.core.ui.components.buttons.segmentedbutton.SegmentedButtons
import com.tangem.core.ui.components.currency.fiaticon.FiatIcon
import com.tangem.core.ui.components.currency.tokenicon.TokenIcon
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.send.impl.R
import com.tangem.features.send.impl.presentation.state.amount.SendAmountSegmentedButtonsConfig
import com.tangem.features.send.impl.presentation.state.SendUiState
import com.tangem.features.send.impl.presentation.ui.amount.AmountFieldContainer

@Composable
internal fun SendAmountContent(amountState: SendUiState.Content.AmountState) {
    Column(
        modifier = Modifier
            .background(TangemTheme.colors.background.tertiary),
    ) {
        Text(
            text = stringResource(R.string.common_send),
            style = TangemTheme.typography.subtitle1,
            color = TangemTheme.colors.text.primary1,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(vertical = TangemTheme.dimens.spacing16)
                .align(CenterHorizontally),
        )
        AmountFieldContainer(amountState = amountState)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = TangemTheme.dimens.spacing12,
                    start = TangemTheme.dimens.spacing16,
                    end = TangemTheme.dimens.spacing16,
                ),
        ) {
            SegmentedButtons(
                modifier = Modifier
                    .height(TangemTheme.dimens.size40)
                    .weight(1f),
                config = amountState.segmentedButtonConfig,
                onClick = { amountState.clickIntents.onCurrencyChangeClick(it.isFiat) },
            ) {
                SendAmountCurrencyButton(it)
            }
            SecondaryButton(
                text = stringResource(R.string.send_max_amount),
                onClick = amountState.clickIntents::onMaxValueClick,
                size = TangemButtonSize.Text,
                shape = RoundedCornerShape(TangemTheme.dimens.radius26),
                modifier = Modifier
                    .padding(start = TangemTheme.dimens.spacing8)
                    .height(TangemTheme.dimens.size40),
            )
        }
    }
}

@Composable
private fun SendAmountCurrencyButton(button: SendAmountSegmentedButtonsConfig) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                horizontal = TangemTheme.dimens.spacing10,
            ),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (button.isFiat) {
            FiatIcon(
                url = button.iconUrl,
                modifier = Modifier
                    .size(TangemTheme.dimens.size18),
            )
        } else {
            button.iconState?.let {
                TokenIcon(
                    state = it,
                    shouldDisplayNetwork = false,
                    modifier = Modifier
                        .size(TangemTheme.dimens.size18),
                )
            }
        }
        Text(
            text = button.title.resolveReference(),
            color = TangemTheme.colors.text.primary1,
            style = TangemTheme.typography.button,
            modifier = Modifier
                .padding(
                    start = TangemTheme.dimens.spacing8,
                ),
        )
    }
}