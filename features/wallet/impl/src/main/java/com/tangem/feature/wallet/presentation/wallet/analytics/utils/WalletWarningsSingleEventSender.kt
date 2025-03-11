package com.tangem.feature.wallet.presentation.wallet.analytics.utils

import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.domain.wallets.usecase.SeedPhraseNotificationUseCase
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletNotification
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletState
import com.tangem.feature.wallet.presentation.wallet.utils.ScreenLifecycleProvider
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject

@ViewModelScoped
internal class WalletWarningsSingleEventSender @Inject constructor(
    private val seedPhraseNotificationUseCase: SeedPhraseNotificationUseCase,
    private val screenLifecycleProvider: ScreenLifecycleProvider,
) {

    suspend fun send(
        userWalletId: UserWalletId,
        displayedUiState: WalletState?,
        newWarnings: List<WalletNotification>,
    ) {
        if (screenLifecycleProvider.isBackgroundState.value) return
        if (newWarnings.isEmpty()) return
        if (displayedUiState == null || displayedUiState.pullToRefreshConfig.isRefreshing) return

        val events = newWarnings.filter { it !in displayedUiState.warnings }

        events.forEach { event ->
            if (event is WalletNotification.Critical.SeedPhraseNotification) {
                seedPhraseNotificationUseCase.notified(userWalletId = userWalletId)
            }
        }
    }
}