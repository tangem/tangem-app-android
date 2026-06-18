package com.tangem.feature.tokendetails.domain

/**
 * Card/wallet-level warnings that are shown on the main screen for a single-currency wallet and must also be
 * displayed in Token Details (redesign), since single-currency wallets now have a Token Details screen.
 *
 * Currency-level warnings are produced separately by [GetCurrencyWarningsUseCase].
 */
internal sealed interface WalletCardWarning {

    data object BackupError : WalletCardWarning

    data object DevCard : WalletCardWarning

    data object FailedCardValidation : WalletCardWarning

    data object TestnetCard : WalletCardWarning

    data class LowSignatures(val count: Int) : WalletCardWarning

    data object NumberOfSignedHashesIncorrect : WalletCardWarning

    data object DemoCard : WalletCardWarning
}