package com.tangem.features.hotwallet.accesscode

import androidx.compose.runtime.Stable
import arrow.core.getOrElse
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.components.fields.PinTextColor
import com.tangem.core.ui.event.consumedEvent
import com.tangem.core.ui.event.triggeredEvent
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.EventMessageAction
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.hotwallet.IsAccessCodeSimpleUseCase
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.wallet.requireHotWallet
import com.tangem.domain.settings.CanUseBiometryUseCase
import com.tangem.domain.settings.SetAskBiometryShownUseCase
import com.tangem.domain.settings.ShouldShowAskBiometryUseCase
import com.tangem.domain.wallets.hot.HotWalletAccessor
import com.tangem.domain.wallets.repository.WalletsRepository
import com.tangem.domain.wallets.usecase.ClearHotWalletContextualUnlockUseCase
import com.tangem.domain.wallets.usecase.GetHotWalletContextualUnlockUseCase
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.features.hotwallet.accesscode.entity.AccessCodeUM
import com.tangem.features.hotwallet.impl.R
import com.tangem.hot.sdk.TangemHotSdk
import com.tangem.hot.sdk.model.HotAuth
import com.tangem.hot.sdk.model.HotWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import kotlin.coroutines.resume

private const val SUCCESS_DISPLAY_DURATION_MS = 400L
private const val ERROR_DISPLAY_DURATION_MS = 500L

@Suppress("LongParameterList")
@Stable
@ModelScoped
internal class AccessCodeModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val getUserWalletUseCase: GetUserWalletUseCase,
    private val getHotWalletContextualUnlockUseCase: GetHotWalletContextualUnlockUseCase,
    private val clearHotWalletContextualUnlockUseCase: ClearHotWalletContextualUnlockUseCase,
    private val hotWalletAccessor: HotWalletAccessor,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val walletsRepository: WalletsRepository,
    private val tangemHotSdk: TangemHotSdk,
    private val shouldShowAskBiometryUseCase: ShouldShowAskBiometryUseCase,
    private val setAskBiometryShownUseCase: SetAskBiometryShownUseCase,
    private val canUseBiometryUseCase: CanUseBiometryUseCase,
    private val isAccessCodeSimpleUseCase: IsAccessCodeSimpleUseCase,
    private val uiMessageSender: UiMessageSender,
) : Model() {

    private val params = paramsContainer.require<AccessCodeComponent.Params>()

    private val settingCodeJobHolder = JobHolder()

    internal val uiState: StateFlow<AccessCodeUM>
        field = MutableStateFlow(getInitialState())

    internal fun onResume() {
        uiState.update { currentState ->
            currentState.copy(
                onAccessCodeChange = ::onAccessCodeChange,
            )
        }
    }

    private fun getInitialState() = AccessCodeUM(
        accessCode = "",
        accessCodeColor = PinTextColor.Primary,
        onAccessCodeChange = ::onAccessCodeChange,
        isLoading = false,
        isConfirmMode = params.accessCodeToConfirm != null,
    )

    private fun onAccessCodeChange(value: String) {
        val isConfirmMode = uiState.value.isConfirmMode
        val accessCodeLength = uiState.value.accessCodeLength

        if (value.length > accessCodeLength) return

        uiState.update { currentState ->
            currentState.copy(
                accessCode = value,
                accessCodeColor = PinTextColor.Primary,
            )
        }

        if (value.length == accessCodeLength) {
            uiState.update { currentState ->
                currentState.copy(
                    onAccessCodeChange = {},
                )
            }

            if (isConfirmMode) {
                modelScope.launch {
                    if (value == params.accessCodeToConfirm) {
                        setCode(params.userWalletId, params.accessCodeToConfirm)
                    } else {
                        showErrorAndReset()
                    }
                }
            } else {
                onNewCodeSet()
            }
        }
    }

    private suspend fun setLoadingIfLongJob(job: Job) {
        delay(timeMillis = 2000)

        if (job.isActive) {
            uiState.update { currentState ->
                currentState.copy(
                    isLoading = true,
                )
            }
        }
    }

    private fun onNewCodeSet() {
        modelScope.launch {
            delay(timeMillis = SUCCESS_DISPLAY_DURATION_MS)

            if (isAccessCodeSimpleUseCase(uiState.value.accessCode)) {
                showSimpleAccessCodeDialog()
            } else {
                setNewCode()
            }
        }
    }

    private fun setNewCode() {
        params.callbacks.onNewAccessCodeInput(params.userWalletId, uiState.value.accessCode)

        uiState.update { currentState ->
            currentState.copy(
                accessCode = "",
            )
        }
    }

    private fun showSimpleAccessCodeDialog() {
        uiMessageSender.send(
            DialogMessage(
                title = resourceReference(R.string.access_code_alert_validation_title),
                message = resourceReference(R.string.access_code_alert_validation_description),
                firstAction = EventMessageAction(
                    title = resourceReference(R.string.access_code_alert_validation_cancel),
                    onClick = {
                        uiState.update { currentState ->
                            currentState.copy(
                                onAccessCodeChange = ::onAccessCodeChange,
                                requestFocus = triggeredEvent(Unit, ::consumeRequestFocusEvent),
                            )
                        }
                    },
                ),
                secondAction = EventMessageAction(
                    title = resourceReference(R.string.access_code_alert_validation_ok),
                    onClick = ::setNewCode,
                ),
                isDismissable = false,
            ),
        )
    }

    private fun consumeRequestFocusEvent() {
        uiState.update { currentState ->
            currentState.copy(requestFocus = consumedEvent())
        }
    }

    private suspend fun showErrorAndReset() {
        uiState.update { currentState ->
            currentState.copy(
                accessCodeColor = PinTextColor.WrongCode,
            )
        }
        delay(timeMillis = ERROR_DISPLAY_DURATION_MS)
        uiState.update { currentState ->
            currentState.copy(
                accessCode = "",
                accessCodeColor = PinTextColor.Primary,
                onAccessCodeChange = ::onAccessCodeChange,
            )
        }
    }

    private suspend fun setCode(userWalletId: UserWalletId, accessCode: String) = coroutineScope {
        if (settingCodeJobHolder.isActive) {
            return@coroutineScope
        }

        params.callbacks.onAccessCodeUpdateStarted(params.userWalletId)

        val userWallet = getUserWalletUseCase(userWalletId)
            .getOrElse { error("User wallet with id $userWalletId not found") }
            .requireHotWallet()

        tryToAskForBiometry()

        val settingCodeJob = launch(dispatchers.main) {
            setCodeOperation(userWallet, accessCode)
            params.callbacks.onAccessCodeUpdated(params.userWalletId)
        }.saveIn(settingCodeJobHolder)

        setLoadingIfLongJob(settingCodeJob)
    }

    /**
     * Set access code for hot wallet
     * !!! Be aware that order of operations is important here !!!
     */
    private suspend fun setCodeOperation(userWallet: UserWallet.Hot, accessCode: String) {
        val unlockHotWallet = getHotWalletContextualUnlockUseCase(userWallet.hotWalletId)
            .getOrNull()
            ?: run {
                require(userWallet.hotWalletId.authType == HotWalletId.AuthType.NoPassword) {
                    "Something went wrong. Hot wallet is locked and cannot be unlocked with NoAuth"
                }

                hotWalletAccessor.unlockContextual(userWallet.hotWalletId)
            }

        val newHotWalletIdWithPass = tangemHotSdk.changeAuth(
            unlockHotWallet = unlockHotWallet,
            auth = HotAuth.Password(accessCode.toCharArray()),
        )

        userWalletsListRepository.saveWithoutLock(
            userWallet.copy(hotWalletId = newHotWalletIdWithPass),
            canOverride = true,
        )

        userWalletsListRepository.setLock(
            userWalletId = userWallet.walletId,
            lockMethod = UserWalletsListRepository.LockMethod.AccessCode(accessCode.toCharArray()),
        )

        if (walletsRepository.requireAccessCode().not() && canUseBiometryUseCase.strict()) {
            val newHotWalletIdWithBiometry = tangemHotSdk.changeAuth(
                unlockHotWallet = unlockHotWallet,
                auth = HotAuth.Biometry,
            )

            userWalletsListRepository.saveWithoutLock(
                userWallet.copy(hotWalletId = newHotWalletIdWithBiometry),
                canOverride = true,
            )
        }

        if (walletsRepository.useBiometricAuthentication()) {
            userWalletsListRepository.setLock(
                userWalletId = userWallet.walletId,
                lockMethod = UserWalletsListRepository.LockMethod.Biometric,
            )
        }

        clearHotWalletContextualUnlockUseCase.invoke(params.userWalletId)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun tryToAskForBiometry() {
        if (!shouldAskForBiometry()) return

        suspendCancellableCoroutine { continuation ->
            var buttonClicked = false
            uiMessageSender.send(
                DialogMessage(
                    title = resourceReference(R.string.common_attention),
                    message = resourceReference(R.string.hot_access_code_set_biometric_ask),
                    firstAction = EventMessageAction(
                        title = resourceReference(R.string.common_allow),
                        onClick = {
                            buttonClicked = true
                            modelScope.launch {
                                setAskBiometryShownUseCase()
                                walletsRepository.setUseBiometricAuthentication(true)
                                walletsRepository.setRequireAccessCode(false)
                                if (continuation.isActive) {
                                    continuation.resume(Unit)
                                }
                            }
                        },
                    ),
                    secondAction = EventMessageAction(
                        title = resourceReference(R.string.save_user_wallet_agreement_dont_allow),
                        onClick = {
                            buttonClicked = true
                            modelScope.launch {
                                setAskBiometryShownUseCase()
                                if (continuation.isActive) {
                                    continuation.resume(Unit)
                                }
                            }
                        },
                    ),
                    onDismissRequest = {
                        if (continuation.isActive && buttonClicked.not()) {
                            continuation.resume(Unit)
                        }
                    },
                ),
            )
        }
    }

    private suspend fun shouldAskForBiometry(): Boolean {
        val canUseBiometry = canUseBiometryUseCase()
        val shouldShowAskBiometry = shouldShowAskBiometryUseCase()

        return canUseBiometry && shouldShowAskBiometry
    }
}