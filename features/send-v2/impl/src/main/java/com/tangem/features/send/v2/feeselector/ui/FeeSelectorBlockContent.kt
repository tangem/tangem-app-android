package com.tangem.features.send.v2.feeselector.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.tangem.core.ui.components.tooltip.TangemTooltip
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.format.bigdecimal.BigDecimalFormatConstants.EMPTY_BALANCE_SIGN
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.fee
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.features.send.v2.api.entity.*
import com.tangem.features.send.v2.impl.R
import kotlinx.collections.immutable.persistentListOf
import java.math.BigDecimal

private const val READ_MORE_TAG = "READ_MORE"

@Composable
internal fun FeeSelectorBlockContent(
    state: FeeSelectorUM,
    onReadMoreClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .background(TangemTheme.colors.background.action)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            modifier = Modifier.size(24.dp),
            painter = painterResource(R.drawable.ic_fee_new_24),
            contentDescription = null,
            tint = TangemTheme.colors.icon.accent,
        )
        FeeSelectorDescription(state = state, onReadMoreClick = onReadMoreClick)
    }
}

@Composable
private fun FeeSelectorDescription(state: FeeSelectorUM, onReadMoreClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.SpaceBetween) {
        FeeSelectorStaticPart(modifier = Modifier.weight(1f), onReadMoreClick = onReadMoreClick)
        when (state) {
            is FeeSelectorUM.Content -> FeeContent(state)
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
                .weight(1f, fill = false),
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
            text = annotatedString,
            modifier = Modifier
                .padding(start = TangemTheme.dimens.spacing6)
                .size(TangemTheme.dimens.size16),
            content = { contentModifier ->
                Icon(
                    modifier = contentModifier.size(TangemTheme.dimens.size16),
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
private fun FeeContent(state: FeeSelectorUM.Content, modifier: Modifier = Modifier) {
    val fiatRate = state.feeFiatRateUM
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        EllipsisText(
            text = if (fiatRate != null) {
                getFiatString(
                    value = state.selectedFeeItem.fee.amount.value,
                    rate = fiatRate.rate,
                    appCurrency = fiatRate.appCurrency,
                    approximate = state.feeExtraInfo.isFeeApproximate,
                )
            } else {
                state.selectedFeeItem.fee.amount.value.format {
                    crypto(
                        symbol = state.selectedFeeItem.fee.amount.currencySymbol,
                        decimals = state.selectedFeeItem.fee.amount.decimals,
                    ).fee(canBeLower = state.feeExtraInfo.isFeeApproximate)
                }
            },
            style = TangemTheme.typography.body1,
            color = TangemTheme.colors.text.tertiary,
            textAlign = TextAlign.End,
            modifier = Modifier.padding(start = TangemTheme.dimens.spacing4),
        )
        Icon(
            modifier = Modifier.size(width = 18.dp, height = 24.dp),
            painter = painterResource(id = R.drawable.ic_select_18_24),
            contentDescription = null,
            tint = TangemTheme.colors.icon.informative,
        )
    }
}

@Preview(showBackground = true, device = Devices.PIXEL_7_PRO)
@Preview(showBackground = true, device = Devices.PIXEL_7_PRO, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun FeeSelectorBlockContent_Preview(@PreviewParameter(FeeSelectorUMProvider::class) state: FeeSelectorUM) {
    TangemThemePreview {
        FeeSelectorBlockContent(modifier = Modifier.fillMaxWidth(), state = state, onReadMoreClick = {})
    }
}

private class FeeSelectorUMProvider : PreviewParameterProvider<FeeSelectorUM> {
    private val maxFeeItem = FeeItem.Market(
        fee = Fee.Common(amount = Amount(value = BigDecimal("100000000"), blockchain = Blockchain.Ethereum)),
    )
    private val lowFeeItem =
        FeeItem.Market(Fee.Common(amount = Amount(value = BigDecimal("0.0002876"), blockchain = Blockchain.Ethereum)))

    override val values: Sequence<FeeSelectorUM> = sequenceOf(
        FeeSelectorUM.Content(
            feeItems = persistentListOf(lowFeeItem),
            selectedFeeItem = lowFeeItem,
            feeExtraInfo = FeeExtraInfo(
                isFeeApproximate = false,
                isFeeConvertibleToFiat = true,
                isTronToken = false,
            ),
            feeNonce = FeeNonce.None,
            feeFiatRateUM = FeeFiatRateUM(
                rate = BigDecimal("2500"),
                appCurrency = AppCurrency.Default,
            ),
            fees = TransactionFee.Single(lowFeeItem.fee),
        ),
        FeeSelectorUM.Content(
            feeItems = persistentListOf(maxFeeItem),
            selectedFeeItem = maxFeeItem,
            feeExtraInfo = FeeExtraInfo(
                isFeeApproximate = false,
                isFeeConvertibleToFiat = true,
                isTronToken = false,
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