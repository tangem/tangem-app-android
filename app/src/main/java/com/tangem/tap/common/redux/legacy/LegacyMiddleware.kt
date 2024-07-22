package com.tangem.tap.common.redux.legacy

import com.tangem.domain.redux.LegacyAction
import com.tangem.domain.tokens.utils.convertToAmount
import com.tangem.tap.common.extensions.inject
import com.tangem.tap.common.feedback.FeedbackEmail
import com.tangem.tap.common.feedback.RateCanBeBetterEmail
import com.tangem.tap.common.feedback.SendTransactionFailedEmail
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.proxy.redux.DaggerGraphState
import com.tangem.tap.scope
import com.tangem.tap.store
import kotlinx.coroutines.launch
import org.rekotlin.Middleware

internal object LegacyMiddleware {
    val legacyMiddleware: Middleware<AppState> = { _, _ ->
        { next ->
            { action ->
                when (action) {
                    is LegacyAction.SendEmailRateCanBeBetter -> {
                        store.state.globalState.feedbackManager?.sendEmail(RateCanBeBetterEmail())
                    }
                    is LegacyAction.SendEmailSupport -> {
                        store.state.globalState.feedbackManager?.sendEmail(FeedbackEmail())
                    }
                    is LegacyAction.StartOnboardingProcess -> {
                        store.dispatch(
                            GlobalAction.Onboarding.Start(action.scanResponse, canSkipBackup = action.canSkipBackup),
                        )
                    }
                    is LegacyAction.SendEmailTransactionFailed -> {
                        if (store.inject(DaggerGraphState::feedbackManagerFeatureToggles).isLocalLogsEnabled) {
                            store.state.globalState.feedbackManager?.sendEmail(
                                SendTransactionFailedEmail(action.errorMessage),
                            )
                        } else {
                            scope.launch {
                                store.inject(DaggerGraphState::walletManagersFacade)
                                    .getOrCreateWalletManager(
                                        userWalletId = action.userWalletId,
                                        network = action.cryptoCurrency.network,
                                    )?.let { walletManager ->
                                        store.state.globalState.feedbackManager?.infoHolder?.updateOnSendError(
                                            walletManager = walletManager,
                                            amountToSend = action.amount?.convertToAmount(action.cryptoCurrency),
                                            feeAmount = action.fee?.convertToAmount(action.cryptoCurrency),
                                            destinationAddress = action.destinationAddress,
                                        )
                                    }
                                store.state.globalState.feedbackManager?.sendEmail(
                                    SendTransactionFailedEmail(action.errorMessage),
                                )
                            }
                        }
                    }
                }
                next(action)
            }
        }
    }
}
