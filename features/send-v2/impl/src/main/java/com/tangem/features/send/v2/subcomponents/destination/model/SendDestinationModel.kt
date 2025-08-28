package com.tangem.features.send.v2.subcomponents.destination.model

import androidx.compose.runtime.Stable
import arrow.core.getOrElse
import com.tangem.common.routing.AppRoute
import com.tangem.common.ui.navigationButtons.NavigationButton
import com.tangem.common.ui.navigationButtons.NavigationUM
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.models.network.CryptoCurrencyAddress
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.isLocked
import com.tangem.domain.models.wallet.isMultiCurrency
import com.tangem.domain.qrscanning.models.SourceType
import com.tangem.domain.qrscanning.usecases.ListenToQrScanningUseCase
import com.tangem.domain.qrscanning.usecases.ParseQrCodeUseCase
import com.tangem.domain.tokens.GetCryptoCurrencyUseCase
import com.tangem.domain.tokens.GetNetworkAddressesUseCase
import com.tangem.domain.transaction.usecase.IsUtxoConsolidationAvailableUseCase
import com.tangem.domain.transaction.usecase.ValidateWalletAddressUseCase
import com.tangem.domain.transaction.usecase.ValidateWalletMemoUseCase
import com.tangem.domain.txhistory.usecase.GetFixedTxHistoryItemsUseCase
import com.tangem.domain.wallets.usecase.GetWalletsUseCase
import com.tangem.features.send.v2.api.SendFeatureToggles
import com.tangem.features.send.v2.api.analytics.CommonSendAnalyticEvents
import com.tangem.features.send.v2.api.analytics.CommonSendAnalyticEvents.SendScreenSource
import com.tangem.features.send.v2.api.entity.PredefinedValues
import com.tangem.features.send.v2.api.subcomponents.destination.SendDestinationComponentParams
import com.tangem.features.send.v2.api.subcomponents.destination.SendDestinationComponentParams.DestinationBlockParams
import com.tangem.features.send.v2.api.subcomponents.destination.entity.DestinationUM
import com.tangem.features.send.v2.impl.R
import com.tangem.features.send.v2.subcomponents.destination.analytics.EnterAddressSource
import com.tangem.features.send.v2.subcomponents.destination.analytics.SendDestinationAnalyticEvents
import com.tangem.features.send.v2.subcomponents.destination.model.transformers.*
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
    private val sendFeatureToggles: SendFeatureToggles,
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
        configDestinationNavigation()
        subscribeOnQRScannerResult()
        initialState()
    }

    private fun initialState() {
        if ((uiState.value as? DestinationUM.Content)?.isInitialized == false || uiState.value is DestinationUM.Empty) {
            _uiState.update(
                SendDestinationInitialStateTransformer(
                    cryptoCurrency = cryptoCurrency,
                    isRedesignEnabled = sendFeatureToggles.isSendRedesignEnabled,
                    isInitialized = true,
                ),
            )
            val params = params as? DestinationBlockParams
            val predefinedValues = params?.predefinedValues as? PredefinedValues.Content.Deeplink
            if (predefinedValues?.address != null) {
                _uiState.update(
                    SendDestinationPredefinedStateTransformer(
                        address = predefinedValues.address,
                        memo = predefinedValues.memo,
                    ),
                )
            }
            initSenderAddress()
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

    fun saveResult() {
        val params = params as? SendDestinationComponentParams.DestinationParams ?: return
        params.callback.onDestinationResult(uiState.value)
    }

    private fun initSenderAddress() {
        modelScope.launch {
            senderAddresses.value = getNetworkAddressesUseCase.invokeSync(
                userWalletId = userWalletId,
                networkRawId = cryptoCurrency.network.id.rawId,
            ).filter { cryptoCurrency.id == it.cryptoCurrency.id }
        }
        senderAddresses.onEach {
            getWalletsAndRecent()
        }.launchIn(modelScope)
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
                                    getNetworkAddressesUseCase.invokeSync(
                                        userWalletId = wallet.walletId,
                                        networkRawId = it.network.id.rawId,
                                    )
                                } else {
                                    null
                                }
                            }
                        } else {
                            getNetworkAddressesUseCase.invokeSync(
                                userWalletId = wallet.walletId,
                                networkRawId = cryptoCurrencyNetwork.id.rawId,
                            )
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
        if (type?.isAutoNext == true && isValidAddress && isValidMemo) {
            saveResult()
            (params as? SendDestinationComponentParams.DestinationParams)?.callback?.onNextClick()
        }
    }

    @Suppress("LongMethod")
    private fun configDestinationNavigation() {
        val params = params as? SendDestinationComponentParams.DestinationParams ?: return
        combine(
            flow = uiState,
            flow2 = params.currentRoute,
            transform = { state, route -> state to route },
        ).onEach { (state, route) ->
            val isRedesignEnabled = sendFeatureToggles.isSendRedesignEnabled
            params.callback.onNavigationResult(
                NavigationUM.Content(
                    title = params.title,
                    subtitle = null,
                    backIconRes = if (route.isEditMode || isRedesignEnabled) {
                        R.drawable.ic_back_24
                    } else {
                        R.drawable.ic_close_24
                    },
                    backIconClick = {
                        if (!route.isEditMode) {
                            analyticsEventHandler.send(
                                CommonSendAnalyticEvents.CloseButtonClicked(
                                    categoryName = params.analyticsCategoryName,
                                    source = SendScreenSource.Address,
                                    isFromSummary = false,
                                    isValid = state.isPrimaryButtonEnabled,
                                ),
                            )
                            saveResult()
                        }
                        params.callback.onBackClick()
                    },
                    additionalIconRes = if (isRedesignEnabled) {
                        null
                    } else {
                        R.drawable.ic_qrcode_scan_24
                    },
                    additionalIconClick = if (isRedesignEnabled) {
                        null
                    } else {
                        ::onQrCodeScanClick
                    },
                    primaryButton = NavigationButton(
                        textReference = if (route.isEditMode) {
                            resourceReference(R.string.common_continue)
                        } else {
                            resourceReference(R.string.common_next)
                        },
                        isEnabled = state.isPrimaryButtonEnabled,
                        onClick = {
                            saveResult()
                            params.callback.onNextClick()
                        },
                    ),
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