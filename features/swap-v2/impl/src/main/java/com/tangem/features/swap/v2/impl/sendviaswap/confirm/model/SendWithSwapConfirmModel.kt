package com.tangem.features.swap.v2.impl.sendviaswap.confirm.model

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.common.ui.amountScreen.converters.AmountReduceByTransformer
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.common.ui.navigationButtons.NavigationButton
import com.tangem.common.ui.navigationButtons.NavigationUM
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.express.models.ExpressProviderType
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.settings.IsSendTapHelpEnabledUseCase
import com.tangem.domain.swap.models.SwapDirection.Companion.withSwapDirection
import com.tangem.domain.tokens.IsAmountSubtractAvailableUseCase
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.usecase.EstimateFeeUseCase
import com.tangem.domain.txhistory.usecase.GetExplorerTransactionUrlUseCase
import com.tangem.features.send.v2.api.SendNotificationsComponent
import com.tangem.features.send.v2.api.SendNotificationsComponent.Params.NotificationData
import com.tangem.features.send.v2.api.callbacks.FeeSelectorModelCallback
import com.tangem.features.send.v2.api.entity.FeeSelectorUM
import com.tangem.features.send.v2.api.subcomponents.destination.entity.DestinationUM
import com.tangem.features.send.v2.api.subcomponents.feeSelector.FeeSelectorReloadTrigger
import com.tangem.features.send.v2.api.subcomponents.notifications.SendNotificationsUpdateListener
import com.tangem.features.send.v2.api.subcomponents.notifications.SendNotificationsUpdateTrigger
import com.tangem.features.swap.v2.impl.R
import com.tangem.features.swap.v2.impl.amount.SwapAmountReduceTrigger
import com.tangem.features.swap.v2.impl.amount.entity.SwapAmountUM
import com.tangem.features.swap.v2.impl.common.SwapUtils.INCREASE_GAS_LIMIT_FOR_CEX
import com.tangem.features.swap.v2.impl.common.entity.ConfirmUM
import com.tangem.features.swap.v2.impl.common.entity.SwapQuoteUM
import com.tangem.features.swap.v2.impl.notifications.SwapNotificationsComponent.Params.SwapNotificationData
import com.tangem.features.swap.v2.impl.notifications.SwapNotificationsUpdateListener
import com.tangem.features.swap.v2.impl.notifications.SwapNotificationsUpdateTrigger
import com.tangem.features.swap.v2.impl.sendviaswap.SendWithSwapRoute
import com.tangem.features.swap.v2.impl.sendviaswap.confirm.SendWithSwapConfirmComponent
import com.tangem.features.swap.v2.impl.sendviaswap.confirm.model.transformers.SendWithSwapConfirmInitialStateTransformer
import com.tangem.features.swap.v2.impl.sendviaswap.confirm.model.transformers.SendWithSwapConfirmationNotificationsTransformer
import com.tangem.features.swap.v2.impl.sendviaswap.entity.SendWithSwapUM
import com.tangem.lib.crypto.BlockchainFeeUtils.patchTransactionFeeForSwap
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.extensions.orZero
import jakarta.inject.Inject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.math.BigDecimal
import com.tangem.utils.transformer.update as transformerUpdate

@Suppress("LongParameterList")
@ModelScoped
internal class SendWithSwapConfirmModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val isSendTapHelpEnabledUseCase: IsSendTapHelpEnabledUseCase,
    private val estimateFeeUseCase: EstimateFeeUseCase,
    private val isAmountSubtractAvailableUseCase: IsAmountSubtractAvailableUseCase,
    private val getExplorerTransactionUrlUseCase: GetExplorerTransactionUrlUseCase,
    private val sendNotificationsUpdateTrigger: SendNotificationsUpdateTrigger,
    private val swapNotificationsUpdateTrigger: SwapNotificationsUpdateTrigger,
    private val sendNotificationsUpdateListener: SendNotificationsUpdateListener,
    private val swapNotificationsUpdateListener: SwapNotificationsUpdateListener,
    private val swapAmountReduceTrigger: SwapAmountReduceTrigger,
    private val feeSelectorReloadTrigger: FeeSelectorReloadTrigger,
    swapTransactionSenderFactory: SwapTransactionSender.Factory,
    paramsContainer: ParamsContainer,
) : Model(), FeeSelectorModelCallback, SendNotificationsComponent.ModelCallback {

    private val params: SendWithSwapConfirmComponent.Params = paramsContainer.require()

    val uiState: StateFlow<SendWithSwapUM>
    field = MutableStateFlow(params.sendWithSwapUM)

    val primaryCurrencyStatus: CryptoCurrencyStatus = params.primaryCryptoCurrencyStatusFlow.value
    val secondaryCurrencyStatus: CryptoCurrencyStatus? = amountUM?.secondaryCryptoCurrencyStatus
    val primaryFeePaidCurrencyStatus: CryptoCurrencyStatus = params.primaryFeePaidCurrencyStatusFlow.value
    val primaryCurrency: CryptoCurrency = primaryCurrencyStatus.currency
    val secondaryCurrency: CryptoCurrency = requireNotNull(amountUM?.secondaryCryptoCurrencyStatus?.currency) {
        "Crypto currency must not be null"
    }

    private val swapTransactionSender = swapTransactionSenderFactory.create(params.userWallet)

    private var isAmountSubtractAvailable = false

    private val amountUM
        get() = uiState.value.amountUM as? SwapAmountUM.Content
    private val destinationUM
        get() = uiState.value.destinationUM as? DestinationUM.Content
    private val feeSelectorUM
        get() = uiState.value.feeSelectorUM as? FeeSelectorUM.Content

    val confirmData: ConfirmData
        get() {
            val amountUM = amountUM
            val fromAmount = amountUM?.swapDirection?.withSwapDirection(
                onDirect = { amountUM.primaryAmount },
                onReverse = { amountUM.secondaryAmount },
            )
            val amountState = fromAmount?.amountField as? AmountState.Data
            return ConfirmData(
                enteredAmount = amountState?.amountTextField?.cryptoAmount?.value,
                reduceAmountBy = amountState?.reduceAmountBy.orZero(),
                isIgnoreReduce = amountState?.isIgnoreReduce == true,
                enteredDestination = destinationUM?.addressTextField?.actualAddress,
                fee = feeSelectorUM?.selectedFeeItem?.fee,
                feeError = (uiState.value.feeSelectorUM as? FeeSelectorUM.Error)?.error,
                fromCryptoCurrencyStatus = amountUM?.swapDirection?.withSwapDirection(
                    onDirect = { primaryCurrencyStatus },
                    onReverse = { secondaryCurrencyStatus },
                ),
                toCryptoCurrencyStatus = amountUM?.swapDirection?.withSwapDirection(
                    onDirect = { secondaryCurrencyStatus },
                    onReverse = { primaryCurrencyStatus },
                ),
                quote = amountUM?.selectedQuote,
                rateType = amountUM?.swapRateType,
            )
        }

    init {
        initAmountSubtractAvailability()
        configConfirmNavigation()
        initialState()
        subscribeOnNotificationUpdates()
    }

    override fun onFeeResult(feeSelectorUM: FeeSelectorUM) {
        uiState.update { it.copy(feeSelectorUM = feeSelectorUM) }
        updateConfirmNotifications()
    }

    fun onAmountResult(amountUM: SwapAmountUM) {
        uiState.update { it.copy(amountUM = amountUM) }
        updateConfirmNotifications()
    }

    fun onDestinationResult(destinationUM: DestinationUM) {
        uiState.update { it.copy(destinationUM = destinationUM) }
        updateConfirmNotifications()
    }

    fun updateState(sendWithSwapUM: SendWithSwapUM) {
        uiState.value = sendWithSwapUM
    }

    override fun onFeeReload() {
        modelScope.launch {
            feeSelectorReloadTrigger.triggerUpdate()
        }
    }

    override fun onAmountIgnore() {
        modelScope.launch {
            swapAmountReduceTrigger.triggerIgnoreReduce()
        }
    }

    override fun onAmountReduceBy(reduceBy: BigDecimal, reduceByDiff: BigDecimal) {
        modelScope.launch {
            swapAmountReduceTrigger.triggerReduceBy(
                reduceBy = AmountReduceByTransformer.ReduceByData(
                    reduceAmountBy = reduceBy,
                    reduceAmountByDiff = reduceByDiff,
                ),
            )
        }
    }

    override fun onAmountReduceTo(reduceTo: BigDecimal) {
        modelScope.launch {
            swapAmountReduceTrigger.triggerReduceTo(reduceTo = reduceTo)
        }
    }

    fun showEditAmount() {
        router.push(SendWithSwapRoute.Amount(isEditMode = true))
    }

    fun showEditDestination() {
        router.push(SendWithSwapRoute.Destination(isEditMode = true))
    }

    suspend fun loadFee(): Either<GetFeeError, TransactionFee> {
        val defaultError = GetFeeError.UnknownError.left()
        val quote = amountUM?.selectedQuote as? SwapQuoteUM.Content ?: return defaultError
        val amountUM = uiState.value.amountUM as? SwapAmountUM.Content ?: return defaultError

        val amountField = amountUM.swapDirection.withSwapDirection(
            onDirect = { amountUM.primaryAmount.amountField },
            onReverse = { amountUM.secondaryAmount.amountField },
        ) as? AmountState.Data ?: return defaultError
        val amountValue = amountField.amountTextField.cryptoAmount.value ?: return defaultError

        return when (val providerType = quote.provider.type) {
            ExpressProviderType.CEX -> {
                estimateFeeUseCase(
                    amount = amountValue,
                    userWallet = params.userWallet,
                    cryptoCurrency = primaryCurrencyStatus.currency,
                ).map {
                    it.patchTransactionFeeForSwap(INCREASE_GAS_LIMIT_FOR_CEX)
                }
            }
            ExpressProviderType.DEX,
            ExpressProviderType.DEX_BRIDGE,
            ExpressProviderType.ONRAMP,
            -> GetFeeError.DataError(
                cause = IllegalStateException("Provider $providerType is not supported in Send With Swap"),
            ).left()
        }
    }

    private fun onSendClick() {
        val provider = confirmData.quote?.provider ?: return
        modelScope.launch {
            swapTransactionSender.sendTransaction(
                confirmData = confirmData,
                isAmountSubtractAvailable = isAmountSubtractAvailable,
                onExpressError = {
                    // todo error
                },
                onSendError = {
                    // todo error
                },
                onSendSuccess = { txHash, timestamp, data ->
                    val txUrl = getExplorerTransactionUrlUseCase(
                        txHash = txHash,
                        networkId = primaryCurrencyStatus.currency.network.id,
                    ).getOrNull().orEmpty()

                    uiState.update {
                        it.copy(
                            confirmUM = ConfirmUM.Success(
                                isPrimaryButtonEnabled = true,
                                transactionDate = timestamp,
                                txUrl = txUrl,
                                provider = provider,
                                swapDataModel = data,
                            ),
                        )
                    }
                    router.replaceAll(SendWithSwapRoute.Success)
                },
            )
        }
    }

    private fun initAmountSubtractAvailability() {
        modelScope.launch {
            isAmountSubtractAvailable =
                isAmountSubtractAvailableUseCase(
                    params.userWallet.walletId,
                    primaryCurrencyStatus.currency,
                ).getOrElse { false }
        }
    }

    private fun initialState() {
        val confirmUM = uiState.value.confirmUM

        modelScope.launch {
            val isShowTapHelp = isSendTapHelpEnabledUseCase().getOrElse { false }
            if (confirmUM is ConfirmUM.Empty) {
                uiState.update {
                    it.copy(
                        confirmUM = SendWithSwapConfirmInitialStateTransformer(
                            isShowTapHelp = isShowTapHelp,
                        ).transform(uiState.value.confirmUM),
                    )
                }
                updateConfirmNotifications()
            }
        }
    }

    private fun updateConfirmNotifications() {
        val amountUM = uiState.value.amountUM as? SwapAmountUM.Content ?: return
        val destinationUM = uiState.value.destinationUM as? DestinationUM.Content ?: return
        val feeSelectorUMContent = uiState.value.feeSelectorUM as? FeeSelectorUM.Content
        val feeSelectorUMError = uiState.value.feeSelectorUM as? FeeSelectorUM.Error

        val amountField = amountUM.swapDirection.withSwapDirection(
            onDirect = { amountUM.primaryAmount.amountField },
            onReverse = { amountUM.secondaryAmount.amountField },
        ) as? AmountState.Data ?: return
        val enteredDestination = destinationUM.addressTextField.actualAddress

        modelScope.launch {
            sendNotificationsUpdateTrigger.triggerUpdate(
                data = NotificationData(
                    destinationAddress = enteredDestination,
                    memo = null,
                    amountValue = amountField.amountTextField.cryptoAmount.value.orZero(),
                    reduceAmountBy = amountField.reduceAmountBy.orZero(),
                    isIgnoreReduce = amountField.isIgnoreReduce,
                    fee = feeSelectorUMContent?.selectedFeeItem?.fee,
                    feeError = feeSelectorUMError?.error,
                ),
            )
            swapNotificationsUpdateTrigger.triggerUpdate(
                data = SwapNotificationData(
                    expressError = (amountUM.selectedQuote as? SwapQuoteUM.Error)?.expressError,
                    fromCryptoCurrency = amountUM.swapDirection.withSwapDirection(
                        onDirect = { primaryCurrency },
                        onReverse = { secondaryCurrency },
                    ),
                ),
            )
            uiState.transformerUpdate(
                SendWithSwapConfirmationNotificationsTransformer(),
            )
        }
    }

    private fun subscribeOnNotificationUpdates() {
        combine(
            flow = sendNotificationsUpdateListener.hasErrorFlow,
            flow2 = swapNotificationsUpdateListener.hasErrorFlow,
        ) { hasSendError, hasSwapError ->
            val hasError = hasSendError || hasSwapError
            uiState.update {
                val feeUM = it.feeSelectorUM as? FeeSelectorUM.Content
                it.copy(
                    confirmUM = (it.confirmUM as? ConfirmUM.Content)?.copy(
                        isPrimaryButtonEnabled = !hasError && feeUM != null,
                    ) ?: it.confirmUM,
                )
            }
        }.launchIn(modelScope)
    }

    private fun configConfirmNavigation() {
        combine(
            flow = uiState,
            flow2 = params.currentRoute,
            transform = { state, route -> state to route },
        ).filter {
            it.second is SendWithSwapRoute.Confirm
        }.onEach { (state, _) ->
            val confirmUM = state.confirmUM
            params.callback.onResult(
                state.copy(
                    navigationUM = NavigationUM.Content(
                        title = resourceReference(id = R.string.send_with_swap_confirm_title),
                        subtitle = null,
                        backIconRes = R.drawable.ic_back_24,
                        backIconClick = router::pop,
                        primaryButton = NavigationButton(
                            textReference = resourceReference(R.string.common_send),
                            iconRes = R.drawable.ic_tangem_24,
                            isEnabled = confirmUM.isPrimaryButtonEnabled,
                            onClick = {
                                when (confirmUM) {
                                    is ConfirmUM.Content -> if (confirmUM.isTransactionInProcess) {
                                        return@NavigationButton
                                    } else {
                                        onSendClick()
                                    }
                                    else -> return@NavigationButton
                                }
                            },
                        ),
                    ),
                ),
            )
        }.launchIn(modelScope)
    }
}