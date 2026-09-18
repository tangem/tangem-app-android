package com.tangem.features.polymarket.impl.onboarding.model

import com.tangem.common.routing.AppRoute
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.res.R
import com.tangem.core.ui.coil.ImagePreloader
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.interactor.CheckNewPolymarketAccountAllowedInteractor
import com.tangem.domain.polymarket.interactor.ResolvePolymarketEntryInteractor
import com.tangem.domain.polymarket.interactor.RunPolymarketOnboardingInteractor
import com.tangem.domain.polymarket.model.PolymarketEntry
import com.tangem.domain.polymarket.model.PolymarketNewAccountAllowance
import com.tangem.domain.polymarket.model.PolymarketOnboardingProgress
import com.tangem.domain.polymarket.model.PolymarketWalletStatus
import com.tangem.domain.stories.GetStoryContentUseCase
import com.tangem.domain.stories.ShouldShowStoriesInteractor
import com.tangem.domain.stories.models.StoryContentIds
import com.tangem.features.polymarket.impl.common.PolymarketLegalUrls
import com.tangem.features.polymarket.impl.navigation.PolymarketRoute
import com.tangem.features.polymarket.impl.onboarding.ui.state.PolymarketOnboardingUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

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
    private val checkNewPolymarketAccountAllowed: CheckNewPolymarketAccountAllowedInteractor,
    private val resolvePolymarketEntryInteractor: ResolvePolymarketEntryInteractor,
    private val runPolymarketOnboardingInteractor: RunPolymarketOnboardingInteractor,
    private val shouldShowStoriesInteractor: ShouldShowStoriesInteractor,
    private val getStoryContentUseCase: GetStoryContentUseCase,
    private val imagePreloader: ImagePreloader,
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

    private var storyContentPrefetch: Deferred<Boolean>? = null

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
                actionButtonText = startButtonText(status = entry.status),
            )
            PolymarketEntry.Undetermined -> uiState.value = welcome(isInProgress = false)
            PolymarketEntry.Reconnect -> uiState.value = reconnect()
            PolymarketEntry.Onboarded -> {
                openFeed()
                return
            }
        }

        prefetchStory()
    }

    private fun startOnboarding() {
        val current = uiState.value as? PolymarketOnboardingUM.Action ?: return
        if (current.isInProgress) return

        uiState.value = current.copyInProgress(isInProgress = true)

        modelScope.launch {
            val allowance = checkNewPolymarketAccountAllowed(userWalletId)

            ensureActive()

            when (allowance) {
                PolymarketNewAccountAllowance.ALLOWED -> Unit
                PolymarketNewAccountAllowance.REGION_BLOCKED -> {
                    showRegionRestrictions()
                    return@launch
                }
                PolymarketNewAccountAllowance.UNDETERMINED -> {
                    stopStarting()
                    reportFailure()
                    return@launch
                }
            }

            runOnboarding()
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
            -> updateAction { it.copyInProgress(isInProgress = true) }
            PolymarketOnboardingProgress.Ready -> completeOnboarding()
            is PolymarketOnboardingProgress.Failed -> {
                stopStarting()
                reportFailure()
            }
        }
    }

    private fun stopStarting() {
        updateAction { it.copyInProgress(isInProgress = false) }
    }

    private fun updateAction(update: (PolymarketOnboardingUM.Action) -> PolymarketOnboardingUM.Action) {
        val current = uiState.value as? PolymarketOnboardingUM.Action ?: return
        uiState.value = update(current)
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
        actionButtonText: TextReference = resourceReference(R.string.prediction_onboarding_start_button),
        isRegionRestrictionsShown: Boolean = false,
    ) = PolymarketOnboardingUM.Welcome(
        isInProgress = isInProgress,
        actionButtonText = actionButtonText,
        onActionClick = ::startOnboarding,
        onPolymarketTermsClick = onPolymarketTermsClick,
        onTangemTermsClick = onTangemTermsClick,
        isRegionRestrictionsShown = isRegionRestrictionsShown,
        onRegionRestrictionsDismiss = ::dismissRegionRestrictions,
    )

    private fun reconnect() = PolymarketOnboardingUM.Reconnect(
        isInProgress = false,
        actionButtonText = resourceReference(R.string.prediction_onboarding_reconnect_button),
        onActionClick = ::startOnboarding,
        onPolymarketTermsClick = onPolymarketTermsClick,
        onTangemTermsClick = onTangemTermsClick,
    )

    private fun startButtonText(status: PolymarketWalletStatus): TextReference = when (status) {
        PolymarketWalletStatus.NOT_CREATED -> resourceReference(R.string.prediction_onboarding_start_button)
        else -> resourceReference(R.string.common_continue)
    }

    private fun openFeed() {
        router.replaceAll(PolymarketRoute.Main(userWalletId = userWalletId))
    }

    private fun completeOnboarding() {
        modelScope.launch {
            if (isStoryAvailable()) {
                router.push(AppRoute.Stories(storyId = STORY_ID, screenSource = STORY_SOURCE))
            }

            openFeed()
        }
    }

    private fun prefetchStory() {
        storyContentPrefetch = modelScope.async { fetchStoryContent() }
    }

    private suspend fun isStoryAvailable(): Boolean = withTimeoutOrNull(STORY_AVAILABILITY_TIMEOUT) {
        val isPrefetched = storyContentPrefetch?.await() == true

        if (!isPrefetched) fetchStoryContent()

        shouldShowStoriesInteractor.invokeSync(STORY_ID)
    } == true

    private suspend fun fetchStoryContent(): Boolean {
        val story = getStoryContentUseCase.invokeSync(id = STORY_ID, refresh = true).getOrNull() ?: return false

        story.getImageUrls().forEach(imagePreloader::preload)

        return true
    }

    private companion object {
        val STORY_ID = StoryContentIds.STORY_POLYMARKET_ONBOARDING.id
        val STORY_SOURCE = AnalyticsParam.ScreensSources.Onboarding.value
        val STORY_AVAILABILITY_TIMEOUT = 1.seconds
    }
}