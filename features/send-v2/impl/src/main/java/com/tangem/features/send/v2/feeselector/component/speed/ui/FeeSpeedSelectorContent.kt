package com.tangem.features.send.v2.feeselector.component.speed.ui

import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.*
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.common.ui.amountScreen.utils.getFiatReference
import com.tangem.core.ui.components.SpacerH8
import com.tangem.core.ui.components.SpacerWMax
import com.tangem.core.ui.components.TextShimmer
import com.tangem.core.ui.components.atoms.text.EllipsisText
import com.tangem.core.ui.components.atoms.text.TextEllipsis
import com.tangem.core.ui.components.inputrow.InputRowEnter
import com.tangem.core.ui.components.inputrow.InputRowEnterInfoAmountV2
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.fee
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.test.SendSelectNetworkFeeBottomSheetTestTags
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.features.send.v2.api.entity.*
import com.tangem.features.send.v2.feeselector.component.speed.FeeSpeedSelectorIntents
import com.tangem.features.send.v2.feeselector.component.speed.StubFeeSpeedSelectorIntents
import com.tangem.features.send.v2.impl.R
import com.tangem.utils.StringsSigns
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import java.math.BigDecimal

@Composable
internal fun FeeSpeedSelectorContent(
    state: FeeSelectorUM.Content,
    intents: FeeSpeedSelectorIntents,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(
            bottom = 16.dp,
            start = 16.dp,
            end = 16.dp,
        ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Title(onLearnMoreClick = intents::onLearnMoreClick)
        SpacerH8()
        FeeSelectorItems(
            state = state,
            intents = intents,
        )
    }
}

@Composable
private fun Title(modifier: Modifier = Modifier, onLearnMoreClick: () -> Unit) {
    val linkText = stringResourceSafe(R.string.common_learn_more)
    val fullString = stringResourceSafe(R.string.fee_selector_choose_speed_description, linkText)
    val linkTextPosition = fullString.length - linkText.length
    val defaultColor = TangemTheme.colors.text.secondary
    val linkColor = TangemTheme.colors.text.accent
    val annotatedString = remember(defaultColor, linkColor, onLearnMoreClick) {
        buildAnnotatedString {
            withStyle(SpanStyle(defaultColor)) {
                append(fullString.substring(0, linkTextPosition))
            }
            withLink(
                link = LinkAnnotation.Clickable(
                    tag = "learn_more",
                    linkInteractionListener = { onLearnMoreClick() },
                ),
                block = {
                    withStyle(SpanStyle(color = linkColor, textDecoration = TextDecoration.None)) {
                        append(
                            fullString.substring(linkTextPosition, fullString.length).replace(
                                ' ',
                                StringsSigns.NON_BREAKING_SPACE,
                            ),
                        )
                    }
                },
            )
        }
    }

    Text(
        modifier = modifier.padding(horizontal = 32.dp),
        text = annotatedString,
        style = TangemTheme.typography.caption2,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun FeeSelectorItems(
    state: FeeSelectorUM.Content,
    intents: FeeSpeedSelectorIntents,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        val feeFiatRateUM = state.feeFiatRateUM
        state.feeItems.fastForEachIndexed { index, item ->
            val isSelected = item.isSameClass(state.selectedFeeItem)
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
                .padding(top = 8.dp)
                .fillMaxWidth()
                .selectedBorder(isSelected = isSelected)
                .clickableSingle(onClick = { intents.onFeeItemSelected(item) })
                .background(TangemTheme.colors.background.action)
            when (item) {
                is FeeItem.Custom -> CustomFeeBlock(
                    modifier = itemModifier,
                    customFee = item,
                    isSelected = isSelected,
                    iconBackgroundColor = iconBackgroundColor,
                    iconTint = iconTint,
                    onValueChange = intents::onCustomFeeValueChange,
                    nonce = state.feeNonce,
                )
                else -> RegularFeeItemContent(
                    modifier = itemModifier,
                    title = item.title,
                    iconRes = item.iconRes,
                    iconBackgroundColor = iconBackgroundColor,
                    iconTint = iconTint,
                    preDot = stringReference(
                        item.fee.amount.value.format {
                            crypto(
                                symbol = item.fee.amount.currencySymbol,
                                decimals = item.fee.amount.decimals,
                            ).fee(canBeLower = state.feeExtraInfo.isFeeApproximate)
                        },
                    ),
                    postDot = if (state.feeExtraInfo.isFeeConvertibleToFiat && feeFiatRateUM != null) {
                        getFiatReference(
                            value = item.fee.amount.value,
                            rate = feeFiatRateUM.rate,
                            appCurrency = feeFiatRateUM.appCurrency,
                        )
                    } else {
                        null
                    },
                    ellipsizeOffset = item.fee.amount.currencySymbol.length,
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
    onValueChange: (Int, String) -> Unit,
    nonce: FeeNonce,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()
    val textHeight = with(LocalDensity.current) {
        textMeasurer.measure("S", style = TangemTheme.typography.subtitle2).size.height.toDp() +
            textMeasurer.measure("S", style = TangemTheme.typography.caption1).size.height.toDp() + 2.dp
    }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 15.dp)
                .testTag(SendSelectNetworkFeeBottomSheetTestTags.CUSTOM_FEE_ITEM),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                modifier = Modifier
                    .size(36.dp)
                    .background(color = iconBackgroundColor, shape = CircleShape)
                    .padding(6.dp)
                    .testTag(SendSelectNetworkFeeBottomSheetTestTags.CUSTOM_ITEM_ICON),
                painter = painterResource(R.drawable.ic_edit_v2_24),
                tint = iconTint,
                contentDescription = null,
            )
            Box(Modifier.heightIn(min = textHeight)) {
                Text(
                    text = stringResourceSafe(R.string.common_custom),
                    color = TangemTheme.colors.text.primary1,
                    style = TangemTheme.typography.subtitle2,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .testTag(SendSelectNetworkFeeBottomSheetTestTags.CUSTOM_ITEM_TITLE),
                )
            }
        }
        AnimatedVisibility(
            visible = isSelected,
            label = "Custom Fee Selected Animation",
            enter = expandVertically().plus(fadeIn()),
            exit = shrinkVertically().plus(fadeOut()),
        ) {
            ExpandedCustomFeeItems(
                customFeeFields = customFee.customValues,
                onValueChange = onValueChange,
                nonce = nonce,
            )
        }
    }
}

@Composable
private fun ExpandedCustomFeeItems(
    customFeeFields: ImmutableList<CustomFeeFieldUM>,
    onValueChange: (Int, String) -> Unit,
    nonce: FeeNonce,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        customFeeFields.fastForEachIndexed { index, field ->
            val isShowDivider = index != customFeeFields.size - 1 || nonce is FeeNonce.Nonce
            if (field.label != null) {
                InputRowEnterInfoAmountV2(
                    text = field.value,
                    decimals = field.decimals,
                    symbol = field.symbol,
                    title = field.title,
                    titleColor = TangemTheme.colors.text.tertiary,
                    info = field.label,
                    description = field.footer,
                    keyboardOptions = field.keyboardOptions,
                    keyboardActions = field.keyboardActions,
                    onValueChange = { onValueChange(index, it) },
                    showDivider = isShowDivider,
                    isReadOnly = field.isReadonly,
                )
            } else {
                InputRowEnterInfoAmountV2(
                    text = field.value,
                    decimals = field.decimals,
                    title = field.title,
                    titleColor = TangemTheme.colors.text.tertiary,
                    symbol = field.symbol,
                    description = field.footer,
                    onValueChange = { onValueChange(index, it) },
                    keyboardOptions = field.keyboardOptions,
                    keyboardActions = field.keyboardActions,
                    showDivider = isShowDivider,
                )
            }
        }

        if (nonce is FeeNonce.Nonce) {
            InputRowEnter(
                text = nonce.nonce?.toString().orEmpty(),
                title = resourceReference(R.string.send_nonce),
                description = resourceReference(R.string.send_nonce_footer),
                onValueChange = nonce.onNonceChange,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                placeholder = resourceReference(R.string.send_nonce_hint),
                titleColor = TangemTheme.colors.text.secondary,
                showDivider = false,
                modifier = Modifier
                    .background(
                        color = TangemTheme.colors.background.primary,
                        shape = TangemTheme.shapes.roundedCornersXMedium,
                    )
                    .testTag(SendSelectNetworkFeeBottomSheetTestTags.NONCE_INPUT_ITEM),
            )
        }
    }
}

@Composable
internal fun RegularFeeItemContent(
    title: TextReference,
    @DrawableRes iconRes: Int,
    iconBackgroundColor: Color,
    iconTint: Color,
    modifier: Modifier = Modifier,
    preDot: TextReference? = null,
    postDot: TextReference? = null,
    ellipsizeOffset: Int? = null,
    showSelectorIcon: Boolean = false,
    isLoading: Boolean = false,
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 15.dp)
                .testTag(SendSelectNetworkFeeBottomSheetTestTags.REGULAR_FEE_ITEM),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                modifier = Modifier
                    .size(36.dp)
                    .background(color = iconBackgroundColor, shape = CircleShape)
                    .padding(6.dp)
                    .testTag(SendSelectNetworkFeeBottomSheetTestTags.REGULAR_ITEM_ICON),
                painter = painterResource(iconRes),
                tint = iconTint,
                contentDescription = null,
            )
            FeeDescription(
                title = title,
                preDot = preDot,
                postDot = postDot,
                ellipsizeOffset = ellipsizeOffset,
                isLoading = isLoading,
            )

            if (showSelectorIcon) {
                SpacerWMax()

                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_select_18_24),
                    tint = TangemTheme.colors.icon.informative,
                    contentDescription = null,
                )
            }
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
    isLoading: Boolean = false,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = title.resolveReference(),
            color = TangemTheme.colors.text.primary1,
            style = TangemTheme.typography.subtitle2,
            modifier = Modifier.testTag(SendSelectNetworkFeeBottomSheetTestTags.REGULAR_ITEM_TITLE),
        )
        when {
            isLoading -> TextShimmer(
                modifier = Modifier.width(122.dp),
                style = TangemTheme.typography.caption1,
            )
            preDot != null -> FeeValueContent(preDot = preDot, postDot = postDot, ellipsizeOffset = ellipsizeOffset)
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
            modifier = Modifier.testTag(SendSelectNetworkFeeBottomSheetTestTags.TOKEN_AMOUNT),
        )
        if (postDot != null) {
            Text(
                text = StringsSigns.DOT,
                style = textStyle,
                color = textColor,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(horizontal = TangemTheme.dimens.spacing4)
                    .testTag(SendSelectNetworkFeeBottomSheetTestTags.DOT_SIGN),
            )
            Text(
                text = postDot.resolveReference(),
                style = textStyle,
                color = textColor,
                modifier = Modifier.testTag(SendSelectNetworkFeeBottomSheetTestTags.FIAT_AMOUNT),
            )
        }
    }
}

@Preview(showBackground = true, device = Devices.PIXEL_7_PRO)
@Preview(showBackground = true, device = Devices.PIXEL_7_PRO, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview(
    @PreviewParameter(FeeSpeedSelectorUMContentProvider::class)
    state: FeeSelectorUM.Content,
) {
    TangemThemePreview {
        Box(Modifier.background(TangemTheme.colors.background.secondary)) {
            FeeSpeedSelectorContent(
                state = state,
                intents = StubFeeSpeedSelectorIntents(),
            )
        }
    }
}

private class FeeSpeedSelectorUMContentProvider : CollectionPreviewParameterProvider<FeeSelectorUM.Content>(
    collection = listOf(
        FeeSelectorUM.Content(
            isPrimaryButtonEnabled = false,
            feeItems = persistentListOf(
                FeeItem.Suggested(
                    title = resourceReference(
                        id = R.string.wc_fee_suggested,
                        formatArgs = wrappedList("Tangem"),
                    ),
                    fee = Fee.Common(Amount(value = BigDecimal("0.1"), blockchain = Blockchain.Ethereum)),
                ),
                FeeItem.Slow(fee = Fee.Common(Amount(value = BigDecimal("0.01"), blockchain = Blockchain.Ethereum))),
                FeeItem.Market(
                    fee = Fee.Common(
                        Amount(
                            value = BigDecimal("0.02"),
                            blockchain = Blockchain.Ethereum,
                        ),
                    ),
                ),
                FeeItem.Fast(fee = Fee.Common(Amount(value = BigDecimal("0.03"), blockchain = Blockchain.Ethereum))),
                customFeeItem,
            ),
            selectedFeeItem = FeeItem.Slow(
                fee = Fee.Common(
                    Amount(
                        value = BigDecimal("0.01"),
                        blockchain = Blockchain.Ethereum,
                    ),
                ),
            ),
            feeExtraInfo = FeeExtraInfo(
                isFeeApproximate = true,
                isFeeConvertibleToFiat = true,
                isTronToken = false,
                feeCryptoCurrencyStatus = cryptoCurrencyStatus,
            ),
            feeFiatRateUM = FeeFiatRateUM(
                rate = BigDecimal.TEN,
                appCurrency = AppCurrency.Default,
            ),
            feeNonce = FeeNonce.None,
            fees = TransactionFee.Single(customFeeItem.fee),
        ),
    ),
)

private val cryptoCurrencyStatus
    get() = CryptoCurrencyStatus(
        currency = CryptoCurrency.Coin(
            id = CryptoCurrency.ID.fromValue("coin⟨BITCOIN⟩bitcoin"),
            network = Network(
                id = Network.ID(
                    value = "bitcoin",
                    derivationPath = Network.DerivationPath.None,
                ),
                backendId = "bitcoin",
                name = "Bitcoin",
                currencySymbol = "BTC",
                derivationPath = Network.DerivationPath.None,
                isTestnet = false,
                standardType = Network.StandardType.Unspecified("bitcoin"),
                hasFiatFeeRate = false,
                canHandleTokens = false,
                transactionExtrasType = Network.TransactionExtrasType.NONE,
                nameResolvingType = Network.NameResolvingType.NONE,
            ),
            name = "Bitcoin",
            symbol = "BTC",
            decimals = 8,
            iconUrl = "https://s3.eu-central-1.amazonaws.com/tangem.api/coins/medium/bitcoin.png",
            isCustom = false,
        ),
        value = CryptoCurrencyStatus.Loading,
    )

private val customFeeItem
    get() = FeeItem.Custom(
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