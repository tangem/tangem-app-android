package com.tangem.feature.swap.ui

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tangem.common.ui.account.AccountTitle
import com.tangem.core.ui.R
import com.tangem.core.ui.components.*
import com.tangem.core.ui.components.buttons.SecondarySmallButton
import com.tangem.core.ui.components.buttons.SmallButtonConfig
import com.tangem.core.ui.components.buttons.common.TangemButtonIconPosition
import com.tangem.core.ui.components.currency.icon.CurrencyIcon
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.components.fields.AmountTextField
import com.tangem.core.ui.components.fields.AmountTextFieldColors
import com.tangem.core.ui.components.fields.visualtransformations.AmountVisualTransformation
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_arrow_swap_horizontal_16
import com.tangem.core.ui.test.SwapTokenScreenTestTags
import com.tangem.feature.swap.domain.models.ui.PriceImpact
import com.tangem.feature.swap.models.SwapCardState
import com.tangem.feature.swap.models.TransactionCardType
import com.tangem.feature.swap.ui.preview.SwapTransactionCardPreview

@Composable
internal fun TransactionCard(
    priceImpact: PriceImpact,
    swapCardState: SwapCardState,
    onSelectTokenClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cardTag = when (swapCardState.type) {
        is TransactionCardType.Inputtable ->
            SwapTokenScreenTestTags.SWAP_CARD
        is TransactionCardType.ReadOnly ->
            SwapTokenScreenTestTags.RECEIVE_CARD
    }

    when (swapCardState) {
        is SwapCardState.Empty -> {
            TransactionCardEmpty(
                cardState = swapCardState,
                onChangeTokenClick = onSelectTokenClick,
                modifier = modifier.testTag(cardTag),
            )
        }
        is SwapCardState.SwapCardData -> {
            TransactionCardData(
                cardState = swapCardState,
                priceImpact = priceImpact,
                onChangeTokenClick = onSelectTokenClick,
                modifier = modifier.testTag(cardTag),
            )
        }
        is SwapCardState.Loading -> TransactionCardLoading(
            modifier = modifier.testTag(cardTag),
        )
    }
}

@Composable
private fun TransactionCardData(
    cardState: SwapCardState.SwapCardData,
    priceImpact: PriceImpact,
    modifier: Modifier = Modifier,
    onChangeTokenClick: (() -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .background(
                shape = RoundedCornerShape(TangemTheme.dimens.radius16),
                color = TangemTheme.colors.background.primary,
            )
            .fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start,
        ) {
            Header(
                balance = stringResourceSafe(
                    R.string.common_balance,
                    cardState.balance,
                ).orMaskWithStars(cardState.isBalanceHidden),
                type = cardState.type,
            )

            Content(
                cardData = cardState,
                priceImpact = priceImpact,
            )
        }

        Box(modifier = Modifier.align(Alignment.BottomEnd)) {
            Token(
                currencyIconState = cardState.currencyIconState,
                tokenSymbol = cardState.tokenSymbol,
            )
        }

        if (onChangeTokenClick != null) {
            Box(modifier = Modifier.align(Alignment.CenterEnd)) {
                ChangeTokenSelector()
            }
            Box(
                Modifier
                    .align(Alignment.CenterEnd)
                    .height(TangemTheme.dimens.size116)
                    .width(TangemTheme.dimens.size102)
                    .clickable(
                        indication = ripple(bounded = false),
                        interactionSource = remember { MutableInteractionSource() },
                    ) { onChangeTokenClick() },
            )
        }
    }
}

@Composable
private fun TransactionCardEmpty(
    cardState: SwapCardState.Empty,
    modifier: Modifier = Modifier,
    onChangeTokenClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .background(
                shape = RoundedCornerShape(TangemTheme.dimens.radius12),
                color = TangemTheme.colors.background.primary,
            )
            .padding(
                top = 12.dp,
                start = 12.dp,
                end = 12.dp,
                bottom = 16.dp,
            )
            .fillMaxWidth(),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        AccountTitle(
            accountTitleUM = cardState.type.accountTitleUM,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = cardState.amountField?.value.orEmpty(),
                    color = TangemTheme.colors.text.disabled,
                    style = TangemTheme.typography.h2,
                    autoSize = TextAutoSize.StepBased(
                        minFontSize = 16.sp,
                        maxFontSize = TangemTheme.typography.h2.fontSize,
                    ),
                    maxLines = 1,
                    modifier = Modifier.testTag(SwapTokenScreenTestTags.SWAP_TEXT_FIELD),
                )
                Text(
                    text = cardState.amountEquivalent.resolveAnnotatedReference(),
                    color = TangemTheme.colors.text.tertiary,
                    style = TangemTheme.typography.body2,
                    modifier = Modifier
                        .defaultMinSize(minHeight = TangemTheme.dimens.size20)
                        .testTag(SwapTokenScreenTestTags.SWAP_FIAT_AMOUNT),
                )
            }
            SecondarySmallButton(
                config = SmallButtonConfig(
                    text = resourceReference(R.string.common_choose_token),
                    icon = TangemButtonIconPosition.End(R.drawable.ic_chevron_24),
                    onClick = onChangeTokenClick,
                ),
            )
        }
    }
}

@Composable
private fun TransactionCardLoading(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(
                shape = RoundedCornerShape(TangemTheme.dimens.radius12),
                color = TangemTheme.colors.background.primary,
            )
            .padding(
                top = 12.dp,
                start = 12.dp,
                end = 12.dp,
                bottom = 16.dp,
            )
            .fillMaxWidth(),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            TextShimmer(
                text = stringResourceSafe(R.string.swapping_to_title),
                style = TangemTheme.typography.subtitle2,
            )
            TextShimmer(
                style = TangemTheme.typography.body2,
                modifier = Modifier
                    .testTag(SwapTokenScreenTestTags.BALANCE)
                    .width(60.dp),
            )
        }
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                TextShimmer(
                    style = TangemTheme.typography.h2,
                    modifier = Modifier
                        .width(100.dp)
                        .testTag(SwapTokenScreenTestTags.SWAP_TEXT_FIELD),
                )
                TextShimmer(
                    style = TangemTheme.typography.body2,
                    modifier = Modifier
                        .defaultMinSize(
                            minHeight = 20.dp,
                            minWidth = 40.dp,
                        )
                        .testTag(SwapTokenScreenTestTags.SWAP_FIAT_AMOUNT),
                )
            }
            SecondarySmallButton(
                config = SmallButtonConfig(
                    text = resourceReference(R.string.common_choose_token),
                    icon = TangemButtonIconPosition.End(R.drawable.ic_chevron_24),
                    isEnabled = false,
                    onClick = {},
                ),
            )
        }
    }
}

@Composable
private fun Header(type: TransactionCardType, balance: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                bottom = TangemTheme.dimens.spacing8,
                top = TangemTheme.dimens.spacing14,
                start = TangemTheme.dimens.spacing12,
                end = TangemTheme.dimens.spacing12,
            )
            .testTag(SwapTokenScreenTestTags.SWAP_BLOCK_HEADER),
    ) {
        val titleColor = if (type.inputError is TransactionCardType.InputError.Empty) {
            TangemTheme.colors.text.tertiary
        } else {
            TangemTheme.colors.text.warning
        }
        AccountTitle(
            accountTitleUM = type.accountTitleUM,
            textColor = titleColor,
        )
        SpacerW16()
        if (balance.isNotBlank()) {
            AnimatedContent(
                targetState = balance,
                label = "",
            ) { balanceText ->
                Text(
                    text = balanceText,
                    color = TangemTheme.colors.text.tertiary,
                    style = TangemTheme.typography.body2,
                    modifier = Modifier.testTag(SwapTokenScreenTestTags.BALANCE),
                )
            }
        } else {
            RectangleShimmer(
                modifier = Modifier
                    .width(TangemTheme.dimens.size80)
                    .height(TangemTheme.dimens.size12),
                radius = TangemTheme.dimens.radius3,
            )
        }
    }
}

@Suppress("LongMethod")
@Composable
private fun Content(cardData: SwapCardState.SwapCardData, priceImpact: PriceImpact) {
    val type = cardData.type
    val amountEquivalent = cardData.amountEquivalent
    Row(
        modifier = Modifier
            .padding(
                start = TangemTheme.dimens.spacing12,
                bottom = TangemTheme.dimens.spacing16,
            ),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Top,
    ) {
        Column(
            modifier = Modifier
                .padding(
                    end = TangemTheme.dimens.spacing92,
                ),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start,
        ) {
            val sumTextModifier = Modifier.defaultMinSize(minHeight = TangemTheme.dimens.size32)
            when (type) {
                is TransactionCardType.ReadOnly -> {
                    val value = cardData.amountField?.value
                    if (value != null) {
                        Text(
                            text = value,
                            color = TangemTheme.colors.text.primary1,
                            style = TangemTheme.typography.h2,
                            autoSize = TextAutoSize.StepBased(
                                minFontSize = 16.sp,
                                maxFontSize = TangemTheme.typography.h2.fontSize,
                            ),
                            maxLines = 1,
                            modifier = sumTextModifier.testTag(SwapTokenScreenTestTags.RECEIVE_TEXT_FIELD),
                        )
                    } else {
                        RectangleShimmer(
                            modifier = Modifier
                                .padding(vertical = TangemTheme.dimens.spacing4)
                                .width(TangemTheme.dimens.size102)
                                .height(TangemTheme.dimens.size24),
                        )
                    }
                }
                is TransactionCardType.Inputtable -> {
                    AmountInputField(cardData = cardData, type = type, modifier = sumTextModifier)
                }
            }

            SpacerH4()

            if (amountEquivalent != null) {
                when (type) {
                    is TransactionCardType.ReadOnly -> ReceiveAmountEquivalent(
                        amountEquivalent = amountEquivalent,
                        type = type,
                        priceImpact = priceImpact,
                    )
                    is TransactionCardType.Inputtable -> SwapAmountEquivalent(
                        amountEquivalent = amountEquivalent,
                        isFiatValue = cardData.amountField?.isFiatValue == true,
                        isFiatUnavailable = cardData.amountField?.isFiatUnavailable == true,
                        onCurrencyChange = type.onCurrencyChange,
                    )
                }
            } else {
                RectangleShimmer(
                    modifier = Modifier
                        .padding(vertical = TangemTheme.dimens.spacing4)
                        .width(TangemTheme.dimens.size40)
                        .height(TangemTheme.dimens.size12)
                        .testTag(SwapTokenScreenTestTags.RECEIVE_AMOUNT_SHIMMER),
                    radius = TangemTheme.dimens.radius3,
                )
            }
        }
    }
}

@Composable
internal fun AmountInputField(
    cardData: SwapCardState.SwapCardData,
    type: TransactionCardType.Inputtable,
    modifier: Modifier = Modifier,
) {
    val amountField = cardData.amountField ?: return
    val focusRequester = remember { FocusRequester() }
    val activeAmount = if (amountField.isFiatValue) {
        amountField.fiatAmount
    } else {
        amountField.cryptoAmount
    }

    AmountTextField(
        value = amountField.value,
        decimals = activeAmount.decimals,
        onValueChange = amountField.onValueChange,
        textStyle = TangemTheme.typography.h2.copy(color = TangemTheme.colors.text.primary1),
        isEnabled = type.isEnabled,
        isAutoResize = true,
        visualTransformation = AmountVisualTransformation(
            currencyCode = cardData.appCurrency.code.takeIf { amountField.isFiatValue },
            symbol = activeAmount.currencySymbol.takeIf { amountField.isFiatValue },
            decimals = activeAmount.decimals,
            symbolColor = TangemTheme.colors.text.disabled,
        ),
        colors = AmountTextFieldColors(
            textColor = TangemTheme.colors.text.primary1,
            disabledTextColor = TangemTheme.colors.text.disabled,
            backgroundColor = TangemTheme.colors.background.primary,
        ),
        isValuePasted = amountField.isValuePasted,
        onValuePastedTriggerDismiss = amountField.onValuePastedTriggerDismiss,
        keyboardOptions = amountField.keyboardOptions,
        keyboardActions = amountField.keyboardActions,
        modifier = modifier
            .focusRequester(focusRequester)
            .onFocusChanged { type.onFocusChanged(it.hasFocus) }
            .testTag(SwapTokenScreenTestTags.SWAP_TEXT_FIELD),
    )

    LaunchedEffect(type.isEnabled) {
        if (type.isEnabled) {
            focusRequester.requestFocus()
        } else {
            focusRequester.freeFocus()
        }
    }
}

@Composable
private fun ReceiveAmountEquivalent(
    amountEquivalent: TextReference,
    type: TransactionCardType.ReadOnly,
    priceImpact: PriceImpact,
) {
    Row(
        modifier = Modifier.defaultMinSize(minHeight = TangemTheme.dimens.size20),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AnimatedContent(targetState = amountEquivalent, label = "") { amount ->
            Text(
                text = amount.resolveAnnotatedReference(),
                color = TangemTheme.colors.text.tertiary,
                style = TangemTheme.typography.body2,
                modifier = Modifier.testTag(SwapTokenScreenTestTags.RECEIVE_FIAT_AMOUNT),
            )
        }
        if (type.shouldShowWarning) {
            SpacerW4()
            IconButton(
                onClick = { type.onWarningClick?.invoke() },
                modifier = Modifier.size(size = TangemTheme.dimens.size20),
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_information_24),
                    contentDescription = null,
                    tint = when (priceImpact.type) {
                        PriceImpact.Type.HIGH -> TangemTheme.colors.text.warning
                        PriceImpact.Type.MEDIUM -> TangemTheme.colors.text.attention
                        else -> TangemTheme.colors.text.tertiary
                    },
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .testTag(SwapTokenScreenTestTags.RECEIVE_FIAT_AMOUNT_INFORMATION_ICON),
                )
            }
        }
    }
}

private const val CURRENCY_TOGGLE_ROTATED_DEGREE = 180f
private const val CURRENCY_TOGGLE_INITIAL_DEGREE = 0f

@Composable
private fun SwapAmountEquivalent(
    amountEquivalent: TextReference,
    isFiatValue: Boolean,
    isFiatUnavailable: Boolean,
    onCurrencyChange: (Boolean) -> Unit,
) {
    val rowModifier = Modifier
        .defaultMinSize(minHeight = TangemTheme.dimens.size20)
        .then(
            if (isFiatUnavailable) {
                Modifier
            } else {
                Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { onCurrencyChange(!isFiatValue) },
                )
            },
        )
    Row(
        modifier = rowModifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing4),
    ) {
        if (!isFiatUnavailable) {
            val iconRotation by animateFloatAsState(
                targetValue = if (isFiatValue) CURRENCY_TOGGLE_ROTATED_DEGREE else CURRENCY_TOGGLE_INITIAL_DEGREE,
                label = "Currency toggle icon rotation",
            )
            Icon(
                imageVector = Icons.ic_arrow_swap_horizontal_16,
                contentDescription = null,
                tint = TangemTheme.colors3.icon.tertiary,
                modifier = Modifier
                    .size(TangemTheme.dimens.size16)
                    .graphicsLayer { rotationZ = iconRotation },
            )
        }
        AnimatedContent(targetState = amountEquivalent, label = "") { amount ->
            Text(
                text = amount.resolveAnnotatedReference(),
                color = TangemTheme.colors.text.tertiary,
                style = TangemTheme.typography.body2,
                modifier = Modifier.testTag(SwapTokenScreenTestTags.SWAP_FIAT_AMOUNT),
            )
        }
    }
}

@Suppress("MagicNumber")
@Composable
fun Token(currencyIconState: CurrencyIconState, tokenSymbol: TextReference) {
    Column(
        modifier = Modifier
            .padding(
                end = TangemTheme.dimens.spacing12,
                bottom = TangemTheme.dimens.spacing12,
            )
            .testTag(SwapTokenScreenTestTags.TOKEN),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.End,
    ) {
        CurrencyIcon(
            state = currencyIconState,
            modifier = Modifier.padding(end = TangemTheme.dimens.spacing16),
        )
        SpacerH4()
        Text(
            text = tokenSymbol.resolveReference(),
            color = TangemTheme.colors.text.primary1,
            maxLines = 1,
            style = TangemTheme.typography.subtitle2,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .defaultMinSize(minWidth = TangemTheme.dimens.size80)
                .testTag(SwapTokenScreenTestTags.TOKEN_SYMBOL),
        )
    }
}

@Composable
fun ChangeTokenSelector() {
    Box(
        modifier = Modifier
            .padding(
                top = TangemTheme.dimens.spacing12,
                start = TangemTheme.dimens.spacing24,
                end = TangemTheme.dimens.spacing12,
            ),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Icon(
            modifier = Modifier
                .size(TangemTheme.dimens.size20)
                .testTag(SwapTokenScreenTestTags.SELECT_TOKEN_ICON),
            painter = painterResource(id = R.drawable.ic_chevron_24),
            tint = TangemTheme.colors.icon.secondary,
            contentDescription = null,
        )
    }
}

// region Preview
@Composable
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun TransactionCard_Preview(@PreviewParameter(PreviewProvider::class) params: SwapCardState) {
    TangemThemePreview {
        TransactionCard(
            priceImpact = PriceImpact.Empty,
            swapCardState = params,
            onSelectTokenClick = {},
            modifier = Modifier,
        )
    }
}

private class PreviewProvider : PreviewParameterProvider<SwapCardState> {
    override val values: Sequence<SwapCardState>
        get() = sequenceOf(
            SwapTransactionCardPreview.sendCard,
            SwapTransactionCardPreview.receiveCard,
            SwapTransactionCardPreview.emptyReadOnlyCard,
            SwapTransactionCardPreview.emptyInputtableCard,
            SwapTransactionCardPreview.loadingCard,
        )
}
// endregion