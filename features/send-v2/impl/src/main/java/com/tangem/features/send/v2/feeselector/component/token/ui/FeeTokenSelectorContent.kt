package com.tangem.features.send.v2.feeselector.component.token.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.core.ui.components.SpacerH8
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.components.token.TokenItem
import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.core.ui.extensions.selectedBorder
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.features.send.v2.api.entity.*
import com.tangem.features.send.v2.feeselector.component.token.FeeTokenSelectorIntents
import com.tangem.features.send.v2.feeselector.component.token.StubFeeTokenSelectorIntents
import com.tangem.features.send.v2.feeselector.component.token.entity.FeeTokenSelectorUM
import com.tangem.features.send.v2.impl.R
import com.tangem.utils.StringsSigns
import kotlinx.collections.immutable.persistentListOf
import java.math.BigDecimal

@Composable
internal fun FeeTokenSelectorContent(
    state: FeeTokenSelectorUM,
    intents: FeeTokenSelectorIntents,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(
            bottom = 16.dp,
            start = 16.dp,
            end = 16.dp,
        ),
    ) {
        Title(onLearnMoreClick = intents::onLearnMoreClick)
        SpacerH8()
        TokenListContent(state)
    }
}

@Composable
private fun Title(modifier: Modifier = Modifier, onLearnMoreClick: () -> Unit) {
    val linkText = stringResourceSafe(R.string.common_learn_more)
    val fullString = stringResourceSafe(R.string.fee_selector_choose_token_description, linkText)
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
internal fun TokenListContent(state: FeeTokenSelectorUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        state.tokens.fastForEach { item ->
            key(item.id) {
                TokenItem(
                    modifier = Modifier
                        .selectedBorder(isSelected = state.selectedToken.id == item.id)
                        .clip(RoundedCornerShape(TangemTheme.dimens.radius14))
                        .background(color = TangemTheme.colors.background.action),
                    state = item,
                    isBalanceHidden = false,
                )
            }
        }
    }
}

@Suppress("LongMethod")
@Preview
@Composable
private fun Preview() {
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

    val parentUM = FeeSelectorUM.Content(
        isPrimaryButtonEnabled = false,
        feeItems = persistentListOf(
            FeeItem.Market(
                fee = Fee.Common(
                    Amount(
                        value = BigDecimal("0.02"),
                        blockchain = Blockchain.Ethereum,
                    ),
                ),
            ),
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
        fees = TransactionFee.Single(
            Fee.Common(
                Amount(
                    value = BigDecimal("0.01"),
                    blockchain = Blockchain.Ethereum,
                ),
            ),
        ),
    )

    val tokenItemState = TokenItemState.Content(
        id = "1",
        iconState = CurrencyIconState.Locked,
        titleState = TokenItemState.TitleState.Content(text = stringReference(value = "Bitcoin")),
        fiatAmountState = TokenItemState.FiatAmountState.Empty,
        subtitle2State = null,
        subtitleState = TokenItemState.SubtitleState.TextContent(
            value = stringReference("Balance: 0,35853044 BTC"),
            isAvailable = false,
        ),
        onItemClick = {},
        onItemLongClick = {},
    )

    val state = FeeTokenSelectorUM(
        parent = parentUM,
        selectedToken = tokenItemState,
        tokens = persistentListOf(tokenItemState, tokenItemState.copy(id = "2")),
    )

    TangemThemePreview {
        FeeTokenSelectorContent(
            state = state,
            intents = StubFeeTokenSelectorIntents(),
            modifier = Modifier
                .background(TangemTheme.colors.background.tertiary),
        )
    }
}