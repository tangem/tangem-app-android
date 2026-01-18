package com.tangem.features.send.v2.feeselector.component.extended.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.common.ui.amountScreen.utils.getFiatReference
import com.tangem.core.ui.R
import com.tangem.core.ui.components.SpacerH8
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.components.icons.IconTint
import com.tangem.core.ui.components.token.TokenItem
import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.core.ui.extensions.conditional
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.fee
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.features.send.v2.api.entity.*
import com.tangem.features.send.v2.feeselector.component.extended.entity.FeeExtendedSelectorUM
import com.tangem.features.send.v2.feeselector.component.speed.ui.RegularFeeItemContent
import kotlinx.collections.immutable.persistentListOf
import java.math.BigDecimal

@Composable
fun FeeExtendedSelectorContent(state: FeeExtendedSelectorUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .padding(
                bottom = 16.dp,
                start = 16.dp,
                end = 16.dp,
            )
            .fillMaxWidth(),
    ) {
        TokenItem(
            modifier = Modifier
                .clip(RoundedCornerShape(TangemTheme.dimens.radius14))
                .background(color = TangemTheme.colors.background.action),
            state = state.token,
            isBalanceHidden = false,
        )

        SpacerH8()

        val feeFiatRateUM = state.parent.feeFiatRateUM
        val fee = state.fee.fee
        val isChooseSpeedAvailable = state.parent.feeItems.size > 1

        RegularFeeItemContent(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(TangemTheme.dimens.radius14))
                .conditional(isChooseSpeedAvailable) {
                    clickable(onClick = state.onFeeClick)
                }
                .background(
                    TangemTheme.colors.background.action,
                    RoundedCornerShape(TangemTheme.dimens.radius14),
                ),
            title = state.fee.title,
            iconRes = state.fee.iconRes,
            iconBackgroundColor = TangemTheme.colors.icon.accent.copy(alpha = 0.1F),
            iconTint = TangemTheme.colors.icon.accent,
            preDot = stringReference(
                fee.amount.value.format {
                    crypto(
                        symbol = fee.amount.currencySymbol,
                        decimals = fee.amount.decimals,
                    ).fee(canBeLower = state.parent.feeExtraInfo.isFeeApproximate)
                },
            ),
            postDot = if (state.parent.feeExtraInfo.isFeeConvertibleToFiat && feeFiatRateUM != null) {
                getFiatReference(
                    value = fee.amount.value,
                    rate = feeFiatRateUM.rate,
                    appCurrency = feeFiatRateUM.appCurrency,
                )
            } else {
                null
            },
            ellipsizeOffset = fee.amount.currencySymbol.length,
            showSelectorIcon = isChooseSpeedAvailable,
            isNotEnoughFunds = state.parent.feeExtraInfo.isNotEnoughFunds,
            isLoading = state.fee.isLoading(),
        )
    }
}

@Suppress("LongMethod")
@Preview
@Composable
private fun Preview() {
    val tokenItemState = TokenItemState.Content(
        id = "1",
        iconState = CurrencyIconState.Locked,
        titleState = TokenItemState.TitleState.Content(text = stringReference(value = "Bitcoin")),
        fiatAmountState = TokenItemState.FiatAmountState.Icon(
            R.drawable.ic_select_18_24,
            tint = IconTint.Informative,
        ),
        subtitle2State = null,
        subtitleState = TokenItemState.SubtitleState.TextContent(
            value = stringReference("Balance: 0,35853044 BTC"),
            isAvailable = false,
        ),
        onItemClick = {},
        onItemLongClick = {},
    )

    val cryptoCurrencyStatus = CryptoCurrencyStatus(
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

    val lowFeeItem =
        FeeItem.Market(Fee.Common(amount = Amount(value = BigDecimal("0.0002876"), blockchain = Blockchain.Ethereum)))

    TangemThemePreview {
        FeeExtendedSelectorContent(
            modifier = Modifier.background(TangemTheme.colors.background.tertiary),
            state = FeeExtendedSelectorUM(
                parent = FeeSelectorUM.Content(
                    isPrimaryButtonEnabled = true,
                    feeItems = persistentListOf(lowFeeItem),
                    selectedFeeItem = lowFeeItem,
                    feeExtraInfo = FeeExtraInfo(
                        isFeeApproximate = false,
                        isFeeConvertibleToFiat = true,
                        isTronToken = false,
                        feeCryptoCurrencyStatus = cryptoCurrencyStatus,
                        isNotEnoughFunds = true,
                    ),
                    feeNonce = FeeNonce.None,
                    feeFiatRateUM = FeeFiatRateUM(
                        rate = BigDecimal("2500"),
                        appCurrency = AppCurrency.Default,
                    ),
                    fees = TransactionFee.Single(lowFeeItem.fee),
                ),
                token = tokenItemState,
                fee = FeeItem.Market(
                    fee = Fee.Common(
                        Amount(
                            value = BigDecimal("0.02"),
                            blockchain = Blockchain.Ethereum,
                        ),
                    ),
                ),
                onFeeClick = {},
            ),
        )
    }
}