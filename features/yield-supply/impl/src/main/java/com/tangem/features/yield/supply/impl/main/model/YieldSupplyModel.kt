package com.tangem.features.yield.supply.impl.main.model

import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.tangem.common.routing.AppRoute.YieldSupplyPromo
import com.tangem.common.routing.AppRouter
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.ui.extensions.combinedReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.TxInfo
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.yield.supply.YieldSupplyStatus
import com.tangem.domain.tokens.FetchCurrencyStatusUseCase
import com.tangem.domain.tokens.GetSingleCryptoCurrencyStatusUseCase
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.domain.yield.supply.usecase.YieldSupplyActivateUseCase
import com.tangem.domain.yield.supply.usecase.YieldSupplyDeactivateUseCase
import com.tangem.domain.yield.supply.usecase.YieldSupplyGetTokenStatusUseCase
import com.tangem.domain.yield.supply.usecase.YieldSupplyIsAvailableUseCase
import com.tangem.features.yield.supply.impl.R
import com.tangem.features.yield.supply.api.YieldSupplyComponent
import com.tangem.features.yield.supply.api.analytics.YieldSupplyAnalytics
import com.tangem.features.yield.supply.impl.main.entity.YieldSupplyUM
import com.tangem.features.yield.supply.impl.main.model.transformers.YieldSupplyTokenStatusSuccessTransformer
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.DelayedWork
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import com.tangem.utils.transformer.update
import javax.inject.Inject
import kotlin.properties.Delegates

@Suppress("LongParameterList")
@ModelScoped
internal class YieldSupplyModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val analyticsEventsHandler: AnalyticsEventHandler,
    private val appRouter: AppRouter,
    private val getUserWalletUseCase: GetUserWalletUseCase,
    private val getSingleCryptoCurrencyStatusUseCase: GetSingleCryptoCurrencyStatusUseCase,
    private val fetchCurrencyStatusUseCase: FetchCurrencyStatusUseCase,
    @DelayedWork private val coroutineScope: CoroutineScope,
    private val getBalanceHidingSettingsUseCase: GetBalanceHidingSettingsUseCase,
    private val yieldSupplyGetTokenStatusUseCase: YieldSupplyGetTokenStatusUseCase,
    private val yieldSupplyIsAvailableUseCase: YieldSupplyIsAvailableUseCase,
    private val yieldSupplyActivateUseCase: YieldSupplyActivateUseCase,
    private val yieldSupplyDeactivateUseCase: YieldSupplyDeactivateUseCase,
) : Model(), YieldSupplyClickIntents {

    private val params = paramsContainer.require<YieldSupplyComponent.Params>()

    val uiState: StateFlow<YieldSupplyUM>
        field = MutableStateFlow<YieldSupplyUM>(YieldSupplyUM.Initial)

    val bottomSheetNavigation: SlotNavigation<Unit> = SlotNavigation()

    private val cryptoCurrency = params.cryptoCurrency
    var userWallet: UserWallet by Delegates.notNull()

    val cryptoCurrencyStatusFlow: StateFlow<CryptoCurrencyStatus>
        field = MutableStateFlow(
            CryptoCurrencyStatus(
                currency = params.cryptoCurrency,
                value = CryptoCurrencyStatus.Loading,
            ),
        )

    val isBalanceHiddenFlow: StateFlow<Boolean>
        field = MutableStateFlow(false)

    private var lastYieldSupplyStatus: YieldSupplyStatus? = null

    init {
        checkIfYieldSupplyIsAvailable()
    }

    private fun checkIfYieldSupplyIsAvailable() {
        modelScope.launch(dispatchers.io) {
            val isAvailable = yieldSupplyIsAvailableUseCase(params.userWalletId, params.cryptoCurrency)
            if (isAvailable) {
                subscribeOnCurrencyStatusUpdates()
                subscribeOnBalanceHidden()
            }
        }
    }

    private fun subscribeOnCurrencyStatusUpdates() {
        modelScope.launch {
            getUserWalletUseCase(params.userWalletId).fold(
                ifRight = { wallet ->
                    userWallet = wallet

                    getSingleCryptoCurrencyStatusUseCase.invokeMultiWallet(
                        userWalletId = params.userWalletId,
                        currencyId = cryptoCurrency.id,
                        isSingleWalletWithTokens = false,
                    ).onEach { maybeCryptoCurrency ->
                        maybeCryptoCurrency.fold(
                            ifRight = { cryptoCurrencyStatus ->
                                cryptoCurrencyStatusFlow.update { cryptoCurrencyStatus }
                                onCryptoCurrencyStatusUpdated(cryptoCurrencyStatus)
                            },
                            ifLeft = {
                                Timber.w(it.toString())
                            },
                        )
                    }.launchIn(modelScope)
                },
                ifLeft = {
                    Timber.w(it.toString())
                    return@launch
                },
            )
        }
    }

    private fun loadTokenStatus() {
        val cryptoCurrencyToken = cryptoCurrency as? CryptoCurrency.Token ?: return
        modelScope.launch(dispatchers.default) {
            yieldSupplyGetTokenStatusUseCase(cryptoCurrencyToken)
                .onRight { tokenStatus ->
                    uiState.update(
                        YieldSupplyTokenStatusSuccessTransformer(
                            tokenStatus = tokenStatus,
                            onStartEarningClick = ::onStartEarningClick,
                        ),
                    )
                }.onLeft {
                    Timber.e(it)
                    uiState.update { YieldSupplyUM.Unavailable }
                }
        }
    }

    override fun onStartEarningClick() {
        appRouter.push(
            YieldSupplyPromo(
                userWalletId = params.userWalletId,
                cryptoCurrency = params.cryptoCurrency,
            ),
        )
    }

    override fun onActiveClick() {
        bottomSheetNavigation.activate(Unit)
    }

    private fun subscribeOnBalanceHidden() {
        getBalanceHidingSettingsUseCase()
            .conflate()
            .distinctUntilChanged()
            .onEach {
                isBalanceHiddenFlow.value = it.isBalanceHidden
            }
            .flowOn(dispatchers.default)
            .launchIn(modelScope)
    }

    @Suppress("MaximumLineLength")
    private fun onCryptoCurrencyStatusUpdated(cryptoCurrencyStatus: CryptoCurrencyStatus) {
        val yieldSupplyStatus = cryptoCurrencyStatus.value.yieldSupplyStatus
        val hasActiveTransaction = cryptoCurrencyStatus.value.hasCurrentNetworkTransactions
        val yieldTransaction = cryptoCurrencyStatus.value.pendingTransactions.firstOrNull {
            it.type is TxInfo.TransactionType.YieldSupply
        }?.type as? TxInfo.TransactionType.YieldSupply
        sendInfoAboutProtocolStatus(cryptoCurrencyStatus)

        when {
            hasActiveTransaction && yieldTransaction != null -> {
                coroutineScope.launch(dispatchers.io) {
                    delay(PROCESSING_UPDATE_DELAY)
                    fetchCurrencyStatusUseCase(userWalletId = userWallet.walletId, cryptoCurrency.id)
                }
                uiState.update {
                    when (yieldTransaction) {
                        TxInfo.TransactionType.YieldSupply.Enter -> YieldSupplyUM.Processing.Enter
                        TxInfo.TransactionType.YieldSupply.Exit -> YieldSupplyUM.Processing.Exit
                    }
                }
            }
            yieldSupplyStatus?.isActive == true -> {
                val cryptoCurrencyToken = cryptoCurrency as? CryptoCurrency.Token ?: return
                if (!yieldSupplyStatus.isAllowedToSpend) {
                    analyticsEventsHandler.send(
                        YieldSupplyAnalytics.NoticeApproveNeeded(
                            token = cryptoCurrency.symbol,
                            blockchain = cryptoCurrency.network.name,
                        ),
                    )
                }
                modelScope.launch(dispatchers.default) {
                    yieldSupplyGetTokenStatusUseCase(cryptoCurrencyToken)
                        .onRight { tokenStatus ->
                            uiState.update {
                                YieldSupplyUM.Content(
                                    title = resourceReference(
                                        R.string.yield_module_token_details_earn_notification_earning_on_your_balance_title,
                                    ),
                                    subtitle = resourceReference(
                                        R.string.yield_module_token_details_earn_notification_earning_on_your_balance_subtitle,
                                    ),
                                    rewardsApy = combinedReference(
                                        resourceReference(
                                            R.string.yield_module_token_details_earn_notification_apy,
                                        ),
                                        stringReference(" ${tokenStatus.apy}%"),
                                    ),
                                    onClick = ::onActiveClick,
                                    isAllowedToSpend = yieldSupplyStatus.isAllowedToSpend,
                                )
                            }
                        }.onLeft {
                            Timber.e(it)
                            uiState.update { YieldSupplyUM.Loading }
                        }
                }
            }

            else -> {
                loadTokenStatus()
            }
        }
    }

    private fun sendInfoAboutProtocolStatus(cryptoCurrencyStatus: CryptoCurrencyStatus) {
        if (lastYieldSupplyStatus == cryptoCurrencyStatus.value.yieldSupplyStatus) return
        val token = cryptoCurrency as? CryptoCurrency.Token ?: return
        modelScope.launch(dispatchers.default) {
            if (cryptoCurrencyStatus.value.yieldSupplyStatus?.isActive == true) {
                yieldSupplyActivateUseCase(token).onRight {
                    lastYieldSupplyStatus = cryptoCurrencyStatus.value.yieldSupplyStatus
                }
            } else {
                yieldSupplyDeactivateUseCase(token).onRight {
                    lastYieldSupplyStatus = cryptoCurrencyStatus.value.yieldSupplyStatus
                }
            }
        }
    }

    private companion object {
        const val PROCESSING_UPDATE_DELAY = 10_000L
    }
}