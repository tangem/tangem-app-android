package com.tangem.feature.tokendetails.presentation.tokendetails.state.factory

import com.tangem.core.ui.components.marketprice.MarketPriceBlockState
import com.tangem.domain.tokens.models.CryptoCurrency
import com.tangem.feature.tokendetails.presentation.tokendetails.TokenDetailsPreviewData
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsBalanceBlockState
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsState
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsTopAppBarConfig
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenInfoBlockState
import com.tangem.feature.tokendetails.presentation.tokendetails.state.factory.TokenDetailsSkeletonStateConverter.SkeletonModel
import com.tangem.feature.tokendetails.presentation.tokendetails.viewmodels.TokenDetailsClickIntents
import com.tangem.features.tokendetails.impl.R
import com.tangem.utils.converter.Converter

internal class TokenDetailsSkeletonStateConverter(
    private val clickIntents: TokenDetailsClickIntents,
) : Converter<SkeletonModel, TokenDetailsState> {

    override fun convert(value: SkeletonModel): TokenDetailsState {
        return TokenDetailsState(
            topAppBarConfig = TokenDetailsTopAppBarConfig(
                onBackClick = clickIntents::onBackClick,
                onMoreClick = clickIntents::onMoreClick,
            ),
            tokenInfoBlockState = TokenInfoBlockState(
                name = value.cryptoCurrency.name,
                iconUrl = requireNotNull(value.cryptoCurrency.iconUrl),
                currency = when (value.cryptoCurrency) {
                    is CryptoCurrency.Coin -> TokenInfoBlockState.Currency.Native
                    is CryptoCurrency.Token -> TokenInfoBlockState.Currency.Token(
                        networkName = value.cryptoCurrency.network.standardType.name,
                        blockchainName = value.cryptoCurrency.network.name,
                        // TODO: [REDACTED_JIRA]
                        networkIcon = R.drawable.img_eth_22,
                    )
                },
            ),
            tokenBalanceBlockState = TokenDetailsBalanceBlockState.Loading(
                TokenDetailsPreviewData.disabledActionButtons,
            ),
            marketPriceBlockState = MarketPriceBlockState.Loading(value.cryptoCurrency.name),
        )
    }

    data class SkeletonModel(val cryptoCurrency: CryptoCurrency)
}