package com.tangem.tap.domain.walletconnect2.domain.models

import com.tangem.tap.domain.walletconnect2.domain.WcRequest
import java.net.URI

sealed interface WalletConnectEvents {
    data class SessionProposal(
        val name: String,
        val description: String,
        val url: String,
        val icons: List<URI>,
        val requiredChainIds: List<String>,
        val optionalChainIds: List<String>,
    ) : WalletConnectEvents

    data class SessionApprovalError(val error: WalletConnectError) : WalletConnectEvents
    data class SessionApprovalSuccess(val topic: String, val accounts: List<Account>) : WalletConnectEvents
    data class SessionDeleted(val topic: String) : WalletConnectEvents

    data class SessionRequest(
        val request: WcRequest,
        val chainId: String?,
        val topic: String,
        val id: Long,
        val metaName: String,
        val metaUrl: String,
        val method: String,
    ) : WalletConnectEvents
}
