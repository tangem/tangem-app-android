package com.tangem.features.markets.portfolio.impl.model

import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.features.markets.portfolio.impl.loader.PortfolioData
import com.tangem.features.markets.portfolio.impl.ui.state.MyPortfolioUM
import com.tangem.features.markets.portfolio.impl.ui.state.MyPortfolioUM.Tokens.AddButtonState
import com.tangem.features.markets.portfolio.impl.ui.state.PortfolioTokenUM
import com.tangem.utils.Provider
import com.tangem.utils.converter.Converter
import com.tangem.utils.extensions.isZero
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

/**
 * Converter from [Map] of [UserWallet] and [CryptoCurrencyStatus] to [MyPortfolioUM.Tokens]
 *
 * @author Andrew Khokhlov on 26/08/2024
 */
@Suppress("LongParameterList")
internal class TokensPortfolioUMConverter(
    private val appCurrency: AppCurrency,
    private val isBalanceHidden: Boolean,
    private val isAllAvailableNetworksAdded: Boolean,
    private val bsConfig: TangemBottomSheetConfig,
    private val onAddClick: () -> Unit,
    private val quickActionsIntents: TokenActionsHandler,
    private val currentState: Provider<MyPortfolioUM>,
    private val updateTokens: ((ImmutableList<PortfolioTokenUM>) -> ImmutableList<PortfolioTokenUM>) -> Unit,
) : Converter<Map<UserWallet, List<PortfolioData.CryptoCurrencyData>>, MyPortfolioUM.Tokens> {

    override fun convert(value: Map<UserWallet, List<PortfolioData.CryptoCurrencyData>>): MyPortfolioUM.Tokens {
        val currentTokensState = currentState() as? MyPortfolioUM.Tokens

        return MyPortfolioUM.Tokens(
            tokens = value
                .flatMap { entry -> entry.value }
                .map { cryptoData ->
                    PortfolioTokenUMConverter(
                        appCurrency = appCurrency,
                        isBalanceHidden = isBalanceHidden,
                        onTokenItemClick = { toggleQuickActions(cryptoData) },
                        tokenActionsHandler = quickActionsIntents,
                    ).convert(value = cryptoData) to cryptoData
                }
                .setQuickActionsVisibility(currentTokensState)
                .toImmutableList(),
            buttonState = if (isAllAvailableNetworksAdded) AddButtonState.Unavailable else AddButtonState.Available,
            addToPortfolioBSConfig = bsConfig,
            onAddClick = onAddClick,
            tokenReceiveBSConfig = (currentState() as? MyPortfolioUM.Tokens)
                ?.tokenReceiveBSConfig
                ?: TangemBottomSheetConfig.Empty,
            tokenActionsBSConfig = (currentState() as? MyPortfolioUM.Tokens)
                ?.tokenActionsBSConfig
                ?: TangemBottomSheetConfig.Empty,
        )
    }

    private fun List<Pair<PortfolioTokenUM, PortfolioData.CryptoCurrencyData>>.setQuickActionsVisibility(
        currentState: MyPortfolioUM.Tokens?,
    ): List<PortfolioTokenUM> {
        if (currentState == null) {
            // if there are more than one token with empty balance, hide quick actions for all
            if (this.count { (_, cryptoData) -> isEmptyBalance(cryptoData) } > 1) {
                return this.map { (token, _) ->
                    token.copy(isQuickActionsShown = false)
                }
            }

            // if during first convert there is one token with empty balance, show quick actions for it
            val firstWithEmptyBalance = this.firstOrNull { (_, cryptoData) -> isEmptyBalance(cryptoData) }?.first

            return this.map { (token, cryptoData) ->
                if (token == firstWithEmptyBalance) {
                    token.copy(isQuickActionsShown = true)
                } else {
                    token.copy(isQuickActionsShown = false)
                }
            }
        }

        val previousList = currentState.tokens

        // if new token with empty balance appeared, show quick actions for it
        val newTokenWithEmptyBalance: PortfolioTokenUM? = this.firstOrNull { (token, cryptoData) ->
            val exist = previousList.any { it.matchWith(token) }

            exist.not() && isEmptyBalance(cryptoData)
        }?.first

        return if (newTokenWithEmptyBalance != null) {
            this.map { (token, _) ->
                token.copy(isQuickActionsShown = token == newTokenWithEmptyBalance)
            }
        } else {
            // otherwise, keep previous state
            this.map { (token, _) ->
                token.copy(
                    isQuickActionsShown = previousList
                        .firstOrNull { it.matchWith(token) }
                        ?.isQuickActionsShown ?: false,
                )
            }
        }
    }

    private fun isEmptyBalance(cryptoData: PortfolioData.CryptoCurrencyData): Boolean {
        return cryptoData.status.value.amount?.isZero() ?: false
    }

    private fun toggleQuickActions(cryptoData: PortfolioData.CryptoCurrencyData) {
        updateTokens { tokenList ->
            tokenList.map {
                it.copy(
                    isQuickActionsShown = if (it.matchWith(cryptoData)) {
                        !it.isQuickActionsShown
                    } else {
                        false
                    },
                )
            }.toImmutableList()
        }
    }

    private fun PortfolioTokenUM.matchWith(token: PortfolioTokenUM): Boolean {
        return this.walletId == token.walletId && this.tokenItemState.id == token.tokenItemState.id
    }

    private fun PortfolioTokenUM.matchWith(cryptoData: PortfolioData.CryptoCurrencyData): Boolean {
        return this.walletId == cryptoData.userWallet.walletId &&
            this.tokenItemState.id == cryptoData.status.currency.id.value
    }
}
