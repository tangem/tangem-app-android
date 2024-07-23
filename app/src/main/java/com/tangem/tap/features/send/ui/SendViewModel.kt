package com.tangem.tap.features.send.ui

import androidx.lifecycle.*
import arrow.core.getOrElse
import com.tangem.common.routing.AppRoute
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.qrscanning.models.SourceType
import com.tangem.domain.qrscanning.usecases.ListenToQrScanningUseCase
import com.tangem.domain.qrscanning.usecases.ParseQrCodeUseCase
import com.tangem.domain.tokens.FetchPendingTransactionsUseCase
import com.tangem.domain.tokens.UpdateDelayedNetworkStatusUseCase
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.usecase.GetSelectedWalletSyncUseCase
import com.tangem.tap.common.analytics.events.Token
import com.tangem.tap.di.DelayedWork
import com.tangem.tap.features.send.redux.AddressActionUi
import com.tangem.tap.features.send.redux.AmountAction
import com.tangem.tap.proxy.AppStateHolder
import com.tangem.tap.store
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@Suppress("LongParameterList")
@HiltViewModel
internal class SendViewModel @Inject constructor(
    private val dispatchers: CoroutineDispatcherProvider,
    private val appStateHolder: AppStateHolder,
    private val getBalanceHidingSettingsUseCase: GetBalanceHidingSettingsUseCase,
    private val updateDelayedCurrencyStatusUseCase: UpdateDelayedNetworkStatusUseCase,
    private val getSelectedWalletSyncUseCase: GetSelectedWalletSyncUseCase,
    private val fetchPendingTransactionsUseCase: FetchPendingTransactionsUseCase,
    private val listenToQrScanningUseCase: ListenToQrScanningUseCase,
    private val parseQrCodeUseCase: ParseQrCodeUseCase,
    @DelayedWork private val coroutineScope: CoroutineScope,
    savedStateHandle: SavedStateHandle,
) : ViewModel(), DefaultLifecycleObserver {

    init {
        listenToQrScanningUseCase(SourceType.SEND)
            .getOrElse { emptyFlow() }
            .onEach(::onQRCodeScanned)
            .launchIn(viewModelScope)
    }

    private val cryptoCurrency: CryptoCurrency? = savedStateHandle[AppRoute.Send.CRYPTO_CURRENCY_KEY]

    override fun onCreate(owner: LifecycleOwner) {
        getBalanceHidingSettingsUseCase()
            .flowWithLifecycle(owner.lifecycle)
            .onEach {
                appStateHolder.mainStore?.dispatch(AmountAction.HideBalance(it.isBalanceHidden))
            }
            .flowOn(dispatchers.main)
            .launchIn(viewModelScope)
    }

    fun updateCurrencyDelayed() {
        if (cryptoCurrency != null) {
            coroutineScope.launch {
                getSelectedWalletSyncUseCase()
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

    private fun onQRCodeScanned(qrScanResult: String) {
        if (cryptoCurrency != null) {
            parseQrCodeUseCase(qrScanResult, cryptoCurrency).fold(
                ifRight = { parsedCode ->
                    store.dispatch(
                        AddressActionUi.PasteAddress(
                            data = parsedCode.address,
                            sourceType = Token.Send.AddressEntered.SourceType.QRCode,
                        ),
                    )
                    parsedCode.amount?.let { amount ->
                        store.dispatch(AmountAction.SetAmount(amount, isUserInput = false))
                    }
                    // parsedCode.memo?.let { }
                },
                ifLeft = {
                    store.dispatch(
                        AddressActionUi.PasteAddress(
                            data = qrScanResult,
                            sourceType = Token.Send.AddressEntered.SourceType.QRCode,
                        ),
                    )
                    Timber.w(it)
                },
            )
        } else {
            store.dispatch(
                AddressActionUi.PasteAddress(
                    data = qrScanResult,
                    sourceType = Token.Send.AddressEntered.SourceType.QRCode,
                ),
            )
        }

        store.dispatch(AddressActionUi.TruncateOrRestore(truncate = true))
    }

    companion object {
        private const val UPDATE_BALANCE_DELAY_MILLIS = 11000L
        private const val TAG = "SendViewModel"
    }
}