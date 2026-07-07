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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tangem.common.ui.account.AccountTitle
import com.tangem.core.ui.R
import com.tangem.core.ui.components.RectangleShimmer
import com.tangem.core.ui.components.SpacerH4
import com.tangem.core.ui.components.SpacerW16
import com.tangem.core.ui.components.TextShimmer
import com.tangem.core.ui.components.buttons.SecondarySmallButton
import com.tangem.core.ui.components.buttons.SmallButtonConfig
import com.tangem.core.ui.components.buttons.common.TangemButtonIconPosition
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.test.SwapTokenScreenTestTags
import com.tangem.feature.swap.domain.models.ui.PriceImpact
import com.tangem.feature.swap.models.SwapCardState
import com.tangem.feature.swap.models.TransactionCardType
import com.tangem.feature.swap.ui.preview.SwapTransactionCardPreview

@Composable
internal fun TransactionCardSimple(
    priceImpact: PriceImpact,
    swapCardState: SwapCardState,
    onSelectTokenClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cardTag = when (swapCardState.type) {
        is TransactionCardType.Inputtable -> SwapTokenScreenTestTags.SWAP_CARD
        is TransactionCardType.ReadOnly -> SwapTokenScreenTestTags.RECEIVE_CARD
    }

    when (swapCardState) {
        is SwapCardState.Empty -> SimpleTransactionCardEmpty(
            cardState = swapCardState,
            onChangeTokenClick = onSelectTokenClick,
            modifier = modifier.testTag(cardTag),
        )
        is SwapCardState.SwapCardData -> SimpleTransactionCardData(
            cardState = swapCardState,
            priceImpact = priceImpact,
            onChangeTokenClick = onSelectTokenClick,
            modifier = modifier.testTag(cardTag),
        )
        is SwapCardState.Loading -> SimpleTransactionCardLoading(modifier = modifier.testTag(cardTag))
    }
}

@Composable
private fun SimpleTransactionCardData(
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
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start,
        ) {
            SimpleHeader(
                balance = cardState.balance,
                isBalanceHidden = cardState.isBalanceHidden,
                type = cardState.type,
            )

            SimpleContent(
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
private fun SimpleTransactionCardEmpty(
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
                        .testTag(SwapTokenScreenTestTags.RECEIVE_FIAT_AMOUNT),
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
private fun SimpleTransactionCardLoading(modifier: Modifier = Modifier) {
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
        Column(modifier = Modifier.fillMaxWidth()) {
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
                        .defaultMinSize(minHeight = 20.dp, minWidth = 40.dp)
                        .testTag(SwapTokenScreenTestTags.RECEIVE_FIAT_AMOUNT),
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
private fun SimpleHeader(
    type: TransactionCardType,
    balance: TextReference,
    isBalanceHidden: Boolean,
    modifier: Modifier = Modifier,
) {
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
        if (balance != TextReference.EMPTY) {
            AnimatedContent(targetState = balance, label = "") { balanceText ->
                Text(
                    text = balanceText.resolveReference().orMaskWithStars(isBalanceHidden),
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
private fun SimpleContent(cardData: SwapCardState.SwapCardData, priceImpact: PriceImpact) {
    val type = cardData.type
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
            modifier = Modifier.padding(end = TangemTheme.dimens.spacing92),
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
            // Keep the same 20dp slot as Detailed (where fiat/shimmer lives)
            // so that Token (BottomEnd) does not shift when switching modes.
            Box(modifier = Modifier.defaultMinSize(minHeight = TangemTheme.dimens.size20)) {
                if (type is TransactionCardType.ReadOnly && type.shouldShowWarning) {
                    WarningIcon(priceImpact = priceImpact, onClick = type.onWarningClick)
                }
            }
        }
    }
}

@Composable
private fun WarningIcon(priceImpact: PriceImpact, onClick: (() -> Unit)?) {
    IconButton(
        onClick = { onClick?.invoke() },
        modifier = Modifier.size(TangemTheme.dimens.size20),
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_information_24),
            contentDescription = null,
            tint = when (priceImpact.type) {
                PriceImpact.Type.HIGH -> TangemTheme.colors.text.warning
                PriceImpact.Type.MEDIUM -> TangemTheme.colors.text.attention
                else -> TangemTheme.colors.text.tertiary
            },
            modifier = Modifier.testTag(SwapTokenScreenTestTags.RECEIVE_FIAT_AMOUNT_INFORMATION_ICON),
        )
    }
}

// region Preview
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TransactionCardSimple_Preview(@PreviewParameter(SimplePreviewProvider::class) params: SwapCardState) {
    TangemThemePreview {
        TransactionCardSimple(
            priceImpact = PriceImpact.Empty,
            swapCardState = params,
            onSelectTokenClick = {},
            modifier = Modifier,
        )
    }
}

private class SimplePreviewProvider : PreviewParameterProvider<SwapCardState> {
    override val values: Sequence<SwapCardState> = sequenceOf(
        SwapTransactionCardPreview.sendCard,
        SwapTransactionCardPreview.receiveCard,
        SwapTransactionCardPreview.emptyReadOnlyCard,
        SwapTransactionCardPreview.emptyInputtableCard,
        SwapTransactionCardPreview.loadingCard,
    )
}
// endregion