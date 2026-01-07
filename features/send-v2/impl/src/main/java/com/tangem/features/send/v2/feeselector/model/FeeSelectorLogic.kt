package com.tangem.features.send.v2.feeselector.model

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.raise.either
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.tokens.GetSingleCryptoCurrencyStatusUseCase
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.models.TransactionFeeExtended
import com.tangem.domain.transaction.usecase.IsFeeApproximateUseCase
import com.tangem.domain.transaction.usecase.gasless.GetAvailableFeeTokensUseCase
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.features.send.v2.api.SendFeatureToggles
import com.tangem.features.send.v2.api.analytics.CommonSendAnalyticEvents.NonceInserted
import com.tangem.features.send.v2.api.entity.FeeItem
import com.tangem.features.send.v2.api.entity.FeeNonce
import com.tangem.features.send.v2.api.entity.FeeSelectorUM
import com.tangem.features.send.v2.api.params.FeeSelectorParams
import com.tangem.features.send.v2.api.subcomponents.feeSelector.FeeSelectorCheckReloadListener
import com.tangem.features.send.v2.api.subcomponents.feeSelector.FeeSelectorCheckReloadTrigger
import com.tangem.features.send.v2.api.subcomponents.feeSelector.FeeSelectorReloadListener
import com.tangem.features.send.v2.api.subcomponents.feeSelector.analytics.CommonSendFeeAnalyticEvents
import com.tangem.features.send.v2.api.subcomponents.feeSelector.analytics.CommonSendFeeAnalyticEvents.GasPriceInserter
import com.tangem.features.send.v2.feeselector.model.transformers.*
import com.tangem.utils.transformer.update
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Suppress("LongParameterList")
internal class FeeSelectorLogic @AssistedInject constructor(
    @Assisted private val params: FeeSelectorParams,
    @Assisted private val modelScope: CoroutineScope,
    private val isFeeApproximateUseCase: IsFeeApproximateUseCase,
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    private val feeSelectorReloadListener: FeeSelectorReloadListener,
    private val feeSelectorCheckReloadListener: FeeSelectorCheckReloadListener,
    private val feeSelectorCheckReloadTrigger: FeeSelectorCheckReloadTrigger,
    private val feeSelectorAlertFactory: FeeSelectorAlertFactory,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val sendFeatureToggles: SendFeatureToggles,
    private val getSingleCryptoCurrencyStatusUseCase: GetSingleCryptoCurrencyStatusUseCase,
    private val getUserWalletUseCase: GetUserWalletUseCase,
    private val getAvailableFeeTokensUseCase: GetAvailableFeeTokensUseCase,
) : FeeSelectorIntents {

    private var appCurrency: AppCurrency = AppCurrency.Default
    private var selectedToken: CryptoCurrencyStatus? = null
    val uiState = MutableStateFlow<FeeSelectorUM>(params.state)

    init {
        initAppCurrency()
        subscribeOnFeeReloadTriggerUpdates()
        subscribeOnFeeCheckReloadTriggerUpdates()
        subscribeOnFeeLoadingStateTriggerUpdates()
        loadFee()
    }

    private fun initAppCurrency() {
        modelScope.launch {
            appCurrency = getSelectedAppCurrencyUseCase.invokeSync().getOrElse { AppCurrency.Default }
        }
    }

    private fun loadFee() {
        if (uiState.value !is FeeSelectorUM.Content) {
            uiState.update(FeeSelectorLoadingTransformer)
        }
        modelScope.launch {
            callLoadFee()
                .fold(
                    ifLeft = { error -> uiState.update(FeeSelectorErrorTransformer(error)) },
                    ifRight = { fee ->
                        uiState.update(
                            FeeSelectorLoadedTransformer(
                                cryptoCurrencyStatus = params.cryptoCurrencyStatus,
                                feeCryptoCurrencyStatus = fee.selectedTokenOrNull ?: params.feeCryptoCurrencyStatus,
                                appCurrency = appCurrency,
                                fees = fee,
                                feeStateConfiguration = params.feeStateConfiguration,
                                isFeeApproximate = isFeeApproximate(fee.transactionFee.normal.amount.type),
                                feeSelectorIntents = this@FeeSelectorLogic,
                            ),
                        )
                    },
                )
        }
    }

    private fun isFeeApproximate(amountType: AmountType): Boolean {
        val networkId = params.feeCryptoCurrencyStatus.currency.network.id
        return isFeeApproximateUseCase(networkId = networkId, amountType = amountType)
    }

    override fun onFeeItemSelected(feeItem: FeeItem) {
        if (feeItem is FeeItem.Custom) {
            analyticsEventHandler.send(
                CommonSendFeeAnalyticEvents.CustomFeeButtonClicked(categoryName = params.analyticsCategoryName),
            )
        }
        uiState.update(FeeItemSelectedTransformer(feeItem))
    }

    override fun onTokenSelected(status: CryptoCurrencyStatus) {
        if (status.currency !is CryptoCurrency.Token ||
            status.currency.id == params.feeCryptoCurrencyStatus.currency.id) {
            selectedToken = null
            loadFee()
            return
        }
        uiState.update(FeeSelectorTokenSelectedTransformer(status))

        uiState.update { state ->
            if (state is FeeSelectorUM.Content) {
                state.copy(
                    selectedFeeItem = FeeItem.Loading,
                    feeItems = persistentListOf(FeeItem.Loading),
                    feeExtraInfo = state.feeExtraInfo.copy(
                        feeCryptoCurrencyStatus = status,
                    ),
                )
            } else {
                state
            }
        }

        if (status.currency is CryptoCurrency.Token) {
            selectedToken = status
        } else {
            selectedToken = null
        }

        loadFee()
    }

    override fun onCustomFeeValueChange(index: Int, value: String) {
        uiState.update(
            FeeSelectorCustomValueChangedTransformer(
                index = index,
                value = value,
                intents = this,
                appCurrency = appCurrency,
                feeCryptoCurrencyStatus = params.feeCryptoCurrencyStatus,
            ),
        )
    }

    override fun onNonceChange(value: String) {
        uiState.update(FeeSelectorNonceChangeTransformer(value = value))
    }

    override fun onDoneClick() {
        val feeSelectorUM = uiState.value as? FeeSelectorUM.Content ?: return
        analyticsEventHandler.send(
            CommonSendFeeAnalyticEvents.SelectedFee(
                categoryName = params.analyticsCategoryName,
                feeType = feeSelectorUM.toAnalyticType(),
                source = params.analyticsSendSource,
            ),
        )
        val isCustomFeeEdited = feeSelectorUM.selectedFeeItem.fee.amount.value != feeSelectorUM.fees.normal.amount.value
        if (feeSelectorUM.selectedFeeItem is FeeItem.Custom && isCustomFeeEdited) {
            analyticsEventHandler.send(GasPriceInserter(categoryName = params.analyticsCategoryName))
        }
        if (feeSelectorUM.feeNonce is FeeNonce.Nonce) {
            analyticsEventHandler.send(
                NonceInserted(
                    categoryName = params.analyticsCategoryName,
                    token = params.feeCryptoCurrencyStatus.currency.symbol,
                    blockchain = params.feeCryptoCurrencyStatus.currency.network.name,
                ),
            )
        }
    }

    private fun subscribeOnFeeReloadTriggerUpdates() {
        feeSelectorReloadListener.reloadTriggerFlow
            .onEach { data ->
                if (data.isRemoveSuggestedFee) {
                    uiState.update(FeeSelectorRemoveSuggestedTransformer)
                }
                loadFee()
            }
            .launchIn(modelScope)
    }

    private fun subscribeOnFeeLoadingStateTriggerUpdates() {
        feeSelectorReloadListener.loadingStateTriggerFlow
            .onEach { uiState.update(FeeSelectorLoadingTransformer) }
            .launchIn(modelScope)
    }

    private fun subscribeOnFeeCheckReloadTriggerUpdates() {
        feeSelectorCheckReloadListener.checkReloadTriggerFlow
            .onEach { checkLoadFee() }
            .launchIn(modelScope)
    }

    private fun checkLoadFee() {
        modelScope.launch {
            callLoadFee().fold(
                ifRight = { newFee ->
                    feeSelectorAlertFactory.getFeeUpdatedAlert(
                        newTransactionFee = newFee.transactionFee,
                        feeSelectorUM = uiState.value,
                        proceedAction = {
                            modelScope.launch {
                                feeSelectorCheckReloadTrigger.callbackCheckResult(true)
                            }
                        },
                        stopAction = {
                            modelScope.launch {
                                feeSelectorCheckReloadTrigger.callbackCheckResult(false)
                            }
                        },
                    )
                },
                ifLeft = { feeError ->
                    feeSelectorCheckReloadTrigger.callbackCheckResult(false)
                    feeSelectorAlertFactory.getFeeUnreachableErrorState(::loadFee)
                },
            )
        }
    }

    @Suppress("UnreachableCode")
    private suspend fun callLoadFee(): Either<GetFeeError, LoadedFeeResult> {
        val extended = params.onLoadFeeExtended
        return if (extended != null && sendFeatureToggles.isGaslessTransactionsEnabled) {
            extended(selectedToken).fold(
                ifLeft = { error ->
                    if (error is GetFeeError.GaslessError) {
                        params.onLoadFee().map { LoadedFeeResult.Basic(it) }
                    } else {
                        Either.Left(error)
                    }
                },
                ifRight = { fee ->
                    populateExtendedFee(fee)
                },
            )
        } else {
            params.onLoadFee().map { LoadedFeeResult.Basic(it) }
        }
    }

    private suspend fun populateExtendedFee(
        fee: TransactionFeeExtended,
    ): Either<GetFeeError, LoadedFeeResult.Extended> = either {
        val selectedToken = if (params.feeCryptoCurrencyStatus.currency.id != fee.feeTokenId) {
            getSingleCryptoCurrencyStatusUseCase
                .invokeMultiWalletSync(
                    userWalletId = params.userWalletId,
                    cryptoCurrencyId = fee.feeTokenId,
                ).getOrElse {
                    raise(GetFeeError.DataError(IllegalStateException("No token found for id: ${fee.feeTokenId}")))
                }
        } else {
            params.feeCryptoCurrencyStatus
        }

        val userWallet = getUserWalletUseCase(params.userWalletId).mapLeft {
            GetFeeError.DataError(IllegalStateException("No wallet found for id: ${params.userWalletId}"))
        }.bind()

        val availableTokens = getAvailableFeeTokensUseCase.invoke(
            userWallet = userWallet,
            network = params.cryptoCurrencyStatus.currency.network,
        ).bind()

        LoadedFeeResult.Extended(
            fee = fee,
            selectedToken = selectedToken,
            availableTokens = availableTokens,
        )
    }

    sealed class LoadedFeeResult {
        data class Extended(
            val fee: TransactionFeeExtended,
            val selectedToken: CryptoCurrencyStatus?,
            val availableTokens: List<CryptoCurrencyStatus>,
        ) : LoadedFeeResult()

        data class Basic(val fee: TransactionFee) : LoadedFeeResult()

        val selectedTokenOrNull: CryptoCurrencyStatus?
            get() = when (this) {
                is Extended -> selectedToken
                is Basic -> null
            }

        val transactionFee: TransactionFee
            get() = when (this) {
                is Extended -> fee.transactionFee
                is Basic -> fee
            }
    }

    @AssistedFactory
    interface Factory {
        fun create(params: FeeSelectorParams, modelScope: CoroutineScope): FeeSelectorLogic
    }
}