package com.tangem.features.hotwallet.accesscode

import androidx.compose.runtime.Stable
import arrow.core.getOrElse
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.domain.core.wallets.UserWalletsListRepository
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.features.hotwallet.accesscode.entity.AccessCodeUM
import com.tangem.hot.sdk.TangemHotSdk
import com.tangem.hot.sdk.model.HotAuth
import com.tangem.hot.sdk.model.UnlockHotWallet
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@Stable
@ModelScoped
internal class AccessCodeModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val getUserWalletUseCase: GetUserWalletUseCase,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val tangemHotSdk: TangemHotSdk,
) : Model() {

    private val params = paramsContainer.require<AccessCodeComponent.Params>()

    internal val uiState: StateFlow<AccessCodeUM>
    field = MutableStateFlow(getInitialState())

    private fun getInitialState() = AccessCodeUM(
        accessCode = "",
        onAccessCodeChange = ::onAccessCodeChange,
        isConfirmMode = params.isConfirmMode,
        buttonEnabled = false,
        buttonInProgress = false,
        onButtonClick = ::onButtonClick,
    )

    private fun onAccessCodeChange(value: String) {
        uiState.update {
            it.copy(
                accessCode = value,
                buttonEnabled = if (params.isConfirmMode) {
                    value == params.accessCodeToConfirm
                } else {
                    value.length == uiState.value.accessCodeLength
                },
            )
        }
    }

    private fun onButtonClick() {
        if (!params.isConfirmMode) {
            params.callbacks.onAccessCodeSet(params.userWalletId, uiState.value.accessCode)
        } else {
            params.accessCodeToConfirm?.let {
                setCode(params.userWalletId, it)
            }
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

                val unlockHotWallet = UnlockHotWallet(userWallet.hotWalletId, HotAuth.NoAuth)
                var updatedHotWalletId = tangemHotSdk.changeAuth(
                    unlockHotWallet = unlockHotWallet,
                    auth = HotAuth.Password(accessCode.toCharArray()),
                )

                updatedHotWalletId = tangemHotSdk.changeAuth(
                    unlockHotWallet = UnlockHotWallet(
                        walletId = updatedHotWalletId,
                        auth = HotAuth.Password(accessCode.toCharArray()),
                    ),
                    auth = HotAuth.Biometry,
                )

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

                userWalletsListRepository.setLock(
                    userWallet.walletId,
                    UserWalletsListRepository.LockMethod.Biometric,
                )

                params.callbacks.onAccessCodeConfirmed(params.userWalletId)
            }.onFailure {
                Timber.e(it)

                uiState.update {
                    it.copy(buttonInProgress = false)
                }
            }
        }
    }
}