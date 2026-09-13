package com.tangem.features.polymarket.impl.onboarding.model

import arrow.core.getOrElse
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.res.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.interactor.ResolvePolymarketEntryInteractor
import com.tangem.domain.polymarket.interactor.RunPolymarketOnboardingInteractor
import com.tangem.domain.polymarket.model.PolymarketEntry
import com.tangem.domain.polymarket.model.PolymarketOnboardingError
import com.tangem.domain.polymarket.model.PolymarketOnboardingProgress
import com.tangem.domain.polymarket.model.PolymarketWalletStatus
import com.tangem.features.polymarket.impl.common.PolymarketLegalUrls
import com.tangem.features.polymarket.impl.navigation.PolymarketRoute
import com.tangem.features.polymarket.impl.onboarding.ui.state.PolymarketOnboardingUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Model of the entry gate. The wallet is already settled by `PolymarketEntryModel`.
 *
 * Opening the gate prompts for nothing: an undetermined wallet lands on Welcome with an idle button rather than
 * an unasked-for card prompt, and the full decision is taken when the user presses it.
 *
 * Nothing blocks the screen on a failure. Welcome is the gate's only content and its button is the only retry,
 * so a failed resolution and a failed run both leave it idle and report through a snackbar.
 */
@ModelScoped
@Suppress("LongParameterList")
internal class PolymarketOnboardingModel @Inject constructor(
    paramsContainer: ParamsContainer,
    private val router: Router,
    private val urlOpener: UrlOpener,
    private val messageSender: UiMessageSender,
    private val resolvePolymarketEntryInteractor: ResolvePolymarketEntryInteractor,
    private val runPolymarketOnboardingInteractor: RunPolymarketOnboardingInteractor,
    override val dispatchers: CoroutineDispatcherProvider,
) : Model() {

    private val userWalletId: UserWalletId = paramsContainer.require<PolymarketOnboardingParams>().userWalletId

    private val onPolymarketTermsClick: () -> Unit = {
        urlOpener.openUrl(PolymarketLegalUrls.polymarketTerms)
    }
    private val onTangemTermsClick: () -> Unit = { urlOpener.openUrl(PolymarketLegalUrls.TANGEM_TERMS) }

    val uiState: StateFlow<PolymarketOnboardingUM>
        field = MutableStateFlow<PolymarketOnboardingUM>(PolymarketOnboardingUM.Resolving)

    private val onboardingJob = JobHolder()

    init {
        resolveEntry()
    }

    fun onCloseClick() {
        router.pop()
    }

    private fun resolveEntry() {
        modelScope.launch {
            val result = resolvePolymarketEntryInteractor.withoutPrompting(userWalletId)

            // The use case reports cancellation as a failure rather than propagating it, so a gate the user has
            // already left would otherwise report itself — and the snackbar is global, landing on their new screen.
            ensureActive()

            result.fold(
                ifLeft = {
                    uiState.value = welcome(isInProgress = false)
                    reportFailure()
                },
                ifRight = { entry -> renderEntry(entry) },
            )
        }
    }

    private fun renderEntry(entry: PolymarketEntry) {
        when (entry) {
            is PolymarketEntry.Onboard -> uiState.value = welcome(
                isInProgress = false,
                startButtonText = startButtonText(status = entry.status),
            )
            PolymarketEntry.Undetermined -> uiState.value = welcome(isInProgress = false)
            PolymarketEntry.Onboarded -> openFeed()
        }
    }

    private fun startOnboarding() {
        val current = uiState.value as? PolymarketOnboardingUM.Welcome ?: return
        if (current.isInProgress) return

        uiState.value = current.copy(isInProgress = true)

        modelScope.launch {
            val result = resolvePolymarketEntryInteractor(userWalletId)

            ensureActive()

            val entry = result.getOrElse {
                stopStarting()
                reportFailure()
                return@launch
            }

            when (entry) {
                is PolymarketEntry.Onboard,
                PolymarketEntry.Undetermined,
                -> runOnboarding()
                PolymarketEntry.Onboarded -> openFeed()
            }
        }.saveIn(onboardingJob)
    }

    private fun showRegionRestrictions() {
        updateWelcome { it.copy(isInProgress = false, isRegionRestrictionsShown = true) }
    }

    private fun dismissRegionRestrictions() {
        updateWelcome { it.copy(isRegionRestrictionsShown = false) }
    }

    private suspend fun CoroutineScope.runOnboarding() {
        runPolymarketOnboardingInteractor(userWalletId).collect { progress ->
            ensureActive()
            render(progress)
        }
    }

    private fun render(progress: PolymarketOnboardingProgress) {
        when (progress) {
            PolymarketOnboardingProgress.Deriving,
            PolymarketOnboardingProgress.AwaitingSignature,
            is PolymarketOnboardingProgress.Working,
            -> updateWelcome { it.copy(isInProgress = true) }
            PolymarketOnboardingProgress.Ready -> openFeed()
            is PolymarketOnboardingProgress.Failed ->
                if (progress.error == PolymarketOnboardingError.RegionBlocked) {
                    showRegionRestrictions()
                } else {
                    stopStarting()
                    reportFailure()
                }
        }
    }

    private fun stopStarting() {
        updateWelcome { it.copy(isInProgress = false) }
    }

    /** Leaves the state alone unless the gate is showing Welcome — the only state these updates apply to. */
    private fun updateWelcome(update: (PolymarketOnboardingUM.Welcome) -> PolymarketOnboardingUM.Welcome) {
        val current = uiState.value as? PolymarketOnboardingUM.Welcome ?: return
        uiState.value = update(current)
    }

    private fun reportFailure() {
        messageSender.send(SnackbarMessage(resourceReference(R.string.common_something_went_wrong)))
    }

    private fun welcome(
        isInProgress: Boolean,
        startButtonText: TextReference = resourceReference(R.string.prediction_onboarding_start_button),
        isRegionRestrictionsShown: Boolean = false,
    ) = PolymarketOnboardingUM.Welcome(
        isInProgress = isInProgress,
        startButtonText = startButtonText,
        onStartClick = ::startOnboarding,
        onPolymarketTermsClick = onPolymarketTermsClick,
        onTangemTermsClick = onTangemTermsClick,
        isRegionRestrictionsShown = isRegionRestrictionsShown,
        onRegionRestrictionsDismiss = ::dismissRegionRestrictions,
    )

    private fun startButtonText(status: PolymarketWalletStatus): TextReference = when (status) {
        PolymarketWalletStatus.NOT_CREATED -> resourceReference(R.string.prediction_onboarding_start_button)
        else -> resourceReference(R.string.common_continue)
    }

    private fun openFeed() {
        router.replaceAll(PolymarketRoute.Main(userWalletId = userWalletId))
    }
}