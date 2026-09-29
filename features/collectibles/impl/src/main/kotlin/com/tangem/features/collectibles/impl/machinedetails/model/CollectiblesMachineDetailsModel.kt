package com.tangem.features.collectibles.impl.machinedetails.model

import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.collectibles.CollectibleGachaMachineNftsBatchAction
import com.tangem.domain.collectibles.CollectibleGachaMachineNftsBatchingContext
import com.tangem.domain.collectibles.models.CollectibleGachaMachine
import com.tangem.domain.collectibles.models.CollectibleGachaMachineNftsListConfig
import com.tangem.domain.collectibles.usecase.GetCollectibleGachaMachineNftsBatchFlowUseCase
import com.tangem.domain.collectibles.usecase.GetCollectibleGachaMachineUseCase
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.collectibles.impl.CollectiblesRoute
import com.tangem.features.collectibles.impl.R
import com.tangem.features.collectibles.impl.machinedetails.CollectiblesMachineDetailsBottomSheetConfig
import com.tangem.features.collectibles.impl.machinedetails.CollectiblesMachineDetailsComponent
import com.tangem.features.collectibles.impl.machinedetails.model.transformer.CollectiblesPrizePoolTransformer
import com.tangem.features.collectibles.impl.machinedetails.model.transformer.CollectiblesUpdateExpectedValueTransformer
import com.tangem.features.collectibles.impl.machinedetails.model.transformer.CollectiblesUpdateMachineDetailsTransformer
import com.tangem.features.collectibles.impl.machinedetails.ui.state.CollectiblesMachineDetailsUM
import com.tangem.pagination.BatchAction
import com.tangem.pagination.BatchFetchResult
import com.tangem.pagination.PaginationStatus
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.PeriodicTask
import com.tangem.utils.coroutines.SingleTaskScheduler
import com.tangem.utils.coroutines.saveIn
import com.tangem.utils.logging.TangemLogger
import com.tangem.utils.transformer.update
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@ModelScoped
internal class CollectiblesMachineDetailsModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val getMachineUseCase: GetCollectibleGachaMachineUseCase,
    private val router: Router,
    private val urlOpener: UrlOpener,
    paramsContainer: ParamsContainer,
    getPrizePoolBatchFlowUseCase: GetCollectibleGachaMachineNftsBatchFlowUseCase,
) : Model() {

    private val params = paramsContainer.require<CollectiblesMachineDetailsComponent.Params>()

    val uiState: StateFlow<CollectiblesMachineDetailsUM>
        field = MutableStateFlow<CollectiblesMachineDetailsUM>(
            CollectiblesMachineDetailsUM.Loading(onBackClick = ::onBackClick),
        )

    val bottomSheetNavigation = SlotNavigation<CollectiblesMachineDetailsBottomSheetConfig>()

    private val actionsFlow = MutableSharedFlow<CollectibleGachaMachineNftsBatchAction>(replay = 1)

    private val prizePoolBatchFlow = getPrizePoolBatchFlowUseCase(
        context = CollectibleGachaMachineNftsBatchingContext(
            actionsFlow = actionsFlow,
            coroutineScope = modelScope,
        ),
        userWalletId = UserWalletId("0x"), // TODO collectibles remove when back updates
    )

    private val machineJob = JobHolder()
    private val expectedValuePolling = SingleTaskScheduler<CollectibleGachaMachine>()
    private val isResumed = MutableStateFlow(false)

    init {
        observePrizePool()
        observeExpectedValuePolling()
        loadMachine()
        reloadPrizePool()
    }

    fun onResume() {
        isResumed.value = true
    }

    fun onPause() {
        isResumed.value = false
    }

    fun onLoadMore() {
        modelScope.launch {
            val status = prizePoolBatchFlow.state.value.status
            if (status !is PaginationStatus.Paginating) return@launch

            val lastResult = status.lastResult
            val canLoadMore = lastResult !is BatchFetchResult.Success || !lastResult.last
            if (canLoadMore) actionsFlow.emit(BatchAction.LoadMore())
        }
    }

    private fun observePrizePool() {
        prizePoolBatchFlow.state
            .onEach { batchListState ->
                uiState.update { prev ->
                    CollectiblesPrizePoolTransformer(
                        batchListState = batchListState,
                        onReloadClick = ::reloadPrizePool,
                    ).transform(prev)
                }
            }
            .launchIn(modelScope)
    }

    private fun loadMachine() {
        modelScope.launch {
            getMachineUseCase(externalId = params.externalId)
                .fold(
                    ifLeft = {
                        uiState.value = CollectiblesMachineDetailsUM.Error(
                            onBackClick = ::onBackClick,
                            message = resourceReference(R.string.collectibles_machine_load_error),
                            onReloadClick = ::loadMachine,
                        )
                    },
                    ifRight = { machine ->
                        uiState.update(
                            CollectiblesUpdateMachineDetailsTransformer(
                                machine = machine,
                                onBackClick = ::onBackClick,
                                onBuyClick = ::onBuyClick,
                                onTurboCheckedChange = ::onTurboCheckedChange,
                                onTurboInfoClick = ::onTurboInfoClick,
                                onPoweredByClick = ::onPoweredByClick,
                            ),
                        )
                    },
                )
        }.saveIn(machineJob)
    }

    // Driven by state, not by the load: a load landing after onPause must not poll behind a stopped screen
    private fun observeExpectedValuePolling() {
        combine(uiState.map { it is CollectiblesMachineDetailsUM.Content }, isResumed) { isLoaded, isResumed ->
            isLoaded && isResumed
        }
            .distinctUntilChanged()
            .onEach { shouldPoll ->
                if (shouldPoll) startExpectedValuePolling() else expectedValuePolling.cancelTask()
            }
            .launchIn(modelScope)
    }

    // The machine endpoint is the only source of the expected value, so it is polled whole and the rest is dropped
    private fun startExpectedValuePolling() {
        expectedValuePolling.scheduleTask(
            scope = modelScope,
            task = PeriodicTask(
                delay = EXPECTED_VALUE_POLLING_DELAY_MS,
                initialDelay = EXPECTED_VALUE_POLLING_DELAY_MS,
                task = {
                    getMachineUseCase(externalId = params.externalId, skipCache = true)
                        .fold(ifLeft = { Result.failure(it.cause) }, ifRight = { Result.success(it) })
                },
                onSuccess = { machine -> uiState.update(CollectiblesUpdateExpectedValueTransformer(machine)) },
                onError = { error -> TangemLogger.e("Unable to refresh the machine expected value", error) },
            ),
        )
    }

    private fun reloadPrizePool() {
        modelScope.launch {
            actionsFlow.emit(
                BatchAction.Reload(
                    requestParams = CollectibleGachaMachineNftsListConfig(externalId = params.externalId),
                ),
            )
        }
    }

    private fun onTurboCheckedChange(isEnabled: Boolean) {
        uiState.update { prev ->
            if (prev !is CollectiblesMachineDetailsUM.Content) return@update prev

            prev.copy(footer = prev.footer.copy(isTurboEnabled = isEnabled))
        }
    }

    fun onDismissBottomSheet() {
        bottomSheetNavigation.dismiss()
    }

    private fun onTurboInfoClick() {
        bottomSheetNavigation.activate(CollectiblesMachineDetailsBottomSheetConfig.TurboInfo)
    }

    private fun onBuyClick() {
        val isTurboEnabled = (uiState.value as? CollectiblesMachineDetailsUM.Content)?.footer?.isTurboEnabled == true
        router.push(CollectiblesRoute.ConfirmOrder(externalId = params.externalId, isTurboEnabled = isTurboEnabled))
    }

    private fun onPoweredByClick() {
        urlOpener.openUrl(COLLECTOR_CRYPT_URL)
    }

    private fun onBackClick() {
        router.pop()
    }

    private companion object {

        const val EXPECTED_VALUE_POLLING_DELAY_MS = 10_000L
        const val COLLECTOR_CRYPT_URL = "https://collectorcrypt.com" // TODO confirm the correct Collector Crypt link
    }
}