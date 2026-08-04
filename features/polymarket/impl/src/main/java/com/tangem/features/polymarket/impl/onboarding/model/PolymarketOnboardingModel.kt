package com.tangem.features.polymarket.impl.onboarding.model

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.res.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.polymarket.model.PolymarketAccessMode
import com.tangem.domain.polymarket.model.PolymarketEntry
import com.tangem.domain.polymarket.model.PolymarketOnboardingProgress
import com.tangem.domain.polymarket.model.PolymarketWalletStatus
import com.tangem.domain.polymarket.usecase.ResolvePolymarketEntryUseCase
import com.tangem.domain.polymarket.usecase.RunPolymarketOnboardingUseCase
import com.tangem.features.polymarket.api.PolymarketComponent
import com.tangem.features.polymarket.impl.navigation.PolymarketRoute
import com.tangem.features.polymarket.impl.onboarding.ui.state.PolymarketOnboardingUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Model of the entry gate.
 *
 * Resolving the entry may open a card session, so it runs once per gate and is repeated only when the user
 * retries. A failed resolution never falls through to the feed: the region is unknown, and treating that as
 * permission would let a restricted user trade — it raises the error overlay instead, which offers only retry.
 *
 * A superseded resolution never reports its outcome: the use case turns cancellation into a failure instead of
 * propagating it, so a retried attempt would otherwise overwrite the fresh state with the stale error.
 *
 * A failed run returns the button to idle rather than raising an overlay: pressing Start again resumes the
 * run, which the use case supports. A failure the backend reports as not retryable will fail again — the
 * known gap the error design has to close.
 */
@ModelScoped
internal class PolymarketOnboardingModel @Inject constructor(
    paramsContainer: ParamsContainer,
    private val router: Router,
    private val urlOpener: UrlOpener,
    private val resolvePolymarketEntryUseCase: ResolvePolymarketEntryUseCase,
    private val runPolymarketOnboardingUseCase: RunPolymarketOnboardingUseCase,
    override val dispatchers: CoroutineDispatcherProvider,
) : Model() {

    private val params = paramsContainer.require<PolymarketComponent.Params>()

    private val onPolymarketTermsClick: () -> Unit = { urlOpener.openUrl(POLYMARKET_TERMS_URL) }
    private val onTangemTermsClick: () -> Unit = { urlOpener.openUrl(TANGEM_TERMS_URL) }

    val uiState: StateFlow<PolymarketOnboardingUM>
        field = MutableStateFlow(welcome(isStarting = true))

    private val resolveJob = JobHolder()
    private val onboardingJob = JobHolder()

    init {
        resolveEntry()
    }

    fun onCloseClick() {
        router.pop()
    }

    private fun onRegionRestrictionsDismiss() {
        openFeed(accessMode = PolymarketAccessMode.READ_ONLY)
    }

    private fun resolveEntry() {
        modelScope.launch {
            uiState.value = welcome(isStarting = true)

            val result = resolvePolymarketEntryUseCase(params.userWalletId)

            ensureActive()

            result.fold(
                ifLeft = {
                    uiState.value = welcome(
                        isStarting = false,
                        overlay = PolymarketOnboardingUM.Overlay.Error(onRetryClick = ::resolveEntry),
                    )
                },
                ifRight = { entry ->
                    when (entry) {
                        is PolymarketEntry.Onboard -> uiState.value = welcome(
                            isStarting = false,
                            startButtonText = startButtonText(status = entry.status),
                        )
                        is PolymarketEntry.Onboarded -> openFeed(accessMode = entry.accessMode)
                        PolymarketEntry.RegionBlocked -> uiState.value = welcome(
                            isStarting = false,
                            overlay = PolymarketOnboardingUM.Overlay.RegionRestrictions(
                                onDismiss = ::onRegionRestrictionsDismiss,
                            ),
                        )
                    }
                },
            )
        }.saveIn(resolveJob)
    }

    private fun startOnboarding() {
        if (uiState.value.isStarting) return

        uiState.value = uiState.value.copy(isStarting = true)

        modelScope.launch {
            runPolymarketOnboardingUseCase(params.userWalletId).collect { progress ->
                ensureActive()
                render(progress)
            }
        }.saveIn(onboardingJob)
    }

    private fun render(progress: PolymarketOnboardingProgress) {
        when (progress) {
            PolymarketOnboardingProgress.Deriving,
            PolymarketOnboardingProgress.AwaitingSignature,
            is PolymarketOnboardingProgress.Working,
            -> uiState.value = uiState.value.copy(isStarting = true)
            PolymarketOnboardingProgress.Ready -> openFeed(accessMode = PolymarketAccessMode.TRADING)
            is PolymarketOnboardingProgress.Failed -> uiState.value = uiState.value.copy(isStarting = false)
        }
    }

    private fun welcome(
        isStarting: Boolean,
        startButtonText: TextReference = resourceReference(R.string.prediction_onboarding_start_button),
        overlay: PolymarketOnboardingUM.Overlay? = null,
    ) = PolymarketOnboardingUM(
        isStarting = isStarting,
        startButtonText = startButtonText,
        onStartClick = ::startOnboarding,
        onPolymarketTermsClick = onPolymarketTermsClick,
        onTangemTermsClick = onTangemTermsClick,
        overlay = overlay,
    )

    private fun startButtonText(status: PolymarketWalletStatus): TextReference = when (status) {
        PolymarketWalletStatus.NOT_CREATED -> resourceReference(R.string.prediction_onboarding_start_button)
        else -> resourceReference(R.string.common_continue)
    }

    private fun openFeed(accessMode: PolymarketAccessMode) {
        router.replaceAll(PolymarketRoute.Main(accessMode = accessMode))
    }

    private companion object {
        const val POLYMARKET_TERMS_URL = "https://polymarket.com/tos"
        const val TANGEM_TERMS_URL = "https://tangem.com/tangem_tos.html"
    }
}