package com.tangem.features.send.v2.feeselector.ui

import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.common.ui.amountScreen.utils.getFiatReference
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.components.atoms.text.EllipsisText
import com.tangem.core.ui.components.atoms.text.TextEllipsis
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheetTitle
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheetWithFooter
import com.tangem.core.ui.components.inputrow.InputRowEnterInfoAmountV2
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.fee
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.features.send.v2.api.entity.CustomFeeFieldUM
import com.tangem.features.send.v2.api.entity.FeeFiatRateUM
import com.tangem.features.send.v2.api.entity.FeeItem
import com.tangem.features.send.v2.api.entity.FeeSelectorUM
import com.tangem.features.send.v2.feeselector.model.FeeSelectorIntents
import com.tangem.features.send.v2.feeselector.model.StubFeeSelectorIntents
import com.tangem.features.send.v2.impl.R
import com.tangem.utils.StringsSigns
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import java.math.BigDecimal
import java.math.BigInteger

@Composable
internal fun FeeSelectorModalBottomSheet(
    state: FeeSelectorUM,
    feeSelectorIntents: FeeSelectorIntents,
    onDismiss: () -> Unit,
) {
    if (state !is FeeSelectorUM.Content) return

    TangemModalBottomSheetWithFooter<TangemBottomSheetConfigContent.Empty>(
        config = TangemBottomSheetConfig(
            isShown = true,
            onDismissRequest = onDismiss,
            content = TangemBottomSheetConfigContent.Empty,
        ),
        containerColor = TangemTheme.colors.background.primary,
        title = {
            TangemModalBottomSheetTitle(
                title = resourceReference(R.string.common_network_fee_title),
                startIconRes = R.drawable.ic_back_24,
                onStartClick = onDismiss,
            )
        },
        content = {
            FeeSelectorItems(
                state = state,
                feeSelectorIntents = feeSelectorIntents,
                modifier = Modifier.padding(vertical = 4.dp, horizontal = 16.dp),
            )
        },
        footer = {
            PrimaryButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                text = stringResourceSafe(R.string.common_done),
                onClick = feeSelectorIntents::onDoneClick,
            )
        },
    )
}

@Suppress("LongMethod", "CyclomaticComplexMethod")
@Composable
private fun FeeSelectorItems(
    state: FeeSelectorUM.Content,
    feeSelectorIntents: FeeSelectorIntents,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        val feeFiatRateUM = state.feeFiatRateUM
        state.feeItems.fastForEachIndexed { index, item ->
            val isSelected = item.isSame(state.selectedFeeItem)
            val lastItem = index == state.feeItems.size - 1
            val iconTint by animateColorAsState(
                targetValue = if (isSelected) TangemTheme.colors.icon.accent else TangemTheme.colors.text.tertiary,
                label = "Fee selector icon tint change",
            )
            val iconBackgroundColor by animateColorAsState(
                targetValue = if (isSelected) {
                    TangemTheme.colors.icon.accent.copy(alpha = 0.1F)
                } else {
                    TangemTheme.colors.background.secondary
                },
                label = "Fee selector icon background change",
            )
            val itemModifier = Modifier
                .fillMaxWidth()
                .background(TangemTheme.colors.background.primary)
                .then(
                    if (isSelected) {
                        Modifier
                            .border(
                                width = 2.5.dp,
                                color = iconTint.copy(alpha = 0.2F),
                                shape = RoundedCornerShape(16.dp),
                            )
                            .padding(2.5.dp)
                            .border(width = 1.dp, color = iconTint, shape = RoundedCornerShape(14.dp))
                            .clip(RoundedCornerShape(14.dp))
                    } else {
                        Modifier
                    },
                )
                .clickableSingle(onClick = { feeSelectorIntents.onFeeItemSelected(item) })
            when (item) {
                is FeeItem.Suggested -> RegularFeeItemContent(
                    modifier = itemModifier,
                    title = item.title,
                    iconRes = R.drawable.ic_star_mini_24,
                    iconBackgroundColor = iconBackgroundColor,
                    iconTint = iconTint,
                    preDot = stringReference(
                        item.fee.amount.value.format {
                            crypto(
                                symbol = item.fee.amount.currencySymbol,
                                decimals = item.fee.amount.decimals,
                            ).fee(canBeLower = state.isFeeApproximate)
                        },
                    ),
                    postDot = if (feeFiatRateUM != null) {
                        getFiatReference(
                            value = item.fee.amount.value,
                            rate = feeFiatRateUM.rate,
                            appCurrency = feeFiatRateUM.appCurrency,
                        )
                    } else {
                        null
                    },
                    ellipsizeOffset = item.fee.amount.currencySymbol.length,
                    showDivider = !isSelected && !lastItem,
                )
                is FeeItem.Slow -> RegularFeeItemContent(
                    modifier = itemModifier,
                    title = resourceReference(R.string.common_fee_selector_option_slow),
                    iconRes = R.drawable.ic_tortoise_24,
                    iconBackgroundColor = iconBackgroundColor,
                    iconTint = iconTint,
                    preDot = stringReference(
                        item.fee.amount.value.format {
                            crypto(
                                symbol = item.fee.amount.currencySymbol,
                                decimals = item.fee.amount.decimals,
                            ).fee(canBeLower = state.isFeeApproximate)
                        },
                    ),
                    postDot = if (feeFiatRateUM != null) {
                        getFiatReference(
                            value = item.fee.amount.value,
                            rate = feeFiatRateUM.rate,
                            appCurrency = feeFiatRateUM.appCurrency,
                        )
                    } else {
                        null
                    },
                    ellipsizeOffset = item.fee.amount.currencySymbol.length,
                    showDivider = !isSelected && !lastItem,
                )
                is FeeItem.Market -> RegularFeeItemContent(
                    modifier = itemModifier,
                    title = resourceReference(R.string.common_fee_selector_option_market),
                    iconRes = R.drawable.ic_bird_24,
                    iconBackgroundColor = iconBackgroundColor,
                    iconTint = iconTint,
                    preDot = stringReference(
                        item.fee.amount.value.format {
                            crypto(
                                symbol = item.fee.amount.currencySymbol,
                                decimals = item.fee.amount.decimals,
                            ).fee(canBeLower = state.isFeeApproximate)
                        },
                    ),
                    postDot = if (feeFiatRateUM != null) {
                        getFiatReference(
                            value = item.fee.amount.value,
                            rate = feeFiatRateUM.rate,
                            appCurrency = feeFiatRateUM.appCurrency,
                        )
                    } else {
                        null
                    },
                    ellipsizeOffset = item.fee.amount.currencySymbol.length,
                    showDivider = !isSelected && !lastItem,
                )
                is FeeItem.Fast -> RegularFeeItemContent(
                    modifier = itemModifier,
                    title = resourceReference(R.string.common_fee_selector_option_fast),
                    iconRes = R.drawable.ic_hare_24,
                    iconBackgroundColor = iconBackgroundColor,
                    iconTint = iconTint,
                    preDot = stringReference(
                        item.fee.amount.value.format {
                            crypto(
                                symbol = item.fee.amount.currencySymbol,
                                decimals = item.fee.amount.decimals,
                            ).fee(canBeLower = state.isFeeApproximate)
                        },
                    ),
                    postDot = if (feeFiatRateUM != null) {
                        getFiatReference(
                            value = item.fee.amount.value,
                            rate = feeFiatRateUM.rate,
                            appCurrency = feeFiatRateUM.appCurrency,
                        )
                    } else {
                        null
                    },
                    ellipsizeOffset = item.fee.amount.currencySymbol.length,
                    showDivider = !isSelected && !lastItem,
                )
                is FeeItem.Custom -> CustomFeeBlock(
                    modifier = itemModifier,
                    customFee = item,
                    isSelected = isSelected,
                    iconBackgroundColor = iconBackgroundColor,
                    iconTint = iconTint,
                    displayNonceInput = state.displayNonceInput,
                    nonce = state.nonce,
                    onNonceChange = state.onNonceChange,
                )
            }
        }
    }
}

@Suppress("LongParameterList")
@Composable
private fun CustomFeeBlock(
    customFee: FeeItem.Custom,
    isSelected: Boolean,
    iconBackgroundColor: Color,
    iconTint: Color,
    displayNonceInput: Boolean,
    nonce: BigInteger?,
    onNonceChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.padding(all = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                modifier = Modifier
                    .size(36.dp)
                    .background(color = iconBackgroundColor, shape = CircleShape)
                    .padding(6.dp),
                painter = painterResource(R.drawable.ic_edit_v2_24),
                tint = iconTint,
                contentDescription = null,
            )
            Text(
                text = stringResourceSafe(R.string.common_custom),
                color = TangemTheme.colors.text.primary1,
                style = TangemTheme.typography.subtitle2,
            )
        }
        AnimatedVisibility(
            visible = isSelected,
            label = "Custom Fee Selected Animation",
            enter = expandVertically().plus(fadeIn()),
            exit = shrinkVertically().plus(fadeOut()),
        ) {
            ExpandedCustomFeeItems(
                customFeeFields = customFee.customValues,
                onValueChange = { _, _ -> },
                displayNonceInput = displayNonceInput,
                nonce = nonce,
                onNonceChange = onNonceChange,
            )
        }
    }
}

@Composable
private fun ExpandedCustomFeeItems(
    customFeeFields: ImmutableList<CustomFeeFieldUM>,
    onValueChange: (Int, String) -> Unit,
    displayNonceInput: Boolean,
    nonce: BigInteger?,
    onNonceChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        customFeeFields.fastForEachIndexed { index, field ->
            val showDivider = index != customFeeFields.size - 1 || displayNonceInput
            if (field.label != null) {
                InputRowEnterInfoAmountV2(
                    text = field.value,
                    decimals = field.decimals,
                    symbol = field.symbol,
                    title = field.title,
                    titleColor = TangemTheme.colors.text.tertiary,
                    info = field.label,
                    keyboardOptions = field.keyboardOptions,
                    keyboardActions = field.keyboardActions,
                    onValueChange = { onValueChange(index, it) },
                    showDivider = showDivider,
                    isReadOnly = field.isReadonly,
                )
            } else {
                InputRowEnterInfoAmountV2(
                    text = field.value,
                    decimals = field.decimals,
                    title = field.title,
                    titleColor = TangemTheme.colors.text.tertiary,
                    symbol = field.symbol,
                    onValueChange = { onValueChange(index, it) },
                    keyboardOptions = field.keyboardOptions,
                    keyboardActions = field.keyboardActions,
                    showDivider = showDivider,
                )
            }
        }

        if (displayNonceInput) {
            // TODO implement v2 input without binding to amount
            InputRowEnterInfoAmountV2(
                text = nonce?.toString() ?: "",
                decimals = 0,
                title = resourceReference(R.string.send_nonce),
                titleColor = TangemTheme.colors.text.tertiary,
                symbol = null,
                onValueChange = onNonceChange,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                keyboardActions = KeyboardActions(),
                showDivider = false,
            )
        }
    }
}

@Composable
private fun RegularFeeItemContent(
    title: TextReference,
    @DrawableRes iconRes: Int,
    iconBackgroundColor: Color,
    iconTint: Color,
    modifier: Modifier = Modifier,
    preDot: TextReference? = null,
    postDot: TextReference? = null,
    ellipsizeOffset: Int? = null,
    showDivider: Boolean = true,
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.padding(all = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                modifier = Modifier
                    .size(36.dp)
                    .background(color = iconBackgroundColor, shape = CircleShape)
                    .padding(6.dp),
                painter = painterResource(iconRes),
                tint = iconTint,
                contentDescription = null,
            )
            FeeDescription(
                title = title,
                preDot = preDot,
                postDot = postDot,
                ellipsizeOffset = ellipsizeOffset,
            )
        }
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 60.dp, end = 12.dp),
                color = TangemTheme.colors.stroke.primary,
                thickness = 0.5.dp,
            )
        }
    }
}

@Composable
private fun FeeDescription(
    title: TextReference,
    modifier: Modifier = Modifier,
    preDot: TextReference? = null,
    postDot: TextReference? = null,
    ellipsizeOffset: Int? = null,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = title.resolveReference(),
            color = TangemTheme.colors.text.primary1,
            style = TangemTheme.typography.subtitle2,
        )
        if (preDot != null) {
            FeeValueContent(preDot = preDot, postDot = postDot, ellipsizeOffset = ellipsizeOffset)
        }
    }
}

@Composable
private fun FeeValueContent(preDot: TextReference, postDot: TextReference?, ellipsizeOffset: Int? = null) {
    val ellipsis = if (ellipsizeOffset == null) {
        TextEllipsis.End
    } else {
        TextEllipsis.OffsetEnd(ellipsizeOffset)
    }
    val textColor = TangemTheme.colors.text.tertiary
    val textStyle = TangemTheme.typography.caption1
    Row {
        EllipsisText(
            text = preDot.resolveReference(),
            style = textStyle,
            color = textColor,
            textAlign = TextAlign.End,
            ellipsis = ellipsis,
        )
        if (postDot != null) {
            Text(
                text = StringsSigns.DOT,
                style = textStyle,
                color = textColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = TangemTheme.dimens.spacing4),
            )
            Text(text = postDot.resolveReference(), style = textStyle, color = textColor)
        }
    }
}

@Preview(showBackground = true, device = Devices.PIXEL_7_PRO)
@Preview(showBackground = true, device = Devices.PIXEL_7_PRO, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun FeeSelectorBS_Preview(
    @PreviewParameter(FeeSelectorUMContentProvider::class)
    state: FeeSelectorUM.Content,
) {
    TangemThemePreview {
        FeeSelectorModalBottomSheet(onDismiss = {}, state = state, feeSelectorIntents = StubFeeSelectorIntents())
    }
}

private class FeeSelectorUMContentProvider : CollectionPreviewParameterProvider<FeeSelectorUM.Content>(
    collection = listOf(
        FeeSelectorUM.Content(
            feeItems = persistentListOf(
                FeeItem.Suggested(
                    title = stringReference("Suggested by Tangem"),
                    fee = Fee.Common(Amount(value = BigDecimal("0.1"), blockchain = Blockchain.Ethereum)),
                ),
                FeeItem.Slow(fee = Fee.Common(Amount(value = BigDecimal("0.01"), blockchain = Blockchain.Ethereum))),
                FeeItem.Market(fee = Fee.Common(Amount(value = BigDecimal("0.02"), blockchain = Blockchain.Ethereum))),
                FeeItem.Fast(fee = Fee.Common(Amount(value = BigDecimal("0.03"), blockchain = Blockchain.Ethereum))),
                customFeeItem,
            ),
            // selectedFeeItem = FeeItem.Market(
            //     amount = Amount(value = BigDecimal("0.02"), blockchain = Blockchain.Ethereum),
            // ),
            selectedFeeItem = customFeeItem,
            isFeeApproximate = true,
            feeFiatRateUM = FeeFiatRateUM(
                rate = BigDecimal.TEN,
                appCurrency = AppCurrency.Default,
            ),
            displayNonceInput = true,
            onNonceChange = {},
            nonce = null,
        ),
    ),
)

private val customFeeItem = FeeItem.Custom(
    fee = Fee.Common(Amount(value = BigDecimal("0.05"), blockchain = Blockchain.Ethereum)),
    customValues = persistentListOf(
        CustomFeeFieldUM(
            value = "0.119806",
            onValueChange = {},
            keyboardOptions = KeyboardOptions(),
            keyboardActions = KeyboardActions(),
            symbol = "ETH",
            decimals = 8,
            title = resourceReference(R.string.send_max_fee),
            footer = resourceReference(R.string.send_custom_amount_fee_footer),
            label = stringReference("~ 0,03 \$"),
            isReadonly = false,
        ),
        CustomFeeFieldUM(
            value = "40000",
            onValueChange = {},
            keyboardOptions = KeyboardOptions(),
            keyboardActions = KeyboardActions(),
            symbol = "GWEI",
            decimals = 8,
            title = resourceReference(R.string.send_gas_price),
            footer = resourceReference(R.string.send_gas_price_footer),
            label = null,
            isReadonly = false,
        ),
        CustomFeeFieldUM(
            value = "31400",
            onValueChange = {},
            keyboardOptions = KeyboardOptions(),
            keyboardActions = KeyboardActions(),
            symbol = null,
            decimals = 8,
            title = resourceReference(R.string.send_gas_limit),
            footer = resourceReference(R.string.send_gas_limit_footer),
            label = null,
            isReadonly = false,
        ),
    ),
)