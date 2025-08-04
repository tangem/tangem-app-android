package com.tangem.features.hotwallet.accesscoderequest

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.domain.wallets.hot.HotWalletPasswordRequester
import com.tangem.features.hotwallet.setaccesscode.ACCESS_CODE_LENGTH
import com.tangem.features.hotwallet.accesscoderequest.entity.HotAccessCodeRequestUM
import com.tangem.hot.sdk.model.HotAuth
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@ModelScoped
internal class HotAccessCodeRequestModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
) : Model() {

    private val result = MutableStateFlow<HotWalletPasswordRequester.Result?>(null)

    val uiState: StateFlow<HotAccessCodeRequestUM>
    field = MutableStateFlow(getInitialState())

    fun dismiss() {
        result.value = HotWalletPasswordRequester.Result.Dismiss
        dismissState()
    }

    fun show(hasBiometry: Boolean) {
        result.value = null // Reset the result when showing the dialog
        uiState.update {
            it.copy(
                isShown = true,
                accessCode = "",
                useBiometricVisible = hasBiometry,
                onAccessCodeChange = ::onAccessCodeChange,
            )
        }
    }

    suspend fun waitResult(): HotWalletPasswordRequester.Result {
        return result.filterNotNull().first().also { result.value = null }
    }

    suspend fun wrongAccessCode() {
        uiState.update {
            it.copy(
                wrongAccessCode = true,
                onAccessCodeChange = {},
            )
        }
        delay(timeMillis = 500) // Delay to show the wrong access code state
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
            it.copy(accessCode = accessCode, wrongAccessCode = false)
        }

        if (accessCode.length == ACCESS_CODE_LENGTH) {
            uiState.update {
                it.copy(onAccessCodeChange = {})
            }

            result.value = HotWalletPasswordRequester.Result.EnteredPassword(HotAuth.Password(accessCode.toCharArray()))
        }
    }

    private fun dismissState() {
        uiState.update {
            it.copy(isShown = false)
        }
    }
}