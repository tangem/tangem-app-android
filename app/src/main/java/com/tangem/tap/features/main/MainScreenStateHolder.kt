package com.tangem.tap.features.main

import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.event.consumedEvent
import com.tangem.core.ui.event.triggeredEvent
import com.tangem.tap.features.main.model.MainScreenState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

internal class MainScreenStateHolder(private val intents: MainIntents) {

    private val notificationsFactory = NotificationsFactory(intents)

    private val stateFlowInternal: MutableStateFlow<MainScreenState> = MutableStateFlow(getInitialState())

    val stateFlow: StateFlow<MainScreenState> = stateFlowInternal

    fun updateWithHiddenBalancesToast(isBalanceHidden: Boolean) {
        stateFlowInternal.update { state ->
            state.copy(
                toast = triggeredEvent(
                    data = if (isBalanceHidden) {
                        notificationsFactory.createBalancesAreHiddenToast()
                    } else {
                        notificationsFactory.createBalancesAreShownToast()
                    },
                    onConsume = ::consumeToastEvent,
                ),
            )
        }
    }

    fun updateWithHiddenBalancesNotification() {
        stateFlowInternal.update { state ->
            state.copy(
                modalNotification = TangemBottomSheetConfig(
                    isShown = true,
                    onDismissRequest = intents::onDismissBottomSheet,
                    content = notificationsFactory.createBalancesAreHiddenModalNotification(),
                ),
            )
        }
    }

    fun updateWithoutModalNotification() {
        stateFlowInternal.update { state ->
            state.copy(modalNotification = state.modalNotification?.copy(isShown = false))
        }
    }

    private fun consumeToastEvent() {
        stateFlowInternal.update { state ->
            state.copy(toast = consumedEvent())
        }
    }

    private fun getInitialState() = MainScreenState(
        toast = consumedEvent(),
        modalNotification = null,
    )
}