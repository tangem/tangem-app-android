package com.tangem.tap.common.redux.legacy

import com.tangem.blockchain.common.AmountType
import com.tangem.domain.feedback.models.BlockchainErrorInfo
import com.tangem.domain.redux.LegacyAction
import com.tangem.domain.utils.convertToSdkAmount
import com.tangem.tap.common.extensions.inject
import com.tangem.tap.common.extensions.stripZeroPlainString
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
                        store.state.globalState.feedbackManager?.sendEmail(
                            feedbackData = RateCanBeBetterEmail(),
                            scanResponse = action.scanResponse,
                        )
                    }
                    is LegacyAction.SendEmailSupport -> {
                        store.state.globalState.feedbackManager?.sendEmail(
                            feedbackData = FeedbackEmail(),
                            scanResponse = action.scanResponse,
                        )
                    }
                    is LegacyAction.StartOnboardingProcess -> {
                        store.dispatch(
                            GlobalAction.Onboarding.Start(action.scanResponse, canSkipBackup = action.canSkipBackup),
                        )
                    }
                    is LegacyAction.SendEmailTransactionFailed -> {
                        if (store.inject(DaggerGraphState::feedbackManagerFeatureToggles).isLocalLogsEnabled) {

                            val amount = action.amount?.convertToSdkAmount(action.cryptoCurrency)
                            store.inject(DaggerGraphState::saveBlockchainErrorUseCase).invoke(
                                error = BlockchainErrorInfo(
                                    errorMessage = action.errorMessage,
                                    blockchainId = action.cryptoCurrency.network.id.value,
                                    derivationPath = action.cryptoCurrency.network.derivationPath.value,
                                    destinationAddress = action.destinationAddress.orEmpty(),
                                    tokenSymbol = if (amount?.type is AmountType.Token) {
                                        amount.currencySymbol
                                    } else {
                                        ""
                                    },
                                    amount = amount?.value?.stripZeroPlainString() ?: "unknown",
                                    fee = action.fee?.convertToSdkAmount(action.cryptoCurrency)
                                        ?.value?.stripZeroPlainString() ?: "unknown",
                                ),
                            )

                            store.state.globalState.feedbackManager?.sendEmail(
                                feedbackData = SendTransactionFailedEmail(action.errorMessage),
                                scanResponse = action.scanResponse,
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
                                            amountToSend = action.amount?.convertToSdkAmount(action.cryptoCurrency),
                                            feeAmount = action.fee?.convertToSdkAmount(action.cryptoCurrency),
                                            destinationAddress = action.destinationAddress,
                                        )
                                    }
                                store.state.globalState.feedbackManager?.sendEmail(
                                    feedbackData = SendTransactionFailedEmail(action.errorMessage),
                                    scanResponse = null,
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
