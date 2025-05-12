package com.tangem.features.onramp.hottokens.model

import arrow.core.getOrElse
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.core.ui.components.tokenlist.state.TokensListItemUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.onramp.GetHotCryptoUseCase
import com.tangem.domain.onramp.model.HotCryptoCurrency
import com.tangem.domain.tokens.GetSingleCryptoCurrencyStatusUseCase
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.features.onramp.hottokens.HotCryptoComponent
import com.tangem.features.onramp.hottokens.converter.HotTokenItemStateConverter
import com.tangem.features.onramp.hottokens.entity.HotCryptoUM
import com.tangem.features.onramp.hottokens.portfolio.entity.OnrampAddToPortfolioBSConfig
import com.tangem.features.onramp.impl.R
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Hot crypto model
 *
 * @param paramsContainer                       params container
 * @param getHotCryptoUseCase                   use case for getting hot crypto
 * @param getSelectedAppCurrencyUseCase         use case for getting selected app currency
 * @property getCryptoCurrencyStatusSyncUseCase use case for getting crypto currency status by id
 * @property dispatchers                        dispatchers
 *
[REDACTED_AUTHOR]
 */
@ModelScoped
internal class HotCryptoModel @Inject constructor(
    paramsContainer: ParamsContainer,
    getHotCryptoUseCase: GetHotCryptoUseCase,
    getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    private val getSingleCryptoCurrencyStatusUseCase: GetSingleCryptoCurrencyStatusUseCase,
    override val dispatchers: CoroutineDispatcherProvider,
) : Model() {

    val bottomSheetNavigation: SlotNavigation<OnrampAddToPortfolioBSConfig> = SlotNavigation()

    val state: StateFlow<HotCryptoUM> get() = _state
    private val _state = MutableStateFlow(value = HotCryptoUM(items = persistentListOf()))

    private val params: HotCryptoComponent.Params = paramsContainer.require()

    init {
        combine(
            flow = getSelectedAppCurrencyUseCase().map { it.getOrElse { AppCurrency.Default } },
            flow2 = getHotCryptoUseCase(params.userWalletId),
        ) { appCurrency, currencies ->
            HotTokenItemStateConverter(appCurrency = appCurrency, onItemClick = ::onTokenClick)
                .convertList(currencies)
                .map(TokensListItemUM::Token)
        }
            .onEach { items ->
                _state.update {
                    HotCryptoUM(
                        items = buildList {
                            if (items.isNotEmpty()) {
                                add(it.getOrCreateGroupTitle())
                            }

                            addAll(items)
                        }
                            .toImmutableList(),
                    )
                }
            }
            .launchIn(modelScope)
    }

    private fun HotCryptoUM.getOrCreateGroupTitle(): TokensListItemUM {
        return items.firstOrNull()
            .takeIf { it is TokensListItemUM.GroupTitle }
            ?: TokensListItemUM.GroupTitle(
                id = R.string.tokens_list_hot_crypto_header,
                text = resourceReference(id = R.string.tokens_list_hot_crypto_header),
            )
    }

    private fun onTokenClick(tokenItemState: TokenItemState, currency: HotCryptoCurrency) {
        bottomSheetNavigation.activate(
            configuration = OnrampAddToPortfolioBSConfig.AddToPortfolio(
                cryptoCurrency = currency.cryptoCurrency,
                currencyIconState = tokenItemState.iconState,
                onSuccessAdding = ::onSuccessAdding,
            ),
        )
    }

    private fun onSuccessAdding(id: CryptoCurrency.ID) {
        modelScope.launch {
            getSingleCryptoCurrencyStatusUseCase.invokeMultiWalletSync(
                userWalletId = params.userWalletId,
                cryptoCurrencyId = id,
            )
                .onRight {
                    bottomSheetNavigation.dismiss()
                    params.onTokenClick(it)
                }
                .onLeft { Timber.d("Unable to get CryptoCurrencyStatus[$id]: $it") }
        }
    }
}