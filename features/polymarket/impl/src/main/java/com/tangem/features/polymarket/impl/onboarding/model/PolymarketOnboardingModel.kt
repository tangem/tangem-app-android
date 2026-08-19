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
import com.tangem.features.polymarket.impl.common.PolymarketUrlBuilder
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
 * Model of the entry gate. The wallet is already settled by `PolymarketEntryModel`; this only decides where the
 * user lands.
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
        urlOpener.openUrl(PolymarketUrlBuilder.build(page = PolymarketUrlBuilder.Page.Terms))
    }
    private val onTangemTermsClick: () -> Unit = { urlOpener.openUrl(TANGEM_TERMS_URL) }

    val uiState: StateFlow<PolymarketOnboardingUM>
        field = MutableStateFlow<PolymarketOnboardingUM>(PolymarketOnboardingUM.Resolving)

    private val onboardingJob = JobHolder()

    init {
        resolveEntry(userWalletId)
    }

    fun onCloseClick() {
        router.pop()
    }

    private fun resolveEntry(walletId: UserWalletId) {
        modelScope.launch {
            resolvePolymarketEntryInteractor.withoutPrompting(walletId).fold(
                ifLeft = {
                    uiState.value = welcome(isStarting = false)
                    reportFailure()
                },
                ifRight = { entry -> renderEntry(entry) },
            )
        }
    }

    private fun renderEntry(entry: PolymarketEntry) {
        when (entry) {
            is PolymarketEntry.Onboard -> uiState.value = welcome(
                isStarting = false,
                startButtonText = startButtonText(status = entry.status),
            )
            PolymarketEntry.Undetermined -> uiState.value = welcome(isStarting = false)
            PolymarketEntry.Onboarded -> openFeed()
        }
    }

    private fun startOnboarding() {
        val current = uiState.value as? PolymarketOnboardingUM.Welcome ?: return
        if (current.isStarting) return

        uiState.value = current.copy(isStarting = true)

        modelScope.launch {
            val entry = resolvePolymarketEntryInteractor(userWalletId)
                .getOrElse {
                    stopStarting()
                    reportFailure()
                    return@launch
                }

            ensureActive()

            when (entry) {
                is PolymarketEntry.Onboard,
                PolymarketEntry.Undetermined,
                -> runOnboarding()
                PolymarketEntry.Onboarded -> openFeed()
            }
        }.saveIn(onboardingJob)
    }

    private fun showRegionRestrictions() {
        uiState.value = welcome(isStarting = false, isRegionRestrictionsShown = true)
    }

    private fun dismissRegionRestrictions() {
        val current = uiState.value as? PolymarketOnboardingUM.Welcome ?: return
        uiState.value = current.copy(isRegionRestrictionsShown = false)
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
            -> (uiState.value as? PolymarketOnboardingUM.Welcome)?.let { uiState.value = it.copy(isStarting = true) }
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
        val current = uiState.value as? PolymarketOnboardingUM.Welcome ?: return
        uiState.value = current.copy(isStarting = false)
    }

    private fun reportFailure() {
        messageSender.send(SnackbarMessage(resourceReference(R.string.common_something_went_wrong)))
    }

    private fun welcome(
        isStarting: Boolean,
        startButtonText: TextReference = resourceReference(R.string.prediction_onboarding_start_button),
        isRegionRestrictionsShown: Boolean = false,
    ) = PolymarketOnboardingUM.Welcome(
        isStarting = isStarting,
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

    private companion object {
        const val TANGEM_TERMS_URL = "https://tangem.com/tangem_tos.html"
    }
}