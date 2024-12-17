package com.tangem.features.onramp.success.model

import arrow.core.getOrElse
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.domain.onramp.GetOnrampStatusUseCase
import com.tangem.domain.onramp.GetOnrampTransactionUseCase
import com.tangem.domain.onramp.OnrampRemoveTransactionUseCase
import com.tangem.domain.onramp.analytics.OnrampAnalyticsEvent
import com.tangem.domain.onramp.model.OnrampStatus
import com.tangem.domain.onramp.model.cache.OnrampTransaction
import com.tangem.domain.tokens.GetCryptoCurrencyUseCase
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.features.onramp.component.OnrampSuccessComponent
import com.tangem.features.onramp.success.entity.OnrampSuccessClickIntents
import com.tangem.features.onramp.success.entity.OnrampSuccessComponentUM
import com.tangem.features.onramp.success.entity.conterter.SetOnrampSuccessContentConverter
import com.tangem.features.onramp.utils.sendOnrampErrorEvent
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@Suppress("LongParameterList")
internal class OnrampSuccessComponentModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val urlOpener: UrlOpener,
    private val getOnrampTransactionUseCase: GetOnrampTransactionUseCase,
    private val getOnrampStatusUseCase: GetOnrampStatusUseCase,
    private val getCryptoCurrencyUseCase: GetCryptoCurrencyUseCase,
    private val onrampRemoveTransactionUseCase: OnrampRemoveTransactionUseCase,
    private val analyticsEventHandler: AnalyticsEventHandler,
    paramsContainer: ParamsContainer,
) : Model(), OnrampSuccessClickIntents {

    private val params: OnrampSuccessComponent.Params = paramsContainer.require()
    private val _state: MutableStateFlow<OnrampSuccessComponentUM> = MutableStateFlow(
        value = OnrampSuccessComponentUM.Loading,
    )

    val state: StateFlow<OnrampSuccessComponentUM> get() = _state.asStateFlow()

    init {
        loadData()
    }

    override fun goToProviderClick(providerLink: String) {
        urlOpener.openUrl(providerLink)
    }

    private fun loadData() {
        modelScope.launch {
            getOnrampTransactionUseCase(externalTxId = params.externalTxId)
                .fold(
                    ifLeft = {
                        Timber.e(it.toString())
                    },
                    ifRight = { transaction ->
                        loadTransactionStatus(transaction)
                    },
                )
        }
    }

    private suspend fun loadTransactionStatus(transaction: OnrampTransaction) {
        val cryptoCurrency = getCryptoCurrencyUseCase(
            transaction.userWalletId,
            transaction.toCurrencyId,
        ).getOrElse { error("Crypto currency not found") }

        getOnrampStatusUseCase(externalTxId = params.externalTxId)
            .fold(
                ifLeft = { error ->
                    analyticsEventHandler.sendOnrampErrorEvent(
                        error = error,
                        tokenSymbol = cryptoCurrency.symbol,
                        providerName = transaction.providerName,
                        paymentMethod = transaction.paymentMethod,
                    )
                    Timber.e(error.toString())
                },
                ifRight = { status ->
                    analyticsEventHandler.send(
                        OnrampAnalyticsEvent.SuccessScreenOpened(
                            providerName = transaction.providerName,
                            currency = transaction.fromCurrency.code,
                            tokenSymbol = cryptoCurrency.symbol,
                            residence = transaction.residency,
                            paymentMethod = transaction.paymentMethod,
                        ),
                    )
                    _state.update {
                        SetOnrampSuccessContentConverter(
                            cryptoCurrency = cryptoCurrency,
                            transaction = transaction,
                            goToProviderClick = ::goToProviderClick,
                        ).convert(status)
                    }
                    removeTransactionIfTerminalStatus(
                        cryptoCurrency = cryptoCurrency,
                        providerName = transaction.providerName,
                        paymentMethod = transaction.paymentMethod,
                        status = status,
                    )
                },
            )
    }

    private fun removeTransactionIfTerminalStatus(
        cryptoCurrency: CryptoCurrency,
        providerName: String,
        paymentMethod: String,
        status: OnrampStatus,
    ) {
        modelScope.launch {
            if (status.status.isTerminal) {
                onrampRemoveTransactionUseCase(status.txId).onLeft { error ->
                    analyticsEventHandler.sendOnrampErrorEvent(
                        error = error,
                        tokenSymbol = cryptoCurrency.symbol,
                        providerName = providerName,
                        paymentMethod = paymentMethod,
                    )
                }
            }
        }
    }
}