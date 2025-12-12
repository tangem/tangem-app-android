package com.tangem.features.onboarding.v2.util.impl.model

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.EventMessageAction
import com.tangem.domain.card.ResetCardUseCase
import com.tangem.domain.card.ResetCardUserCodeParams
import com.tangem.domain.card.common.util.getBackupCardsCount
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.onboarding.v2.impl.R
import com.tangem.features.onboarding.v2.util.ResetCardsComponent
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
    private val resetCardUseCase: ResetCardUseCase,
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
        val userCodeParams = ResetCardUserCodeParams(
            isAccessCodeSet = createdUserWallet.scanResponse.card.isAccessCodeSet,
            isPasscodeSet = createdUserWallet.scanResponse.card.isPasscodeSet,
        )

        suspendCancellableCoroutine { continuation ->
            uiMessageSender.send(
                DialogMessage.invoke(
                    title = resourceReference(R.string.reset_cards_dialog_first_title),
                    isDismissable = true,
                    message = resourceReference(R.string.reset_cards_dialog_first_description),
                    firstActionBuilder = {
                        cancelAction(isWarning = true) {
                            callbacks.onCancel()
                            onDismissRequest()
                            continuation.resume(Unit)
                        }
                    },
                    secondActionBuilder = {
                        EventMessageAction(
                            title = resourceReference(R.string.card_settings_action_sheet_reset),
                            isWarning = false,
                            onClick = {
                                modelScope.launch {
                                    onDismissRequest()
                                    resetOrCancel { resetPrimaryCard(createdUserWallet, userCodeParams) }
                                    continuation.resume(Unit)
                                    startResetBackupCardsFlow(createdUserWallet, userCodeParams)
                                }
                            },
                        )
                    },
                    onDismissRequest = {},
                ),
            )
        }
    }

    private suspend fun startResetBackupCardsFlow(
        createdUserWallet: UserWallet.Cold,
        userCodeParams: ResetCardUserCodeParams,
    ) {
        val backupCardsCount = createdUserWallet.scanResponse.getBackupCardsCount() ?: 0

        if (backupCardsCount == 0) {
            completeResetFlow()
            return
        }

        var canceled = false

        repeat(backupCardsCount) { index ->
            suspendCancellableCoroutine { continuation ->
                uiMessageSender.send(
                    DialogMessage.invoke(
                        title = resourceReference(R.string.card_settings_continue_reset_alert_title),
                        isDismissable = false,
                        message = resourceReference(R.string.reset_cards_dialog_next_device_description),
                        firstActionBuilder = {
                            cancelAction(isWarning = true) {
                                onDismissRequest()
                                recommendResetAgain(
                                    onCancel = {
                                        canceled = true
                                        callbacks.onCancel()
                                        continuation.resume(Unit)
                                    },
                                    onReset = onReset@{
                                        this@onReset.onDismissRequest()

                                        modelScope.launch {
                                            resetOrCancel {
                                                resetBackupCard(
                                                    cardNumber = index + 2,
                                                    params = userCodeParams,
                                                    userWalletId = createdUserWallet.walletId,
                                                )
                                            }
                                            continuation.resume(Unit)
                                        }
                                    },
                                )
                            }
                        },
                        secondActionBuilder = {
                            EventMessageAction(
                                title = resourceReference(R.string.card_settings_action_sheet_reset),
                                onClick = {
                                    onDismissRequest()

                                    modelScope.launch {
                                        resetOrCancel {
                                            resetBackupCard(
                                                cardNumber = index + 2,
                                                params = userCodeParams,
                                                userWalletId = createdUserWallet.walletId,
                                            )
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

            if (canceled) return
        }

        completeResetFlow()
    }

    private fun recommendResetAgain(onCancel: () -> Unit, onReset: EventMessageAction.BuilderScope.() -> Unit) {
        uiMessageSender.send(
            DialogMessage.invoke(
                title = resourceReference(R.string.card_reset_alert_incomplete_title),
                isDismissable = false,
                message = resourceReference(R.string.card_reset_alert_incomplete_message),
                firstActionBuilder = {
                    cancelAction(isWarning = true) {
                        onCancel()
                        onDismissRequest()
                    }
                },
                secondActionBuilder = {
                    EventMessageAction(
                        title = resourceReference(R.string.card_settings_action_sheet_reset),
                        onClick = {
                            onReset()
                        },
                    )
                },
            ),
        )
    }

    private suspend fun completeResetFlow() {
        suspendCancellableCoroutine { continuation ->
            uiMessageSender.send(
                DialogMessage.invoke(
                    title = resourceReference(R.string.card_settings_completed_reset_alert_title),
                    isDismissable = false,
                    message = when (params.source) {
                        ResetCardsComponent.Params.Source.Upgrade ->
                            resourceReference(R.string.card_reset_alert_finish_message)
                        ResetCardsComponent.Params.Source.Onboarding ->
                            resourceReference(R.string.card_settings_completed_reset_alert_message)
                    },
                    firstActionBuilder = {
                        EventMessageAction(
                            title = when (params.source) {
                                ResetCardsComponent.Params.Source.Upgrade ->
                                    resourceReference(R.string.card_reset_alert_finish_ok_button)
                                ResetCardsComponent.Params.Source.Onboarding ->
                                    resourceReference(R.string.common_done)
                            },
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

    private suspend fun resetOrCancel(action: suspend () -> Boolean) {
        while (true) {
            if (action()) {
                return
            }

            suspendCancellableCoroutine { continuation ->
                recommendResetAgain(
                    onCancel = {
                        callbacks.onCancel()
                        continuation.cancel()
                    },
                    onReset = {
                        continuation.resume(Unit)
                    },
                )
            }
        }
    }

    private suspend fun resetPrimaryCard(
        createdUserWallet: UserWallet.Cold,
        params: ResetCardUserCodeParams,
    ): Boolean {
        val result = resetCardUseCase.invoke(
            cardId = createdUserWallet.scanResponse.card.cardId,
            params = params,
        )

        return result.isRight()
    }

    private suspend fun resetBackupCard(
        cardNumber: Int,
        params: ResetCardUserCodeParams,
        userWalletId: UserWalletId,
    ): Boolean {
        return resetCardUseCase.invoke(
            cardNumber = cardNumber,
            params = params,
            userWalletId = userWalletId,
        ).getOrNull() == true
    }
}