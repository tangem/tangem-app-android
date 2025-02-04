package com.tangem.features.onramp.hottokens.portfolio.model

import arrow.core.getOrElse
import com.tangem.core.decompose.di.ComponentScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.domain.card.DerivePublicKeysUseCase
import com.tangem.domain.tokens.AddCryptoCurrenciesUseCase
import com.tangem.features.onramp.hottokens.portfolio.OnrampAddToPortfolioComponent
import com.tangem.features.onramp.hottokens.portfolio.entity.OnrampAddToPortfolioUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
 *
[REDACTED_AUTHOR]
 */
@ComponentScoped
internal class OnrampAddToPortfolioModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val derivePublicKeysUseCase: DerivePublicKeysUseCase,
    private val addCryptoCurrenciesUseCase: AddCryptoCurrenciesUseCase,
) : Model() {

    private val params: OnrampAddToPortfolioComponent.Params = paramsContainer.require()

    val state: StateFlow<OnrampAddToPortfolioUM> get() = _state
    private val _state = MutableStateFlow(value = getInitialState())

    private fun getInitialState(): OnrampAddToPortfolioUM {
        return OnrampAddToPortfolioUM(
            currencyName = params.cryptoCurrency.name,
            networkName = params.cryptoCurrency.network.name,
            currencyIconState = params.currencyIconState,
            onAddClick = ::onAddClick,
        )
    }

    private fun onAddClick() {
        modelScope.launch {
            derivePublicKeysUseCase(params.userWalletId, listOfNotNull(params.cryptoCurrency)).getOrElse {
                Timber.e("Failed to derive public keys: $it")
            }

            addCryptoCurrenciesUseCase(
                userWalletId = params.userWalletId,
                currencies = listOfNotNull(params.cryptoCurrency),
            )
                .onRight { params.onSuccessAdding(params.cryptoCurrency.id) }
        }
    }
}