package com.tangem.features.markets.portfolio.impl.model

import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.features.markets.portfolio.impl.ui.state.MyPortfolioUM
import com.tangem.features.markets.portfolio.impl.ui.state.MyPortfolioUM.Tokens.AddButtonState
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.toImmutableList

/**
 * Converter from [Map] of [UserWallet] and [CryptoCurrencyStatus] to [MyPortfolioUM.Tokens]
 *
 * @author Andrew Khokhlov on 26/08/2024
 */
internal class TokensPortfolioUMConverter(
    private val appCurrency: AppCurrency,
    private val isBalanceHidden: Boolean,
    private val isAllAvailableNetworksAdded: Boolean,
    private val bsConfig: TangemBottomSheetConfig,
    private val onAddClick: () -> Unit,
    private val onTokenItemClick: (index: Int, id: CryptoCurrency.ID) -> Unit,
) : Converter<Map<UserWallet, List<CryptoCurrencyStatus>>, MyPortfolioUM.Tokens> {

    override fun convert(value: Map<UserWallet, List<CryptoCurrencyStatus>>): MyPortfolioUM.Tokens {
        return MyPortfolioUM.Tokens(
            tokens = value
                .flatMap { entry ->
                    entry.value.map { entry.key to it }
                }
                .mapIndexed { index, status ->
                    PortfolioTokenUMConverter(
                        appCurrency = appCurrency,
                        isBalanceHidden = isBalanceHidden,
                        onTokenItemClick = { onTokenItemClick(index, it.currency.id) },
                    ).convert(status)
                }
                .toImmutableList(),
            buttonState = if (isAllAvailableNetworksAdded) AddButtonState.Unavailable else AddButtonState.Available,
            bsConfig = bsConfig,
            onAddClick = onAddClick,
        )
    }
}
