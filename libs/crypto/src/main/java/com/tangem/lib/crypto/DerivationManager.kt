package com.tangem.lib.crypto

import com.tangem.lib.crypto.models.Currency

@Deprecated(message = "Use DerivePublicKeysUseCase instead")
interface DerivationManager {

    /**
     * Makes derivation for [Currency] if it is missing and adds token to wallet
     */
    suspend fun deriveAndAddTokens(currency: Currency): String
}