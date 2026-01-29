package com.tangem.features.feed.components.market.details.portfolio.add.impl.model

import com.tangem.common.ui.account.AccountIconUM
import com.tangem.common.ui.account.CryptoPortfolioIconConverter
import com.tangem.common.ui.account.PortfolioSelectUM
import com.tangem.common.ui.account.toUM
import com.tangem.common.ui.addtoken.AddTokenUM
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.ui.components.currency.icon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.iconResId
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.models.account.AccountStatus.*
import com.tangem.features.feed.components.market.details.portfolio.add.SelectedNetwork
import com.tangem.features.feed.components.market.details.portfolio.add.SelectedPortfolio
import com.tangem.features.feed.components.market.details.portfolio.add.impl.AddTokenComponent
import com.tangem.features.feed.impl.R
import javax.inject.Inject

@ModelScoped
internal class AddTokenUiBuilder @Inject constructor(
    paramsContainer: ParamsContainer,
) {
    private val params = paramsContainer.require<AddTokenComponent.Params>()

    private fun createNetwork(selectedNetwork: SelectedNetwork): AddTokenUM.Network {
        return AddTokenUM.Network(
            icon = selectedNetwork.cryptoCurrency.network.iconResId,
            name = stringReference(selectedNetwork.cryptoCurrency.network.name),
            editable = selectedNetwork.isAvailableMoreNetwork,
            onClick = { params.callbacks.onChangeNetworkClick() },
        )
    }

    private fun createPortfolio(selectedPortfolio: SelectedPortfolio): PortfolioSelectUM {
        val accountIcon: AccountIconUM?
        val portfolioName: TextReference
        when (selectedPortfolio.isAccountMode) {
            false -> {
                accountIcon = null
                portfolioName = stringReference(selectedPortfolio.userWallet.name)
            }
            true -> {
                val accountStatus = selectedPortfolio.account.account
                portfolioName = accountStatus.account.accountName.toUM().value
                accountIcon = when (accountStatus) {
                    is Crypto.Portfolio -> CryptoPortfolioIconConverter.convert(accountStatus.account.icon)
                    is Payment -> AccountIconUM.Payment
                }
            }
        }
        return PortfolioSelectUM(
            icon = accountIcon,
            name = portfolioName,
            isAccountMode = selectedPortfolio.isAccountMode,
            isMultiChoice = selectedPortfolio.isAvailableMorePortfolio,
            onClick = { params.callbacks.onChangePortfolioClick() },
        )
    }

    fun updateContent(
        selectedPortfolio: SelectedPortfolio,
        selectedNetwork: SelectedNetwork,
        isTangemIconVisible: Boolean,
        onConfirmClick: () -> Unit,
    ): AddTokenUM {
        // its may happens when change portfolio after selected both params in line navigation
        val isAvailableNetwork = selectedPortfolio.account.availableToAddNetworks
            .any { selectedNetwork.selectedNetwork.networkId == it.networkId }
        val button = AddTokenUM.Button(
            isEnabled = isAvailableNetwork,
            showProgress = false,
            isTangemIconVisible = isTangemIconVisible,
            text = resourceReference(R.string.common_add),
            onConfirmClick = onConfirmClick,
        )
        val networkUM = createNetwork(selectedNetwork)
        val portfolioUM = createPortfolio(selectedPortfolio)
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