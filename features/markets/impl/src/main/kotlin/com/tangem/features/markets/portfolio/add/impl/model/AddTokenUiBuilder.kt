package com.tangem.features.markets.portfolio.add.impl.model

import com.tangem.common.ui.account.CryptoPortfolioIconUM
import com.tangem.common.ui.account.toUM
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.ui.components.currency.icon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.iconResId
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.models.account.AccountStatus
import com.tangem.features.markets.impl.R
import com.tangem.features.markets.portfolio.add.api.SelectedNetwork
import com.tangem.features.markets.portfolio.add.api.SelectedPortfolio
import com.tangem.features.markets.portfolio.add.impl.AddTokenComponent
import com.tangem.features.markets.portfolio.add.impl.ui.state.AddTokenUM

@ModelScoped
internal class AddTokenUiBuilder(
    paramsContainer: ParamsContainer,
) {
    private val params = paramsContainer.require<AddTokenComponent.Params>()

    private fun createNetwork(selectedNetwork: SelectedNetwork): AddTokenUM.Network {
        return AddTokenUM.Network(
            icon = selectedNetwork.cryptoCurrency.network.iconResId,
            name = stringReference(selectedNetwork.cryptoCurrency.network.name),
            editable = selectedNetwork.availableMoreNetwork,
            onClick = params.onChangeNetworkClick,
        )
    }

    private fun createPortfolio(selectedPortfolio: SelectedPortfolio): AddTokenUM.Portfolio {
        val accountIcon: CryptoPortfolioIconUM?
        val portfolioName: TextReference
        when (selectedPortfolio.isAccountMode) {
            false -> {
                accountIcon = null
                portfolioName = stringReference(selectedPortfolio.userWallet.name)
            }
            true -> {
                portfolioName = selectedPortfolio.account.account.accountName.toUM().value
                accountIcon = when (selectedPortfolio.account) {
                    is AccountStatus.CryptoPortfolio -> selectedPortfolio.account.account.icon.toUM()
                }
            }
        }
        return AddTokenUM.Portfolio(
            accountIconUM = accountIcon,
            name = portfolioName,
            editable = selectedPortfolio.availableMorePortfolio,
            onClick = params.onChangePortfolioClick,
        )
    }

    fun updateContent(
        current: AddTokenUM,
        selectedPortfolio: SelectedPortfolio,
        selectedNetwork: SelectedNetwork,
        isTangemIconVisible: Boolean,
        onConfirmClick: () -> Unit,
    ): AddTokenUM = current.copy(
        network = createNetwork(selectedNetwork),
        portfolio = createPortfolio(selectedPortfolio),
        button = current.button.copy(
            isEnabled = true,
            showProgress = false,
            isTangemIconVisible = isTangemIconVisible,
            onConfirmClick = onConfirmClick,
        ),
    )

    fun getInitialState(): AddTokenUM {
        val selectedNetwork = params.selectedNetwork.value
        val selectedPortfolio = params.selectedPortfolio.value
        val networkUM = createNetwork(selectedNetwork)
        val accountIcon: CryptoPortfolioIconUM?
        val portfolioName: TextReference
        when (selectedPortfolio.isAccountMode) {
            false -> {
                accountIcon = null
                portfolioName = stringReference(selectedPortfolio.userWallet.name)
            }
            true -> {
                portfolioName = selectedPortfolio.account.account.accountName.toUM().value
                accountIcon = when (selectedPortfolio.account) {
                    is AccountStatus.CryptoPortfolio -> selectedPortfolio.account.account.icon.toUM()
                }
            }
        }
        val portfolioUM = AddTokenUM.Portfolio(
            accountIconUM = accountIcon,
            name = portfolioName,
            editable = selectedPortfolio.availableMorePortfolio,
            onClick = params.onChangePortfolioClick,
        )

        val button = AddTokenUM.Button(
            isEnabled = false,
            showProgress = false,
            isTangemIconVisible = false,
            text = resourceReference(R.string.common_add),
            onConfirmClick = { },
        )
        val currency = selectedNetwork.cryptoCurrency
        val tokenToAdd = TokenItemState.Content(
            id = currency.id.value,
            iconState = CryptoCurrencyToIconStateConverter().convert(currency),
            titleState = TokenItemState.TitleState.Content(stringReference(currency.name)),
            fiatAmountState = TokenItemState.FiatAmountState.Content(text = ""),
            subtitle2State = TokenItemState.Subtitle2State.TextContent(text = ""),
            subtitleState = TokenItemState.SubtitleState.TextContent(stringReference(currency.symbol)),
            onItemClick = null,
            onItemLongClick = null,
        )
        return AddTokenUM(
            tokenToAdd = tokenToAdd,
            network = networkUM,
            portfolio = portfolioUM,
            button = button,
        )
    }

    companion object {

        fun AddTokenUM.toggleProgress(showProgress: Boolean) = this.copy(
            button = this.button.copy(showProgress = showProgress),
        )
    }
}