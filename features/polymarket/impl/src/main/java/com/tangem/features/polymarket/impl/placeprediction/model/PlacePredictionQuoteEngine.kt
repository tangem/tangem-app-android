package com.tangem.features.polymarket.impl.placeprediction.model

import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.PredictionAccountStatusValue
import com.tangem.domain.polymarket.PolymarketPositionsRepository
import com.tangem.domain.polymarket.flow.PredictionAccountStatusSupplier
import com.tangem.domain.polymarket.flow.PredictionBalancePoller
import com.tangem.domain.polymarket.flow.PredictionFundingWindow
import com.tangem.domain.polymarket.interactor.RefreshPredictionDepositRequisitesInteractor
import com.tangem.domain.polymarket.model.PolymarketPositionId
import com.tangem.domain.polymarket.model.PredictionOrderQuoteRequest
import com.tangem.domain.polymarket.model.PredictionOrderSide
import com.tangem.domain.polymarket.model.PredictionPriceError
import com.tangem.domain.polymarket.model.findOutcome
import com.tangem.domain.polymarket.usecase.CheckPolymarketGeoblockUseCase
import com.tangem.domain.polymarket.usecase.DerivePolymarketAddressesUseCase
import com.tangem.domain.polymarket.usecase.GetPolymarketCollateralCurrencyUseCase
import com.tangem.domain.polymarket.usecase.GetPolymarketEventUseCase
import com.tangem.domain.polymarket.usecase.GetPredictionOrderQuoteUseCase
import com.tangem.domain.polymarket.usecase.GetPredictionPriceUseCase
import com.tangem.features.polymarket.impl.placeprediction.PlacePredictionComponent
import com.tangem.features.polymarket.impl.placeprediction.entity.AmountFractionUM
import com.tangem.features.polymarket.impl.placeprediction.entity.OrderSideUM
import com.tangem.features.polymarket.impl.placeprediction.entity.PlacePredictionUM
import com.tangem.features.polymarket.impl.placeprediction.entity.PreparedPrediction
import com.tangem.features.polymarket.impl.placeprediction.entity.PriceErrorUM
import com.tangem.features.polymarket.impl.placeprediction.entity.SellPositionUM
import com.tangem.features.polymarket.impl.placeprediction.entity.SlippageUM
import com.tangem.features.polymarket.impl.placeprediction.entity.TradingPermissionUM
import com.tangem.features.polymarket.impl.placeprediction.entity.canPollPrice
import com.tangem.features.polymarket.impl.placeprediction.entity.enteredAmount
import com.tangem.features.polymarket.impl.placeprediction.entity.isMarketClosed
import com.tangem.features.polymarket.impl.placeprediction.entity.isQuotable
import com.tangem.features.polymarket.impl.placeprediction.model.transformers.SetAddFundsAvailabilityTransformer
import com.tangem.features.polymarket.impl.placeprediction.model.transformers.SetAmountFractionTransformer
import com.tangem.features.polymarket.impl.placeprediction.model.transformers.SetAmountTransformer
import com.tangem.features.polymarket.impl.placeprediction.model.transformers.SetAwaitingFundsTransformer
import com.tangem.features.polymarket.impl.placeprediction.model.transformers.SetBalanceTransformer
import com.tangem.features.polymarket.impl.placeprediction.model.transformers.SetCollateralTransformer
import com.tangem.features.polymarket.impl.placeprediction.model.transformers.SetMarketTransformer
import com.tangem.features.polymarket.impl.placeprediction.model.transformers.SetOutcomePriceTransformer
import com.tangem.features.polymarket.impl.placeprediction.model.transformers.SetPositionTransformer
import com.tangem.features.polymarket.impl.placeprediction.model.transformers.SetPriceErrorTransformer
import com.tangem.features.polymarket.impl.placeprediction.model.transformers.SetQuoteLoadingTransformer
import com.tangem.features.polymarket.impl.placeprediction.model.transformers.SetQuoteResultTransformer
import com.tangem.features.polymarket.impl.placeprediction.model.transformers.SetSellPositionStateTransformer
import com.tangem.features.polymarket.impl.placeprediction.model.transformers.SetTradingPermissionTransformer
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import com.tangem.utils.logging.TangemLogger
import com.tangem.utils.transformer.Transformer
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal

internal const val QUOTE_DEBOUNCE_MILLIS = 500L
internal const val QUOTE_POLL_INTERVAL_MILLIS = 3_000L

@Suppress("LongParameterList", "TooManyFunctions")
internal class PlacePredictionQuoteEngine @AssistedInject constructor(
    @Assisted private val params: PlacePredictionComponent.Params,
    @Assisted private val uiState: MutableStateFlow<PlacePredictionUM>,
    @Assisted private val scope: CoroutineScope,
    router: Router,
    messageSender: UiMessageSender,
    private val dispatchers: CoroutineDispatcherProvider,
    private val getPolymarketEventUseCase: GetPolymarketEventUseCase,
    getPolymarketCollateralCurrencyUseCase: GetPolymarketCollateralCurrencyUseCase,
    private val derivePolymarketAddressesUseCase: DerivePolymarketAddressesUseCase,
    private val getPredictionOrderQuoteUseCase: GetPredictionOrderQuoteUseCase,
    private val getPredictionPriceUseCase: GetPredictionPriceUseCase,
    private val checkPolymarketGeoblockUseCase: CheckPolymarketGeoblockUseCase,
    private val positionsRepository: PolymarketPositionsRepository,
    private val predictionFundingWindow: PredictionFundingWindow,
    private val predictionAccountStatusSupplier: PredictionAccountStatusSupplier,
    private val predictionBalancePoller: PredictionBalancePoller,
    refreshPredictionDepositRequisitesInteractor: RefreshPredictionDepositRequisitesInteractor,
) {

    private val quoteJobHolder = JobHolder()
    private val tradingAllowanceJobHolder = JobHolder()
    private val positionJobHolder = JobHolder()
    private val balancePollJobHolder = JobHolder()

    private val activeStatus = MutableStateFlow<PredictionAccountStatusValue.Active?>(value = null)

    private val tradingPermission = MutableStateFlow<TradingPermissionUM?>(value = null)

    private val isFundingWindowOpen = MutableStateFlow<Boolean?>(value = null)

    private val isAddFundsEnabled = MutableStateFlow<Boolean?>(value = null)

    private val isHeld = MutableStateFlow(false)

    val addFunds = PlacePredictionAddFunds(
        userWalletId = params.userWalletId,
        activeStatus = activeStatus,
        router = router,
        messageSender = messageSender,
        refreshInteractor = refreshPredictionDepositRequisitesInteractor,
        scope = scope,
    )

    private var isNegRiskMarket: Boolean? = null

    private var positionId: PolymarketPositionId? = null
    private var tradedAssetId: String = params.assetId
    private var isTradedAssetResolved = params is PlacePredictionComponent.Params.Sell

    private var isResumed = false

    init {
        write(SetCollateralTransformer(currency = getPolymarketCollateralCurrencyUseCase()))
        when (val params = params) {
            is PlacePredictionComponent.Params.Buy -> loadMarket(params = params)
            is PlacePredictionComponent.Params.Sell -> loadPosition(params = params)
        }
        observeAccountStatus()
        observeFundingWindow()
        loadTradingAllowance()
    }

    fun onResume() {
        isResumed = true
        if (tradingPermission.value == TradingPermissionUM.Unknown) loadTradingAllowance()
        addFunds.retryIfFailed()
        predictionBalancePoller.subscribe(userWalletId = params.userWalletId)
            .launchIn(scope)
            .saveIn(balancePollJobHolder)
        if (!isHeld.value) refresh()
    }

    fun onPause() {
        isResumed = false
        quoteJobHolder.cancel()
        balancePollJobHolder.cancel()
    }

    fun onAmountChange(value: String) {
        write(SetAmountTransformer(value = value))
        restartQuoteLoop(withDebounce = true)
    }

    fun onFractionClick(fraction: AmountFractionUM) {
        uiState.update(SetAmountFractionTransformer(fraction = fraction))
        restartQuoteLoop(withDebounce = false)
    }

    fun applySlippage(percent: BigDecimal) {
        if (uiState.value.slippage.percent.compareTo(percent) == 0) return

        write { state ->
            SetAmountTransformer(value = state.amountValue)
                .transform(state.copy(slippage = SlippageUM.of(percent = percent), minAmount = null))
        }
        restartQuoteLoop(withDebounce = false)
    }

    fun retryQuote() {
        if (isHeld.value || reloadPositionIfLost()) return

        restartQuoteLoop(withDebounce = false)
    }

    fun prepare(): PreparedPrediction? {
        val state = uiState.value
        val isNegRisk = isNegRiskMarket ?: return null
        if (!state.isPrimaryButtonEnabled) return null

        quoteJobHolder.cancel()
        return PreparedPrediction(params = params, state = state, isNegRisk = isNegRisk, positionId = positionId)
    }

    fun holdForSubmit() {
        isHeld.value = true
        quoteJobHolder.cancel()
    }

    fun release() {
        if (!isHeld.value) return

        isHeld.value = false
        resyncDroppedUpdates()
        if (isResumed) refresh()
    }

    private fun resyncDroppedUpdates() {
        write(SetBalanceTransformer(balance = activeStatus.value.actualBalance()))
        tradingPermission.value?.let { write(SetTradingPermissionTransformer(permission = it)) }
        isFundingWindowOpen.value?.let { write(SetAwaitingFundsTransformer(isAwaitingFunds = it)) }
        isAddFundsEnabled.value?.let { write(SetAddFundsAvailabilityTransformer(isEnabled = it)) }
    }

    private fun refresh() {
        if (params is PlacePredictionComponent.Params.Sell) loadPosition(params = params)
        restartQuoteLoop(withDebounce = false)
    }

    private fun write(transformer: Transformer<PlacePredictionUM>) {
        write(transform = transformer::transform)
    }

    private fun write(transform: (PlacePredictionUM) -> PlacePredictionUM) {
        if (!isHeld.value) uiState.update(transform)
    }

    private fun PredictionAccountStatusValue.Active?.actualBalance(): BigDecimal? =
        this?.takeIf { it.source == StatusSource.ACTUAL }?.balance

    private fun loadTradingAllowance() {
        if (tradingAllowanceJobHolder.isActive) return

        scope.launch {
            val permission = withContext(dispatchers.default) {
                checkPolymarketGeoblockUseCase().fold(
                    ifLeft = { TradingPermissionUM.Unknown },
                    ifRight = { isBlocked ->
                        if (isBlocked) TradingPermissionUM.Restricted else TradingPermissionUM.Allowed
                    },
                )
            }

            tradingPermission.value = permission
            write(SetTradingPermissionTransformer(permission = permission))
        }.saveIn(tradingAllowanceJobHolder)
    }

    private fun observeAccountStatus() {
        predictionAccountStatusSupplier(userWalletId = params.userWalletId)
            .onEach { status -> activeStatus.value = status as? PredictionAccountStatusValue.Active }
            .flowOn(dispatchers.default)
            .launchIn(scope)

        activeStatus
            .map { status -> status.actualBalance() }
            .distinctUntilChanged()
            .onEach { balance -> write(SetBalanceTransformer(balance = balance)) }
            .launchIn(scope)

        if (params.side != PredictionOrderSide.BUY) return

        activeStatus
            .filterNotNull()
            .take(count = 1)
            .onEach { addFunds.prefetch() }
            .launchIn(scope)

        addFunds.isEnabled(permission = tradingPermission)
            .onEach { isEnabled ->
                isAddFundsEnabled.value = isEnabled
                write(SetAddFundsAvailabilityTransformer(isEnabled = isEnabled))
            }
            .launchIn(scope)
    }

    private fun observeFundingWindow() {
        predictionFundingWindow.isOpen(userWalletId = params.userWalletId)
            .onEach { isOpen ->
                isFundingWindowOpen.value = isOpen
                write(SetAwaitingFundsTransformer(isAwaitingFunds = isOpen))
            }
            .flowOn(dispatchers.default)
            .launchIn(scope)
    }

    private fun loadMarket(params: PlacePredictionComponent.Params.Buy) {
        scope.launch {
            val event = withContext(dispatchers.default) {
                getPolymarketEventUseCase(userWalletId = params.userWalletId, eventId = params.eventId).getOrNull()
            } ?: return@launch

            val market = event.markets.firstOrNull { it.id == params.marketId } ?: return@launch
            val outcome = market.findOutcome(params.assetId) ?: return@launch

            isNegRiskMarket = market.isNegRisk
            tradedAssetId = outcome.assetId
            isTradedAssetResolved = true
            positionId = market.conditionId?.let { PolymarketPositionId(conditionId = it, assetId = outcome.assetId) }

            write(SetMarketTransformer(market = market, outcome = outcome))

            restartQuoteLoop(withDebounce = false)
        }
    }

    /** @return whether a fresh read was started, in which case it — not the caller — restarts the loop. */
    private fun reloadPositionIfLost(): Boolean {
        val params = params as? PlacePredictionComponent.Params.Sell ?: return false
        val position = (uiState.value.order as? OrderSideUM.Sell)?.position ?: return false

        if (position is SellPositionUM.Loading || position is SellPositionUM.Held) return false

        write(SetSellPositionStateTransformer(position = SellPositionUM.Loading))
        loadPosition(params = params)
        return true
    }

    private fun failPosition(position: SellPositionUM) {
        write(SetSellPositionStateTransformer(position = position))
        restartQuoteLoop(withDebounce = false)
    }

    private fun loadPosition(params: PlacePredictionComponent.Params.Sell) {
        if (positionJobHolder.isActive) return

        scope.launch {
            val addresses = withContext(dispatchers.default) {
                derivePolymarketAddressesUseCase.stored(userWalletId = params.userWalletId)
            }
            if (addresses == null) {
                logger.e("No stored addresses; the screen keeps nothing sellable")
                failPosition(position = SellPositionUM.Unreadable)
                return@launch
            }

            val position = withContext(dispatchers.default) {
                positionsRepository.getPosition(addresses = addresses, id = params.positionId)
            }.fold(
                ifLeft = { error ->
                    logger.e("Position could not be read, the screen keeps nothing sellable: $error")
                    failPosition(position = SellPositionUM.Unreadable)
                    return@launch
                },
                ifRight = { position ->
                    position ?: run {
                        logger.e("Position is not held or not indexed yet; the screen keeps nothing sellable")
                        failPosition(position = SellPositionUM.NotHeld)
                        return@launch
                    }
                },
            )

            isNegRiskMarket = position.isNegRisk

            write(SetPositionTransformer(position = position))

            restartQuoteLoop(withDebounce = false)
        }.saveIn(positionJobHolder)
    }

    private fun restartQuoteLoop(withDebounce: Boolean) {
        quoteJobHolder.cancel()

        if (!isResumed || !isTradedAssetResolved || isHeld.value) return
        if (!uiState.value.isMarketAcceptingOrders || !uiState.value.canPollPrice()) return

        scope.launch {
            if (withDebounce && uiState.value.isQuotable()) delay(timeMillis = QUOTE_DEBOUNCE_MILLIS)

            while (isActive) {
                val canKeepPolling = if (uiState.value.isQuotable()) {
                    requestQuote()
                    !uiState.value.quote.isMarketClosed()
                } else {
                    requestPrice()
                }
                if (!canKeepPolling) return@launch
                delay(timeMillis = QUOTE_POLL_INTERVAL_MILLIS)
            }
        }.saveIn(quoteJobHolder)
    }

    private suspend fun requestPrice(): Boolean {
        val result = withContext(dispatchers.default) {
            getPredictionPriceUseCase(assetId = tradedAssetId, side = params.side.opposite())
        }

        return result.fold(
            ifLeft = { error ->
                when (error) {
                    PredictionPriceError.MarketClosed -> {
                        write(SetPriceErrorTransformer(error = PriceErrorUM.MarketClosed))
                        false
                    }
                    PredictionPriceError.InvalidAsset -> {
                        write(SetPriceErrorTransformer(error = PriceErrorUM.Unknown))
                        false
                    }
                    PredictionPriceError.EmptyBook -> {
                        write(SetPriceErrorTransformer(error = PriceErrorUM.EmptyBook))
                        true
                    }
                    PredictionPriceError.Network,
                    is PredictionPriceError.Unknown,
                    -> true
                }
            },
            ifRight = { price ->
                write(SetOutcomePriceTransformer(price = price))
                true
            },
        )
    }

    private suspend fun requestQuote() {
        val state = uiState.value
        val amount = state.enteredAmount() ?: return
        if (!state.isQuotable()) return

        write(SetQuoteLoadingTransformer)

        val result = withContext(dispatchers.default) {
            getPredictionOrderQuoteUseCase( userWalletId = params.userWalletId,
                request = PredictionOrderQuoteRequest(
                    marketId = params.marketId,
                    assetId = tradedAssetId,
                    side = params.side,
                    amount = amount,
                    slippagePercent = state.slippage.percent,
                ),
            )
        }

        write(
            SetQuoteResultTransformer(
                result = result,
                receivedAtMillis = System.currentTimeMillis(),
                assetId = tradedAssetId,
            ),
        )
    }

    @AssistedFactory
    interface Factory {
        fun create(
            params: PlacePredictionComponent.Params,
            uiState: MutableStateFlow<PlacePredictionUM>,
            scope: CoroutineScope,
        ): PlacePredictionQuoteEngine
    }

    private companion object {
        val logger = TangemLogger.withTag(tag = "PlacePredictionQuoteEngine")
    }
}