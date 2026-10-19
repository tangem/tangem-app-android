package com.tangem.tap.domain.backup

import com.tangem.core.local.connection.NetworkConnectionManager
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.wallet.isLocked
import com.tangem.domain.wallets.usecase.SendPendingWalletCardsBackupUseCase
import com.tangem.features.onboarding.v2.OnboardingV2FeatureToggles
import com.tangem.utils.coroutines.AppCoroutineScope
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class WalletCardsBackupSender @Inject constructor(
    private val sendPendingWalletCardsBackupUseCase: SendPendingWalletCardsBackupUseCase,
    private val networkConnectionManager: NetworkConnectionManager,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val onboardingV2FeatureToggles: OnboardingV2FeatureToggles,
    private val appScope: AppCoroutineScope,
) {

    private val isStarted = AtomicBoolean(false)

    fun start() {
        if (onboardingV2FeatureToggles.isCardLinkedStatusUpdateEnabled.not()) return
        if (isStarted.getAndSet(true)) return

        appScope.launch {
            merge(cameOnline(), walletsUnlocked())
                .conflate()
                .collect { sendPending() }
        }
    }

    private fun cameOnline(): Flow<Unit> = networkConnectionManager.isOnlineFlow
        .filter { isOnline -> isOnline }
        .map { }

    private fun walletsUnlocked(): Flow<Unit> = userWalletsListRepository.userWallets
        .filterNotNull()
        .map { wallets -> wallets.filterNot(UserWallet::isLocked).mapTo(mutableSetOf(), UserWallet::walletId) }
        .scan(emptySet<UserWalletId>() to emptySet<UserWalletId>()) { (_, previous), current ->
            previous to current
        }
        .filter { (previous, current) -> (current - previous).isNotEmpty() }
        .map { }

    private suspend fun sendPending() {
        sendPendingWalletCardsBackupUseCase().onLeft { error ->
            TangemLogger.e("Pending cards backup reports deferred: $error")
        }
    }
}