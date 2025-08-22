package com.tangem.features.onramp.hottokens.portfolio.model

import arrow.core.getOrElse
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.domain.card.DerivePublicKeysUseCase
import com.tangem.domain.tokens.AddCryptoCurrenciesUseCase
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.features.onramp.hottokens.portfolio.OnrampAddToPortfolioComponent
import com.tangem.features.onramp.hottokens.portfolio.entity.OnrampAddToPortfolioUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Model for adding token to portfolio
 *
 * @param paramsContainer               params container
 * @property dispatchers                dispatchers
 * @property derivePublicKeysUseCase    use case for deriving public key
 * @property addCryptoCurrenciesUseCase use case for adding crypto currency
 * @property getUserWalletUseCase       use case for getting user wallet by id
 *
[REDACTED_AUTHOR]
 */
@ModelScoped
internal class OnrampAddToPortfolioModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val derivePublicKeysUseCase: DerivePublicKeysUseCase,
    private val addCryptoCurrenciesUseCase: AddCryptoCurrenciesUseCase,
    private val getUserWalletUseCase: GetUserWalletUseCase,
) : Model() {

    private val params: OnrampAddToPortfolioComponent.Params = paramsContainer.require()

    val state: StateFlow<OnrampAddToPortfolioUM> get() = _state
    private val _state = MutableStateFlow(value = getInitialState())

    private fun getInitialState(): OnrampAddToPortfolioUM {
        return OnrampAddToPortfolioUM(
            walletName = getUserWalletName(),
            currencyName = params.cryptoCurrency.name,
            networkName = params.cryptoCurrency.network.name,
            currencyIconState = params.currencyIconState,
            addButtonUM = OnrampAddToPortfolioUM.AddButtonUM(isProgress = false, onClick = ::onAddClick),
        )
    }

    private fun getUserWalletName(): String {
        return getUserWalletUseCase(params.userWalletId)
            .onLeft { Timber.e("Unable to get wallet name by id [${params.userWalletId}]: $it") }
            .fold(ifLeft = { "" }, ifRight = UserWallet::name)
    }

    private fun onAddClick() {
        modelScope.launch {
            changeAddButtonProgressStatus(isProgress = true)

            derivePublicKeysUseCase(params.userWalletId, listOfNotNull(params.cryptoCurrency)).getOrElse {
                Timber.e("Failed to derive public keys: $it")

                changeAddButtonProgressStatus(isProgress = false)
            }

            addCryptoCurrenciesUseCase(
                userWalletId = params.userWalletId,
                currency = params.cryptoCurrency,
            )
                .onRight { params.onSuccessAdding(params.cryptoCurrency.id) }
                .onLeft { changeAddButtonProgressStatus(isProgress = false) }
        }
    }

    private fun changeAddButtonProgressStatus(isProgress: Boolean) {
        _state.update {
            it.copy(addButtonUM = it.addButtonUM.copy(isProgress = isProgress))
        }
    }
}