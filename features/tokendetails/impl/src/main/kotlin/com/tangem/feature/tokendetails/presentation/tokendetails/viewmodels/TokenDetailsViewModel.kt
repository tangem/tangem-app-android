package com.tangem.feature.tokendetails.presentation.tokendetails.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.*
import arrow.core.getOrElse
import com.tangem.common.Provider
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.tokens.GetCurrencyUseCase
import com.tangem.domain.tokens.models.CryptoCurrency
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.usecase.GetSelectedWalletUseCase
import com.tangem.feature.tokendetails.presentation.router.InnerTokenDetailsRouter
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsState
import com.tangem.feature.tokendetails.presentation.tokendetails.state.factory.TokenDetailsStateFactory
import com.tangem.features.tokendetails.navigation.TokenDetailsRouter
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import kotlin.properties.Delegates

@HiltViewModel
internal class TokenDetailsViewModel @Inject constructor(
    private val dispatchers: CoroutineDispatcherProvider,
    private val getSelectedWalletUseCase: GetSelectedWalletUseCase,
    private val getCurrencyUseCase: GetCurrencyUseCase,
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel(), DefaultLifecycleObserver, TokenDetailsClickIntents {

    private val cryptoCurrency: CryptoCurrency = savedStateHandle[TokenDetailsRouter.SELECTED_CURRENCY_KEY]
        ?: error("no expected parameter CryptoCurrency found")

    var router by Delegates.notNull<InnerTokenDetailsRouter>()

    private val marketPriceJobHolder = JobHolder()

    private val selectedAppCurrencyFlow: StateFlow<AppCurrency> = createSelectedAppCurrencyFlow()
    private val stateFactory = TokenDetailsStateFactory(
        currentStateProvider = Provider { uiState },
        appCurrencyProvider = Provider(selectedAppCurrencyFlow::value),
        clickIntents = this,
    )
    var uiState: TokenDetailsState by mutableStateOf(stateFactory.getInitialState(cryptoCurrency))
        private set

    override fun onCreate(owner: LifecycleOwner) {
        updateContent(selectedWallet = getWallet(), refresh = false)
    }

    private fun getWallet(): UserWallet {
        return getSelectedWalletUseCase()
            .fold(
                ifLeft = { error("Can not get selected wallet $it") },
                ifRight = { it },
            )
    }

    private fun updateContent(selectedWallet: UserWallet, refresh: Boolean) {
        updateMarketPrice(selectedWallet = selectedWallet, refresh = refresh)
    }

    private fun updateMarketPrice(selectedWallet: UserWallet, refresh: Boolean) {
        getCurrencyUseCase(userWalletId = selectedWallet.walletId, currencyId = cryptoCurrency.id, refresh = refresh)
            .distinctUntilChanged()
            .onEach { uiState = stateFactory.getCurrencyLoadedBalanceState(it) }
            .flowOn(dispatchers.io)
            .launchIn(viewModelScope)
            .saveIn(marketPriceJobHolder)
    }

    private fun createSelectedAppCurrencyFlow(): StateFlow<AppCurrency> {
        return getSelectedAppCurrencyUseCase()
            .map { maybeAppCurrency ->
                maybeAppCurrency.getOrElse { AppCurrency.Default }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = AppCurrency.Default,
            )
    }

    override fun onBackClick() {
        router.popBackStack()
    }

    override fun onMoreClick() {
        TODO("Not yet implemented")
    }
}