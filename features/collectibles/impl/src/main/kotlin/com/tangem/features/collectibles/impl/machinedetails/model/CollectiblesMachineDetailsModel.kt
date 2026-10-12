package com.tangem.features.collectibles.impl.machinedetails.model

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.collectibles.CollectibleGachaMachineNftsBatchAction
import com.tangem.domain.collectibles.CollectibleGachaMachineNftsBatchingContext
import com.tangem.domain.collectibles.models.CollectibleGachaMachineNftsListConfig
import com.tangem.domain.collectibles.usecase.GetCollectibleGachaMachineNftsBatchFlowUseCase
import com.tangem.domain.collectibles.usecase.GetCollectibleGachaMachineUseCase
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.collectibles.impl.R
import com.tangem.features.collectibles.impl.machinedetails.CollectiblesMachineDetailsComponent
import com.tangem.features.collectibles.impl.machinedetails.model.transformer.CollectiblesPrizePoolTransformer
import com.tangem.features.collectibles.impl.machinedetails.model.transformer.CollectiblesUpdateMachineDetailsTransformer
import com.tangem.features.collectibles.impl.machinedetails.ui.state.CollectiblesMachineDetailsUM
import com.tangem.pagination.BatchAction
import com.tangem.pagination.BatchFetchResult
import com.tangem.pagination.PaginationStatus
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import com.tangem.utils.transformer.update
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@ModelScoped
internal class CollectiblesMachineDetailsModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val getMachineUseCase: GetCollectibleGachaMachineUseCase,
    private val router: Router,
    paramsContainer: ParamsContainer,
    getPrizePoolBatchFlowUseCase: GetCollectibleGachaMachineNftsBatchFlowUseCase,
) : Model() {

    private val params = paramsContainer.require<CollectiblesMachineDetailsComponent.Params>()

    val uiState: StateFlow<CollectiblesMachineDetailsUM>
        field = MutableStateFlow<CollectiblesMachineDetailsUM>(
            CollectiblesMachineDetailsUM.Loading(onBackClick = ::onBackClick),
        )

    private val actionsFlow = MutableSharedFlow<CollectibleGachaMachineNftsBatchAction>(replay = 1)

    private val prizePoolBatchFlow = getPrizePoolBatchFlowUseCase(
        context = CollectibleGachaMachineNftsBatchingContext(
            actionsFlow = actionsFlow,
            coroutineScope = modelScope,
        ),
        userWalletId = UserWalletId("0x"), // TODO collectibles remove when back updates
    )

    private val machineJob = JobHolder()

    init {
        observePrizePool()
        loadMachine()
        reloadPrizePool()
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
                            message = resourceReference(R.string.collectibles_main_search_load_error),
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
                            ),
                        )
                    },
                )
        }.saveIn(machineJob)
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

    @Suppress("EmptyFunctionBlock")
    private fun onTurboInfoClick() {
        // TODO([REDACTED_TASK_KEY]): the Turbo mode explainer sheet is not designed yet.
    }

    private fun onBuyClick() {
        // TODO([REDACTED_TASK_KEY])
    }

    private fun onBackClick() {
        router.pop()
    }
}