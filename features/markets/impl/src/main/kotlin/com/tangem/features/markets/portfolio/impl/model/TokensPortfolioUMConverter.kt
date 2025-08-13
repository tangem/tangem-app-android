package com.tangem.features.markets.portfolio.impl.model

import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
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
[REDACTED_AUTHOR]
 */
@Suppress("LongParameterList")
internal class TokensPortfolioUMConverter(
    private val appCurrency: AppCurrency,
    private val isBalanceHidden: Boolean,
    private val addButtonState: AddButtonState,
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
                .setQuickActionsVisibility(currentState = currentTokensState)
                .toImmutableList(),
            buttonState = addButtonState,
            addToPortfolioBSConfig = bsConfig,
            onAddClick = onAddClick,
            tokenReceiveBSConfig = (currentState() as? MyPortfolioUM.Tokens)
                ?.tokenReceiveBSConfig
                ?: TangemBottomSheetConfig.Empty,
        )
    }

    private fun List<Pair<PortfolioTokenUM, PortfolioData.CryptoCurrencyData>>.setQuickActionsVisibility(
        currentState: MyPortfolioUM.Tokens?,
    ): List<PortfolioTokenUM> {
        return when {
            // if there is only one token and it has empty balance, show quick actions for it
            currentState == null && this.size == 1 && isEmptyBalance(this.first().second) -> {
                this.map { (token, _) ->
                    token.copy(isQuickActionsShown = true)
                }
            }
            // if there is no previous state, hide quick actions for all tokens
            currentState == null -> {
                this.map { (token, _) ->
                    token.copy(isQuickActionsShown = false)
                }
            }
            else -> {
                val previousList = currentState.tokens

                // otherwise, keep previous state
                this.map { (token, _) ->
                    token.copy(
                        isQuickActionsShown = previousList
                            .firstOrNull { it.matchWith(token) }
                            ?.isQuickActionsShown == true,
                    )
                }
            }
        }
    }

    private fun isEmptyBalance(cryptoData: PortfolioData.CryptoCurrencyData): Boolean {
        return cryptoData.status.value.amount?.isZero() == true
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