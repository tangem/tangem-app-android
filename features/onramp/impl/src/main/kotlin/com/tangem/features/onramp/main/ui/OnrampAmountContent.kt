package com.tangem.features.onramp.main.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.tangem.common.ui.amountScreen.models.AmountFieldModel
import com.tangem.core.ui.components.TextShimmer
import com.tangem.core.ui.components.fields.AmountTextField
import com.tangem.core.ui.components.fields.visualtransformations.AmountVisualTransformation
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.utils.rememberDecimalFormat
import com.tangem.features.onramp.impl.R
import com.tangem.features.onramp.main.entity.OnrampAmountBlockUM
import com.tangem.features.onramp.main.entity.OnrampAmountSecondaryFieldUM
import com.tangem.features.onramp.main.entity.OnrampCurrencyUM

@Composable
internal fun OnrampAmountContent(state: OnrampAmountBlockUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(shape = RoundedCornerShape(size = TangemTheme.dimens.radius16))
            .background(TangemTheme.colors.background.action)
            .padding(vertical = TangemTheme.dimens.spacing28),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        OnrampCurrencyIcon(currencyUM = state.currencyUM)
        OnrampAmountField(
            amountField = state.amountFieldModel,
            isLoading = state.secondaryFieldModel is OnrampAmountSecondaryFieldUM.Loading,
        )
        OnrampAmountSecondary(state = state.secondaryFieldModel)
    }
}

@Composable
private fun OnrampAmountField(amountField: AmountFieldModel, isLoading: Boolean) {
    val decimalFormat = rememberDecimalFormat()
    val requester = remember { FocusRequester() }
    AmountTextField(
        value = amountField.fiatValue,
        decimals = amountField.fiatAmount.decimals,
        visualTransformation = AmountVisualTransformation(
            decimals = amountField.fiatAmount.decimals,
            symbol = amountField.fiatAmount.currencySymbol,
            currencyCode = amountField.fiatAmount.currencySymbol,
            decimalFormat = decimalFormat,
        ),
        onValueChange = amountField.onValueChange,
        keyboardOptions = amountField.keyboardOptions,
        keyboardActions = amountField.keyboardActions,
        textStyle = TangemTheme.typography.h2.copy(
            color = TangemTheme.colors.text.primary1,
            textAlign = TextAlign.Center,
        ),
        isEnabled = !amountField.isError && !isLoading,
        isAutoResize = true,
        isValuePasted = amountField.isValuePasted,
        onValuePastedTriggerDismiss = amountField.onValuePastedTriggerDismiss,
        modifier = Modifier
            .focusRequester(requester)
            .padding(
                top = TangemTheme.dimens.spacing24,
                start = TangemTheme.dimens.spacing12,
                end = TangemTheme.dimens.spacing12,
            )
            .requiredHeightIn(min = TangemTheme.dimens.size32),
    )

    LaunchedEffect(key1 = Unit) {
        requester.requestFocus()
    }
}

@Composable
private fun OnrampAmountSecondary(state: OnrampAmountSecondaryFieldUM) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = TangemTheme.dimens.spacing8,
                start = TangemTheme.dimens.spacing12,
                end = TangemTheme.dimens.spacing12,
            ),
        contentAlignment = Alignment.Center,
    ) {
        when (state) {
            is OnrampAmountSecondaryFieldUM.Content -> Text(
                text = state.amount.resolveReference(),
                style = TangemTheme.typography.caption2.copy(textDirection = TextDirection.ContentOrLtr),
                color = TangemTheme.colors.text.tertiary,
                textAlign = TextAlign.Center,
            )
            is OnrampAmountSecondaryFieldUM.Error -> Text(
                text = state.error.resolveReference(),
                color = TangemTheme.colors.text.warning,
                style = TangemTheme.typography.caption2,
                textAlign = TextAlign.Center,
            )
            is OnrampAmountSecondaryFieldUM.Loading -> TextShimmer(
                style = TangemTheme.typography.caption2,
                modifier = Modifier.width(TangemTheme.dimens.size62),
            )
        }
    }
}

@Composable
private fun OnrampCurrencyIcon(currencyUM: OnrampCurrencyUM, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = currencyUM.onClick)
            .padding(start = TangemTheme.dimens.spacing24),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing8),
    ) {
        AsyncImage(
            modifier = Modifier
                .size(TangemTheme.dimens.size40)
                .clip(CircleShape),
            model = currencyUM.iconUrl,
            contentDescription = null,
        )
        Icon(
            modifier = Modifier.size(TangemTheme.dimens.size16),
            painter = painterResource(id = R.drawable.ic_chevron_24),
            tint = TangemTheme.colors.icon.informative,
            contentDescription = null,
        )
    }
}