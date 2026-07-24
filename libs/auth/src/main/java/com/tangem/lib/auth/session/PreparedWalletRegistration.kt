package com.tangem.lib.auth.session

import com.tangem.datasource.api.auth.models.request.WalletRegistrationRequest

/**
 * Opaque result of [WalletRegistrar.prepare] — the fully assembled, signed wallet-registration
 * request, ready to be sent by [WalletRegistrar.submit]. Callers hold it between the in-session
 * signing phase and the after-session network phase without inspecting its contents.
 */
class PreparedWalletRegistration internal constructor(
    internal val walletId: String,
    internal val request: WalletRegistrationRequest,
)