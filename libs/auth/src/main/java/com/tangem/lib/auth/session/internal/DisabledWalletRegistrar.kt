package com.tangem.lib.auth.session.internal

import arrow.core.Either
import arrow.core.left
import com.tangem.lib.auth.session.PreparedWalletRegistration
import com.tangem.lib.auth.session.WalletRegistrar
import com.tangem.lib.auth.session.WalletRegistrationError
import com.tangem.lib.auth.session.WalletSigner
import com.tangem.utils.annotations.RemoveWithToggle

@RemoveWithToggle("AND_15438_BACKEND_AUTHENTICATION_ENABLED")
internal object DisabledWalletRegistrar : WalletRegistrar {

    override suspend fun register(walletId: String, signer: WalletSigner): Either<WalletRegistrationError, Unit> =
        WalletRegistrationError.Disabled.left()

    override suspend fun prepare(
        walletId: String,
        signer: WalletSigner,
    ): Either<WalletRegistrationError, PreparedWalletRegistration?> = WalletRegistrationError.Disabled.left()

    override suspend fun submit(prepared: PreparedWalletRegistration): Either<WalletRegistrationError, Unit> =
        WalletRegistrationError.Disabled.left()
}