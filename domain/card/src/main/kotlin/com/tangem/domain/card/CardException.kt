package com.tangem.domain.card

import com.tangem.domain.card.model.CardWalletData

sealed class ScanCardException : Exception() {
    class Generic(override val cause: Throwable?) : ScanCardException()

    object EmptyChains : ScanCardException()

    class WalletNotCreated(val walletData: CardWalletData) : ScanCardException()

    class WrongCardId(val requestedCardId: String) : ScanCardException()

    sealed class SaltPayActivationError : ScanCardException() {
        object PutVisaCard : SaltPayActivationError()
    }
}
