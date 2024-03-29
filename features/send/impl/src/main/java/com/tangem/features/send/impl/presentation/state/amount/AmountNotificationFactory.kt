package com.tangem.features.send.impl.presentation.state.amount

import com.tangem.features.send.impl.presentation.state.SendUiState
import com.tangem.features.send.impl.presentation.state.SendUiStateType
import com.tangem.features.send.impl.presentation.state.StateRouter
import com.tangem.features.send.impl.presentation.state.SendNotification
import com.tangem.features.send.impl.presentation.state.fee.FeeSelectorState
import com.tangem.features.send.impl.presentation.viewmodel.SendClickIntents
import com.tangem.utils.Provider
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

internal class AmountNotificationFactory(
    private val stateRouterProvider: Provider<StateRouter>,
    private val currentStateProvider: Provider<SendUiState>,
    private val clickIntents: SendClickIntents,
) {

    fun create() = stateRouterProvider().currentState
        .filter { it.type == SendUiStateType.Amount }
        .map {
            buildList {
                addFeeUnreachableNotification()
            }.toImmutableList()
        }

    private fun MutableList<SendNotification>.addFeeUnreachableNotification() {
        val state = currentStateProvider()
        val feeState = state.feeState ?: return

        if (feeState.feeSelectorState is FeeSelectorState.Error) {
            add(
                SendNotification.Warning.NetworkFeeUnreachable(clickIntents::feeReload),
            )
        }
    }
}
