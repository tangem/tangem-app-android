package com.tangem.features.onramp.hottokens.portfolio.entity

import com.tangem.common.ui.account.CryptoPortfolioIconUM
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
import com.tangem.domain.account.usecase.IsAccountsModeEnabledUseCase
import com.tangem.domain.models.account.AccountStatus
import com.tangem.features.onramp.hottokens.portfolio.OnrampAddTokenComponent
import com.tangem.features.onramp.hottokens.portfolio.OnrampAddTokenComponent.AddHotCryptoData
import com.tangem.features.onramp.impl.R
import javax.inject.Inject

@ModelScoped
internal class OnrampAddTokenUiBuilder @Inject constructor(
    paramsContainer: ParamsContainer,
    private val isAccountsModeEnabledUseCase: IsAccountsModeEnabledUseCase,
) {
    private val params = paramsContainer.require<OnrampAddTokenComponent.Params>()

    private fun createNetwork(tokenToAdd: AddHotCryptoData): AddTokenUM.Network {
        return AddTokenUM.Network(
            icon = tokenToAdd.cryptoCurrency.network.iconResId,
            name = stringReference(tokenToAdd.cryptoCurrency.network.name),
            editable = false,
            onClick = {},
        )
    }

    private suspend fun createPortfolio(tokenToAdd: AddHotCryptoData): PortfolioSelectUM {
        val accountIcon: CryptoPortfolioIconUM?
        val portfolioName: TextReference
        val isAccountMode = isAccountsModeEnabledUseCase.invokeSync()
        when (isAccountMode) {
            false -> {
                accountIcon = null
                portfolioName = stringReference(tokenToAdd.userWallet.name)
            }
            true -> {
                val accountStatus = tokenToAdd.account
                portfolioName = accountStatus.account.accountName.toUM().value
                accountIcon = when (accountStatus) {
                    is AccountStatus.CryptoPortfolio -> accountStatus.account.icon.toUM()
                }
            }
        }
        return PortfolioSelectUM(
            icon = accountIcon,
            name = portfolioName,
            isAccountMode = isAccountMode,
            isMultiChoice = tokenToAdd.availableMorePortfolio,
            onClick = { params.callbacks.onChangePortfolioClick() },
        )
    }

    suspend fun updateContent(
        isTangemIconVisible: Boolean,
        onConfirmClick: () -> Unit,
        tokenToAdd: AddHotCryptoData,
    ): AddTokenUM {
        val button = AddTokenUM.Button(
            showProgress = false,
            isTangemIconVisible = isTangemIconVisible,
            text = resourceReference(R.string.common_add),
            onConfirmClick = onConfirmClick,
            isEnabled = true,
        )
        val networkUM = createNetwork(tokenToAdd)
        val portfolioUM = createPortfolio(tokenToAdd)
        val currency = tokenToAdd.cryptoCurrency
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