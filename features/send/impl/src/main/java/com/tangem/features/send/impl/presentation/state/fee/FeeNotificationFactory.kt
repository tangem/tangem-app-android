package com.tangem.features.send.impl.presentation.state.fee

import com.tangem.features.send.impl.presentation.state.*
import com.tangem.features.send.impl.presentation.viewmodel.SendClickIntents
import com.tangem.utils.Provider
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

@Suppress("LongParameterList")
internal class FeeNotificationFactory(
    private val currentStateProvider: Provider<SendUiState>,
    private val stateRouterProvider: Provider<StateRouter>,
    private val clickIntents: SendClickIntents,
) {

    fun create() = stateRouterProvider().currentState
        .filter { it.type == SendUiStateType.Fee || it.type == SendUiStateType.EditFee }
        .map {
            val state = currentStateProvider()
            val feeState = state.getFeeState(stateRouterProvider().isEditState) ?: return@map persistentListOf()
            buildList {
                addFeeUnreachableNotification(feeState.feeSelectorState)
            }.toImmutableList()
        }

    private fun MutableList<SendNotification>.addFeeUnreachableNotification(feeSelectorState: FeeSelectorState) {
        if (feeSelectorState is FeeSelectorState.Error) {
            add(SendNotification.Warning.NetworkFeeUnreachable(clickIntents::feeReload))
        }
    }
}