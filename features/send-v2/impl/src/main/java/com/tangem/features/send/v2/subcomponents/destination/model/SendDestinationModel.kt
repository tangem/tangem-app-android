package com.tangem.features.send.v2.subcomponents.destination.model

import androidx.compose.runtime.Stable
import arrow.core.getOrElse
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.qrscanning.models.SourceType
import com.tangem.domain.qrscanning.usecases.ListenToQrScanningUseCase
import com.tangem.domain.qrscanning.usecases.ParseQrCodeUseCase
import com.tangem.domain.tokens.GetCryptoCurrencyUseCase
import com.tangem.domain.tokens.GetNetworkAddressesUseCase
import com.tangem.domain.tokens.model.CryptoCurrencyAddress
import com.tangem.domain.transaction.usecase.IsUtxoConsolidationAvailableUseCase
import com.tangem.domain.transaction.usecase.ValidateWalletAddressUseCase
import com.tangem.domain.transaction.usecase.ValidateWalletMemoUseCase
import com.tangem.domain.txhistory.usecase.GetFixedTxHistoryItemsUseCase
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.usecase.GetWalletsUseCase
import com.tangem.features.send.v2.common.NavigationUM
import com.tangem.features.send.v2.impl.R
import com.tangem.features.send.v2.send.SendRoute
import com.tangem.features.send.v2.send.analytics.SendAnalyticEvents
import com.tangem.features.send.v2.send.analytics.SendAnalyticEvents.SendScreenSource
import com.tangem.features.send.v2.send.ui.state.ButtonsUM
import com.tangem.features.send.v2.subcomponents.destination.SendDestinationComponentParams
import com.tangem.features.send.v2.subcomponents.destination.analytics.EnterAddressSource
import com.tangem.features.send.v2.subcomponents.destination.analytics.SendDestinationAnalyticEvents
import com.tangem.features.send.v2.subcomponents.destination.model.transformers.*
import com.tangem.features.send.v2.subcomponents.destination.ui.state.DestinationUM
import com.tangem.features.send.v2.subcomponents.destination.ui.state.DestinationWalletUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import com.tangem.utils.coroutines.waitForDelay
import com.tangem.utils.transformer.update
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@Stable
@ModelScoped
@Suppress("LongParameterList", "LargeClass")
internal class SendDestinationModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val appRouter: AppRouter,
    private val validateWalletAddressUseCase: ValidateWalletAddressUseCase,
    private val validateWalletMemoUseCase: ValidateWalletMemoUseCase,
    private val getWalletsUseCase: GetWalletsUseCase,
    private val getCryptoCurrencyUseCase: GetCryptoCurrencyUseCase,
    private val getNetworkAddressesUseCase: GetNetworkAddressesUseCase,
    private val getFixedTxHistoryItemsUseCase: GetFixedTxHistoryItemsUseCase,
    private val isUtxoConsolidationAvailableUseCase: IsUtxoConsolidationAvailableUseCase,
    private val listenToQrScanningUseCase: ListenToQrScanningUseCase,
    private val parseQrCodeUseCase: ParseQrCodeUseCase,
    private val analyticsEventHandler: AnalyticsEventHandler,
) : Model(), SendDestinationClickIntents {
    private val params: SendDestinationComponentParams = paramsContainer.require()

    private val _uiState = MutableStateFlow(params.state)
    val uiState = _uiState.asStateFlow()

    private val analyticsCategoryName = params.analyticsCategoryName
    private val cryptoCurrency = params.cryptoCurrency
    private val userWalletId = params.userWalletId

    private val senderAddresses = MutableStateFlow<List<CryptoCurrencyAddress>>(emptyList())

    private var validationJobHolder = JobHolder()

    init {
        initSenderAddress()
        configDestinationNavigation()
        subscribeOnQRScannerResult()
        initialState()
    }

    private fun initSenderAddress() {
        modelScope.launch {
            senderAddresses.value = getNetworkAddressesUseCase.invokeSync(userWalletId, cryptoCurrency.network)
                .filter { cryptoCurrency.id == it.cryptoCurrency.id }
        }
        senderAddresses.onEach {
            getWalletsAndRecent()
        }.launchIn(modelScope)
    }

    private fun initialState() {
        if ((uiState.value as? DestinationUM.Content)?.isInitialized == false) {
            _uiState.update(
                SendDestinationInitialStateTransformer(
                    cryptoCurrency = cryptoCurrency,
                    isInitialized = true,
                ),
            )
            val params = params as? SendDestinationComponentParams.DestinationBlockParams
            if (params?.predefinedAddressValue != null) {
                _uiState.update(
                    SendDestinationPredefinedStateTransformer(
                        address = params.predefinedAddressValue,
                        memo = params.predefinedMemoValue,
                    ),
                )
            }
        }
    }

    fun updateState(destinationUM: DestinationUM) {
        if (destinationUM is DestinationUM.Content && destinationUM.isInitialized) {
            _uiState.value = destinationUM
        }
    }

    override fun onRecipientAddressValueChange(value: String, type: EnterAddressSource) {
        _uiState.update(
            SendDestinationAddressTransformer(
                address = value,
                isPasted = type.isPasted,
            ),
        )
        val memo = (uiState.value as? DestinationUM.Content)?.memoTextField?.value
        validate(address = value, memo = memo, type)
    }

    override fun onRecipientMemoValueChange(value: String, isValuePasted: Boolean) {
        _uiState.update(
            SendDestinationMemoTransformer(
                memo = value,
                isPasted = isValuePasted,
            ),
        )
        val address = (uiState.value as? DestinationUM.Content)?.addressTextField?.value.orEmpty()
        validate(address = address, memo = value)
    }

    override fun onQrCodeScanClick() {
        analyticsEventHandler.send(SendDestinationAnalyticEvents.QrCodeButtonClicked(analyticsCategoryName))
        router.push(
            AppRoute.QrScanning(source = AppRoute.QrScanning.Source.Send(cryptoCurrency.network.name)),
        )
    }

    private fun subscribeOnQRScannerResult() {
        listenToQrScanningUseCase(SourceType.SEND)
            .getOrElse { emptyFlow() }
            .onEach(::onQrCodeScanned)
            .launchIn(modelScope)
    }

    private fun onQrCodeScanned(address: String) {
        val parsedQrCode = parseQrCodeUseCase(address, cryptoCurrency).getOrNull() ?: return
        _uiState.update(
            SendDestinationPredefinedStateTransformer(
                address = parsedQrCode.address,
                memo = parsedQrCode.memo,
            ),
        )
        validate(
            address = parsedQrCode.address,
            memo = parsedQrCode.memo,
            type = EnterAddressSource.QRCode,
        )
    }

    private fun getWalletsAndRecent() {
        combine(
            flow = getWalletsUseCase().conflate().map {
                waitForDelay(RECENT_LOAD_DELAY) { it.toAvailableWallets() }
            },
            flow2 = getFixedTxHistoryItemsUseCase(
                userWalletId = userWalletId,
                currency = cryptoCurrency,
                pageSize = RECENT_TX_SIZE,
            ).getOrElse { flowOf(emptyList()) }.map {
                waitForDelay(RECENT_LOAD_DELAY) { it }
            }.conflate(),
        ) { destinationWalletList, txHistoryList ->
            val isUtxoConsolidationAvailable = isUtxoConsolidationAvailableUseCase.invokeSync(
                userWalletId = userWalletId,
                network = cryptoCurrency.network,
            )

            _uiState.update(
                SendDestinationRecentListTransformer(
                    cryptoCurrency = cryptoCurrency,
                    senderAddress = senderAddresses.value.firstOrNull()?.address,
                    isUtxoConsolidationAvailable = isUtxoConsolidationAvailable,
                    destinationWalletList = destinationWalletList,
                    txHistoryList = txHistoryList,
                ),
            )
        }.launchIn(modelScope)
    }

    private suspend fun List<UserWallet>.toAvailableWallets(): List<DestinationWalletUM> {
        return coroutineScope {
            val cryptoCurrencyNetwork = cryptoCurrency.network

            return@coroutineScope filterNot { it.isLocked }
                .map { wallet ->
                    async {
                        val addresses = if (!wallet.isMultiCurrency) {
                            getCryptoCurrencyUseCase(wallet.walletId).getOrNull()?.let {
                                if (it.network.id == cryptoCurrencyNetwork.id) {
                                    getNetworkAddressesUseCase.invokeSync(wallet.walletId, it.network)
                                } else {
                                    null
                                }
                            }
                        } else {
                            getNetworkAddressesUseCase.invokeSync(wallet.walletId, cryptoCurrencyNetwork)
                        }
                        wallet to addresses
                    }
                }.awaitAll()
                .asSequence()
                .mapNotNull { (wallet, addresses) ->
                    addresses?.map { (cryptoCurrency, address) ->
                        DestinationWalletUM(
                            name = wallet.name,
                            address = address,
                            cryptoCurrency = cryptoCurrency,
                            userWalletId = wallet.walletId,
                        )
                    }
                }.flatten()
                .toList()
        }
    }

    private fun validate(address: String, memo: String?, type: EnterAddressSource? = null) {
        modelScope.launch {
            _uiState.update(SendDestinationValidationStartedTransformer)

            val addressValidationResult = validateWalletAddressUseCase(
                userWalletId = userWalletId,
                network = cryptoCurrency.network,
                address = address,
                senderAddresses = senderAddresses.value,
            )
            val memoValidationResult = validateWalletMemoUseCase(
                memo = memo.orEmpty(),
                network = cryptoCurrency.network,
            )

            type?.let {
                analyticsEventHandler.send(
                    SendDestinationAnalyticEvents.AddressEntered(
                        categoryName = analyticsCategoryName,
                        source = it,
                        isValid = addressValidationResult.isRight(),
                    ),
                )
            }
            _uiState.update(SendDestinationValidationResultTransformer(addressValidationResult, memoValidationResult))
            autoNextFromRecipient(type, addressValidationResult.isRight(), memoValidationResult.isRight())
        }.saveIn(validationJobHolder)
    }

    private fun autoNextFromRecipient(type: EnterAddressSource?, isValidAddress: Boolean, isValidMemo: Boolean) {
        val isRecent = type == EnterAddressSource.RecentAddress
        if (isRecent && isValidAddress && isValidMemo) onNextClick()
    }

    private fun saveResult() {
        val params = params as? SendDestinationComponentParams.DestinationParams ?: return
        params.callback.onDestinationResult(uiState.value)
    }

    private fun onNextClick() {
        saveResult()
        if ((params as? SendDestinationComponentParams.DestinationParams)?.isEditMode == true) {
            router.pop()
        } else {
            router.push(SendRoute.Amount(isEditMode = false))
        }
    }

    private fun configDestinationNavigation() {
        val params = params as? SendDestinationComponentParams.DestinationParams ?: return
        combine(
            flow = uiState,
            flow2 = params.currentRoute,
            transform = { state, route -> state to route },
        ).onEach { (state, route) ->
            params.callback.onNavigationResult(
                NavigationUM.Content(
                    title = resourceReference(R.string.send_recipient_label),
                    subtitle = null,
                    backIconRes = if (route.isEditMode) {
                        R.drawable.ic_back_24
                    } else {
                        R.drawable.ic_close_24
                    },
                    backIconClick = {
                        if (route.isEditMode) {
                            router.pop()
                        } else {
                            analyticsEventHandler.send(
                                SendAnalyticEvents.CloseButtonClicked(
                                    source = SendScreenSource.Address,
                                    isFromSummary = false,
                                    isValid = state.isPrimaryButtonEnabled,
                                ),
                            )
                            appRouter.pop()
                        }
                    },
                    additionalIconRes = R.drawable.ic_qrcode_scan_24,
                    additionalIconClick = ::onQrCodeScanClick,
                    primaryButton = ButtonsUM.PrimaryButtonUM(
                        text = if (route.isEditMode) {
                            resourceReference(R.string.common_continue)
                        } else {
                            resourceReference(R.string.common_next)
                        },
                        isEnabled = state.isPrimaryButtonEnabled,
                        onClick = ::onNextClick,
                    ),
                    prevButton = null,
                    secondaryPairButtonsUM = null,
                ),
            )
        }.launchIn(modelScope)
    }

    private companion object {
        const val RECENT_TX_SIZE = 100
        const val RECENT_LOAD_DELAY = 500L
    }
}