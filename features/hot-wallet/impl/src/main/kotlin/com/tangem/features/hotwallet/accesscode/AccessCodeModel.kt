package com.tangem.features.hotwallet.accesscode

import androidx.compose.runtime.Stable
import arrow.core.getOrElse
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.EventMessageAction
import com.tangem.domain.core.wallets.UserWalletsListRepository
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.settings.CanUseBiometryUseCase
import com.tangem.domain.settings.SetAskBiometryShownUseCase
import com.tangem.domain.settings.ShouldShowAskBiometryUseCase
import com.tangem.domain.wallets.repository.WalletsRepository
import com.tangem.domain.wallets.usecase.ClearHotWalletContextualUnlockUseCase
import com.tangem.domain.wallets.usecase.GetHotWalletContextualUnlockUseCase
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.features.hotwallet.accesscode.entity.AccessCodeUM
import com.tangem.features.hotwallet.impl.R
import com.tangem.hot.sdk.TangemHotSdk
import com.tangem.hot.sdk.model.HotAuth
import com.tangem.hot.sdk.model.UnlockHotWallet
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import javax.inject.Inject
import kotlin.coroutines.resume

@Suppress("LongParameterList")
@Stable
@ModelScoped
internal class AccessCodeModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val getUserWalletUseCase: GetUserWalletUseCase,
    private val getHotWalletContextualUnlockUseCase: GetHotWalletContextualUnlockUseCase,
    private val clearHotWalletContextualUnlockUseCase: ClearHotWalletContextualUnlockUseCase,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val walletsRepository: WalletsRepository,
    private val tangemHotSdk: TangemHotSdk,
    private val shouldShowAskBiometryUseCase: ShouldShowAskBiometryUseCase,
    private val setAskBiometryShownUseCase: SetAskBiometryShownUseCase,
    private val canUseBiometryUseCase: CanUseBiometryUseCase,
    private val uiMessageSender: UiMessageSender,
) : Model() {

    private val params = paramsContainer.require<AccessCodeComponent.Params>()

    internal val uiState: StateFlow<AccessCodeUM>
    field = MutableStateFlow(getInitialState())

    private fun getInitialState() = AccessCodeUM(
        accessCode = "",
        onAccessCodeChange = ::onAccessCodeChange,
        isConfirmMode = params.accessCodeToConfirm != null,
        buttonEnabled = false,
        buttonInProgress = false,
        onButtonClick = ::onButtonClick,
    )

    private fun onAccessCodeChange(value: String) {
        uiState.update {
            it.copy(
                accessCode = value,
                buttonEnabled = if (params.accessCodeToConfirm != null) {
                    value == params.accessCodeToConfirm
                } else {
                    value.length == uiState.value.accessCodeLength
                },
            )
        }
    }

    private fun onButtonClick() {
        if (params.accessCodeToConfirm == null) {
            params.callbacks.onNewAccessCodeInput(params.userWalletId, uiState.value.accessCode)
        } else {
            setCode(params.userWalletId, params.accessCodeToConfirm)
        }
    }

    private fun setCode(userWalletId: UserWalletId, accessCode: String) {
        modelScope.launch {
            uiState.update {
                it.copy(buttonInProgress = true)
            }

            runCatching {
                val userWallet = getUserWalletUseCase(userWalletId)
                    .getOrElse { error("User wallet with id $userWalletId not found") }
                if (userWallet !is UserWallet.Hot) return@launch

                val unlockHotWallet = getHotWalletContextualUnlockUseCase(userWallet.hotWalletId)
                    .getOrNull()
                    ?: UnlockHotWallet(userWallet.hotWalletId, HotAuth.NoAuth)
                var updatedHotWalletId = tangemHotSdk.changeAuth(
                    unlockHotWallet = unlockHotWallet,
                    auth = HotAuth.Password(accessCode.toCharArray()),
                )

                tryToAskForBiometry()

                if (walletsRepository.requireAccessCode().not()) {
                    updatedHotWalletId = tangemHotSdk.changeAuth(
                        unlockHotWallet = UnlockHotWallet(
                            walletId = updatedHotWalletId,
                            auth = HotAuth.Password(accessCode.toCharArray()),
                        ),
                        auth = HotAuth.Biometry,
                    )
                }

                userWalletsListRepository.saveWithoutLock(
                    userWallet.copy(
                        hotWalletId = updatedHotWalletId,
                        backedUp = true,
                    ),
                    canOverride = true,
                )

                userWalletsListRepository.setLock(
                    userWallet.walletId,
                    UserWalletsListRepository.LockMethod.AccessCode(accessCode.toCharArray()),
                )

                if (walletsRepository.useBiometricAuthentication()) {
                    userWalletsListRepository.setLock(
                        userWallet.walletId,
                        UserWalletsListRepository.LockMethod.Biometric,
                    )
                }

                params.callbacks.onAccessCodeUpdated(params.userWalletId)
            }.onFailure {
                Timber.e(it)

                uiState.update {
                    it.copy(buttonInProgress = false)
                }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun tryToAskForBiometry() {
        if (!shouldAskForBiometry()) return

        suspendCancellableCoroutine { continuation ->
            uiMessageSender.send(
                DialogMessage(
                    title = resourceReference(R.string.common_attention),
                    message = resourceReference(R.string.hot_access_code_set_biometric_ask),
                    firstAction = EventMessageAction(
                        title = resourceReference(R.string.common_allow),
                        onClick = {
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
                            modelScope.launch {
                                setAskBiometryShownUseCase()
                                if (continuation.isActive) {
                                    continuation.resume(Unit)
                                }
                            }
                        },
                    ),
                    onDismissRequest = {
                        modelScope.launch {
                            if (continuation.isActive) {
                                continuation.resume(Unit)
                            }
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

    override fun onDestroy() {
        clearHotWalletContextualUnlockUseCase.invoke(params.userWalletId)
        super.onDestroy()
    }
}