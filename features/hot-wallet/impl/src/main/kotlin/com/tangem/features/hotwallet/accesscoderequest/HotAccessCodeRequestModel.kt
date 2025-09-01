package com.tangem.features.hotwallet.accesscoderequest

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.ui.components.fields.PinTextColor
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.core.wallets.UserWalletsListRepository
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.wallets.hot.HotWalletAccessCodeAttemptsRepository
import com.tangem.domain.wallets.hot.HotWalletAccessCodeAttemptsRepository.Attempts
import com.tangem.domain.wallets.hot.HotWalletPasswordRequester
import com.tangem.features.hotwallet.accesscode.ACCESS_CODE_LENGTH
import com.tangem.features.hotwallet.accesscoderequest.entity.HotAccessCodeRequestUM
import com.tangem.features.hotwallet.impl.R
import com.tangem.hot.sdk.model.HotAuth
import com.tangem.hot.sdk.model.HotWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@ModelScoped
internal class HotAccessCodeRequestModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val hotAccessCodeAttemptsRepository: HotWalletAccessCodeAttemptsRepository,
    private val userWalletsListRepository: UserWalletsListRepository,
) : Model() {

    private val result = MutableStateFlow<HotWalletPasswordRequester.Result?>(null)
    private val currentRequest = MutableStateFlow<HotWalletPasswordRequester.AttemptRequest?>(null)
    private val attemptsRequestJobHolder = JobHolder()

    private val HotWalletPasswordRequester.AttemptRequest.attemptId
        get() = HotWalletAccessCodeAttemptsRepository.AttemptId(
            hotWalletId = hotWalletId,
            auth = authMode,
        )

    val uiState: StateFlow<HotAccessCodeRequestUM>
    field = MutableStateFlow(getInitialState())

    suspend fun show(attemptRequest: HotWalletPasswordRequester.AttemptRequest) {
        if (userWalletExists(attemptRequest.hotWalletId).not()) {
            Timber.e("User wallet with id ${attemptRequest.hotWalletId} does not exist")
            result.value = HotWalletPasswordRequester.Result.Dismiss
            return
        }

        currentRequest.value = attemptRequest
        result.value = null // Reset the result when showing the dialog
        subscribeToAttempts(id = attemptRequest.attemptId)
        uiState.update {
            it.copy(
                isShown = true,
                accessCode = "",
                useBiometricVisible = attemptRequest.hasBiometry,
                onAccessCodeChange = ::onAccessCodeChange,
            )
        }
    }

    suspend fun waitResult(): HotWalletPasswordRequester.Result {
        return result.filterNotNull().first().also { result.value = null }
    }

    fun dismiss() {
        result.value = HotWalletPasswordRequester.Result.Dismiss
        attemptsRequestJobHolder.cancel()
        dismissState()
    }

    suspend fun wrongAccessCode() {
        val currentRequest = currentRequest.value ?: return
        hotAccessCodeAttemptsRepository.incrementAttempts(currentRequest.attemptId)
        uiState.update {
            it.copy(
                accessCodeColor = PinTextColor.WrongCode,
                onAccessCodeChange = {},
            )
        }
        delay(timeMillis = 500) // Delay to show the wrong access code state
    }

    suspend fun successfulAuthentication() {
        val currentRequest = currentRequest.value ?: return
        hotAccessCodeAttemptsRepository.resetAttempts(currentRequest.hotWalletId)
        uiState.update {
            it.copy(
                accessCodeColor = PinTextColor.Success,
                onAccessCodeChange = {},
            )
        }
        delay(timeMillis = 200) // Delay to show the success state
    }

    private fun getInitialState() = HotAccessCodeRequestUM(
        onDismiss = ::dismiss,
        onAccessCodeChange = ::onAccessCodeChange,
        accessCode = "",
        useBiometricClick = {
            dismissState()
            result.value = HotWalletPasswordRequester.Result.UseBiometry
        },
    )

    private fun onAccessCodeChange(accessCode: String) {
        if (accessCode.length > ACCESS_CODE_LENGTH) return

        uiState.update {
            it.copy(
                accessCode = accessCode,
                accessCodeColor = PinTextColor.Primary,
            )
        }

        if (accessCode.length == ACCESS_CODE_LENGTH) {
            uiState.update {
                it.copy(onAccessCodeChange = {})
            }

            result.value = HotWalletPasswordRequester.Result.EnteredPassword(HotAuth.Password(accessCode.toCharArray()))
        }
    }

    private fun subscribeToAttempts(id: HotWalletAccessCodeAttemptsRepository.AttemptId) {
        fun remainingSecondsToText(remainingSeconds: Int): TextReference? {
            return if (remainingSeconds > 0) {
                resourceReference(
                    R.string.access_code_check_warining_wait,
                    wrappedList(remainingSeconds),
                )
            } else {
                null
            }
        }

        suspend fun collectAttempts(attempts: Attempts) {
            when (attempts) {
                is Attempts.FastForward -> {
                    /** ignore */
                }
                is Attempts.WithDelay -> {
                    uiState.update {
                        it.copy(
                            wrongAccessCodeText = remainingSecondsToText(attempts.remainingSeconds),
                            onAccessCodeChange = ::onAccessCodeChange.takeIf { attempts.remainingSeconds <= 0 }
                                ?: {},
                        )
                    }
                }
                is Attempts.BeforeDeletion -> {
                    uiState.update {
                        it.copy(
                            wrongAccessCodeText = remainingSecondsToText(attempts.remainingSeconds)
                                ?: resourceReference(
                                    R.string.access_code_check_warining_delete,
                                    wrappedList(attempts.remainingAttemptsCountBeforeDeletion),
                                ),
                            onAccessCodeChange = ::onAccessCodeChange.takeIf { attempts.remainingSeconds <= 0 }
                                ?: {},
                        )
                    }
                }
                Attempts.Deletion -> deleteUserWallet()
            }
        }

        modelScope.launch {
            hotAccessCodeAttemptsRepository.getAttempts(id)
                .collectLatest { attempts -> collectAttempts(attempts) }
        }.saveIn(attemptsRequestJobHolder)
    }

    private suspend fun userWalletExists(id: HotWalletId): Boolean {
        return userWalletsListRepository.userWalletsSync()
            .any { it is UserWallet.Hot && it.hotWalletId == id }
    }

    private suspend fun deleteUserWallet() {
        val currentRequest = currentRequest.value ?: return
        val userWallet = userWalletsListRepository.userWalletsSync()
            .firstOrNull { it is UserWallet.Hot && it.hotWalletId == currentRequest.hotWalletId } ?: return
        userWalletsListRepository.delete(listOf(userWallet.walletId))
        dismiss()
    }

    private fun dismissState() {
        uiState.update {
            it.copy(isShown = false)
        }
    }
}