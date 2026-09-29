package com.tangem.features.tangempay.account.cashback

import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.domain.pay.usecase.GetCashbackDeactivationDismissedUseCase
import com.tangem.domain.pay.usecase.GetCashbackSummaryUseCase
import com.tangem.domain.pay.usecase.ObserveCashbackSummaryUseCase
import com.tangem.domain.pay.usecase.SetCashbackDeactivationDismissedUseCase
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents
import com.tangem.features.tangempay.account.TangemPayAccountDetailsInnerRoute
import com.tangem.features.tangempay.cashback.impl.model.TangemPayCashbackDateFormatter
import com.tangem.features.tangempay.common.userWalletId
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Suppress("LongParameterList")
internal class TangemPayCashbackController @AssistedInject constructor(
    @Assisted private val scope: CoroutineScope,
    @Assisted private val accountStatus: StateFlow<AccountStatus.Payment>,
    observeCashbackSummaryUseCase: ObserveCashbackSummaryUseCase,
    private val getCashbackSummaryUseCase: GetCashbackSummaryUseCase,
    getCashbackDeactivationDismissedUseCase: GetCashbackDeactivationDismissedUseCase,
    private val setCashbackDeactivationDismissedUseCase: SetCashbackDeactivationDismissedUseCase,
    private val analytics: AnalyticsEventHandler,
    private val router: Router,
) {

    private val userWalletId = accountStatus.value.userWalletId

    private val refreshState = MutableStateFlow(CashbackRefreshState())
    private val refreshJobHolder = JobHolder()
    private val isDeactivationGotItTapped = MutableStateFlow(false)

    private val isDeactivationDismissed = combine(
        flow { emit(getCashbackDeactivationDismissedUseCase(userWalletId)) },
        isDeactivationGotItTapped,
    ) { isDismissedEarlier, isGotItTapped -> isDismissedEarlier || isGotItTapped }

    private val content: Flow<CashbackContent> = combine(
        observeCashbackSummaryUseCase(userWalletId),
        refreshState,
        isDeactivationDismissed,
    ) { summary, lastRefresh, isDismissed ->
        resolveCashbackContent(summary, lastRefresh, isDismissed)
    }

    private val visibleContent: StateFlow<CashbackContent?> = combine(content, accountStatus) { resolved, status ->
        resolved.takeIf { status.value is PaymentAccountStatusValue.Loaded }
    }.stateIn(scope, SharingStarted.Eagerly, null)

    private val umFactory = CashbackUMFactory(
        dateFormatter = TangemPayCashbackDateFormatter(),
        onWidgetClick = ::onWidgetClick,
        onMenuItemClick = ::onMenuItemClick,
        onGotIt = ::onDeactivationGotIt,
        onReload = ::refresh,
    )

    val state: StateFlow<CashbackUM> = visibleContent
        .map { visible -> umFactory.create(visible ?: CashbackContent.Hidden) }
        .stateIn(scope, SharingStarted.Eagerly, CashbackUM())

    init {
        sendImpressions()
        cancelRefreshOnDeactivation()
    }

    fun refresh() {
        if (refreshJobHolder.isActive) return
        if (accountStatus.value.value is PaymentAccountStatusValue.Deactivated) return

        scope.launch {
            refreshState.update { it.copy(isInProgress = true) }
            try {
                val isFailed = getCashbackSummaryUseCase(userWalletId).isLeft()
                refreshState.value = CashbackRefreshState(isFailed = isFailed, isInProgress = false)
            } finally {
                refreshState.update { it.copy(isInProgress = false) }
            }
        }.saveIn(refreshJobHolder)
    }

    private fun sendImpressions() {
        visibleContent
            .filterNotNull()
            .map(CashbackContent::impression)
            .distinctUntilChanged()
            .filterNotNull()
            .onEach { impression -> analytics.send(impression.toAnalyticsEvent()) }
            .launchIn(scope)
    }

    private fun cancelRefreshOnDeactivation() {
        accountStatus
            .filter { it.value is PaymentAccountStatusValue.Deactivated }
            .onEach { refreshJobHolder.cancel() }
            .launchIn(scope)
    }

    private fun onWidgetClick() {
        analytics.send(TangemPayAnalyticsEvents.Cashback.BannerClicked())
        router.push(TangemPayAccountDetailsInnerRoute.Cashback)
    }

    private fun onMenuItemClick() {
        analytics.send(TangemPayAnalyticsEvents.Cashback.ButtonInSettingsClicked())
        router.push(TangemPayAccountDetailsInnerRoute.Cashback)
    }

    private fun onDeactivationGotIt() {
        analytics.send(TangemPayAnalyticsEvents.Cashback.DeactivationBannerGotItClicked())
        isDeactivationGotItTapped.value = true
        scope.launch { setCashbackDeactivationDismissedUseCase(userWalletId) }
    }

    @AssistedFactory
    interface Factory {
        fun create(scope: CoroutineScope, accountStatus: StateFlow<AccountStatus.Payment>): TangemPayCashbackController
    }
}