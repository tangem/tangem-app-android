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