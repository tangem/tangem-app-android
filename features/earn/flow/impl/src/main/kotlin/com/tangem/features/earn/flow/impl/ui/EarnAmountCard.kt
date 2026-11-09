@file:Suppress("MagicNumber")

package com.tangem.features.earn.flow.impl.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.tangem.common.ui.account.AccountTitle
import com.tangem.common.ui.amountScreen.AmountScreenClickIntents
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.common.ui.amountScreen.ui.AmountFieldV2
import com.tangem.core.res.R
import com.tangem.core.ui.components.TextShimmer
import com.tangem.core.ui.components.currency.icon.CurrencyIcon
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.row.TangemRow
import com.tangem.core.ui.ds2.row.TangemRowVerticalAlignment
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.test.SendScreenTestTags

/** The field itself is the shared one of Send, so input, currency switch and validation stay the same. */
@Composable
internal fun EarnAmountCard(
    amountState: AmountState,
    clickIntents: AmountScreenClickIntents,
    isSourceShown: Boolean,
    modifier: Modifier = Modifier,
) {
    val data = amountState as? AmountState.Data
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(TangemTheme.colors3.bg.secondary),
    ) {
        if (isSourceShown) {
            val sourceModifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp)
            if (data != null) {
                AccountTitle(
                    accountTitleUM = data.accountTitleUM,
                    textStyle = TangemTheme.typography3.caption.medium,
                    modifier = sourceModifier,
                )
            } else {
                TextShimmer(style = TangemTheme.typography3.caption.medium, modifier = sourceModifier.width(60.dp))
            }
        }
        AmountFieldV2(
            amountUM = amountState,
            onValueChange = clickIntents::onAmountValueChange,
            onValuePastedTriggerDismiss = clickIntents::onAmountPasteTriggerDismiss,
            onCurrencyChange = clickIntents::onCurrencyChangeClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 40.dp),
        )
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            thickness = 1.dp,
            color = TangemTheme.colors3.border.secondary,
        )
        BalanceRow(data = data, onMaxClick = clickIntents::onMaxValueClick)
    }
}

@Composable
private fun BalanceRow(data: AmountState.Data?, onMaxClick: () -> Unit) {
    TangemRow(
        verticalAlignment = TangemRowVerticalAlignment.Center,
        startSlot = {
            CurrencyIcon(
                state = data?.tokenIconState ?: CurrencyIconState.Loading,
                shouldDisplayNetwork = false,
                iconSize = 40.dp,
            )
        },
        titleSlot = {
            if (data != null) {
                Text(
                    text = data.tokenName.resolveReference(),
                    style = TangemTheme.typography3.body.medium,
                    color = TangemTheme.colors3.text.primary,
                    maxLines = 1,
                    modifier = Modifier.testTag(SendScreenTestTags.TOKEN_NAME),
                )
            } else {
                TextShimmer(style = TangemTheme.typography3.body.medium, modifier = Modifier.width(56.dp))
            }
        },
        subtitleSlot = {
            if (data != null) {
                Text(
                    text = data.availableBalanceCrypto.resolveReference(),
                    style = TangemTheme.typography3.caption.medium,
                    color = TangemTheme.colors3.text.secondary,
                    maxLines = 1,
                    modifier = Modifier.testTag(SendScreenTestTags.PRIMARY_AMOUNT),
                )
            } else {
                TextShimmer(style = TangemTheme.typography3.caption.medium, modifier = Modifier.width(72.dp))
            }
        },
        endSlot = {
            TangemButton(
                variant = TangemButton.Variant.Secondary,
                size = TangemButton.Size.X8,
                text = resourceReference(R.string.send_max_amount),
                onClick = onMaxClick,
                modifier = Modifier.testTag(SendScreenTestTags.MAX_BUTTON),
            )
        },
    )
}