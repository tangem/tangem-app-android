package com.tangem.features.onboarding.v2.visa.impl.child.inprogress.model

import androidx.compose.runtime.Stable
import com.tangem.common.extensions.toHexString
import com.tangem.core.decompose.di.ComponentScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.domain.visa.model.VisaActivationRemoteState
import com.tangem.domain.visa.model.VisaCardId
import com.tangem.domain.visa.repository.VisaActivationRepository
import com.tangem.features.onboarding.v2.visa.impl.child.inprogress.OnboardingVisaInProgressComponent
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@Stable
@ComponentScoped
internal class OnboardingVisaInProgressModel @Inject constructor(
    paramsContainer: ParamsContainer,
    visaActivationRepositoryFactory: VisaActivationRepository.Factory,
    override val dispatchers: CoroutineDispatcherProvider,
) : Model() {

    private val params = paramsContainer.require<OnboardingVisaInProgressComponent.Config>()
    private val visaActivationRepository = visaActivationRepositoryFactory.create(
        VisaCardId(
            cardId = params.scanResponse.card.cardId,
            cardPublicKey = params.scanResponse.card.cardPublicKey.toHexString(),
        ),
    )
    val onDone = MutableSharedFlow<Unit>()

    init {
        modelScope.launch {
            while (true) {
                val result = runCatching {
                    visaActivationRepository.getActivationRemoteStateLongPoll()
                }.getOrNull()

                if (result == VisaActivationRemoteState.WaitingPinCode) {
                    onDone.emit(Unit)
                    break
                }

                delay(timeMillis = 1000)
            }
        }
    }
}