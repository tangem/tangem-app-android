package com.tangem.features.swap.v2.impl.sendviaswap.confirm.model

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import com.tangem.blockchain.common.transaction.TransactionFee
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
import com.tangem.features.send.v2.api.SendNotificationsComponent.Params.NotificationData
import com.tangem.features.send.v2.api.callbacks.FeeSelectorModelCallback
import com.tangem.features.send.v2.api.entity.FeeSelectorUM
import com.tangem.features.send.v2.api.subcomponents.destination.entity.DestinationUM
import com.tangem.features.send.v2.api.subcomponents.notifications.SendNotificationsUpdateTrigger
import com.tangem.features.swap.v2.impl.R
import com.tangem.features.swap.v2.impl.amount.entity.SwapAmountUM
import com.tangem.features.swap.v2.impl.common.SwapUtils.INCREASE_GAS_LIMIT_FOR_CEX
import com.tangem.features.swap.v2.impl.common.entity.ConfirmUM
import com.tangem.features.swap.v2.impl.common.entity.SwapQuoteUM
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
import com.tangem.utils.transformer.update as transformerUpdate

@Suppress("LongParameterList")
@ModelScoped
internal class SendWithSwapConfirmModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val isSendTapHelpEnabledUseCase: IsSendTapHelpEnabledUseCase,
    private val estimateFeeUseCase: EstimateFeeUseCase,
    private val isAmountSubtractAvailableUseCase: IsAmountSubtractAvailableUseCase,
    private val sendNotificationsUpdateTrigger: SendNotificationsUpdateTrigger,
    paramsContainer: ParamsContainer,
) : Model(), FeeSelectorModelCallback {

    private val params: SendWithSwapConfirmComponent.Params = paramsContainer.require()

    val uiState: StateFlow<SendWithSwapUM>
    field = MutableStateFlow(params.sendWithSwapUM)

    val primaryCurrencyStatus: CryptoCurrencyStatus = params.primaryCryptoCurrencyStatusFlow.value
    val primaryFeePaidCurrencyStatus: CryptoCurrencyStatus = params.primaryFeePaidCurrencyStatusFlow.value

    private val amountUM = uiState.value.amountUM as? SwapAmountUM.Content

    val secondaryCurrency: CryptoCurrency = requireNotNull(amountUM?.secondaryCryptoCurrencyStatus?.currency) {
        "Crypto currency must not be null"
    }

    private var isAmountSubtractAvailable = false

    init {
        initAmountSubtractAvailability()
        configConfirmNavigation()
        initialState()
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
            -> {
                // todo send with swap
                GetFeeError.UnknownError.left()
            }
            ExpressProviderType.ONRAMP,
            -> GetFeeError.DataError(
                cause = IllegalStateException("Provider $providerType is not supported in Send With Swap"),
            ).left()
        }
    }

    private fun onSendClick() {
        // todo swap send tx
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
            uiState.transformerUpdate(
                SendWithSwapConfirmationNotificationsTransformer(),
            )
        }
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