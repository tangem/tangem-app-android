package com.tangem.features.yield.supply.impl.main.model

import android.os.SystemClock
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.tangem.common.routing.AppRoute.YieldSupplyPromo
import com.tangem.common.routing.AppRouter
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.combinedReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.currency.hasNotSuppliedAmount
import com.tangem.domain.models.currency.shouldShowNotSuppliedInfoIcon
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.yield.supply.YieldSupplyStatus
import com.tangem.domain.networks.single.SingleNetworkStatusFetcher
import com.tangem.domain.tokens.GetSingleCryptoCurrencyStatusUseCase
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.domain.yield.supply.YieldSupplyRepository
import com.tangem.domain.yield.supply.models.YieldSupplyEnterStatus
import com.tangem.domain.yield.supply.usecase.YieldSupplyActivateUseCase
import com.tangem.domain.yield.supply.usecase.YieldSupplyDeactivateUseCase
import com.tangem.domain.yield.supply.usecase.YieldSupplyGetTokenStatusUseCase
import com.tangem.domain.yield.supply.usecase.YieldSupplyIsAvailableUseCase
import com.tangem.domain.yield.supply.usecase.YieldSupplyMinAmountUseCase
import com.tangem.features.yield.supply.api.YieldSupplyComponent
import com.tangem.features.yield.supply.api.analytics.YieldSupplyAnalytics
import com.tangem.features.yield.supply.impl.R
import com.tangem.features.yield.supply.impl.main.entity.YieldSupplyUM
import com.tangem.features.yield.supply.impl.main.model.transformers.YieldSupplyTokenStatusSuccessTransformer
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.DelayedWork
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import com.tangem.utils.transformer.update
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import kotlin.properties.Delegates

@Suppress("LongParameterList", "LargeClass")
@ModelScoped
internal class YieldSupplyModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val analyticsEventsHandler: AnalyticsEventHandler,
    private val appRouter: AppRouter,
    private val getUserWalletUseCase: GetUserWalletUseCase,
    private val getSingleCryptoCurrencyStatusUseCase: GetSingleCryptoCurrencyStatusUseCase,
    private val singleNetworkStatusFetcher: SingleNetworkStatusFetcher,
    @DelayedWork private val coroutineScope: CoroutineScope,
    private val getBalanceHidingSettingsUseCase: GetBalanceHidingSettingsUseCase,
    private val yieldSupplyGetTokenStatusUseCase: YieldSupplyGetTokenStatusUseCase,
    private val yieldSupplyIsAvailableUseCase: YieldSupplyIsAvailableUseCase,
    private val yieldSupplyActivateUseCase: YieldSupplyActivateUseCase,
    private val yieldSupplyDeactivateUseCase: YieldSupplyDeactivateUseCase,
    private val yieldSupplyRepository: YieldSupplyRepository,
    private val yieldSupplyMinAmountUseCase: YieldSupplyMinAmountUseCase,
) : Model(), YieldSupplyClickIntents {

    private val params = paramsContainer.require<YieldSupplyComponent.Params>()

    val uiState: StateFlow<YieldSupplyUM>
        field = MutableStateFlow<YieldSupplyUM>(YieldSupplyUM.Initial)

    val bottomSheetNavigation: SlotNavigation<Unit> = SlotNavigation()

    private val handleNavigation = params.handleNavigation
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
    private val fetchCurrencyJobHolder = JobHolder()

    private var lastStatusCheckTimestamp = 0L

    init {
        checkIfYieldSupplyIsAvailable()

        val protocolStatus = yieldSupplyRepository.getTokenProtocolStatus(
            userWalletId = params.userWalletId,
            cryptoCurrency = cryptoCurrency,
        )
        if (handleNavigation != null && protocolStatus == null) {
            if (handleNavigation) {
                modelScope.launch {
                    delay(timeMillis = 1000)
                    bottomSheetNavigation.activate(Unit)
                }
            } else {
                uiState
                    .filterIsInstance<YieldSupplyUM.Available>()
                    .take(1)
                    .onEach { state ->
                        appRouter.push(
                            YieldSupplyPromo(
                                userWalletId = params.userWalletId,
                                cryptoCurrency = params.cryptoCurrency,
                                apy = state.apy,
                            ),
                        )
                    }.launchIn(modelScope)
            }
        }
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
                    uiState.update { YieldSupplyUM.Initial }
                }
        }
    }

    override fun onStartEarningClick() {
        val yieldSupplyUM = uiState.value
        val apy = when (yieldSupplyUM) {
            is YieldSupplyUM.Available -> yieldSupplyUM.apy
            is YieldSupplyUM.Content -> yieldSupplyUM.apy
            else -> ""
        }
        appRouter.push(
            YieldSupplyPromo(
                userWalletId = params.userWalletId,
                cryptoCurrency = params.cryptoCurrency,
                apy = apy,
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
    private fun onCryptoCurrencyStatusUpdated(cryptoCurrencyStatus: CryptoCurrencyStatus) = modelScope.launch {
        val yieldSupplyStatus = cryptoCurrencyStatus.value.yieldSupplyStatus
        val tokenProtocolStatus = yieldSupplyRepository.getTokenProtocolStatus(
            userWallet.walletId,
            cryptoCurrency,
        )
        val tokenPendingStatus = yieldSupplyRepository.getTokenPendingStatus(
            userWallet.walletId,
            cryptoCurrencyStatus,
        )

        val isActive = yieldSupplyStatus?.isActive == true
        val isCryptoCurrencyStatusFromCache = cryptoCurrencyStatus.value.sources.networkSource != StatusSource.ACTUAL
        val processing = uiState.value is YieldSupplyUM.Processing
        Timber.d(
            "YIELD " +
                "yieldSupplyStatus $yieldSupplyStatus " +
                "tokenProtocolStatus $tokenProtocolStatus " +
                "tokenPendingStatus $tokenPendingStatus " +
                "isActive $isActive " +
                "processing $processing " +
                "isCryptoCurrencyStatusFromCache $isCryptoCurrencyStatusFromCache",
        )
        if (isCryptoCurrencyStatusFromCache && processing) {
            return@launch
        }

        when {
            tokenProtocolStatus != null && tokenPendingStatus != null -> {
                showProcessing(tokenPendingStatus)
                lastStatusCheckTimestamp = 0L
            }
            tokenProtocolStatus == YieldSupplyEnterStatus.Exit && isActive ||
                tokenProtocolStatus == YieldSupplyEnterStatus.Enter && !isActive -> {
                if (lastStatusCheckTimestamp != 0L) {
                    if (SystemClock.elapsedRealtime() - lastStatusCheckTimestamp > MAX_STATUS_CHECK_LIMIT) {
                        loadStatus(cryptoCurrencyStatus)
                        lastStatusCheckTimestamp = 0L
                    } else {
                        showProcessing(tokenProtocolStatus)
                    }
                } else {
                    showProcessing(tokenProtocolStatus)
                    lastStatusCheckTimestamp = SystemClock.elapsedRealtime()
                }
            }
            else -> {
                loadStatus(cryptoCurrencyStatus)
                lastStatusCheckTimestamp = 0L
            }
        }
    }

    private fun showProcessing(status: YieldSupplyEnterStatus) {
        uiState.update {
            when (status) {
                YieldSupplyEnterStatus.Enter -> YieldSupplyUM.Processing.Enter
                YieldSupplyEnterStatus.Exit -> YieldSupplyUM.Processing.Exit
            }
        }
        fetchCurrencyWithDelay()
    }

    private fun loadStatus(cryptoCurrencyStatus: CryptoCurrencyStatus) {
        val yieldSupplyStatus = cryptoCurrencyStatus.value.yieldSupplyStatus
        modelScope
            .launch {
                yieldSupplyRepository.saveTokenProtocolStatus(
                    userWalletId = userWallet.walletId,
                    cryptoCurrency = cryptoCurrency,
                    yieldSupplyEnterStatus = null,
                )
                sendInfoAboutProtocolStatus(cryptoCurrencyStatus)
                if (yieldSupplyStatus?.isActive == true) {
                    loadActiveState(
                        cryptoCurrencyStatus = cryptoCurrencyStatus,
                        yieldSupplyStatus = yieldSupplyStatus,
                    )
                } else {
                    loadTokenStatus()
                }
            }
    }

    private fun fetchCurrencyWithDelay() {
        coroutineScope.launch(dispatchers.io) {
            delay(PROCESSING_UPDATE_DELAY)
            singleNetworkStatusFetcher(
                params = SingleNetworkStatusFetcher.Params(
                    userWalletId = userWallet.walletId,
                    network = cryptoCurrency.network,
                ),
            ).onLeft {
                fetchCurrencyWithDelay()
            }
        }.saveIn(fetchCurrencyJobHolder)
    }

    private fun loadActiveState(cryptoCurrencyStatus: CryptoCurrencyStatus, yieldSupplyStatus: YieldSupplyStatus) {
        val cryptoCurrencyToken = cryptoCurrency as? CryptoCurrency.Token ?: return
        val showWarningIcon = !yieldSupplyStatus.isAllowedToSpend
        val state = uiState.value
        val isShowInfoIconPrevState = when (state) {
            is YieldSupplyUM.Content -> state.showInfoIcon
            else -> false
        }
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
                            showWarningIcon = showWarningIcon,
                            showInfoIcon = isShowInfoIconPrevState,
                            apy = tokenStatus.apy.toString(),
                        )
                    }
                    computeAndApplyShowInfoIcon(cryptoCurrencyStatus)
                }.onLeft {
                    Timber.e(it)
                    uiState.update {
                        YieldSupplyUM.Content(
                            title = resourceReference(
                                R.string.yield_module_token_details_earn_notification_earning_on_your_balance_title,
                            ),
                            subtitle = resourceReference(
                                R.string.yield_module_token_details_earn_notification_earning_on_your_balance_subtitle,
                            ),
                            rewardsApy = TextReference.EMPTY,
                            onClick = ::onActiveClick,
                            showWarningIcon = showWarningIcon,
                            showInfoIcon = isShowInfoIconPrevState,
                            apy = "",
                        )
                    }
                    computeAndApplyShowInfoIcon(cryptoCurrencyStatus)
                }
        }
    }

    private fun computeAndApplyShowInfoIcon(cryptoCurrencyStatus: CryptoCurrencyStatus) {
        modelScope.launch(dispatchers.default) {
            val showInfoIcon = if (cryptoCurrencyStatus.hasNotSuppliedAmount()) {
                val minAmount = yieldSupplyMinAmountUseCase(userWallet, cryptoCurrencyStatus).getOrNull()
                if (minAmount != null) {
                    cryptoCurrencyStatus.shouldShowNotSuppliedInfoIcon(minAmount)
                } else {
                    false
                }
            } else {
                false
            }
            uiState.update { state ->
                when (state) {
                    is YieldSupplyUM.Content -> state.copy(showInfoIcon = showInfoIcon)
                    else -> state
                }
            }
        }
    }

    private fun sendInfoAboutProtocolStatus(cryptoCurrencyStatus: CryptoCurrencyStatus) {
        if (lastYieldSupplyStatus == cryptoCurrencyStatus.value.yieldSupplyStatus) return
        val token = cryptoCurrency as? CryptoCurrency.Token ?: return
        val address = cryptoCurrencyStatus.value.networkAddress?.defaultAddress?.value ?: return
        modelScope.launch(dispatchers.default) {
            if (cryptoCurrencyStatus.value.yieldSupplyStatus?.isActive == true) {
                yieldSupplyActivateUseCase(
                    userWalletId = userWallet.walletId,
                    cryptoCurrency = token,
                    address = address,
                ).onRight {
                    lastYieldSupplyStatus = cryptoCurrencyStatus.value.yieldSupplyStatus
                }
            } else {
                yieldSupplyDeactivateUseCase(token, address).onRight {
                    lastYieldSupplyStatus = cryptoCurrencyStatus.value.yieldSupplyStatus
                }
            }
        }
    }

    private companion object {
        const val PROCESSING_UPDATE_DELAY = 10_000L
        const val MAX_STATUS_CHECK_LIMIT = 10_000L
    }
}