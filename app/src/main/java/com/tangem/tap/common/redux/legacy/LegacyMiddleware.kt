package com.tangem.tap.common.redux.legacy

import com.tangem.blockchain.common.AmountType
import com.tangem.domain.feedback.models.BlockchainErrorInfo
import com.tangem.domain.feedback.models.CardInfo
import com.tangem.domain.feedback.models.FeedbackEmailType
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.redux.LegacyAction
import com.tangem.domain.utils.convertToSdkAmount
import com.tangem.tap.common.extensions.dispatchWithMain
import com.tangem.tap.common.extensions.inject
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.features.details.redux.DetailsAction
import com.tangem.tap.proxy.redux.DaggerGraphState
import com.tangem.tap.scope
import com.tangem.tap.store
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import com.tangem.utils.extensions.stripZeroPlainString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.rekotlin.Middleware

internal object LegacyMiddleware {
    private val prepareDetailsScreenJobHolder = JobHolder()

    val legacyMiddleware: Middleware<AppState> = { _, _ ->
        { next ->
            { action ->
                when (action) {
                    is LegacyAction.StartOnboardingProcess -> {
                        store.dispatch(
                            GlobalAction.Onboarding.Start(
                                scanResponse = action.scanResponse,
                                canSkipBackup = action.canSkipBackup,
                            ),
                        )
                    }
                    is LegacyAction.SendEmailRateCanBeBetter -> {
                        store.state.globalState.feedbackManager?.sendEmail(
                            type = FeedbackEmailType.RateCanBeBetter(cardInfo = getCardInfo(action.scanResponse)),
                        )
                    }
                    is LegacyAction.SendEmailSupport -> {
                        store.state.globalState.feedbackManager?.sendEmail(
                            type = FeedbackEmailType.DirectUserRequest(cardInfo = getCardInfo(action.scanResponse)),
                        )
                    }
                    is LegacyAction.SendEmailTransactionFailed -> {
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
                            type = FeedbackEmailType.TransactionSendingProblem(
                                cardInfo = getCardInfo(action.scanResponse),
                            ),
                        )
                    }
                    is LegacyAction.PrepareDetailsScreen -> {
                        val userWalletsListManager = store.inject(DaggerGraphState::generalUserWalletsListManager)
                        val walletsRepository = store.inject(DaggerGraphState::walletsRepository)

                        userWalletsListManager.selectedUserWallet
                            .distinctUntilChanged()
                            .onEach { selectedUserWallet ->
                                store.dispatchWithMain(
                                    DetailsAction.PrepareScreen(
                                        scanResponse = selectedUserWallet.scanResponse,
                                        shouldSaveUserWallets = walletsRepository.shouldSaveUserWalletsSync(),
                                    ),
                                )
                            }
                            .flowOn(Dispatchers.IO)
                            .launchIn(scope)
                            .saveIn(prepareDetailsScreenJobHolder)
                    }
                }
                next(action)
            }
        }
    }

    private fun getCardInfo(scanResponse: ScanResponse): CardInfo {
        return store.inject(DaggerGraphState::getCardInfoUseCase).invoke(scanResponse)
            .getOrNull()
            ?: error("Card info not found")
    }
}
