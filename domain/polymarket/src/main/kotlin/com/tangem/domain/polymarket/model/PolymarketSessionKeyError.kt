package com.tangem.domain.polymarket.model

sealed interface PolymarketSessionKeyError : PolymarketError {

    data object KeyMissing : PolymarketSessionKeyError

    data object InvalidDigest : PolymarketSessionKeyError

    data class Failure(val cause: Throwable) : PolymarketSessionKeyError

    data object InvalidWalletId : PolymarketSessionKeyError

    data object MalformedRequest : PolymarketSessionKeyError

    data object InvalidDeadline : PolymarketSessionKeyError

    data object InvalidValidUntil : PolymarketSessionKeyError

    data object InvalidCall : PolymarketSessionKeyError

    data object InvalidSignature : PolymarketSessionKeyError

    data object Unauthorized : PolymarketSessionKeyError

    data object WalletNotOwned : PolymarketSessionKeyError

    data object SessionKeyNotFound : PolymarketSessionKeyError

    data object WalletNotReady : PolymarketSessionKeyError

    data object SessionKeyOwnedByAnotherWallet : PolymarketSessionKeyError

    data object SessionKeyBusy : PolymarketSessionKeyError

    data object DeviceHasSessionKey : PolymarketSessionKeyError

    data object InvalidReplacement : PolymarketSessionKeyError

    data object SessionKeyLimitReached : PolymarketSessionKeyError

    data object SessionKeyNotToggleable : PolymarketSessionKeyError

    data class RelayerRejected(val detail: String?) : PolymarketSessionKeyError

    data object NonceAlreadyUsed : PolymarketSessionKeyError

    data object UpstreamUnavailable : PolymarketSessionKeyError

    data object Network : PolymarketSessionKeyError

    data class Unknown(val httpCode: Int?, val reason: String?, val detail: String?) : PolymarketSessionKeyError
}