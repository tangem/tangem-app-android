package com.tangem.features.polymarket.impl.onboarding.model

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.polymarket.model.PolymarketAccessMode
import com.tangem.domain.polymarket.model.PolymarketEntry
import com.tangem.domain.polymarket.usecase.ResolvePolymarketEntryUseCase
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
 * permission would let a restricted user trade.
 *
 * A superseded resolution never reports its outcome: the use case turns cancellation into a failure instead of
 * propagating it, so a retried attempt would otherwise overwrite the fresh state with the stale error.
 */
@ModelScoped
internal class PolymarketOnboardingModel @Inject constructor(
    paramsContainer: ParamsContainer,
    private val router: Router,
    private val resolvePolymarketEntryUseCase: ResolvePolymarketEntryUseCase,
    override val dispatchers: CoroutineDispatcherProvider,
) : Model() {

    private val params = paramsContainer.require<PolymarketComponent.Params>()

    val uiState: StateFlow<PolymarketOnboardingUM>
        field = MutableStateFlow<PolymarketOnboardingUM>(PolymarketOnboardingUM.Loading)

    private val resolveJob = JobHolder()

    init {
        resolveEntry()
    }

    fun onRegionRestrictionsDismiss() {
        openFeed(accessMode = PolymarketAccessMode.READ_ONLY)
    }

    private fun resolveEntry() {
        modelScope.launch {
            uiState.value = PolymarketOnboardingUM.Loading

            val result = resolvePolymarketEntryUseCase(params.userWalletId)

            ensureActive()

            result.fold(
                ifLeft = { uiState.value = PolymarketOnboardingUM.Failed(onRetryClick = ::resolveEntry) },
                ifRight = { entry ->
                    when (entry) {
                        is PolymarketEntry.Onboard -> uiState.value = PolymarketOnboardingUM.Welcome
                        is PolymarketEntry.Onboarded -> openFeed(accessMode = entry.accessMode)
                        PolymarketEntry.RegionBlocked -> uiState.value = PolymarketOnboardingUM.RegionBlocked
                    }
                },
            )
        }.saveIn(resolveJob)
    }

    private fun openFeed(accessMode: PolymarketAccessMode) {
        router.replaceAll(PolymarketRoute.Main(accessMode = accessMode))
    }
}