package com.tangem.domain.polymarket.usecase

import com.tangem.domain.core.error.DataError
import com.tangem.domain.polymarket.model.PolymarketAuthError
import com.tangem.domain.polymarket.model.PolymarketDerivationError
import com.tangem.domain.polymarket.model.PolymarketOnboardingError
import com.tangem.domain.polymarket.model.PolymarketSigningError
import com.tangem.domain.polymarket.model.PolymarketWalletError

internal fun PolymarketDerivationError.toOnboardingError(): PolymarketOnboardingError =
    PolymarketOnboardingError.Derivation(cause = this)

internal fun PolymarketWalletError.toOnboardingError(): PolymarketOnboardingError = when (this) {
    PolymarketWalletError.Network -> PolymarketOnboardingError.Network
    else -> PolymarketOnboardingError.Wallet(cause = this)
}

internal fun DataError.toOnboardingError(): PolymarketOnboardingError = when (this) {
    is DataError.NetworkError -> PolymarketOnboardingError.Network
    else -> PolymarketOnboardingError.Unknown
}

internal fun PolymarketSigningError.toOnboardingError(): PolymarketOnboardingError =
    PolymarketOnboardingError.Signing(cause = this)

internal fun PolymarketAuthError.toOnboardingError(): PolymarketOnboardingError = when (this) {
    PolymarketAuthError.Network -> PolymarketOnboardingError.Network
    else -> PolymarketOnboardingError.Auth(cause = this)
}

internal fun PolymarketOnboardingError.isRetryable(): Boolean = when (this) {
    is PolymarketOnboardingError.AddressMismatch -> false
    is PolymarketOnboardingError.Wallet -> when (cause) {
        PolymarketWalletError.InvalidRequest,
        PolymarketWalletError.Unauthorized,
        -> false
        else -> true
    }
    is PolymarketOnboardingError.Signing -> when (cause) {
        PolymarketSigningError.NotDerived,
        PolymarketSigningError.MissingWallet,
        -> false
        else -> true
    }
    is PolymarketOnboardingError.Derivation -> when (cause) {
        PolymarketDerivationError.MissingWallet,
        PolymarketDerivationError.DerivationUnsupported,
        -> false
        else -> true
    }
    is PolymarketOnboardingError.Auth,
    PolymarketOnboardingError.DeploymentFailed,
    PolymarketOnboardingError.ApprovalsFailed,
    PolymarketOnboardingError.Network,
    PolymarketOnboardingError.Unknown,
    -> true
}