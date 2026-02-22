package com.tangem.features.tangempay.model

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.repository.OnboardingRepository
import com.tangem.domain.pay.usecase.TangemPayMainScreenCustomerInfoUseCase
import com.tangem.features.tangempay.TangemPayMainBannerComponent
import com.tangem.features.tangempay.TangemPayMainBannerState
import com.tangem.features.tangempay.TangemPayMainEvent
import com.tangem.features.tangempay.TangemPayMainEventListener
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TANGEM_PAY_UPDATE_INTERVAL = 60_000L

@Suppress("UnusedPrivateProperty")
@Stable
@ModelScoped
internal class TangemPayMainBannerModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    paramsContainer: ParamsContainer,
    private val eventListener: TangemPayMainEventListener,
    private val tangemPayOnboardingRepository: OnboardingRepository,
    private val tangemPayMainScreenCustomerInfoUseCase: TangemPayMainScreenCustomerInfoUseCase,
) : Model() {

    private val params = paramsContainer.require<TangemPayMainBannerComponent.Params>()

    private val updateTangemPayJobHolder = JobHolder()

    private val states: Map<UserWalletId, TangemPayMainBannerState> = emptyMap()
    private val _uiState = MutableStateFlow(TangemPayMainBannerState.Empty)
    val uiState: StateFlow<TangemPayMainBannerState> = _uiState

    init {
        subscribeToEvents()
    }

    private fun subscribeToEvents() {
        eventListener.event.onEach { event ->
            when (event) {
                is TangemPayMainEvent.SelectWallet -> TODO("[REDACTED_JIRA]")
                is TangemPayMainEvent.Update -> onUpdate(event)
            }
        }.launchIn(modelScope)
    }

    @Suppress("UnusedParameter", "UnusedPrivateMember")
    private fun getState(userWalletId: UserWalletId): TangemPayMainBannerState {
        TODO("[REDACTED_JIRA]")
    }

    private suspend fun onUpdate(event: TangemPayMainEvent.Update) {
        if (event.isInBackground) {
            updateTangemPayJobHolder.cancel()
            return
        }

        val savedCustomerInfo =
            tangemPayOnboardingRepository.getSavedCustomerInfo(event.userWalletId)

        val isShouldLaunchPeriodicUpdate = savedCustomerInfo?.cardInfo == null &&
            tangemPayOnboardingRepository.isTangemPayInitialDataProduced(event.userWalletId)

        if (isShouldLaunchPeriodicUpdate) {
            updateTangemPayJobHolder.cancel()
            modelScope.launch {
                tangemPayMainScreenCustomerInfoUseCase.fetch(event.userWalletId)
                while (isActive) {
                    delay(TANGEM_PAY_UPDATE_INTERVAL)
                    tangemPayMainScreenCustomerInfoUseCase.fetch(event.userWalletId)
                }
            }.saveIn(updateTangemPayJobHolder)
        } else {
            // Don't refresh customer info periodically if the card was already issued, only update on swipe to refresh
            tangemPayMainScreenCustomerInfoUseCase.fetch(event.userWalletId)
        }
    }
}