package com.tangem.features.polymarket.impl.model

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.sessionkey.operation.PolymarketSessionKeyOperation
import com.tangem.domain.polymarket.sessionkey.operation.PolymarketSessionKeyOperationOutcome
import com.tangem.domain.polymarket.sessionkey.operation.PolymarketSessionKeyOperationRequest
import com.tangem.domain.polymarket.sessionkey.operation.PolymarketSessionKeyOperationStage
import com.tangem.domain.polymarket.usecase.AcknowledgePolymarketSessionKeyOutcomeUseCase
import com.tangem.domain.polymarket.usecase.ObservePolymarketSessionKeyOperationUseCase
import com.tangem.domain.polymarket.usecase.ObservePolymarketSessionKeyOutcomeUseCase
import com.tangem.features.polymarket.api.PolymarketFeatureToggles
import com.tangem.features.polymarket.impl.fasttrading.model.PolymarketSessionKeyMessagesFactory
import com.tangem.features.polymarket.impl.navigation.PolymarketRoute
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@ModelScoped
internal class PolymarketModel @Inject constructor(
    featureToggles: PolymarketFeatureToggles,
    private val observeSessionKeyOutcome: ObservePolymarketSessionKeyOutcomeUseCase,
    private val observeSessionKeyOperation: ObservePolymarketSessionKeyOperationUseCase,
    private val acknowledgeSessionKeyOutcome: AcknowledgePolymarketSessionKeyOutcomeUseCase,
    private val messageSender: UiMessageSender,
    override val dispatchers: CoroutineDispatcherProvider,
) : Model() {

    /** Initial route of the feature stack — [PolymarketRoute.Entry] decides where the user actually lands. */
    val initialRoute: PolymarketRoute = PolymarketRoute.Entry

    private val activeWalletId = MutableStateFlow<UserWalletId?>(null)
    private val isVisible = MutableStateFlow(false)

    init {
        if (featureToggles.isPolymarketSessionKeysEnabled) {
            toastSessionKeyOutcomes()
            toastSubmittedRevocations()
        }
    }

    fun onActiveWalletChanged(userWalletId: UserWalletId?) {
        activeWalletId.value = userWalletId
    }

    fun onVisibilityChanged(isVisible: Boolean) {
        this.isVisible.value = isVisible
    }

    private fun toastSessionKeyOutcomes() {
        observeVisibleWallet { walletId ->
            observeSessionKeyOutcome(walletId).filterNotNull().map { outcome -> walletId to outcome }
        }
            .onEach { (walletId, outcome) -> toast(walletId, outcome) }
            .launchIn(modelScope)
    }

    private fun toastSubmittedRevocations() {
        observeVisibleWallet(observeSessionKeyOperation::invoke)
            .mapNotNull { operation -> operation?.takeIf { it.isSubmittedRevocation() }?.id }
            .distinctUntilChanged()
            .onEach { messageSender.send(PolymarketSessionKeyToastFactory.revokingToast()) }
            .launchIn(modelScope)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun <T> observeVisibleWallet(observe: (UserWalletId) -> Flow<T>): Flow<T> =
        combine(isVisible, activeWalletId) { isVisible, walletId -> walletId.takeIf { isVisible } }
            .flatMapLatest { walletId -> if (walletId == null) emptyFlow() else observe(walletId) }

    private fun PolymarketSessionKeyOperation.isSubmittedRevocation(): Boolean =
        request is PolymarketSessionKeyOperationRequest.Revoke && stage == PolymarketSessionKeyOperationStage.REVOKING

    private fun toast(userWalletId: UserWalletId, outcome: PolymarketSessionKeyOperationOutcome) {
        PolymarketSessionKeyMessagesFactory.create(outcome)?.let(messageSender::send)
        acknowledgeSessionKeyOutcome(userWalletId, outcome.id)
    }
}