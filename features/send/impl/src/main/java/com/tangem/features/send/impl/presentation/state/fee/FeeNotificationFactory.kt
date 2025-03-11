package com.tangem.features.send.impl.presentation.state.fee

import com.tangem.common.ui.notifications.NotificationsFactory.addFeeUnreachableNotification
import com.tangem.features.send.impl.presentation.state.SendUiState
import com.tangem.features.send.impl.presentation.state.SendUiStateType
import com.tangem.features.send.impl.presentation.state.StateRouter
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
                addFeeUnreachableNotification(
                    feeError = (feeState.feeSelectorState as? FeeSelectorState.Error)?.error,
                    tokenName = state.cryptoCurrencyName,
                    onReload = clickIntents::feeReload,
                )
            }.toImmutableList()
        }
}