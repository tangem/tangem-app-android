package com.tangem.tap.domain

import com.tangem.core.analytics.utils.TrackingContextProxy
import com.tangem.domain.card.repository.CardSdkConfigRepository
import com.tangem.domain.common.wallets.UserWalletSelectedHandler
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.settings.repositories.SettingsRepository
import com.tangem.sdk.api.TangemSdkManager
import com.tangem.utils.coroutines.AppCoroutineScope
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveInAndJoin
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Default implementation of [UserWalletSelectedHandler].
 *
 * Runs three side effects on every selection: updates the analytics tracking context, updates the
 * Tangem SDK displayed card-id numbers count (cold wallets only), and recomputes the access code
 * request policy (cold wallets only). Hot wallets trigger only the tracking-context update.
 *
 * Invocations are serialised via [JobHolder]: if a new [invoke] arrives while the previous one is
 * still running, the previous load is cancelled and the new one replaces it. The method suspends
 * until the newly launched load completes.
 */
@Singleton
internal class DefaultUserWalletSelectedHandler @Inject constructor(
    private val trackingContextProxy: TrackingContextProxy,
    private val tangemSdkManager: TangemSdkManager,
    private val settingsRepository: SettingsRepository,
    private val cardSdkConfigRepository: CardSdkConfigRepository,
    private val appCoroutineScope: AppCoroutineScope,
) : UserWalletSelectedHandler {

    private val loadUserWalletDataJob: JobHolder = JobHolder()

    override suspend fun invoke(userWallet: UserWallet) {
        appCoroutineScope.launch { loadUserWalletData(userWallet) }
            .saveInAndJoin(loadUserWalletDataJob)
    }

    private suspend fun loadUserWalletData(userWallet: UserWallet) {
        trackingContextProxy.setContext(userWallet)

        if (userWallet is UserWallet.Cold) {
            val scanResponse = userWallet.scanResponse
            tangemSdkManager.changeDisplayedCardIdNumbersCount(scanResponse)
            updateAccessCodeRequestPolicy(scanResponse)
        }
    }

    private suspend fun updateAccessCodeRequestPolicy(scanResponse: ScanResponse) {
        val shouldSaveAccessCodes = settingsRepository.shouldSaveAccessCodes()

        cardSdkConfigRepository.setAccessCodeRequestPolicy(
            isBiometricsRequestPolicy = shouldSaveAccessCodes && scanResponse.card.isAccessCodeSet,
        )
    }
}