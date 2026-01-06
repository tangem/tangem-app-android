package com.tangem.features.send.v2.feeselector.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.common.ui.amountScreen.utils.getFiatString
import com.tangem.core.ui.components.TextShimmer
import com.tangem.core.ui.components.atoms.text.EllipsisText
import com.tangem.core.ui.components.audits.AuditLabel
import com.tangem.core.ui.components.audits.AuditLabelUM
import com.tangem.core.ui.components.tooltip.TangemTooltip
import com.tangem.core.ui.extensions.annotatedReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.format.bigdecimal.BigDecimalFormatConstants.EMPTY_BALANCE_SIGN
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.fee
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.test.FeeSelectorBlockTestTags
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.features.send.v2.api.entity.*
import com.tangem.features.send.v2.impl.R
import com.tangem.utils.extensions.isSingleItem
import kotlinx.collections.immutable.persistentListOf
import java.math.BigDecimal

private const val READ_MORE_TAG = "READ_MORE"

@Composable
internal fun FeeSelectorBlockContent(
    state: FeeSelectorUM,
    isGaslessFeatureEnabled: Boolean,
    onReadMoreClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .background(TangemTheme.colors.background.action)
            .padding(12.dp)
            .testTag(FeeSelectorBlockTestTags.SELECTOR_BLOCK),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            modifier = Modifier
                .size(24.dp)
                .testTag(FeeSelectorBlockTestTags.ICON),
            painter = painterResource(R.drawable.ic_fee_new_24),
            contentDescription = null,
            tint = TangemTheme.colors.icon.accent,
        )
        FeeSelectorDescription(
            state = state,
            isGaslessFeatureEnabled = isGaslessFeatureEnabled,
            onReadMoreClick = onReadMoreClick,
        )
    }
}

@Composable
private fun FeeSelectorDescription(
    state: FeeSelectorUM,
    isGaslessFeatureEnabled: Boolean,
    onReadMoreClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.SpaceBetween) {
        FeeSelectorStaticPart(modifier = Modifier.weight(1f), onReadMoreClick = onReadMoreClick)
        when (state) {
            is FeeSelectorUM.Content -> FeeContent(state, isGaslessFeatureEnabled)
            is FeeSelectorUM.Loading -> FeeLoading()
            is FeeSelectorUM.Error -> FeeError()
        }
    }
}

@Composable
private fun FeeSelectorStaticPart(onReadMoreClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(
            modifier = Modifier
                .padding(start = TangemTheme.dimens.spacing4)
                .weight(1f, fill = false)
                .testTag(FeeSelectorBlockTestTags.TITLE),
            text = stringResourceSafe(R.string.common_network_fee_title),
            style = TangemTheme.typography.body1,
            color = TangemTheme.colors.text.primary1,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        val linkText = stringResourceSafe(R.string.common_read_more)
        val fullString = stringResourceSafe(R.string.common_fee_selector_footer, linkText)
        val linkTextPosition = fullString.length - linkText.length
        val defaultStyle = TangemTheme.colors.text.primary2
        val linkStyle = TangemTheme.colors.text.accent
        val annotatedString = remember(defaultStyle, linkStyle) {
            buildAnnotatedString {
                withStyle(SpanStyle(defaultStyle)) {
                    append(fullString.substring(0, linkTextPosition))
                }
                withLink(
                    link = LinkAnnotation.Clickable(
                        tag = READ_MORE_TAG,
                        linkInteractionListener = { onReadMoreClick() },
                    ),
                    block = {
                        withStyle(SpanStyle(linkStyle)) {
                            append(fullString.substring(linkTextPosition, fullString.length))
                        }
                    },
                )
            }
        }
        TangemTooltip(
            text = annotatedReference(annotatedString),
            modifier = Modifier
                .padding(start = TangemTheme.dimens.spacing6)
                .size(TangemTheme.dimens.size16)
                .clip(CircleShape),
            content = { contentModifier ->
                Icon(
                    modifier = contentModifier
                        .size(TangemTheme.dimens.size16)
                        .testTag(FeeSelectorBlockTestTags.TOOLTIP_ICON),
                    painter = painterResource(id = R.drawable.ic_token_info_24),
                    contentDescription = null,
                    tint = TangemTheme.colors.icon.informative,
                )
            },
        )
    }
}

@Composable
private fun FeeError() {
    Text(
        text = EMPTY_BALANCE_SIGN,
        color = TangemTheme.colors.text.primary1,
        style = TangemTheme.typography.body2,
    )
}

@Composable
private fun FeeLoading() {
    TextShimmer(
        radius = TangemTheme.dimens.radius3,
        style = TangemTheme.typography.body1,
        modifier = Modifier.width(width = TangemTheme.dimens.size90),
    )
}

@Composable
private fun FeeContent(state: FeeSelectorUM.Content, isGaslessFeatureEnabled: Boolean, modifier: Modifier = Modifier) {
    val fiatRate = state.feeFiatRateUM
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        if (isGaslessFeatureEnabled) {
            AuditLabel(
                state = AuditLabelUM(
                    text = stringReference(state.selectedFeeItem.fee.amount.currencySymbol),
                    type = AuditLabelUM.Type.General,
                ),
            )
        }

        EllipsisText(
            text = if (state.feeExtraInfo.isFeeConvertibleToFiat && fiatRate != null) {
                getFiatString(
                    value = state.selectedFeeItem.fee.amount.value,
                    rate = fiatRate.rate,
                    appCurrency = fiatRate.appCurrency,
                    approximate = state.feeExtraInfo.isFeeApproximate,
                )
            } else {
                state.selectedFeeItem.fee.amount.value
                    .format {
                        crypto(
                            symbol = state.selectedFeeItem.fee.amount.currencySymbol,
                            decimals = state.selectedFeeItem.fee.amount.decimals,
                        ).fee(canBeLower = state.feeExtraInfo.isFeeApproximate)
                    }
            },
            style = TangemTheme.typography.body1,
            color = TangemTheme.colors.text.tertiary,
            textAlign = TextAlign.End,
            modifier = Modifier
                .padding(start = TangemTheme.dimens.spacing4)
                .testTag(FeeSelectorBlockTestTags.FEE_AMOUNT),
        )
        if (!state.feeItems.isSingleItem()) {
            Icon(
                modifier = Modifier
                    .size(width = 18.dp, height = 24.dp)
                    .testTag(FeeSelectorBlockTestTags.SELECT_FEE_ICON),
                painter = painterResource(id = R.drawable.ic_select_18_24),
                contentDescription = null,
                tint = TangemTheme.colors.icon.informative,
            )
        }
    }
}

@Preview(showBackground = true, device = Devices.PIXEL_7_PRO)
@Preview(showBackground = true, device = Devices.PIXEL_7_PRO, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun FeeSelectorBlockContent_Preview(@PreviewParameter(FeeSelectorUMProvider::class) state: FeeSelectorUM) {
    TangemThemePreview {
        FeeSelectorBlockContent(
            modifier = Modifier.fillMaxWidth(),
            isGaslessFeatureEnabled = true,
            state = state,
            onReadMoreClick = {},
        )
    }
}

private class FeeSelectorUMProvider : PreviewParameterProvider<FeeSelectorUM> {
    private val maxFeeItem
        get() = FeeItem.Market(
            fee = Fee.Common(amount = Amount(value = BigDecimal("100000000"), blockchain = Blockchain.Hedera)),
        )
    private val lowFeeItem
        get() = FeeItem.Market(
            Fee.Common(
                amount = Amount(
                    value = BigDecimal("0.0002876"),
                    blockchain = Blockchain.Ethereum,
                ),
            ),
        )

    val cryptoCurrencyStatus
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

    override val values: Sequence<FeeSelectorUM> = sequenceOf(
        FeeSelectorUM.Content(
            isPrimaryButtonEnabled = true,
            feeItems = persistentListOf(lowFeeItem),
            selectedFeeItem = lowFeeItem,
            feeExtraInfo = FeeExtraInfo(
                isFeeApproximate = false,
                isFeeConvertibleToFiat = true,
                isTronToken = false,
                feeCryptoCurrencyStatus = cryptoCurrencyStatus,
            ),
            feeNonce = FeeNonce.None,
            feeFiatRateUM = FeeFiatRateUM(
                rate = BigDecimal("2500"),
                appCurrency = AppCurrency.Default,
            ),
            fees = TransactionFee.Single(lowFeeItem.fee),
        ),
        FeeSelectorUM.Content(
            isPrimaryButtonEnabled = false,
            feeItems = persistentListOf(lowFeeItem, maxFeeItem),
            selectedFeeItem = maxFeeItem,
            feeExtraInfo = FeeExtraInfo(
                isFeeApproximate = false,
                isFeeConvertibleToFiat = true,
                isTronToken = false,
                feeCryptoCurrencyStatus = cryptoCurrencyStatus,
            ),
            feeNonce = FeeNonce.None,
            feeFiatRateUM = FeeFiatRateUM(
                rate = BigDecimal("2500000000000"),
                appCurrency = AppCurrency.Default,
            ),
            fees = TransactionFee.Single(maxFeeItem.fee),
        ),
        FeeSelectorUM.Error(GetFeeError.UnknownError),
        FeeSelectorUM.Loading,
    )
}