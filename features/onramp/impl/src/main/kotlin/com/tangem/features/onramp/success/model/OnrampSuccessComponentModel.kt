package com.tangem.features.onramp.success.model

import arrow.core.getOrElse
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.clipboard.ClipboardManager
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.message.DialogMessage
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.onramp.GetOnrampStatusUseCase
import com.tangem.domain.onramp.GetOnrampTransactionUseCase
import com.tangem.domain.onramp.OnrampRemoveTransactionUseCase
import com.tangem.domain.onramp.analytics.OnrampAnalyticsEvent
import com.tangem.domain.onramp.model.OnrampStatus
import com.tangem.domain.onramp.model.cache.OnrampTransaction
import com.tangem.domain.onramp.model.error.OnrampError
import com.tangem.domain.tokens.GetCryptoCurrencyUseCase
import com.tangem.domain.tokens.model.analytics.TokenOnrampAnalyticsEvent
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.features.onramp.component.OnrampSuccessComponent
import com.tangem.features.onramp.impl.R
import com.tangem.features.onramp.success.entity.OnrampSuccessClickIntents
import com.tangem.features.onramp.success.entity.OnrampSuccessComponentUM
import com.tangem.features.onramp.success.entity.conterter.SetOnrampSuccessContentConverter
import com.tangem.features.onramp.utils.sendOnrampErrorEvent
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.PeriodicTask
import com.tangem.utils.coroutines.SingleTaskScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import kotlin.properties.Delegates

@Suppress("LongParameterList")
internal class OnrampSuccessComponentModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val urlOpener: UrlOpener,
    private val getOnrampTransactionUseCase: GetOnrampTransactionUseCase,
    private val getOnrampStatusUseCase: GetOnrampStatusUseCase,
    private val getCryptoCurrencyUseCase: GetCryptoCurrencyUseCase,
    private val onrampRemoveTransactionUseCase: OnrampRemoveTransactionUseCase,
    private val getUserWalletUseCase: GetUserWalletUseCase,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val messageSender: UiMessageSender,
    private val clipboardManager: ClipboardManager,
    private val router: Router,
    paramsContainer: ParamsContainer,
) : Model(), OnrampSuccessClickIntents {

    private val params: OnrampSuccessComponent.Params = paramsContainer.require()
    private val _state: MutableStateFlow<OnrampSuccessComponentUM> = MutableStateFlow(
        value = OnrampSuccessComponentUM.Loading,
    )

    val state: StateFlow<OnrampSuccessComponentUM> get() = _state.asStateFlow()

    private var userWallet: UserWallet by Delegates.notNull()
    private var cryptoCurrency: CryptoCurrency by Delegates.notNull()
    private var expressTxStatusTaskScheduler = SingleTaskScheduler<Unit>()

    init {
        loadData()
    }

    override fun goToProviderClick(providerLink: String) {
        analyticsEventHandler.send(TokenOnrampAnalyticsEvent.GoToProvider)
        urlOpener.openUrl(providerLink)
    }

    override fun onCopyClick(copiedText: String) {
        clipboardManager.setText(text = copiedText, isSensitive = false)
    }

    private fun loadData() {
        modelScope.launch {
            getOnrampTransactionUseCase(txId = params.txId)
                .fold(
                    ifLeft = { error ->
                        Timber.e(error.toString())
                        showErrorAlert(error)
                    },
                    ifRight = { transaction ->
                        userWallet = getUserWalletUseCase(transaction.userWalletId).getOrElse {
                            Timber.e("UserWallet found")
                            // this case should never happened
                            showErrorAlert(OnrampError.DomainError("UserWallet not found"))
                            return@launch
                        }
                        cryptoCurrency = getCryptoCurrencyUseCase(
                            userWallet = userWallet,
                            cryptoCurrencyId = transaction.toCurrencyId,
                        ).getOrElse {
                            Timber.e("Crypto currency not found")
                            showErrorAlert(OnrampError.DomainError(null))
                            return@launch
                        }

                        startStatusUpdateTask(transaction)
                    },
                )
        }
    }

    private fun startStatusUpdateTask(transaction: OnrampTransaction) {
        expressTxStatusTaskScheduler.cancelTask()
        expressTxStatusTaskScheduler.scheduleTask(
            modelScope,
            PeriodicTask(
                isDelayFirst = false,
                delay = EXPRESS_STATUS_UPDATE_DELAY,
                task = {
                    runCatching {
                        loadTransactionStatus(transaction)
                    }
                },
                onSuccess = { /* no-op */ },
                onError = { /* no-op */ },
            ),
        )
    }

    private suspend fun loadTransactionStatus(transaction: OnrampTransaction) {
        val isTerminal = (state.value as? OnrampSuccessComponentUM.Content)?.activeStatus?.isTerminal
        if (isTerminal == null || isTerminal == false) {
            getOnrampStatusUseCase(userWallet = userWallet, txId = transaction.txId)
                .fold(
                    ifLeft = { error ->
                        analyticsEventHandler.sendOnrampErrorEvent(
                            error = error,
                            tokenSymbol = cryptoCurrency.symbol,
                            providerName = transaction.providerName,
                            paymentMethod = transaction.paymentMethod,
                        )
                        Timber.e(error.toString())
                        showErrorAlert(error)
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
                                onCopyClick = ::onCopyClick,
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
    }

    private fun showErrorAlert(error: OnrampError) {
        val errorCode = (error as? OnrampError.DataError)?.code
        val message = DialogMessage(
            message = if (errorCode.isNullOrBlank()) {
                resourceReference(R.string.common_unknown_error)
            } else {
                resourceReference(R.string.express_error_code, wrappedList(errorCode))
            },
            firstActionBuilder = {
                okAction(router::pop)
            },
        )

        messageSender.send(message)
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

    private companion object {
        const val EXPRESS_STATUS_UPDATE_DELAY = 10_000L
    }
}