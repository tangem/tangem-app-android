package com.tangem.features.yield.supply.impl.subcomponents.approve.model

import arrow.core.getOrElse
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.Basic
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.components.currency.icon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.tokens.GetFeePaidCryptoCurrencyStatusSyncUseCase
import com.tangem.domain.transaction.usecase.CreateApprovalTransactionUseCase
import com.tangem.domain.transaction.usecase.GetFeeUseCase
import com.tangem.domain.transaction.usecase.SendTransactionUseCase
import com.tangem.domain.yield.supply.usecase.YieldSupplyGetContractAddressUseCase
import com.tangem.features.yield.supply.api.analytics.YieldSupplyAnalytics
import com.tangem.features.yield.supply.impl.R
import com.tangem.features.yield.supply.impl.common.YieldSupplyAlertFactory
import com.tangem.features.yield.supply.impl.common.entity.YieldSupplyActionUM
import com.tangem.features.yield.supply.impl.common.entity.YieldSupplyFeeUM
import com.tangem.features.yield.supply.impl.common.entity.transformer.YieldSupplyTransactionInProgressTransformer
import com.tangem.features.yield.supply.impl.common.entity.transformer.YieldSupplyTransactionReadyTransformer
import com.tangem.features.yield.supply.impl.subcomponents.approve.YieldSupplyApproveComponent
import com.tangem.features.yield.supply.impl.subcomponents.notifications.YieldSupplyNotificationsComponent
import com.tangem.features.yield.supply.impl.subcomponents.notifications.YieldSupplyNotificationsUpdateTrigger
import com.tangem.features.yield.supply.impl.subcomponents.notifications.entity.YieldSupplyNotificationData
import com.tangem.core.ui.extensions.TextReference
import com.tangem.utils.TangemBlogUrlBuilder
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.transformer.update
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@Suppress("LongParameterList")
@ModelScoped
internal class YieldSupplyApproveModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    paramsContainer: ParamsContainer,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val urlOpener: UrlOpener,
    private val yieldSupplyNotificationsUpdateTrigger: YieldSupplyNotificationsUpdateTrigger,
    private val createApprovalTransactionUseCase: CreateApprovalTransactionUseCase,
    private val getFeeUseCase: GetFeeUseCase,
    private val sendTransactionUseCase: SendTransactionUseCase,
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    private val getFeePaidCryptoCurrencyStatusSyncUseCase: GetFeePaidCryptoCurrencyStatusSyncUseCase,
    private val yieldSupplyGetContractAddressUseCase: YieldSupplyGetContractAddressUseCase,
    private val yieldSupplyAlertFactory: YieldSupplyAlertFactory,
) : Model(), YieldSupplyNotificationsComponent.ModelCallback {

    private val params: YieldSupplyApproveComponent.Params = paramsContainer.require()

    private val cryptoCurrencyStatus
        get() = params.cryptoCurrencyStatusFlow.value
    private val cryptoCurrency = cryptoCurrencyStatus.currency
    private var userWallet = params.userWallet

    val feeCryptoCurrencyStatusFlow: StateFlow<CryptoCurrencyStatus>
        field = MutableStateFlow(
            CryptoCurrencyStatus(
                cryptoCurrencyStatus.currency,
                value = CryptoCurrencyStatus.Loading,
            ),
        )

    private val feeCryptoCurrencyStatus: CryptoCurrencyStatus
        get() = feeCryptoCurrencyStatusFlow.value

    private var appCurrency = AppCurrency.Default

    val uiState: StateFlow<YieldSupplyActionUM>
        field: MutableStateFlow<YieldSupplyActionUM> = MutableStateFlow(
            YieldSupplyActionUM(
                title = resourceReference(R.string.yield_module_approve_sheet_title),
                subtitle = resourceReference(
                    id = R.string.yield_module_approve_sheet_subtitle,
                    formatArgs = wrappedList(cryptoCurrency.symbol),
                ),
                footer = resourceReference(R.string.yield_module_approve_sheet_fee_note),
                footerLink = resourceReference(R.string.common_read_more),
                currencyIconState = CryptoCurrencyToIconStateConverter().convert(cryptoCurrency),
                yieldSupplyFeeUM = YieldSupplyFeeUM.Loading,
                isPrimaryButtonEnabled = false,
                isTransactionSending = false,
            ),
        )

    init {
        modelScope.launch {
            appCurrency = getSelectedAppCurrencyUseCase.invokeSync().getOrElse { AppCurrency.Default }
            subscribeOnCurrencyStatusUpdates()
            subscribeOnNotificationsErrors()
        }
    }

    fun onReadMoreClick() {
        urlOpener.openUrl(TangemBlogUrlBuilder.FEE_BLOG_LINK)
    }

    override fun onFeeReload() {
        modelScope.launch {
            onLoadFee()
        }
    }

    fun onClick() {
        params.callback.onTransactionProgress(true)

        val yieldSupplyFeeUM = uiState.value.yieldSupplyFeeUM as? YieldSupplyFeeUM.Content ?: return
        uiState.update(YieldSupplyTransactionInProgressTransformer)

        analyticsEventHandler.send(YieldSupplyAnalytics.ButtonGiveApprove(
            token = cryptoCurrency.symbol,
            blockchain = cryptoCurrency.network.name,
        ))

        modelScope.launch(dispatchers.default) {
            sendTransactionUseCase(
                txData = yieldSupplyFeeUM.transactionDataList.first(),
                userWallet = userWallet,
                network = cryptoCurrency.network,
            ).fold(
                ifLeft = { error ->
                    Timber.e(error.toString())
                    uiState.update(YieldSupplyTransactionReadyTransformer)
                    analyticsEventHandler.send(
                        YieldSupplyAnalytics.EarnErrors(
                            action = YieldSupplyAnalytics.Action.Approve,
                            errorDescription = error.getAnalyticsDescription(),
                        ),
                    )
                    yieldSupplyAlertFactory.getSendTransactionErrorState(
                        error = error,
                        popBack = params.callback::onBackClick,
                        onFailedTxEmailClick = { errorMessage ->
                            modelScope.launch(dispatchers.default) {
                                yieldSupplyAlertFactory.onFailedTxEmailClick(
                                    userWallet = userWallet,
                                    cryptoCurrency = cryptoCurrency,
                                    errorMessage = errorMessage,
                                )
                            }
                        },
                    )
                    params.callback.onTransactionProgress(false)
                },
                ifRight = {
                    val event = AnalyticsParam.TxSentFrom.Earning(
                        blockchain = cryptoCurrency.network.name,
                        token = cryptoCurrency.symbol,
                        feeType = AnalyticsParam.FeeType.Normal,
                    )
                    analyticsEventHandler.send(
                        Basic.TransactionSent(
                            sentFrom = event,
                            memoType = Basic.TransactionSent.MemoType.Null,
                        ),
                    )
                    analyticsEventHandler.send(YieldSupplyAnalytics.ApprovalAction(
                        token = cryptoCurrency.symbol,
                        blockchain = cryptoCurrency.network.name,
                        action = YieldSupplyAnalytics.Action.Approve,
                    ))
                    params.callback.onTransactionSent()
                },
            )
        }
    }

    private fun subscribeOnCurrencyStatusUpdates() {
        modelScope.launch {
            feeCryptoCurrencyStatusFlow.update {
                getFeePaidCryptoCurrencyStatusSyncUseCase(
                    userWalletId = userWallet.walletId,
                    cryptoCurrencyStatus = cryptoCurrencyStatus,
                ).getOrNull() ?: cryptoCurrencyStatus
            }
            onLoadFee()
        }
    }

    private suspend fun onLoadFee() {
        if (cryptoCurrency !is CryptoCurrency.Token || uiState.value.isTransactionSending) return

        val contractAddress = yieldSupplyGetContractAddressUseCase.invoke(
            userWalletId = userWallet.walletId,
            cryptoCurrency = cryptoCurrency,
        ).getOrNull() ?: return

        val approvalTransitionData = createApprovalTransactionUseCase(
            cryptoCurrencyStatus = cryptoCurrencyStatus,
            userWalletId = userWallet.walletId,
            contractAddress = cryptoCurrency.contractAddress,
            spenderAddress = contractAddress,
            amount = null,
        ).getOrElse {
            Timber.e(it)
            return
        }

        uiState.update { it.copy(yieldSupplyFeeUM = YieldSupplyFeeUM.Loading) }

        getFeeUseCase(
            transactionData = approvalTransitionData,
            userWallet = userWallet,
            network = cryptoCurrency.network,
        ).fold(
            ifLeft = { feeError ->
                yieldSupplyNotificationsUpdateTrigger.triggerUpdate(
                    data = YieldSupplyNotificationData(
                        feeValue = null,
                        feeError = feeError,
                    ),
                )
                uiState.update {
                    it.copy(yieldSupplyFeeUM = YieldSupplyFeeUM.Error)
                }
            },
            ifRight = { fee ->
                applyFee(fee, approvalTransitionData)
            },
        )
    }

    private suspend fun applyFee(transactionFee: TransactionFee, approvalTransitionData: TransactionData.Uncompiled) {
        val feeCryptoValue = transactionFee.normal.amount.value

        val feeFiatValue = feeCryptoCurrencyStatus.value.fiatRate?.let { rate ->
            feeCryptoValue?.multiply(rate)
        }
        val fiatFee = feeFiatValue.format { fiat(appCurrency.code, appCurrency.symbol) }

        uiState.update {
            if (cryptoCurrencyStatus.value is CryptoCurrencyStatus.Loading) {
                it.copy(yieldSupplyFeeUM = YieldSupplyFeeUM.Loading)
            } else {
                it.copy(
                    isPrimaryButtonEnabled = true,
                    yieldSupplyFeeUM = YieldSupplyFeeUM.Content(
                        transactionDataList = persistentListOf(
                            approvalTransitionData.copy(fee = transactionFee.normal),
                        ),
                        feeFiatValue = stringReference(fiatFee),
                        tokenFeeFiatValue = TextReference.EMPTY,
                        maxNetworkFeeFiatValue = TextReference.EMPTY,
                        minTopUpFiatValue = TextReference.EMPTY,
                        feeNoteValue = TextReference.EMPTY,
                        estimatedFiatValue = TextReference.EMPTY,
                    ),
                )
            }
        }
        yieldSupplyNotificationsUpdateTrigger.triggerUpdate(
            data = YieldSupplyNotificationData(
                feeValue = feeCryptoValue,
                feeError = null,
            ),
        )
    }

    private fun subscribeOnNotificationsErrors() {
        yieldSupplyNotificationsUpdateTrigger.hasErrorFlow
            .onEach { hasError ->
                uiState.update {
                    it.copy(isPrimaryButtonEnabled = !hasError)
                }
            }.launchIn(modelScope)
    }
}