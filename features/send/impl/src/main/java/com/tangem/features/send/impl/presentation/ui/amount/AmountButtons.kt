package com.tangem.features.send.impl.presentation.ui.amount

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import com.tangem.core.ui.components.SecondaryButton
import com.tangem.core.ui.components.SpacerWMax
import com.tangem.core.ui.components.buttons.common.TangemButtonSize
import com.tangem.core.ui.components.buttons.segmentedbutton.SegmentedButtons
import com.tangem.core.ui.components.currency.fiaticon.FiatIcon
import com.tangem.core.ui.components.currency.tokenicon.TokenIcon
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.send.impl.R
import com.tangem.features.send.impl.presentation.state.amount.SendAmountSegmentedButtonsConfig
import com.tangem.features.send.impl.presentation.viewmodel.SendClickIntents
import kotlinx.collections.immutable.PersistentList

private const val AMOUNT_BUTTONS_KEY = "amountButtonsKey"

internal fun LazyListScope.buttons(
    segmentedButtonConfig: PersistentList<SendAmountSegmentedButtonsConfig>,
    clickIntents: SendClickIntents,
    isMaxButtonEnabled: Boolean,
) {
    item(
        key = AMOUNT_BUTTONS_KEY,
    ) {
        val hapticFeedback = LocalHapticFeedback.current
        Row(
            modifier = Modifier.padding(top = TangemTheme.dimens.spacing12),
        ) {
            if (segmentedButtonConfig.isNotEmpty()) {
                SegmentedButtons(
                    modifier = Modifier
                        .weight(1f)
                        .height(TangemTheme.dimens.size40),
                    config = segmentedButtonConfig,
                    showIndication = false,
                    onClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        clickIntents.onCurrencyChangeClick(it.isFiat)
                    },
                ) {
                    SendAmountCurrencyButton(it)
                }
            } else {
                SpacerWMax()
            }
            SecondaryButton(
                text = stringResource(R.string.send_max_amount),
                enabled = isMaxButtonEnabled,
                onClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    clickIntents.onMaxValueClick()
                },
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
                size = TangemTheme.dimens.size18,
                modifier = Modifier.size(TangemTheme.dimens.size18),
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