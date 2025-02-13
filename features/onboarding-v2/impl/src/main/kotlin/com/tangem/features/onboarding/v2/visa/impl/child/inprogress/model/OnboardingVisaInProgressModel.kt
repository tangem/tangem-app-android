package com.tangem.features.onboarding.v2.visa.impl.child.inprogress.model

import androidx.compose.runtime.Stable
import com.tangem.common.extensions.toHexString
import com.tangem.core.decompose.di.ComponentScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.datasource.local.visa.VisaAuthTokenStorage
import com.tangem.datasource.local.visa.VisaOTPStorage
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.visa.model.*
import com.tangem.domain.visa.repository.VisaActivationRepository
import com.tangem.domain.visa.repository.VisaAuthRepository
import com.tangem.domain.wallets.builder.UserWalletBuilder
import com.tangem.domain.wallets.legacy.UserWalletsListManager
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.usecase.GenerateWalletNameUseCase
import com.tangem.features.onboarding.v2.visa.impl.child.inprogress.OnboardingVisaInProgressComponent.Config
import com.tangem.features.onboarding.v2.visa.impl.child.inprogress.OnboardingVisaInProgressComponent.Params
import com.tangem.features.onboarding.v2.visa.impl.route.OnboardingVisaRoute
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@Suppress("LongParameterList")
@Stable
@ComponentScoped
internal class OnboardingVisaInProgressModel @Inject constructor(
    paramsContainer: ParamsContainer,
    visaActivationRepositoryFactory: VisaActivationRepository.Factory,
    override val dispatchers: CoroutineDispatcherProvider,
    private val visaAuthRepository: VisaAuthRepository,
    private val visaAuthTokenStorage: VisaAuthTokenStorage,
    private val otpStorage: VisaOTPStorage,
    private val generateWalletNameUseCase: GenerateWalletNameUseCase,
    private val userWalletsListManager: UserWalletsListManager,
) : Model() {

    private val params = paramsContainer.require<Config>()
    private val visaActivationRepository = visaActivationRepositoryFactory.create(
        VisaCardId(
            cardId = params.scanResponse.card.cardId,
            cardPublicKey = params.scanResponse.card.cardPublicKey.toHexString(),
        ),
    )
    val onDone = MutableSharedFlow<Params.DoneEvent>()

    init {
        modelScope.launch {
            while (true) {
                val result = runCatching {
                    visaActivationRepository.getActivationRemoteStateLongPoll()
                }.getOrNull() ?: continue

                when (result) {
                    is VisaActivationRemoteState.CardWalletSignatureRequired,
                    VisaActivationRemoteState.BlockedForActivation,
                    -> {
                        // TODO show alert inconsistent state
                        return@launch
                    }
                    is VisaActivationRemoteState.CustomerWalletSignatureRequired,
                    VisaActivationRemoteState.PaymentAccountDeploying,
                    VisaActivationRemoteState.WaitingForActivationFinishing,
                    -> {
                    }
                    is VisaActivationRemoteState.WaitingPinCode -> {
                        navigateToPinCode(result.activationOrderInfo)
                        return@launch
                    }
                    VisaActivationRemoteState.Activated -> {
                        finishActivation()
                        return@launch
                    }
                }

                delay(timeMillis = 1000)
            }
        }
    }

    private suspend fun navigateToPinCode(activationOrderInfo: VisaActivationOrderInfo) {
        onDone.emit(Params.DoneEvent.NavigateTo(OnboardingVisaRoute.PinCode(activationOrderInfo)))
    }

    private suspend fun finishActivation() {
        val authTokens = visaAuthTokenStorage.get(params.scanResponse.card.cardId)
            ?: error("Auth tokens are not found. This should not happen.")

        val newTokens = runCatching {
            visaAuthRepository.refreshAccessTokens(authTokens.refreshToken)
        }.getOrElse {
            // TODO show alert
            return
        }

        val userWallet = createUserWallet(params.scanResponse, newTokens)
        userWalletsListManager.save(userWallet)
        visaAuthTokenStorage.remove(params.scanResponse.card.cardId)
        otpStorage.removeOTP(params.scanResponse.card.cardId)

        onDone.emit(Params.DoneEvent.Activated)
    }

    private suspend fun createUserWallet(scanResponse: ScanResponse, authTokens: VisaAuthTokens): UserWallet =
        withContext(dispatchers.io) {
            val newActivationStatus = VisaCardActivationStatus.Activated(visaAuthTokens = authTokens)

            requireNotNull(
                value = UserWalletBuilder(
                    scanResponse.copy(visaCardActivationStatus = newActivationStatus),
                    generateWalletNameUseCase,
                ).build(),
                lazyMessage = { "User wallet not created" },
            )
        }
}