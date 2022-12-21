package com.tangem.tap.common.redux

import com.tangem.domain.common.ScanResponse
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.preferencesStorage
import com.tangem.tap.tangemSdkManager
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
        tangemSdkManager.setAccessCodeRequestPolicy(
            useBiometricsForAccessCode = preferencesStorage.shouldSaveAccessCodes &&
                scanResponse.card.isAccessCodeSet,
        )
    }
}
