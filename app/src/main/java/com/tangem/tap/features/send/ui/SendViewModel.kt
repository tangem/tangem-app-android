package com.tangem.tap.features.send.ui

import androidx.lifecycle.*
import com.tangem.domain.balancehiding.IsBalanceHiddenUseCase
import com.tangem.domain.balancehiding.ListenToFlipsUseCase
import com.tangem.domain.tokens.FetchPendingTransactionsUseCase
import com.tangem.domain.tokens.UpdateDelayedNetworkStatusUseCase
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.usecase.GetSelectedWalletUseCase
import com.tangem.features.send.navigation.SendRouter
import com.tangem.tap.di.DelayedWork
import com.tangem.tap.features.send.redux.AmountAction
import com.tangem.tap.proxy.AppStateHolder
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

@Suppress("LongParameterList")
@HiltViewModel
internal class SendViewModel @Inject constructor(
    private val dispatchers: CoroutineDispatcherProvider,
    private val appStateHolder: AppStateHolder,
    private val isBalanceHiddenUseCase: IsBalanceHiddenUseCase,
    private val listenToFlipsUseCase: ListenToFlipsUseCase,
    private val updateDelayedCurrencyStatusUseCase: UpdateDelayedNetworkStatusUseCase,
    private val getSelectedWalletUseCase: GetSelectedWalletUseCase,
    private val fetchPendingTransactionsUseCase: FetchPendingTransactionsUseCase,
    @DelayedWork private val coroutineScope: CoroutineScope,
    savedStateHandle: SavedStateHandle,
) : ViewModel(), DefaultLifecycleObserver {

    private val cryptoCurrency: CryptoCurrency? = savedStateHandle[SendRouter.CRYPTO_CURRENCY_KEY]

    override fun onCreate(owner: LifecycleOwner) {
        isBalanceHiddenUseCase()
            .flowWithLifecycle(owner.lifecycle)
            .onEach { isBalanceHidden ->
                withContext(dispatchers.main) {
                    appStateHolder.mainStore?.dispatch(AmountAction.HideBalance(isBalanceHidden))
                }
            }
            .launchIn(viewModelScope)

        viewModelScope.launch {
            listenToFlipsUseCase()
                .flowWithLifecycle(owner.lifecycle)
                .collect()
        }
    }

    fun updateCurrencyDelayed() {
        if (cryptoCurrency != null) {
            coroutineScope.launch {
                getSelectedWalletUseCase()
                    .fold(
                        ifLeft = { Timber.e(it.toString()) },
                        ifRight = { wallet ->
                            // we should update network to find pending tx after 1 sec
                            updateForPendingTx(wallet, cryptoCurrency.network)
                            // we should update network for new balance
                            updateForBalance(wallet, cryptoCurrency.network)
                        },
                    )
            }
        } else {
            Timber.w("$TAG: cryptoCurrency is null, legacy flow")
        }
    }

    private suspend fun updateForPendingTx(userWallet: UserWallet, network: Network) {
        fetchPendingTransactionsUseCase(userWallet.walletId, setOf(network))
    }

    private suspend fun updateForBalance(userWallet: UserWallet, network: Network) {
        updateDelayedCurrencyStatusUseCase(
            userWalletId = userWallet.walletId,
            network = network,
            delayMillis = UPDATE_BALANCE_DELAY_MILLIS,
            refresh = true,
        )
    }

    companion object {
        private const val UPDATE_BALANCE_DELAY_MILLIS = 11000L
        private const val TAG = "SendViewModel"
    }
}
