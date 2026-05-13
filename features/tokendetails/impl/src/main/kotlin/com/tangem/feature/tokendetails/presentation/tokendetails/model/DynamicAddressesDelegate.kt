package com.tangem.feature.tokendetails.presentation.tokendetails.model

import com.tangem.common.core.TangemSdkError
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.api.ResettableOneTimeEventSender
import com.tangem.core.decompose.di.GlobalUiMessageSender
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.res.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.common.ui.amountScreen.utils.getFiatString
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.dynamicaddresses.CreateConsolidationTransactionUseCase
import com.tangem.domain.dynamicaddresses.IsDynamicAddressesConsolidationRequiredUseCase
import com.tangem.domain.dynamicaddresses.EnableDynamicAddressesError
import com.tangem.domain.dynamicaddresses.EnableDynamicAddressesUseCase
import com.tangem.domain.dynamicaddresses.GetDerivedXpubUseCase
import com.tangem.domain.dynamicaddresses.model.DynamicAddressesStatus
import com.tangem.domain.dynamicaddresses.repository.DynamicAddressesRepository
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.utils.Provider
import com.tangem.domain.transaction.error.SendTransactionError
import com.tangem.domain.transaction.usecase.GetFeeUseCase
import com.tangem.domain.transaction.usecase.SendTransactionUseCase
import com.tangem.domain.wallets.usecase.GetExtendedPublicKeyForCurrencyUseCase
import com.tangem.feature.tokendetails.presentation.tokendetails.analytics.TokenDetailsAnalyticsEvent
import com.tangem.feature.tokendetails.presentation.tokendetails.ui.components.dynamicaddresses.DynamicAddressesBottomSheetConfig
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.logging.TangemLogger
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Suppress("LongParameterList", "LargeClass")
internal class DynamicAddressesDelegate @AssistedInject constructor(
    private val enableDynamicAddressesUseCase: EnableDynamicAddressesUseCase,
    private val isConsolidationRequiredUseCase: IsDynamicAddressesConsolidationRequiredUseCase,
    private val createConsolidationTransactionUseCase: CreateConsolidationTransactionUseCase,
    private val getFeeUseCase: GetFeeUseCase,
    private val sendTransactionUseCase: SendTransactionUseCase,
    private val getDerivedXpubUseCase: GetDerivedXpubUseCase,
    private val dynamicAddressesRepository: DynamicAddressesRepository,
    private val getExtendedPublicKeyUseCase: GetExtendedPublicKeyForCurrencyUseCase,
    private val analyticsEventHandler: AnalyticsEventHandler,
    @GlobalUiMessageSender private val uiMessageSender: UiMessageSender,
    private val dispatchers: CoroutineDispatcherProvider,
    @Assisted private val userWallet: UserWallet,
    @Assisted private val cryptoCurrencyStatusProvider: Provider<CryptoCurrencyStatus?>,
    @Assisted private val appCurrencyProvider: Provider<AppCurrency>,
    @Assisted private val coroutineScope: CoroutineScope,
    @Assisted("showBottomSheet") private val showBottomSheet: () -> Unit,
    @Assisted("dismissBottomSheet") private val dismissBottomSheet: () -> Unit,
    @Assisted("onDynamicAddressesStateChanged") private val onDynamicAddressesStateChanged: () -> Unit,
) {

    private val userWalletId get() = userWallet.walletId

    private val _bottomSheetConfig = MutableStateFlow<DynamicAddressesBottomSheetConfig>(
        DynamicAddressesBottomSheetConfig.Enable(
            isCardScanRequired = false,
            onEnableClick = {},
        ),
    )
    val bottomSheetConfig: StateFlow<DynamicAddressesBottomSheetConfig> = _bottomSheetConfig.asStateFlow()

    private val resettableOneTimeEventSender = ResettableOneTimeEventSender(analyticsEventHandler)

    // region Entry point

    fun onDynamicAddressesClick() {
        val currency = cryptoCurrencyStatusProvider()?.currency ?: return
        analyticsEventHandler.send(TokenDetailsAnalyticsEvent.DynamicAddressesScreenOpened(currency))
        coroutineScope.launch(dispatchers.main) {
            val status = dynamicAddressesRepository.getStatus(userWalletId, currency.network).first()
            when (status) {
                DynamicAddressesStatus.ENABLED,
                DynamicAddressesStatus.ENABLED_REQUIRES_SETUP,
                -> onDisableFlow(currency.network)
                DynamicAddressesStatus.DISABLED -> onEnableFlow(currency.network)
            }
        }
    }

    // endregion

    // region Enable flow

    private suspend fun onEnableFlow(network: Network) {
        val hasConflicts = dynamicAddressesRepository.hasConflictingCustomTokens(userWalletId, network)
        if (hasConflicts) {
            cryptoCurrencyStatusProvider()?.currency?.let {
                analyticsEventHandler.send(TokenDetailsAnalyticsEvent.Notice.DynamicAddressesUnavailable(it))
            }
            _bottomSheetConfig.value = DynamicAddressesBottomSheetConfig.ConflictingCustomTokens(
                onDismissClick = dismissBottomSheet,
            )
            showBottomSheet()
            return
        }

        val isCardScanRequired = !isXpubAlreadyDerived(network)
        _bottomSheetConfig.value = DynamicAddressesBottomSheetConfig.Enable(
            isCardScanRequired = isCardScanRequired,
            onEnableClick = ::onEnableClick,
        )
        showBottomSheet()
    }

    private fun onEnableClick() {
        val currency = cryptoCurrencyStatusProvider()?.currency ?: return
        val network = currency.network
        analyticsEventHandler.send(TokenDetailsAnalyticsEvent.ButtonEnableDynamicAddresses(currency))
        coroutineScope.launch(dispatchers.main) {
            _bottomSheetConfig.value = DynamicAddressesBottomSheetConfig.Enable(
                isCardScanRequired = false,
                isLoading = true,
                onEnableClick = {},
            )

            val xpub = getExtendedPublicKeyUseCase(userWalletId, network).fold(
                ifLeft = { error ->
                    if (isUserCancellation(error)) {
                        dismissBottomSheet()
                    } else {
                        TangemLogger.e("Failed to get XPUB: ${error.message}")
                        analyticsEventHandler.send(
                            TokenDetailsAnalyticsEvent.Error.DynamicAddressesUnavailable(currency),
                        )
                        _bottomSheetConfig.value = DynamicAddressesBottomSheetConfig.ServiceUnavailable(
                            onDismissClick = dismissBottomSheet,
                        )
                    }
                    return@launch
                },
                ifRight = { it },
            )

            enableDynamicAddressesUseCase(userWalletId, network, xpub).fold(
                ifLeft = { error ->
                    analyticsEventHandler.send(
                        TokenDetailsAnalyticsEvent.Error.DynamicAddressesUnavailable(currency),
                    )
                    _bottomSheetConfig.value = when (error) {
                        is EnableDynamicAddressesError.ConflictingCustomTokens ->
                            DynamicAddressesBottomSheetConfig.ConflictingCustomTokens(
                                onDismissClick = dismissBottomSheet,
                            )
                        is EnableDynamicAddressesError.ServiceError -> {
                            TangemLogger.e("Failed to enable dynamic addresses: ${error.cause.message}")
                            DynamicAddressesBottomSheetConfig.ServiceUnavailable(
                                onDismissClick = dismissBottomSheet,
                            )
                        }
                    }
                },
                ifRight = {
                    analyticsEventHandler.send(TokenDetailsAnalyticsEvent.DynamicAddressesEnabled(currency))
                    dismissBottomSheet()
                    onDynamicAddressesStateChanged()
                    uiMessageSender.send(
                        SnackbarMessage(message = resourceReference(R.string.dynamic_addresses_enabled_toast_title)),
                    )
                },
            )
        }
    }

    // endregion

    // region Disable flow

    private fun onDisableFlow(network: Network) {
        coroutineScope.launch(dispatchers.main) {
            isConsolidationRequiredUseCase(userWalletId, network).fold(
                ifLeft = { error ->
                    TangemLogger.e(
                        "Error in consolidation required check: ${error.message}",
                    )
                    _bottomSheetConfig.value = DynamicAddressesBottomSheetConfig.ServiceUnavailable(
                        onDismissClick = dismissBottomSheet,
                    )
                    showBottomSheet()
                },
                ifRight = { isConsolidationRequired ->
                    if (!isConsolidationRequired) {
                        showSimpleDisableSheet()
                    } else {
                        showDisableSheetAndLoadFee()
                    }
                },
            )
        }
    }

    private fun showSimpleDisableSheet() {
        _bottomSheetConfig.value = DynamicAddressesBottomSheetConfig.DisableWithoutConsolidation(
            onDisableClick = ::onSimpleDisableClick,
            onReadMoreClick = ::onReadMoreClick,
        )
        showBottomSheet()
    }

    private fun onSimpleDisableClick() {
        val currency = cryptoCurrencyStatusProvider()?.currency ?: return
        val network = currency.network
        analyticsEventHandler.send(TokenDetailsAnalyticsEvent.ButtonDisableDynamicAddresses(currency))
        coroutineScope.launch(dispatchers.main) {
            runSuspendCatching { dynamicAddressesRepository.disable(userWalletId, network) }
                .onSuccess {
                    analyticsEventHandler.send(TokenDetailsAnalyticsEvent.DynamicAddressesDisabled(currency))
                    dismissBottomSheet()
                    onDynamicAddressesStateChanged()
                    uiMessageSender.send(
                        SnackbarMessage(message = resourceReference(R.string.dynamic_addresses_disabled_popup_title)),
                    )
                }
                .onFailure { e ->
                    TangemLogger.e("Failed to disable dynamic addresses: ${e.message}")
                    _bottomSheetConfig.value = DynamicAddressesBottomSheetConfig.ServiceUnavailable(
                        onDismissClick = dismissBottomSheet,
                    )
                }
        }
    }

    private fun showDisableSheetAndLoadFee() {
        resettableOneTimeEventSender.reset(NOT_ENOUGH_FEE_EVENT_KEY)
        _bottomSheetConfig.value = DynamicAddressesBottomSheetConfig.DisableWithConsolidation(
            feeState = DynamicAddressesBottomSheetConfig.DisableFeeState.Loading,
            onDisableClick = ::onDisableClick,
            onRefreshFee = ::loadDisableFee,
            onReadMoreClick = ::onReadMoreClick,
        )
        showBottomSheet()
        loadDisableFee()
    }

    private fun loadDisableFee() {
        coroutineScope.launch(dispatchers.main) {
            _bottomSheetConfig.value = disableWithConsolidationConfig().copy(
                feeState = DynamicAddressesBottomSheetConfig.DisableFeeState.Loading,
            )

            val status = cryptoCurrencyStatusProvider()
            val currency = status?.currency
            val balance = status?.value?.amount
            val address = status?.value?.networkAddress?.defaultAddress?.value

            if (currency == null || balance == null || address == null) {
                _bottomSheetConfig.value = disableWithConsolidationConfig().copy(
                    feeState = DynamicAddressesBottomSheetConfig.DisableFeeState.Error,
                )
                return@launch
            }

            getFeeUseCase(
                amount = balance,
                destination = address,
                userWallet = userWallet,
                cryptoCurrency = currency,
            ).fold(
                ifLeft = {
                    resettableOneTimeEventSender.sendEventOnce(
                        key = NOT_ENOUGH_FEE_EVENT_KEY,
                        event = TokenDetailsAnalyticsEvent.Notice.NotEnoughFee(
                            currency = currency,
                            source = TokenDetailsAnalyticsEvent.Notice.NotEnoughFee.Source.DynamicAddresses,
                        ),
                    )
                    _bottomSheetConfig.value = disableWithConsolidationConfig().copy(
                        feeState = DynamicAddressesBottomSheetConfig.DisableFeeState.Error,
                    )
                },
                ifRight = { txFee ->
                    val fee = txFee.normal
                    val fiatFormatted = getFiatString(
                        value = fee.amount.value,
                        rate = status.value.fiatRate,
                        appCurrency = appCurrencyProvider(),
                        approximate = true,
                    )
                    _bottomSheetConfig.value = disableWithConsolidationConfig().copy(
                        feeState = DynamicAddressesBottomSheetConfig.DisableFeeState.Content(
                            feeSymbol = currency.symbol,
                            fiatFormatted = fiatFormatted,
                        ),
                    )
                },
            )
        }
    }

    private fun disableWithConsolidationConfig(): DynamicAddressesBottomSheetConfig.DisableWithConsolidation {
        return _bottomSheetConfig.value as? DynamicAddressesBottomSheetConfig.DisableWithConsolidation
            ?: DynamicAddressesBottomSheetConfig.DisableWithConsolidation(
                onDisableClick = ::onDisableClick,
                onRefreshFee = ::loadDisableFee,
                onReadMoreClick = ::onReadMoreClick,
            )
    }

    private fun onReadMoreClick() {
        // TODO: Replace with actual URL
    }

    private fun onDisableClick() {
        val currency = cryptoCurrencyStatusProvider()?.currency ?: return
        val network = currency.network
        analyticsEventHandler.send(TokenDetailsAnalyticsEvent.ButtonDisableDynamicAddresses(currency))
        coroutineScope.launch(dispatchers.main) {
            _bottomSheetConfig.value = disableWithConsolidationConfig().copy(
                isSending = true,
            )

            val txData = createConsolidationTransactionUseCase(userWalletId, network).fold(
                ifLeft = { error ->
                    TangemLogger.e("Failed to create consolidation tx: ${error.message}")
                    _bottomSheetConfig.value = DynamicAddressesBottomSheetConfig.ServiceUnavailable(
                        onDismissClick = dismissBottomSheet,
                    )
                    return@launch
                },
                ifRight = { it },
            )

            sendTransactionUseCase(
                txData = txData,
                userWallet = userWallet,
                network = network,
            ).fold(
                ifLeft = { error ->
                    if (error is SendTransactionError.UserCancelledError) {
                        dismissBottomSheet()
                    } else {
                        TangemLogger.e("Failed to send consolidation tx: $error")
                        _bottomSheetConfig.value = DynamicAddressesBottomSheetConfig.ServiceUnavailable(
                            onDismissClick = dismissBottomSheet,
                        )
                    }
                },
                ifRight = {
                    try {
                        dynamicAddressesRepository.disable(userWalletId, network)
                    } catch (e: Exception) {
                        TangemLogger.e("Failed to disable dynamic addresses after consolidation: ${e.message}")
                    }
                    analyticsEventHandler.send(TokenDetailsAnalyticsEvent.DynamicAddressesDisabled(currency))
                    dismissBottomSheet()
                    onDynamicAddressesStateChanged()
                    uiMessageSender.send(
                        SnackbarMessage(
                            message = resourceReference(R.string.dynamic_addresses_disabled_popup_title),
                        ),
                    )
                },
            )
        }
    }

    // endregion

    // region Common

    private suspend fun isXpubAlreadyDerived(network: Network): Boolean {
        return getDerivedXpubUseCase(userWalletId, network) != null
    }

    private fun isUserCancellation(error: Throwable): Boolean {
        return error is TangemSdkError.UserCancelled || error.cause is TangemSdkError.UserCancelled
    }

    // endregion

    @AssistedFactory
    interface Factory {
        @Suppress("LongParameterList")
        fun create(
            userWallet: UserWallet,
            cryptoCurrencyStatusProvider: Provider<CryptoCurrencyStatus?>,
            appCurrencyProvider: Provider<AppCurrency>,
            coroutineScope: CoroutineScope,
            @Assisted("showBottomSheet") showBottomSheet: () -> Unit,
            @Assisted("dismissBottomSheet") dismissBottomSheet: () -> Unit,
            @Assisted("onDynamicAddressesStateChanged") onDynamicAddressesStateChanged: () -> Unit,
        ): DynamicAddressesDelegate
    }

    private companion object {
        const val NOT_ENOUGH_FEE_EVENT_KEY = "DynamicAddressesNotEnoughFee"
    }
}