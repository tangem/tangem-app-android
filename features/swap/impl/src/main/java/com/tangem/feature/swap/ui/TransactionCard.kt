package com.tangem.feature.swap.ui

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.TextFieldValue
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
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
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
                type = cardState.type,
                amountEquivalent = cardState.amountEquivalent,
                textFieldValue = cardState.amountTextFieldValue,
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
                    text = cardState.amountTextFieldValue?.text.orEmpty(),
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
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
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
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                bottom = TangemTheme.dimens.spacing8,
                top = TangemTheme.dimens.spacing14,
                start = TangemTheme.dimens.spacing12,
                end = TangemTheme.dimens.spacing12,
            )
            .testTag(SwapTokenScreenTestTags.SWAP_BLOCK_HEADER),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
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
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .testTag(SwapTokenScreenTestTags.BALANCE),
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
private fun Content(
    type: TransactionCardType,
    amountEquivalent: TextReference?,
    priceImpact: PriceImpact,
    textFieldValue: TextFieldValue?,
) {
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
                    if (textFieldValue != null) {
                        Text(
                            text = textFieldValue.text,
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
                    val focusRequester = remember { FocusRequester() }

                    AutoSizeTextField(
                        modifier = sumTextModifier.testTag(SwapTokenScreenTestTags.SWAP_TEXT_FIELD),
                        focusRequester = focusRequester,
                        textFieldValue = textFieldValue ?: TextFieldValue(),
                        isEnabled = type.isEnabled,
                        onAmountChange = { type.onAmountChanged(it) },
                        onFocusChange = type.onFocusChanged,
                    )

                    LaunchedEffect(Unit) {
                        focusRequester.requestFocus()
                    }
                }
            }

            SpacerH4()

            if (amountEquivalent != null) {
                if (type is TransactionCardType.ReadOnly) {
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
                                onClick = {
                                    type.onWarningClick?.invoke()
                                },
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
                } else {
                    AnimatedContent(targetState = amountEquivalent, label = "") { amount ->
                        Text(
                            text = amount.resolveAnnotatedReference(),
                            color = TangemTheme.colors.text.tertiary,
                            style = TangemTheme.typography.body2,
                            modifier = Modifier
                                .defaultMinSize(minHeight = TangemTheme.dimens.size20)
                                .testTag(SwapTokenScreenTestTags.SWAP_FIAT_AMOUNT),
                        )
                    }
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