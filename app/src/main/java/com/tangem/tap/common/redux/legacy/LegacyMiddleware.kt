package com.tangem.tap.common.redux.legacy

import com.tangem.domain.redux.LegacyAction
import com.tangem.tap.common.feedback.RateCanBeBetterEmail
import com.tangem.tap.common.feedback.SendTransactionFailedEmail
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.store
import org.rekotlin.Middleware

internal object LegacyMiddleware {
    val legacyMiddleware: Middleware<AppState> = { _, _ ->
        { next ->
            { action ->
                when (action) {
                    is LegacyAction.SendEmailRateCanBeBetter -> {
                        store.state.globalState.feedbackManager?.sendEmail(RateCanBeBetterEmail())
                    }
                    is LegacyAction.StartOnboardingProcess -> {
                        store.dispatch(
                            GlobalAction.Onboarding.Start(action.scanResponse, canSkipBackup = action.canSkipBackup),
                        )
                    }
                    is LegacyAction.SendEmailTransactionFailed -> {
                        store.state.globalState.feedbackManager?.sendEmail(
                            SendTransactionFailedEmail(action.errorMessage),
                        )
                    }
                }
                next(action)
            }
        }
    }
}