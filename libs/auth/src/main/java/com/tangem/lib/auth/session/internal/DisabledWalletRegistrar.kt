package com.tangem.lib.auth.session.internal

import arrow.core.Either
import arrow.core.left
import com.tangem.lib.auth.session.PreparedWalletRegistration
import com.tangem.lib.auth.session.WalletNonce
import com.tangem.lib.auth.session.WalletRegistrar
import com.tangem.lib.auth.session.WalletRegistrationError
import com.tangem.lib.auth.session.WalletSigner
import com.tangem.utils.annotations.RemoveWithToggle

@RemoveWithToggle("AND_15438_BACKEND_AUTHENTICATION_ENABLED")
internal object DisabledWalletRegistrar : WalletRegistrar {

    override suspend fun register(walletId: String, signer: WalletSigner): Either<WalletRegistrationError, Unit> =
        WalletRegistrationError.Disabled.left()

    override suspend fun isRegistered(walletId: String): Boolean = true

    override suspend fun hasActiveSession(): Boolean = false

    override suspend fun requestNonce(): Either<WalletRegistrationError, WalletNonce> =
        WalletRegistrationError.Disabled.left()

    override suspend fun prepare(
        walletId: String,
        nonce: WalletNonce,
        signer: WalletSigner,
        shouldForce: Boolean,
    ): Either<WalletRegistrationError, PreparedWalletRegistration?> = WalletRegistrationError.Disabled.left()

    override suspend fun forgetRegistration(walletId: String): Either<WalletRegistrationError, Unit> =
        WalletRegistrationError.Disabled.left()

    override suspend fun submit(prepared: PreparedWalletRegistration): Either<WalletRegistrationError, Unit> =
        WalletRegistrationError.Disabled.left()

    override suspend fun unregister(walletId: String): Either<WalletRegistrationError, Unit> =
        WalletRegistrationError.Disabled.left()

    override suspend fun clearRegistrations() = Unit
}