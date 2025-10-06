package com.tangem.features.yield.supply.impl.main.model

import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.tangem.common.routing.AppRoute.YieldSupplyPromo
import com.tangem.common.routing.AppRouter
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.TxInfo
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.tokens.FetchCurrencyStatusUseCase
import com.tangem.domain.tokens.GetSingleCryptoCurrencyStatusUseCase
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.domain.yield.supply.usecase.YieldSupplyGetTokenStatusUseCase
import com.tangem.features.yield.supply.api.YieldSupplyComponent
import com.tangem.features.yield.supply.impl.main.entity.YieldSupplyUM
import com.tangem.features.yield.supply.impl.main.entity.LoadingStatusMode
import com.tangem.features.yield.supply.impl.main.model.transformers.YieldSupplyTokenStatusFailureTransformer
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
    private val appRouter: AppRouter,
    private val getUserWalletUseCase: GetUserWalletUseCase,
    private val getSingleCryptoCurrencyStatusUseCase: GetSingleCryptoCurrencyStatusUseCase,
    private val fetchCurrencyStatusUseCase: FetchCurrencyStatusUseCase,
    @DelayedWork private val coroutineScope: CoroutineScope,
    private val getBalanceHidingSettingsUseCase: GetBalanceHidingSettingsUseCase,
    private val yieldSupplyGetTokenStatusUseCase: YieldSupplyGetTokenStatusUseCase,
) : Model(), YieldSupplyClickIntents {

    private val params = paramsContainer.require<YieldSupplyComponent.Params>()

    val uiState: StateFlow<YieldSupplyUM>
        field = MutableStateFlow<YieldSupplyUM>(YieldSupplyUM.Loading)

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

    init {
        subscribeOnCurrencyStatusUpdates()
        subscribeOnBalanceHidden()
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
                                onDataLoaded(cryptoCurrencyStatus)
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

    private fun loadTokenStatus(mode: LoadingStatusMode) {
        val cryptoCurrencyToken = cryptoCurrency as? CryptoCurrency.Token ?: return
        modelScope.launch(dispatchers.default) {
            yieldSupplyGetTokenStatusUseCase(cryptoCurrencyToken)
                .onRight { tokenStatus ->
                    uiState.update(
                        YieldSupplyTokenStatusSuccessTransformer(
                            tokenStatus = tokenStatus,
                            onStartEarningClick = ::onStartEarningClick,
                            mode = mode,
                        ),
                    )
                }.onLeft {
                    uiState.update(YieldSupplyTokenStatusFailureTransformer(mode))
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

    private fun onDataLoaded(cryptoCurrencyStatus: CryptoCurrencyStatus) {
        val yieldSupplyStatus = cryptoCurrencyStatus.value.yieldSupplyStatus
        val hasActiveTransaction = cryptoCurrencyStatus.value.hasCurrentNetworkTransactions
        val yieldTransaction = cryptoCurrencyStatus.value.pendingTransactions.firstOrNull {
            it.type is TxInfo.TransactionType.YieldSupply
        }?.type as? TxInfo.TransactionType.YieldSupply

        val yieldSupplyUM = when {
            hasActiveTransaction && yieldTransaction != null -> {
                coroutineScope.launch(dispatchers.io) {
                    delay(PROCESSING_UPDATE_DELAY)
                    fetchCurrencyStatusUseCase(userWalletId = userWallet.walletId, cryptoCurrency.id)
                }
                when (yieldTransaction) {
                    TxInfo.TransactionType.YieldSupply.Enter -> YieldSupplyUM.Processing.Enter
                    TxInfo.TransactionType.YieldSupply.Exit -> YieldSupplyUM.Processing.Exit
                }
            }
            yieldSupplyStatus?.isActive == true ->
                YieldSupplyUM.Content(
                    rewardsBalance = TextReference.EMPTY,
                    rewardsApy = TextReference.EMPTY,
                    onClick = ::onActiveClick,
                    isAllowedToSpend = yieldSupplyStatus.isAllowedToSpend,
                )
            else -> YieldSupplyUM.Loading
        }

        uiState.update { yieldSupplyUM }

        when (yieldSupplyUM) {
            is YieldSupplyUM.Loading -> loadTokenStatus(LoadingStatusMode.Initial)
            is YieldSupplyUM.Content -> loadTokenStatus(LoadingStatusMode.LoadApy)
            else -> Unit
        }
    }

    private companion object {
        const val PROCESSING_UPDATE_DELAY = 10_000L
    }
}