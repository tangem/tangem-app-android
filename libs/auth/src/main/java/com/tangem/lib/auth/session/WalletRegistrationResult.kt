package com.tangem.lib.auth.session

/** Successful outcome of [WalletRegistrar.register]: whether this call bound the wallet or it was bound before. */
enum class WalletRegistrationResult {
    Registered,
    AlreadyRegistered,
}