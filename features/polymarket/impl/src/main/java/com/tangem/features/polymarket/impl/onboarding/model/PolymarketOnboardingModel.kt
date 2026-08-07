package com.tangem.features.polymarket.impl.onboarding.model

import arrow.core.getOrElse
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.res.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.model.PolymarketAccessMode
import com.tangem.domain.polymarket.model.PolymarketEntry
import com.tangem.domain.polymarket.model.PolymarketOnboardingProgress
import com.tangem.domain.polymarket.model.PolymarketWalletStatus
import com.tangem.domain.polymarket.interactor.ResolvePolymarketEntryInteractor
import com.tangem.domain.polymarket.usecase.RunPolymarketOnboardingUseCase
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
 * Model of the entry gate.
 *
 * The wallet is settled before this screen is reached — `PolymarketEntryModel` resolves it, choosing among
 * eligible wallets when the caller left that choice to the user. This model only resolves the entry decision
 * for that already-fixed wallet.
 *
 * Opening the gate prompts the user for nothing. It resolves only as far as it can without a card session or a
 * wallet unlock, so a wallet that has not derived the Polymarket key here lands on the Welcome screen with an
 * idle button instead of an unasked-for card prompt. The full decision — including that derivation — is taken
 * when the user presses the button, and it goes through the same use case, so the two can never disagree.
 *
 * That decision is deliberately deferred, not guessed: an undetermined wallet is never reported as having no
 * deposit wallet. A user returning after a reinstall or on a second device presses the button once and is then
 * taken to their existing wallet rather than being told it does not exist.
 *
 * A failed resolution never falls through to the feed: the region is unknown, and treating that as permission
 * would let a restricted user trade. Which failure surface applies depends on who asked — a failure while the
 * gate opens raises the error overlay, since there is nothing else on screen to retry from, while a failure
 * after the user pressed the button returns that button to idle, because the button is itself the retry.
 *
 * A superseded resolution never reports its outcome: the use case turns cancellation into a failure instead of
 * propagating it, so a retried attempt would otherwise overwrite the fresh state with the stale error.
 */
@ModelScoped
internal class PolymarketOnboardingModel @Inject constructor(
    paramsContainer: ParamsContainer,
    private val router: Router,
    private val urlOpener: UrlOpener,
    private val resolvePolymarketEntryInteractor: ResolvePolymarketEntryInteractor,
    private val runPolymarketOnboardingUseCase: RunPolymarketOnboardingUseCase,
    override val dispatchers: CoroutineDispatcherProvider,
) : Model() {

    private val userWalletId: UserWalletId = paramsContainer.require<PolymarketOnboardingParams>().userWalletId

    private val onPolymarketTermsClick: () -> Unit = { urlOpener.openUrl(POLYMARKET_TERMS_URL) }
    private val onTangemTermsClick: () -> Unit = { urlOpener.openUrl(TANGEM_TERMS_URL) }

    val uiState: StateFlow<PolymarketOnboardingUM>
        field = MutableStateFlow(welcome(isStarting = true))

    private val resolveJob = JobHolder()
    private val onboardingJob = JobHolder()

    init {
        resolveEntry(userWalletId)
    }

    fun onCloseClick() {
        router.pop()
    }

    private fun onRegionRestrictionsDismiss() {
        openFeed(accessMode = PolymarketAccessMode.READ_ONLY)
    }

    private fun resolveEntry(walletId: UserWalletId) {
        modelScope.launch {
            uiState.value = welcome(isStarting = true)

            val result = resolvePolymarketEntryInteractor.withoutPrompting(walletId)

            ensureActive()

            result.fold(
                ifLeft = {
                    uiState.value = welcome(
                        isStarting = false,
                        overlay = PolymarketOnboardingUM.Overlay.Error(onRetryClick = { resolveEntry(walletId) }),
                    )
                },
                ifRight = { entry -> renderEntry(entry) },
            )
        }.saveIn(resolveJob)
    }

    private fun renderEntry(entry: PolymarketEntry) {
        when (entry) {
            is PolymarketEntry.Onboard -> uiState.value = welcome(
                isStarting = false,
                startButtonText = startButtonText(status = entry.status),
            )
            PolymarketEntry.Undetermined -> uiState.value = welcome(isStarting = false)
            is PolymarketEntry.Onboarded -> openFeed(accessMode = entry.accessMode)
            PolymarketEntry.RegionBlocked -> uiState.value = welcome(
                isStarting = false,
                overlay = PolymarketOnboardingUM.Overlay.RegionRestrictions(
                    onDismiss = ::onRegionRestrictionsDismiss,
                ),
            )
        }
    }

    private fun startOnboarding() {
        if (uiState.value.isStarting) return

        uiState.value = uiState.value.copy(isStarting = true)

        modelScope.launch {
            val entry = resolvePolymarketEntryInteractor(userWalletId)
                .getOrElse {
                    uiState.value = uiState.value.copy(isStarting = false)
                    return@launch
                }

            ensureActive()

            when (entry) {
                // The gate could not decide, or decided there is onboarding left to do — either way the run is
                // next, and it re-reads the status itself before signing anything.
                is PolymarketEntry.Onboard,
                PolymarketEntry.Undetermined,
                -> runOnboarding()
                is PolymarketEntry.Onboarded -> openFeed(accessMode = entry.accessMode)
                PolymarketEntry.RegionBlocked -> uiState.value = welcome(
                    isStarting = false,
                    overlay = PolymarketOnboardingUM.Overlay.RegionRestrictions(
                        onDismiss = ::onRegionRestrictionsDismiss,
                    ),
                )
            }
        }.saveIn(onboardingJob)
    }

    private suspend fun CoroutineScope.runOnboarding() {
        runPolymarketOnboardingUseCase(userWalletId).collect { progress ->
            ensureActive()
            render(progress)
        }
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
        router.replaceAll(PolymarketRoute.Main(accessMode = accessMode, userWalletId = userWalletId))
    }

    private companion object {
        const val POLYMARKET_TERMS_URL = "https://polymarket.com/tos"
        const val TANGEM_TERMS_URL = "https://tangem.com/tangem_tos.html"
    }
}