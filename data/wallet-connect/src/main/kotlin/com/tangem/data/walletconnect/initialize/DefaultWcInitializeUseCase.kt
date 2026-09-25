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

        // Sessions are persisted by the app itself, so this answers without waking the SDK.
        val hasSessions = withContext(dispatchers.io) { store.sessions.first().isNotEmpty() }

        if (hasSessions) wcSdk.ensureInitialized()
    }
}