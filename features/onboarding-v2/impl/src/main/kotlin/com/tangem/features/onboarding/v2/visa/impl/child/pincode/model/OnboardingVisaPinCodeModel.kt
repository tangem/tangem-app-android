package com.tangem.features.onboarding.v2.visa.impl.child.pincode.model

import androidx.compose.runtime.Stable
import com.tangem.common.extensions.toHexString
import com.tangem.core.decompose.di.ComponentScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.datasource.local.visa.VisaAuthTokenStorage
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.visa.SetVisaPinCodeUseCase
import com.tangem.domain.visa.model.VisaCardActivationStatus
import com.tangem.domain.visa.model.VisaCardId
import com.tangem.domain.wallets.builder.UserWalletBuilder
import com.tangem.domain.wallets.legacy.UserWalletsListManager
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.usecase.GenerateWalletNameUseCase
import com.tangem.features.onboarding.v2.visa.impl.child.pincode.OnboardingVisaPinCodeComponent
import com.tangem.features.onboarding.v2.visa.impl.child.pincode.ui.state.OnboardingVisaPinCodeUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@Stable
@ComponentScoped
@Suppress("LongParameterList")
internal class OnboardingVisaPinCodeModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val generateWalletNameUseCase: GenerateWalletNameUseCase,
    private val userWalletsListManager: UserWalletsListManager,
    private val authTokenStorage: VisaAuthTokenStorage,
    private val otpStorage: VisaAuthTokenStorage,
    private val setVisaPinCodeUseCase: SetVisaPinCodeUseCase,
) : Model() {

    private val params = paramsContainer.require<OnboardingVisaPinCodeComponent.Config>()
    private val visaCardId = VisaCardId(
        cardId = params.scanResponse.card.cardId,
        cardPublicKey = params.scanResponse.card.cardPublicKey.toHexString(),
    )
    private val _uiState = MutableStateFlow(getInitialState())

    val uiState = _uiState.asStateFlow()
    val onDone = MutableSharedFlow<Unit>()

    private fun getInitialState(): OnboardingVisaPinCodeUM {
        return OnboardingVisaPinCodeUM(
            submitButtonEnabled = false,
            onPinCodeChange = ::onPinCodeChange,
            onSubmitClick = ::onSubmitClick,
        )
    }

    private fun onPinCodeChange(pin: String) {
        if (pin.all { it.isDigit() }) {
            _uiState.update {
                it.copy(
                    pinCode = pin,
                    submitButtonEnabled = checkPinCode(pin),
                )
            }
        }
    }

    private fun onSubmitClick() {
        if (checkPinCode(_uiState.value.pinCode).not()) return

        modelScope.launch {
            loading(true)

            setVisaPinCodeUseCase(
                pinCode = _uiState.value.pinCode,
                visaCardId = visaCardId,
                activationOrderId = params.activationOrderId,
            ).onLeft {
                loading(false)
                return@launch
            }

            saveWallet()

            loading(false)
        }
    }

    private fun checkPinCode(pin: String): Boolean {
        return pin.length == PIN_CODE_LENGTH
    }

    private fun loading(state: Boolean) {
        _uiState.update { it.copy(submitButtonLoading = state) }
    }

    private suspend fun saveWallet() {
        val userWallet = createUserWallet(params.scanResponse)
        userWalletsListManager.save(userWallet)
        authTokenStorage.remove(params.scanResponse.card.cardId)
        otpStorage.remove(params.scanResponse.card.cardId)
        onDone.emit(Unit)
    }

    private suspend fun createUserWallet(scanResponse: ScanResponse): UserWallet = withContext(dispatchers.io) {
        val newActivationStatus = VisaCardActivationStatus.Activated(
            visaAuthTokens = authTokenStorage.get(scanResponse.card.cardId)
                ?: error("Impossible state. Wrong feature implementation"),
        )

        requireNotNull(
            value = UserWalletBuilder(
                scanResponse.copy(visaCardActivationStatus = newActivationStatus),
                generateWalletNameUseCase,
            ).build(),
            lazyMessage = { "User wallet not created" },
        )
    }

    private companion object {
        const val PIN_CODE_LENGTH = 4
    }
}