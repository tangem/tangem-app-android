package com.tangem.tap.features.home.redux

import com.tangem.domain.settings.usercountry.models.needApplyFCARestrictions
import com.tangem.tap.common.redux.AppState
import kotlinx.collections.immutable.toImmutableList
import org.rekotlin.Action

object HomeReducer {
    fun reduce(action: Action, state: AppState): HomeState = internalReduce(action, state)
}

private fun internalReduce(action: Action, appState: AppState): HomeState {
    if (action !is HomeAction) return appState.homeState

    return when (action) {
        is HomeAction.ScanInProgress -> {
            appState.homeState.copy(scanInProgress = action.scanInProgress)
        }
        is HomeAction.UserCountryLoaded -> {
            val stories = if (action.userCountry.needApplyFCARestrictions()) {
                getRestrictedStories()
            } else {
                Stories.entries
            }
            appState.homeState.copy(
                stories = stories.toImmutableList(),
            )
        }
        else -> appState.homeState
    }
}