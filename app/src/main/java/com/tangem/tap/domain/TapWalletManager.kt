package com.tangem.tap.domain

import com.tangem.blockchain.common.Token
import com.tangem.blockchain.common.Wallet
import com.tangem.core.analytics.Analytics
import com.tangem.domain.common.extensions.withMainContext
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.tap.common.extensions.setContext
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.features.onboarding.products.twins.redux.TwinCardsAction
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

    suspend fun onWalletSelected(userWallet: UserWallet) {
        // If a previous job was running, it gets cancelled before the new one starts,
        // ensuring that only one job is active at any given time.
        loadUserWalletDataJob = CoroutineScope(dispatchers.io)
            .launch { loadUserWalletData(userWallet) }
            .also { it.join() }
    }

    private suspend fun loadUserWalletData(userWallet: UserWallet) {
        Analytics.setContext(userWallet.scanResponse)
        val scanResponse = userWallet.scanResponse

        tangemSdkManager.changeDisplayedCardIdNumbersCount(scanResponse)

        withMainContext {
            // Order is important
            store.dispatch(TwinCardsAction.IfTwinsPrepareState(scanResponse))
            store.dispatch(GlobalAction.SaveScanResponse(scanResponse))
        }
    }
}

fun Wallet.getFirstToken(): Token? = getTokens().toList().getOrNull(index = 0)