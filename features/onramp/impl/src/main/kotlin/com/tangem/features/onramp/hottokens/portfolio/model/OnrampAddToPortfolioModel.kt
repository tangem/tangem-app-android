package com.tangem.features.onramp.hottokens.portfolio.model

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.domain.account.status.usecase.ManageCryptoCurrenciesUseCase
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.features.onramp.hottokens.portfolio.OnrampAddToPortfolioComponent
import com.tangem.features.onramp.hottokens.portfolio.entity.OnrampAddToPortfolioUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.tangem.utils.logging.TangemLogger
import javax.inject.Inject

/**
 * Model for adding token to portfolio
 *
 * @param paramsContainer                  params container
 * @property dispatchers                   dispatchers
 * @property manageCryptoCurrenciesUseCase use case for managing crypto currencies
 * @property getUserWalletUseCase          use case for getting user wallet by id
 *
[REDACTED_AUTHOR]
 */
@ModelScoped
internal class OnrampAddToPortfolioModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val manageCryptoCurrenciesUseCase: ManageCryptoCurrenciesUseCase,
    private val getUserWalletUseCase: GetUserWalletUseCase,
) : Model() {

    private val params: OnrampAddToPortfolioComponent.Params = paramsContainer.require()

    val state: StateFlow<OnrampAddToPortfolioUM>
        field = MutableStateFlow(value = getInitialState())

    private fun getInitialState(): OnrampAddToPortfolioUM {
        return OnrampAddToPortfolioUM(
            walletName = getUserWalletName(),
            currencyName = params.cryptoCurrency.name,
            networkName = params.cryptoCurrency.network.name,
            currencyIconState = params.currencyIconState,
            addButtonUM = OnrampAddToPortfolioUM.AddButtonUM(
                isProgress = false,
                isTangemIconVisible = isTangemIconVisible(),
                onClick = ::onAddClick,
            ),
        )
    }

    private fun isTangemIconVisible(): Boolean {
        return getUserWalletUseCase(params.userWalletId)
            .onLeft { TangemLogger.e("Unable to get wallet by id [${params.userWalletId}]: $it") }
            .fold(ifLeft = { false }, ifRight = { it is UserWallet.Cold })
    }

    private fun getUserWalletName(): String {
        return getUserWalletUseCase(params.userWalletId)
            .onLeft { TangemLogger.e("Unable to get wallet name by id [${params.userWalletId}]: $it") }
            .fold(ifLeft = { "" }, ifRight = UserWallet::name)
    }

    private fun onAddClick() {
        modelScope.launch {
            changeAddButtonProgressStatus(isProgress = true)

            val accountId = AccountId.forMainCryptoPortfolio(params.userWalletId)
            manageCryptoCurrenciesUseCase(accountId = accountId, add = params.cryptoCurrency)
                .onRight { params.onSuccessAdding(params.cryptoCurrency.id) }
                .onLeft { throwable ->
                    TangemLogger.e("Failed to add crypto currency: $throwable")
                    changeAddButtonProgressStatus(isProgress = false)
                }
        }
    }

    private fun changeAddButtonProgressStatus(isProgress: Boolean) {
        state.update { prevState ->
            prevState.copy(addButtonUM = prevState.addButtonUM.copy(isProgress = isProgress))
        }
    }
}