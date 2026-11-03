package com.tangem.feature.wallet.link

import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.wallets.registration.WalletLinkRequester
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class WalletLinkRequesterProxy @Inject constructor() : WalletLinkRequester {

    val componentRequester = MutableStateFlow<WalletLinkRequester?>(null)

    override suspend fun requestLink(userWallet: UserWallet): WalletLinkRequester.Result {
        return withTimeout(timeMillis = COMPONENT_WAIT_TIMEOUT_MS) {
            componentRequester.filterNotNull().first()
        }.requestLink(userWallet)
    }

    private companion object {
        const val COMPONENT_WAIT_TIMEOUT_MS = 1_000L
    }
}