package com.tangem.features.polymarket.impl.model

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.sessionkey.operation.PolymarketSessionKeyOperationOutcome
import com.tangem.domain.polymarket.usecase.AcknowledgePolymarketSessionKeyOutcomeUseCase
import com.tangem.domain.polymarket.usecase.ObservePolymarketSessionKeyOutcomeUseCase
import com.tangem.features.polymarket.api.PolymarketFeatureToggles
import com.tangem.features.polymarket.impl.fasttrading.model.PolymarketSessionKeyToastFactory
import com.tangem.features.polymarket.impl.navigation.PolymarketRoute
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@ModelScoped
internal class PolymarketModel @Inject constructor(
    featureToggles: PolymarketFeatureToggles,
    private val observeSessionKeyOutcome: ObservePolymarketSessionKeyOutcomeUseCase,
    private val acknowledgeSessionKeyOutcome: AcknowledgePolymarketSessionKeyOutcomeUseCase,
    private val messageSender: UiMessageSender,
    override val dispatchers: CoroutineDispatcherProvider,
) : Model() {

    /** Initial route of the feature stack — [PolymarketRoute.Entry] decides where the user actually lands. */
    val initialRoute: PolymarketRoute = PolymarketRoute.Entry

    private val activeWalletId = MutableStateFlow<UserWalletId?>(null)

    init {
        if (featureToggles.isPolymarketSessionKeysEnabled) {
            toastSessionKeyOutcomes()
        }
    }

    fun onActiveWalletChanged(userWalletId: UserWalletId?) {
        activeWalletId.value = userWalletId
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun toastSessionKeyOutcomes() {
        activeWalletId
            .flatMapLatest { walletId ->
                if (walletId == null) {
                    emptyFlow()
                } else {
                    observeSessionKeyOutcome(walletId).filterNotNull().map { outcome -> walletId to outcome }
                }
            }
            .onEach { (walletId, outcome) -> toast(walletId, outcome) }
            .launchIn(modelScope)
    }

    private fun toast(userWalletId: UserWalletId, outcome: PolymarketSessionKeyOperationOutcome) {
        PolymarketSessionKeyToastFactory.create(outcome)?.let(messageSender::send)
        acknowledgeSessionKeyOutcome(userWalletId, outcome.id)
    }
}