package com.tangem.features.send.impl.presentation.ui.fee

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.core.ui.components.RectangleShimmer
import com.tangem.core.ui.components.SpacerWMax
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.utils.BigDecimalFormatter
import com.tangem.features.send.impl.R
import com.tangem.features.send.impl.presentation.state.fee.FeeSelectorState
import com.tangem.features.send.impl.presentation.state.fee.FeeType
import com.tangem.features.send.impl.presentation.ui.common.FooterContainer
import com.tangem.features.send.impl.presentation.viewmodel.SendClickIntents

private val DEFAULT_FEE_OPTIONS = listOf(
    R.string.common_fee_selector_option_slow to R.drawable.ic_tortoise_24,
    R.string.common_fee_selector_option_market to R.drawable.ic_bird_24,
    R.string.common_fee_selector_option_fast to R.drawable.ic_hare_24,
)

@Suppress("LongMethod")
@Composable
internal fun SendSpeedSelector(
    state: FeeSelectorState,
    clickIntents: SendClickIntents,
    modifier: Modifier = Modifier,
) {
    FooterContainer(
        footer = stringResource(R.string.common_fee_selector_footer),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(TangemTheme.shapes.roundedCornersXMedium)
                .background(TangemTheme.colors.background.action),
        ) {
            when (state) {
                FeeSelectorState.Error -> {
                    SendSpeedSelectorItemError()
                }
                FeeSelectorState.Loading -> {
                    SendSpeedSelectorItemLoading()
                }
                is FeeSelectorState.Content -> {
                    when (state.fees) {
                        is TransactionFee.Choosable -> {
                            val isSelected = state.selectedFee
                            SendSpeedSelectorItem(
                                titleRes = R.string.common_fee_selector_option_slow,
                                iconRes = R.drawable.ic_tortoise_24,
                                amount = TextReference.Str(state.fees.minimum.amount.value.toString()),
                                symbol = TextReference.Str(state.fees.minimum.amount.currencySymbol),
                                isSelected = isSelected == FeeType.SLOW,
                                onSelect = { clickIntents.onFeeSelectorClick(FeeType.SLOW) },
                            )
                            SendSpeedSelectorItem(
                                titleRes = R.string.common_fee_selector_option_market,
                                iconRes = R.drawable.ic_bird_24,
                                amount = TextReference.Str(state.fees.normal.amount.value.toString()),
                                symbol = TextReference.Str(state.fees.normal.amount.currencySymbol),
                                isSelected = isSelected == FeeType.MARKET,
                                onSelect = { clickIntents.onFeeSelectorClick(FeeType.MARKET) },
                            )
                            SendSpeedSelectorItem(
                                titleRes = R.string.common_fee_selector_option_fast,
                                iconRes = R.drawable.ic_hare_24,
                                amount = TextReference.Str(state.fees.priority.amount.value.toString()),
                                symbol = TextReference.Str(state.fees.priority.amount.currencySymbol),
                                isSelected = isSelected == FeeType.FAST,
                                onSelect = { clickIntents.onFeeSelectorClick(FeeType.FAST) },
                                showDivider = state.fees.normal is Fee.Ethereum,
                            )
                            AnimatedVisibility(
                                visible = state.fees.normal is Fee.Ethereum,
                                label = "Custom fee appearance animation",
                            ) {
                                SendSpeedSelectorItem(
                                    titleRes = R.string.common_fee_selector_option_custom,
                                    iconRes = R.drawable.ic_edit_24,
                                    isSelected = isSelected == FeeType.CUSTOM,
                                    onSelect = { clickIntents.onFeeSelectorClick(FeeType.CUSTOM) },
                                    showDivider = state.fees.normal !is Fee.Ethereum,
                                )
                            }
                        }
                        is TransactionFee.Single -> {
                            SendSpeedSelectorItem(
                                titleRes = R.string.common_fee_selector_option_market,
                                iconRes = R.drawable.ic_bird_24,
                                isSelected = true,
                                amount = TextReference.Str(state.fees.normal.amount.value.toString()),
                                symbol = TextReference.Str(state.fees.normal.amount.currencySymbol),
                                onSelect = { clickIntents.onFeeSelectorClick(FeeType.MARKET) },
                                showDivider = false,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SendSpeedSelectorItemLoading() {
    repeat(DEFAULT_FEE_OPTIONS.size) {
        val (text, iconRes) = DEFAULT_FEE_OPTIONS[it]
        Row(modifier = Modifier.fillMaxWidth()) {
            SelectorTitleContent(
                titleRes = text,
                iconRes = iconRes,
            )
            SpacerWMax()
            RectangleShimmer(
                radius = TangemTheme.dimens.radius3,
                modifier = Modifier
                    .padding(
                        top = TangemTheme.dimens.spacing18,
                        bottom = TangemTheme.dimens.spacing18,
                        end = TangemTheme.dimens.spacing12,
                    )
                    .size(
                        width = TangemTheme.dimens.size90,
                        height = TangemTheme.dimens.size12,
                    ),
            )
        }
    }
}

@Composable
private fun SendSpeedSelectorItemError() {
    repeat(DEFAULT_FEE_OPTIONS.size) {
        val (text, iconRes) = DEFAULT_FEE_OPTIONS[it]
        Row(modifier = Modifier.fillMaxWidth()) {
            SelectorTitleContent(
                titleRes = text,
                iconRes = iconRes,
            )
            SpacerWMax()
            Text(
                text = BigDecimalFormatter.EMPTY_BALANCE_SIGN,
                style = TangemTheme.typography.body2,
                color = TangemTheme.colors.text.primary1,
                modifier = Modifier
                    .padding(
                        vertical = TangemTheme.dimens.spacing14,
                        horizontal = TangemTheme.dimens.spacing12,
                    ),
            )
        }
    }
}

@Composable
private fun SendSpeedSelectorItem(
    @StringRes titleRes: Int,
    @DrawableRes iconRes: Int,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
    amount: TextReference? = null,
    symbol: TextReference? = null,
    isSelected: Boolean = false,
    showDivider: Boolean = true,
) {
    val iconTint by animateColorAsState(
        targetValue = if (isSelected) {
            TangemTheme.colors.icon.accent
        } else {
            TangemTheme.colors.icon.informative
        },
        label = "Selector icon tint change",
    )

    val textStyle = if (isSelected) {
        TangemTheme.typography.subtitle2
    } else {
        TangemTheme.typography.body2
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onSelect() },
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            SelectorTitleContent(
                titleRes = titleRes,
                iconRes = iconRes,
                iconTint = iconTint,
                textStyle = textStyle,
            )
            if (amount != null && symbol != null) {
                SelectorValueContent(
                    amount = amount,
                    symbol = symbol,
                    textStyle = textStyle,
                )
            }
        }
        if (showDivider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(TangemTheme.dimens.size1)
                    .padding(horizontal = TangemTheme.dimens.spacing12)
                    .background(TangemTheme.colors.stroke.primary)
                    .align(Alignment.BottomCenter),
            )
        }
    }
}

@Composable
private fun SelectorTitleContent(
    @StringRes titleRes: Int,
    @DrawableRes iconRes: Int,
    iconTint: Color = TangemTheme.colors.icon.informative,
    textStyle: TextStyle = TangemTheme.typography.body2,
) {
    Icon(
        painter = painterResource(iconRes),
        tint = iconTint,
        contentDescription = null,
        modifier = Modifier
            .padding(
                start = TangemTheme.dimens.spacing12,
                top = TangemTheme.dimens.spacing12,
                bottom = TangemTheme.dimens.spacing12,
            ),
    )
    Text(
        text = stringResource(titleRes),
        style = textStyle,
        color = TangemTheme.colors.text.primary1,
        modifier = Modifier
            .padding(
                start = TangemTheme.dimens.spacing8,
                top = TangemTheme.dimens.spacing14,
                bottom = TangemTheme.dimens.spacing14,
            ),
    )
}

@Composable
private fun RowScope.SelectorValueContent(amount: TextReference, symbol: TextReference, textStyle: TextStyle) {
    Text(
        text = amount.resolveReference(),
        style = textStyle,
        color = TangemTheme.colors.text.primary1,
        textAlign = TextAlign.End,
        overflow = TextOverflow.Ellipsis,
        maxLines = 1,
        modifier = Modifier
            .weight(1f)
            .padding(
                start = TangemTheme.dimens.spacing4,
                top = TangemTheme.dimens.spacing14,
                bottom = TangemTheme.dimens.spacing14,
            ),
    )
    Text(
        text = symbol.resolveReference(),
        style = textStyle,
        color = TangemTheme.colors.text.primary1,
        modifier = Modifier
            .padding(
                start = TangemTheme.dimens.spacing1,
                end = TangemTheme.dimens.spacing12,
                top = TangemTheme.dimens.spacing14,
                bottom = TangemTheme.dimens.spacing14,
            ),
    )
}

//region preview
@Preview
@Composable
private fun FeeSelectorPreview_Light() {
    TangemTheme {
        SendSpeedSelectorItemLoading()
    }
}

@Preview
@Composable
private fun FeeSelectorPreview_Dark() {
    TangemTheme(isDark = true) {
        SendSpeedSelectorItemLoading()
    }
}
//endregion