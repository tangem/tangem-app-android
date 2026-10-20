package com.tangem.data.polymarket.signing

import com.tangem.common.core.TangemSdkError
import com.tangem.domain.polymarket.model.PolymarketSigningError
import com.tangem.domain.wallets.signing.DeriveAndSignError

internal fun Throwable.toPolymarketSigningError(): PolymarketSigningError = when (this) {
    is TangemSdkError.UserCancelled -> PolymarketSigningError.UserCancelled
    is TangemSdkError.WalletNotFound -> PolymarketSigningError.MissingWallet
    is TangemSdkError.ExceptionError -> PolymarketSigningError.Unknown
    is TangemSdkError -> PolymarketSigningError.CardError
    else -> PolymarketSigningError.Unknown
}

internal fun DeriveAndSignError.toPolymarketSigningError(): PolymarketSigningError = when (this) {
    DeriveAndSignError.UserCancelled -> PolymarketSigningError.UserCancelled
    DeriveAndSignError.WalletNotFound -> PolymarketSigningError.MissingWallet
    DeriveAndSignError.CardError -> PolymarketSigningError.CardError
    DeriveAndSignError.Unknown -> PolymarketSigningError.Unknown
}