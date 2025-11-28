package com.tangem.features.onboarding.v2.util.impl.model

import com.tangem.common.CompletionResult
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.EventMessageAction
import com.tangem.domain.card.common.util.getBackupCardsCount
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.onboarding.v2.impl.R
import com.tangem.features.onboarding.v2.util.ResetCardsComponent
import com.tangem.sdk.api.TangemSdkManager
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume

@ModelScoped
internal class ResetCardsModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val tangemSdkManager: TangemSdkManager,
    private val uiMessageSender: UiMessageSender,
) : Model() {

    private val params = paramsContainer.require<ResetCardsComponent.Params>()
    private val callbacks = params.callbacks
    private val jobHolder = JobHolder()

    fun startResetCardsFlow(wallet: UserWallet.Cold) {
        modelScope.launch {
            startFullResetFlow(wallet)
        }.saveIn(jobHolder)
    }

    private suspend fun startFullResetFlow(createdUserWallet: UserWallet.Cold) {
        suspendCancellableCoroutine { continuation ->
            uiMessageSender.send(
                DialogMessage.invoke(
                    title = resourceReference(R.string.reset_cards_dialog_first_title),
                    isDismissable = false,
                    message = resourceReference(R.string.reset_cards_dialog_first_description),
                    firstActionBuilder = {
                        cancelAction {
                            callbacks.onCancel()
                            onDismissRequest()
                            continuation.resume(Unit)
                        }
                    },
                    secondActionBuilder = {
                        EventMessageAction(
                            title = resourceReference(R.string.card_settings_action_sheet_reset),
                            isWarning = true,
                            onClick = {
                                modelScope.launch {
                                    repeatUntilTrue { resetPrimaryCard(createdUserWallet) }
                                    continuation.resume(Unit)
                                    onDismissRequest()
                                    startResetBackupCardsFlow(createdUserWallet)
                                }
                            },
                        )
                    },
                    onDismissRequest = {},
                ),
            )
        }
    }

    private suspend fun startResetBackupCardsFlow(createdUserWallet: UserWallet.Cold) {
        val backupCardsCount = createdUserWallet.scanResponse.getBackupCardsCount() ?: 0

        if (backupCardsCount == 0) {
            completeResetFlow()
            return
        }

        repeat(backupCardsCount) { index ->
            suspendCancellableCoroutine { continuation ->
                uiMessageSender.send(
                    DialogMessage.invoke(
                        title = resourceReference(R.string.card_settings_continue_reset_alert_title),
                        isDismissable = false,
                        message = resourceReference(R.string.reset_cards_dialog_next_device_description),
                        firstActionBuilder = {
                            cancelAction {
                                callbacks.onCancel()
                                onDismissRequest()
                                continuation.resume(Unit)
                            }
                        },
                        secondActionBuilder = {
                            EventMessageAction(
                                title = resourceReference(R.string.card_settings_action_sheet_reset),
                                isWarning = true,
                                onClick = {
                                    modelScope.launch {
                                        repeatUntilTrue { resetBackupCard(index + 1, createdUserWallet.walletId) }
                                        onDismissRequest()
                                        if (index == backupCardsCount - 1) {
                                            completeResetFlow()
                                        }
                                        continuation.resume(Unit)
                                    }
                                },
                            )
                        },
                        onDismissRequest = {},
                    ),
                )
            }
        }
    }

    private suspend fun completeResetFlow() {
        suspendCancellableCoroutine { continuation ->
            uiMessageSender.send(
                DialogMessage.invoke(
                    title = resourceReference(R.string.card_settings_completed_reset_alert_title),
                    isDismissable = false,
                    message = resourceReference(R.string.reset_cards_dialog_complete_description),
                    firstActionBuilder = {
                        EventMessageAction(
                            title = resourceReference(R.string.common_done),
                            onClick = {
                                modelScope.launch {
                                    onDismissRequest()
                                    callbacks.onComplete()
                                    continuation.resume(Unit)
                                }
                            },
                        )
                    },
                    onDismissRequest = {},
                ),
            )
        }
    }

    private suspend fun resetPrimaryCard(createdUserWallet: UserWallet.Cold): Boolean {
        val scanResponse = createdUserWallet.scanResponse

        val result = tangemSdkManager.resetToFactorySettings(
            cardId = scanResponse.card.cardId,
            allowsRequestAccessCodeFromRepository = true,
        )

        return when (result) {
            is CompletionResult.Failure -> false
            is CompletionResult.Success -> true
        }
    }

    private suspend fun resetBackupCard(cardIndex: Int, userWalletId: UserWalletId): Boolean {
        val result = tangemSdkManager.resetBackupCard(cardIndex, userWalletId)
        return when (result) {
            is CompletionResult.Failure -> false
            is CompletionResult.Success -> true
        }
    }

    private inline fun repeatUntilTrue(action: () -> Boolean) {
        while (true) {
            if (action()) break
        }
    }
}