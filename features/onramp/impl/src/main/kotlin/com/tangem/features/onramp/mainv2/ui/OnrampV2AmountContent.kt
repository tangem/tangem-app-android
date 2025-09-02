package com.tangem.features.onramp.mainv2.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.tangem.common.ui.amountScreen.models.AmountFieldModel
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.TextShimmer
import com.tangem.core.ui.components.fields.AmountTextField
import com.tangem.core.ui.components.fields.visualtransformations.AmountVisualTransformation
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.test.BuyTokenDetailsScreenTestTags
import com.tangem.core.ui.utils.rememberDecimalFormat
import com.tangem.features.onramp.impl.R
import com.tangem.features.onramp.mainv2.entity.OnrampNewAmountSecondaryFieldUM
import com.tangem.features.onramp.mainv2.entity.OnrampNewCurrencyUM
import com.tangem.features.onramp.mainv2.entity.OnrampV2MainComponentUM

@Composable
internal fun OnrampV2AmountContent(state: OnrampV2MainComponentUM.Content, modifier: Modifier = Modifier) {
    val padding = remember(state.offersBlockState.isBlockVisible) {
        if (state.offersBlockState.isBlockVisible) {
            22.dp
        } else {
            46.dp
        }
    }

    Column(
        modifier = modifier
            .background(
                color = TangemTheme.colors.background.action,
                shape = RoundedCornerShape(size = TangemTheme.dimens.radius16),
            )
            .padding(vertical = padding)
            .animateContentSize(animationSpec = tween(durationMillis = 300)),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        OnrampHeaderTitle()

        SpacerH(12.dp)

        OnrampAmountField(
            amountField = state.amountBlockState.amountFieldModel,
            currencyCode = state.amountBlockState.currencyUM.code,
        )

        SpacerH(8.dp)

        OnrampAmountSecondary(state = state.amountBlockState.secondaryFieldModel)

        SpacerH(20.dp)

        OnrampCurrencyIcon(currencyUM = state.amountBlockState.currencyUM)
    }
}

@Composable
private fun OnrampHeaderTitle() {
    Text(
        text = stringResourceSafe(R.string.onramp_you_will_pay_title),
        style = TangemTheme.typography.subtitle2,
        color = TangemTheme.colors.text.tertiary,
    )
}

@Composable
private fun OnrampAmountField(amountField: AmountFieldModel, currencyCode: String) {
    val decimalFormat = rememberDecimalFormat()
    val requester = remember { FocusRequester() }
    AmountTextField(
        value = amountField.fiatValue,
        decimals = amountField.fiatAmount.decimals,
        visualTransformation = AmountVisualTransformation(
            decimals = amountField.fiatAmount.decimals,
            symbol = currencyCode,
            currencyCode = currencyCode,
            decimalFormat = decimalFormat,
            symbolColor = TangemTheme.colors.text.disabled,
        ),
        onValueChange = amountField.onValueChange,
        keyboardOptions = amountField.keyboardOptions,
        keyboardActions = amountField.keyboardActions,
        textStyle = TangemTheme.typography.head.copy(
            color = TangemTheme.colors.text.primary1,
            textAlign = TextAlign.Center,
        ),
        isEnabled = !amountField.isError,
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
            .requiredHeightIn(min = TangemTheme.dimens.size32)
            .testTag(BuyTokenDetailsScreenTestTags.FIAT_AMOUNT_TEXT_FIELD),
    )

    LaunchedEffect(key1 = Unit) {
        requester.requestFocus()
    }
}

@Composable
private fun OnrampAmountSecondary(state: OnrampNewAmountSecondaryFieldUM) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = TangemTheme.dimens.spacing8,
                start = TangemTheme.dimens.spacing12,
                end = TangemTheme.dimens.spacing12,
            )
            .testTag(BuyTokenDetailsScreenTestTags.TOKEN_AMOUNT),
        contentAlignment = Alignment.Center,
    ) {
        when (state) {
            is OnrampNewAmountSecondaryFieldUM.Content -> Text(
                text = state.amount.resolveReference(),
                style = TangemTheme.typography.caption2.copy(textDirection = TextDirection.ContentOrLtr),
                color = TangemTheme.colors.text.tertiary,
                textAlign = TextAlign.Center,
            )
            is OnrampNewAmountSecondaryFieldUM.Error -> Text(
                text = state.error.resolveReference(),
                color = TangemTheme.colors.text.warning,
                style = TangemTheme.typography.caption2,
                textAlign = TextAlign.Center,
            )
            is OnrampNewAmountSecondaryFieldUM.Loading -> TextShimmer(
                style = TangemTheme.typography.caption2,
                modifier = Modifier.width(TangemTheme.dimens.size62),
            )
        }
    }
}

@Composable
private fun OnrampCurrencyIcon(currencyUM: OnrampNewCurrencyUM, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(TangemTheme.colors.button.secondary)
            .clickable(onClick = currencyUM.onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        AsyncImage(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .testTag(BuyTokenDetailsScreenTestTags.FIAT_CURRENCY_ICON),
            model = currencyUM.iconUrl,
            contentDescription = null,
        )
        Text(
            text = currencyUM.code,
            color = TangemTheme.colors.text.primary1,
            style = TangemTheme.typography.body2.copy(fontWeight = FontWeight.SemiBold),
            textAlign = TextAlign.Center,
        )
        Icon(
            modifier = Modifier
                .size(TangemTheme.dimens.size16)
                .testTag(BuyTokenDetailsScreenTestTags.EXPAND_FIAT_LIST_BUTTON),
            painter = painterResource(id = R.drawable.ic_chevron_24),
            tint = TangemTheme.colors.icon.informative,
            contentDescription = null,
        )
    }
}