package com.tangem.common.ui.amountScreen.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import com.tangem.common.ui.R
import com.tangem.common.ui.amountScreen.AmountScreenClickIntents
import com.tangem.common.ui.amountScreen.models.AmountSegmentedButtonsConfig
import com.tangem.core.ui.components.SpacerWMax
import com.tangem.core.ui.components.buttons.segmentedbutton.SegmentedButtons
import com.tangem.core.ui.components.currency.fiaticon.FiatIcon
import com.tangem.core.ui.components.currency.icon.CurrencyIcon
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.test.StakingSendScreenTestTags
import kotlinx.collections.immutable.PersistentList

private const val AMOUNT_BUTTONS_KEY = "amountButtonsKey"

internal fun LazyListScope.buttons(
    segmentedButtonConfig: PersistentList<AmountSegmentedButtonsConfig>,
    clickIntents: AmountScreenClickIntents,
    isSegmentedButtonsEnabled: Boolean,
    selectedButton: Int,
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
                    initialSelectedItem = segmentedButtonConfig.getOrNull(selectedButton),
                    isEnabled = isSegmentedButtonsEnabled,
                ) {
                    AmountCurrencyButton(
                        button = it,
                        isSegmentedButtonsEnabled = isSegmentedButtonsEnabled,
                    )
                }
            } else {
                SpacerWMax()
            }
            Text(
                text = stringResourceSafe(R.string.send_max_amount),
                style = TangemTheme.typography.button,
                color = TangemTheme.colors.text.primary1,
                modifier = Modifier
                    .padding(start = TangemTheme.dimens.spacing8)
                    .height(TangemTheme.dimens.size40)
                    .clip(shape = RoundedCornerShape(TangemTheme.dimens.radius26))
                    .background(TangemTheme.colors.button.secondary)
                    .clickable {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        clickIntents.onMaxValueClick()
                    }
                    .padding(
                        vertical = TangemTheme.dimens.spacing10,
                        horizontal = TangemTheme.dimens.spacing34,
                    )
                    .testTag(StakingSendScreenTestTags.MAX_BUTTON),
            )
        }
    }
}

@Composable
private fun AmountCurrencyButton(button: AmountSegmentedButtonsConfig, isSegmentedButtonsEnabled: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                horizontal = TangemTheme.dimens.spacing10,
            )
            .testTag(StakingSendScreenTestTags.CURRENCY_BUTTON),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val iconModifier = Modifier
            .size(TangemTheme.dimens.size18)
            .padding(horizontal = TangemTheme.dimens.spacing1)
        if (button.isFiat) {
            FiatIcon(
                url = button.iconUrl,
                size = TangemTheme.dimens.size18,
                isGrayscale = !isSegmentedButtonsEnabled,
                modifier = iconModifier.testTag(StakingSendScreenTestTags.FIAT_ICON),
            )
        } else if (button.iconState != null) {
            CurrencyIcon(
                state = button.iconState,
                shouldDisplayNetwork = false,
                modifier = iconModifier.testTag(StakingSendScreenTestTags.CURRENCY_ICON),
            )
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