package com.tangem.features.txhistory.joint.requests.model

import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.jointaccount.model.JointOperation
import com.tangem.domain.jointaccount.polling.JointAccountPollingCoordinator
import com.tangem.domain.models.network.Network
import com.tangem.features.txhistory.component.JointCosignRequestsComponent
import com.tangem.features.txhistory.entity.JointCosignRequestsUM
import com.tangem.features.txhistory.joint.requests.JointOperationSlotConfig
import com.tangem.features.txhistory.joint.requests.converter.JointCosignRequestConverter
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@ModelScoped
internal class JointCosignRequestsModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    delegate: JointCosignRequestsDelegate,
    getBalanceHidingSettingsUseCase: GetBalanceHidingSettingsUseCase,
    private val pollingCoordinator: JointAccountPollingCoordinator,
) : Model() {

    private val params: JointCosignRequestsComponent.Params = paramsContainer.require()

    private val isJointCurrency = params.currency.network.derivationPath is Network.DerivationPath.Joint

    private val isResumed = MutableStateFlow(false)

    val operationNavigation = SlotNavigation<JointOperationSlotConfig>()

    private val converter = JointCosignRequestConverter(currency = params.currency, onClick = ::openOperation)

    val state: StateFlow<JointCosignRequestsUM> = combine(
        flow = delegate.observe(userWalletId = params.userWalletId, currency = params.currency),
        flow2 = getBalanceHidingSettingsUseCase().map { it.isBalanceHidden }.distinctUntilChanged(),
    ) { request, isBalanceHidden ->
        JointCosignRequestsUM(
            items = listOfNotNull(request).map(converter::convert).toImmutableList(),
            isBalanceHidden = isBalanceHidden,
            isSending = request?.operation?.status == JointOperation.Status.PENDING,
        )
    }
        .flowOn(dispatchers.io)
        .stateIn(modelScope, SharingStarted.WhileSubscribed(), initialValue = JointCosignRequestsUM.Empty)

    private var isPendingOperationConsumed = false

    init {
        subscribePolling()
    }

    fun onResume() {
        isResumed.value = true
    }

    fun onPause() {
        isResumed.value = false
    }

    /**
     * The backend won't silent-push a new cosign request, so while this screen is on top a 60-second poll keeps

     * while none was known yet would never be discovered. [pollingCoordinator] paces itself — this only decides
     * whether to collect, and only for a currency that actually belongs to a joint account.
     */
    private fun subscribePolling() {
        if (!isJointCurrency) return

        isResumed
            .flatMapLatest { resumed ->
                if (resumed) pollingCoordinator.poll(walletId = params.userWalletId) else emptyFlow()
            }
            .launchIn(modelScope)
    }

    /**
     * Opens the operation carried by [JointCosignRequestsComponent.Params.pendingOperationId] (e.g. a `joint_tx`
     * push notification), once. Must be called by the owning component only after its `childSlot` observing
     * [operationNavigation] is wired up — otherwise the activation has no subscriber and is silently dropped.
     */
    fun consumePendingOperation() {
        if (isPendingOperationConsumed) return
        isPendingOperationConsumed = true
        params.pendingOperationId?.let(::openOperation)
    }

    private fun openOperation(operationId: String) {
        operationNavigation.activate(JointOperationSlotConfig(operationId = operationId))
    }
}