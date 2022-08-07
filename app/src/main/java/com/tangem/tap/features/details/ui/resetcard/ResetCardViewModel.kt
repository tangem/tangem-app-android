package com.tangem.tap.features.details.ui.resetcard

import com.tangem.tap.common.analytics.AnalyticsEvent
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.features.details.redux.CardSettingsState
import com.tangem.tap.features.details.redux.DetailsAction
import org.rekotlin.Store

class ResetCardViewModel(private val store: Store<AppState>) {

    fun updateState(state: CardSettingsState?): ResetCardScreenState {
        return ResetCardScreenState(
            accepted = state?.resetConfirmed ?: false,
            onAcceptWarningToggleClick = { store.dispatch(DetailsAction.ResetToFactory.Confirm(it)) },
            onResetButtonClick = {
                store.state.globalState.analyticsHandlers?.triggerEvent(event = AnalyticsEvent.FACTORY_RESET_TAPPED)
                store.dispatch(DetailsAction.ResetToFactory.Proceed)
                                 },
        )
    }
}
