package com.tangem.tap.common.redux

import com.tangem.domain.models.scan.ScanResponse
import com.tangem.tap.common.extensions.inject
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.mainScope
import com.tangem.tap.proxy.redux.DaggerGraphState
import com.tangem.tap.store
import kotlinx.coroutines.launch
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
        mainScope.launch {
            val shouldSaveAccessCodes = store.inject(DaggerGraphState::settingsRepository).shouldSaveAccessCodes()

            store.inject(DaggerGraphState::cardSdkConfigRepository).setAccessCodeRequestPolicy(
                isBiometricsRequestPolicy = shouldSaveAccessCodes && scanResponse.card.isAccessCodeSet,
            )
        }
    }
}