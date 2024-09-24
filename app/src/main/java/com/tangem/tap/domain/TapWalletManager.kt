package com.tangem.tap.domain

import com.tangem.blockchain.common.Token
import com.tangem.blockchain.common.Wallet
import com.tangem.core.analytics.Analytics
import com.tangem.core.analytics.models.Basic
import com.tangem.datasource.config.ConfigManager
import com.tangem.domain.common.extensions.withMainContext
import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.operations.attestation.Attestation
import com.tangem.tap.common.extensions.inject
import com.tangem.tap.common.extensions.setContext
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.features.onboarding.products.twins.redux.TwinCardsAction
import com.tangem.tap.proxy.redux.DaggerGraphState
import com.tangem.tap.store
import com.tangem.tap.tangemSdkManager
import com.tangem.utils.coroutines.AppCoroutineDispatcherProvider
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class TapWalletManager(
    private val dispatchers: CoroutineDispatcherProvider = AppCoroutineDispatcherProvider(),
) {

    private var loadUserWalletDataJob: Job? = null
        set(value) {
            field?.cancel()
            field = value
        }

    suspend fun onWalletSelected(userWallet: UserWallet, sendAnalyticsEvent: Boolean) {
        // If a previous job was running, it gets cancelled before the new one starts,
        // ensuring that only one job is active at any given time.
        loadUserWalletDataJob = CoroutineScope(dispatchers.io)
            .launch { loadUserWalletData(userWallet, sendAnalyticsEvent) }
            .also { it.join() }
    }

    private suspend fun loadUserWalletData(userWallet: UserWallet, sendAnalyticsEvent: Boolean) {
        Analytics.setContext(userWallet.scanResponse)
        if (sendAnalyticsEvent) {
            Analytics.send(Basic.WalletOpened())
        }
        val scanResponse = userWallet.scanResponse
        val card = scanResponse.card
        val attestationFailed = card.attestation.status == Attestation.Status.Failed

        tangemSdkManager.changeDisplayedCardIdNumbersCount(scanResponse)

        val featureToggles = store.inject(DaggerGraphState::feedbackManagerFeatureToggles)
        if (!featureToggles.isLocalLogsEnabled) {
            store.state.globalState.feedbackManager?.infoHolder?.setCardInfo(scanResponse)
        }

        updateConfigManager(scanResponse)
        withMainContext {
            // Order is important
            store.dispatch(TwinCardsAction.IfTwinsPrepareState(scanResponse))
            store.dispatch(GlobalAction.SaveScanResponse(scanResponse))
            store.dispatch(GlobalAction.SetIfCardVerifiedOnline(!attestationFailed))
        }
    }

    fun updateConfigManager(data: ScanResponse) {
        val configManager = store.state.globalState.configManager

        if (data.cardTypesResolver.isStart2Coin()) {
            configManager?.turnOff(ConfigManager.IS_TOP_UP_ENABLED)
        } else {
            configManager?.resetToDefault(ConfigManager.IS_TOP_UP_ENABLED)
        }
    }
}

fun Wallet.getFirstToken(): Token? = getTokens().toList().getOrNull(index = 0)
