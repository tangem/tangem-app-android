package com.tangem.tap.common.redux

import com.tangem.domain.models.scan.ScanResponse
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.preferencesStorage
import com.tangem.tap.proxy.redux.DaggerGraphState
import com.tangem.tap.store
import org.rekotlin.Middleware

class AccessCodeRequestPolicyMiddleware {
    val middleware: Middleware<AppState> = { _, _ ->
        { next ->
            { action ->
                if (action is GlobalAction.SaveScanResponse) {
                    updateAccessCodeRequestPolicy(action.scanResponse)
                }
                next(action)
            }
        }
    }

    private fun updateAccessCodeRequestPolicy(scanResponse: ScanResponse) {
        store.state.daggerGraphState.get(DaggerGraphState::cardSdkConfigRepository).setAccessCodeRequestPolicy(
            isBiometricsRequestPolicy = preferencesStorage.shouldSaveAccessCodes && scanResponse.card.isAccessCodeSet,
        )
    }
}