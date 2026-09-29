package com.tangem.features.collectibles.impl.confirmorder.model

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.res.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.collectibles.account.flow.CollectiblesAccountStatusSupplier
import com.tangem.domain.collectibles.models.CollectibleGachaMachine
import com.tangem.domain.collectibles.usecase.GetCollectibleGachaMachineUseCase
import com.tangem.features.collectibles.impl.confirmorder.CollectiblesConfirmOrderComponent
import com.tangem.features.collectibles.impl.confirmorder.model.converter.CollectiblesConfirmOrderConverter
import com.tangem.features.collectibles.impl.confirmorder.ui.state.CollectiblesConfirmOrderUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@ModelScoped
internal class CollectiblesConfirmOrderModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val getMachineUseCase: GetCollectibleGachaMachineUseCase,
    private val accountStatusSupplier: CollectiblesAccountStatusSupplier,
    private val router: Router,
    paramsContainer: ParamsContainer,
) : Model() {

    private val params = paramsContainer.require<CollectiblesConfirmOrderComponent.Params>()

    val uiState: StateFlow<CollectiblesConfirmOrderUM>
        field = MutableStateFlow<CollectiblesConfirmOrderUM>(
            CollectiblesConfirmOrderUM.Loading(onBackClick = ::onBackClick),
        )

    private val isConfirming = MutableStateFlow(false)

    private val orderConverter = CollectiblesConfirmOrderConverter(
        onBackClick = ::onBackClick,
        onApproveClick = ::onApproveClick,
    )

    init {
        loadMachine()
    }

    private fun loadMachine() {
        modelScope.launch {
            // Served from memory when opened from the catalogue
            getMachineUseCase(externalId = params.externalId, skipCache = false)
                .fold(
                    ifLeft = {
                        uiState.value = CollectiblesConfirmOrderUM.Error(
                            onBackClick = ::onBackClick,
                            message = resourceReference(R.string.collectibles_machine_load_error),
                            onReloadClick = ::loadMachine,
                        )
                    },
                    ifRight = { machine -> observeBalance(machine) },
                )
        }
    }

    // The catalogue below in the stack keeps this flow subscribed, so the balance is replayed without a request
    private suspend fun observeBalance(machine: CollectibleGachaMachine) {
        val vaultBalanceFlow = accountStatusSupplier(params.userWalletId)
            .mapNotNull { status -> status.balance }
            .distinctUntilChanged()

        combine(vaultBalanceFlow, isConfirming) { vaultBalance, isConfirming ->
            orderConverter.convert(
                CollectiblesConfirmOrderConverter.Input(
                    machine = machine,
                    vaultBalance = vaultBalance,
                    isConfirming = isConfirming,
                ),
            )
        }.collect { content -> uiState.value = content }
    }

    private fun onApproveClick() {
        isConfirming.value = true
        // TODO([REDACTED_TASK_KEY]): generateTx -> sign -> submitTx
    }

    private fun onBackClick() {
        router.pop()
    }
}