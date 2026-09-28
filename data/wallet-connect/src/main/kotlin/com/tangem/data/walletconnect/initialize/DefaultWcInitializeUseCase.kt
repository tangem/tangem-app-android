package com.tangem.data.walletconnect.initialize

import com.tangem.data.walletconnect.sdk.WcSdk
import com.tangem.data.walletconnect.store.WalletConnectStore
import com.tangem.domain.walletconnect.featuretoggle.WalletConnectFeatureToggles
import com.tangem.domain.walletconnect.usecase.initialize.WcInitializeUseCase
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

internal class DefaultWcInitializeUseCase(
    private val wcSdk: WcSdk,
    private val store: WalletConnectStore,
    private val featureToggles: WalletConnectFeatureToggles,
    private val dispatchers: CoroutineDispatcherProvider,
) : WcInitializeUseCase {

    override suspend fun ensureInitialized() = wcSdk.ensureInitialized()

    override suspend fun initializeOnAppStart() {
        if (!featureToggles.isOnDemandInitEnabled) {
            wcSdk.ensureInitialized()
            return
        }

        // Both stores are persisted by the app itself, so this answers without waking the SDK. A pending
        // approval counts: the process can die between approving a session and its settlement, and that
        // settlement only arrives over a live SDK.
        val hasLiveWork = withContext(dispatchers.io) {
            store.sessions.first().isNotEmpty() || store.pendingApproval.first().isNotEmpty()
        }

        if (hasLiveWork) wcSdk.ensureInitialized()
    }
}