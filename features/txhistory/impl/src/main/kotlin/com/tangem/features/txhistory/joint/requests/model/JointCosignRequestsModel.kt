package com.tangem.features.txhistory.joint.requests.model

import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.features.txhistory.component.JointCosignRequestsComponent
import com.tangem.features.txhistory.entity.JointCosignRequestsUM
import com.tangem.features.txhistory.joint.requests.JointOperationSlotConfig
import com.tangem.features.txhistory.joint.requests.converter.JointCosignRequestConverter
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@ModelScoped
internal class JointCosignRequestsModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    delegate: JointCosignRequestsDelegate,
    getBalanceHidingSettingsUseCase: GetBalanceHidingSettingsUseCase,
) : Model() {

    private val params: JointCosignRequestsComponent.Params = paramsContainer.require()

    val operationNavigation = SlotNavigation<JointOperationSlotConfig>()

    private val converter = JointCosignRequestConverter(currency = params.currency, onClick = ::openOperation)

    val state: StateFlow<JointCosignRequestsUM> = combine(
        flow = delegate.observe(userWalletId = params.userWalletId, currency = params.currency),
        flow2 = getBalanceHidingSettingsUseCase().map { it.isBalanceHidden }.distinctUntilChanged(),
    ) { request, isBalanceHidden ->
        JointCosignRequestsUM(
            items = listOfNotNull(request).map(converter::convert).toImmutableList(),
            isBalanceHidden = isBalanceHidden,
        )
    }
        .flowOn(dispatchers.io)
        .stateIn(modelScope, SharingStarted.WhileSubscribed(), initialValue = JointCosignRequestsUM.Empty)

    private fun openOperation(operationId: String) {
        operationNavigation.activate(JointOperationSlotConfig(operationId = operationId))
    }
}